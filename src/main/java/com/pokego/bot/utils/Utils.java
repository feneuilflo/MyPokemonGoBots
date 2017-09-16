package com.pokego.bot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.pokego.bot.Constants;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.inventory.PokeBank;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.Encounter;
import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.map.pokemon.ThrowProperties;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Evolutions;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.settings.PokeballSelector;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.MapUtil;
import com.pokegoapi.util.PokeDictionary;

import POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;
import POGOProtos.Networking.Responses.NicknamePokemonResponseOuterClass.NicknamePokemonResponse;
import POGOProtos.Networking.Responses.UpgradePokemonResponseOuterClass.UpgradePokemonResponse;
import POGOProtos.Networking.Responses.UseItemEncounterResponseOuterClass.UseItemEncounterResponse;
import POGOProtos.Networking.Responses.UseItemPotionResponseOuterClass.UseItemPotionResponse;
import POGOProtos.Networking.Responses.UseItemReviveResponseOuterClass.UseItemReviveResponse;
import rx.Observable;

public final class Utils {

	private static final double SPEED_KMH = 12.0;

	private static final int NB_MAX_POKEMON_EXP = 300;
	private static final int NB_MAX_POKEMON = 400;
	private static final AtomicInteger cntCatchedPokemon = new AtomicInteger();

	private Utils() {
	}

	public static void moveToPoint(PokemonGo go, Point destination) {
		Path path = new Path(go.getPoint(), destination, SPEED_KMH);
		path.start(go);
		Observable.interval(2, TimeUnit.SECONDS) //
				.takeUntil(l -> path.isComplete()) //
				.toBlocking() //
				.subscribe(l -> {
					// Calculate the desired intermediate point for the current time
					Point point = path.calculateIntermediate(go);
					// Set the API location to that point
					go.setLatitude(point.getLatitude());
					go.setLongitude(point.getLongitude());
					System.out.println("Time left: " + (int) (path.getTimeLeft(go) / 1000) + " seconds.");
				});
	}

	public static void moveToPokestop(PokemonGo go, Pokestop destinationPokestop) throws RequestFailedException {
		if (!destinationPokestop.inRange()) {
			String name = destinationPokestop.getName();
			trace(String.format("Traveling to %s at %.1fKMPH!", name, SPEED_KMH));
			moveToPoint(go, new Point(destinationPokestop.getLatitude(), destinationPokestop.getLongitude()));
			System.out.println(String.format("Reached %s", name));
		}
	}

	public static List<Pokestop> lookForPokestop(PokemonGo api) throws RequestFailedException {
		trace("##### Recherche de pokestops à proximité #####");

		Set<Pokestop> pokestops = api.getMap().getMapObjects().getPokestops();
		System.out.println("Found " + pokestops.size() + " pokestops in the current area.");

		List<Pokestop> inRangePokestops = pokestops.stream() //
				.filter(Pokestop::inRange) //
				.collect(Collectors.toList());
		System.out.println("Found " + inRangePokestops.size() + " in range pokestops in the current area.");

		return inRangePokestops;
	}

	public static void lootPokestop(PokemonGo go, Pokestop pokestop) throws RequestFailedException {
		if (pokestop.canLoot() && pokestop.inRange()) {
			System.out.println("Looting pokestop " + pokestop.getName() + "...");
			PokestopLootResult result = pokestop.loot();
			System.out.println("Pokestop loot returned result: " + result.getResult());

			if (go.getInventories().getItemBag().getItemsCount() >= go.getInventories().getItemBag().getMaxStorage()) {
				// full inventory
				Utils.freeInventory(go);
			}
		}
	}

	public static void catchPkmInArea(PokemonGo go)
			throws RequestFailedException, InterruptedException, NoSuchItemException {
		// stop catching pokemon if too many pkm captured
		if (cntCatchedPokemon.get() > NB_MAX_POKEMON) {
			return;
		}

		displayNearbyPokemons(go);

		Set<CatchablePokemon> catchablePokemon = go.getMap().getMapObjects().getPokemon();
		if (catchablePokemon.isEmpty()) {
			return;
		}

		trace("##### Capture de pokemons à proximité #####");
		ItemBag bag = go.getInventories().getItemBag();
		Random random = new Random();
		List<Long> catchedPokemonIds = new ArrayList<>();
		for (CatchablePokemon cp : catchablePokemon) {
			if (cp.hasEncountered())
				continue;

			// stop catching common pkm when too many pkm captured
			if (cntCatchedPokemon.get() > NB_MAX_POKEMON_EXP //
					&& Constants.POKEMON_ID_EXP.contains(cp.getPokemonId())) {
				continue;
			}

			// Encounter this pokemon
			Encounter encounter = cp.encounter();

			// If the encounter was successful, attempt to catch this pokemon
			if (encounter.isSuccessful()) {
				System.out.println("Encountered: " + cp.getPokemonId());

				List<Pokeball> usablePokeballs = bag.getUsablePokeballs();

				if (usablePokeballs.size() > 0) {
					// Select pokeball with smart selector to print what pokeball is used
					double probability = encounter.getCaptureProbability();
					Pokeball pokeball = PokeballSelector.SMART.select(usablePokeballs, probability);
					System.out.println("Attempting to catch: " + cp.getPokemonId() + " with " + pokeball + " ("
							+ probability + ")");

					// Throw pokeballs until capture or flee
					while (encounter.isActive()) {
						// Wait between Pokeball throws
						Thread.sleep(500 + random.nextInt(1000));

						// use pinap berry on great pkm if proba high enough
						int pinapberryCount = bag.getItem(ItemId.ITEM_PINAP_BERRY).getCount();
						if (encounter.getActiveItem() == null && pinapberryCount > 0 && probability > 0.3 //
								&& !Constants.POKEMON_ID_EXP.contains(cp.getPokemonId())) {
							System.out.println("using a pinap berry...");
							UseItemEncounterResponse.Status s = encounter.useItem(ItemId.ITEM_PINAP_BERRY);
							if (s != UseItemEncounterResponse.Status.SUCCESS) {
								System.out.println("failed to use item: " + s);
							}
						}

						// If no item is active, use a razzberry
						int razzberryCount = bag.getItem(ItemId.ITEM_RAZZ_BERRY).getCount();
						if (encounter.getActiveItem() == null && razzberryCount > 0 && probability < 0.5) {
							System.out.println("using a razz berry...");
							UseItemEncounterResponse.Status s = encounter.useItem(ItemId.ITEM_RAZZ_BERRY);
							if (s != UseItemEncounterResponse.Status.SUCCESS) {
								System.out.println("failed to use item: " + s);
							}
						}

						// use nana bery if no item active
						int nanabberryCount = bag.getItem(ItemId.ITEM_NANAB_BERRY).getCount();
						if (encounter.getActiveItem() == null && nanabberryCount > 0) {
							System.out.println("using a nanab berry...");
							UseItemEncounterResponse.Status s = encounter.useItem(ItemId.ITEM_NANAB_BERRY);
							if (s != UseItemEncounterResponse.Status.SUCCESS) {
								System.out.println("failed to use item: " + s);
							}
						}

						// Throw pokeball with random properties
						encounter.throwPokeball(PokeballSelector.SMART, ThrowProperties.random());

						if (encounter.getStatus() == CatchStatus.CATCH_SUCCESS) {
							System.out.println("Catched !");
							catchedPokemonIds.add(encounter.getCapturedPokemon());
						} else {
							System.out.println("failed: " + encounter.getStatus());
						}
					}
				} else {
					System.out.println("Skipping Pokemon, we have no Pokeballs!");
					break;
				}

				// Wait for animation before catching next pokemon
				Thread.sleep(3000 + random.nextInt(1000));
			} else {
				System.out.println("Failed to encounter pokemon: " + encounter.getEncounterResult());
			}
		}

		// display catched pokemons
		// Print pokemon stats
		if (!catchedPokemonIds.isEmpty()) {
			go.getInventories().updateInventories(true);
			PokeBank pokebank = go.getInventories().getPokebank();
			for (long id : catchedPokemonIds) {
				Pokemon pokemon = pokebank.getPokemonById(id);
				if (pokemon != null) {
					onNewPokemon(go, pokemon);
				}
			}
		}

		// transfer pokemon if near max capacity
		if (go.getInventories().getPokebank().getPokemons().size() > go.getInventories().getPokebank().getMaxStorage()
				* 0.8) {
			releasePokemons(go);
		}
	}

	public static void onNewPokemon(PokemonGo go, Pokemon pkm) {
		cntCatchedPokemon.incrementAndGet();

		double iv = pkm.getIvInPercentage();
		int number = pkm.getPokemonId().getNumber();
		String name = PokeDictionary.getDisplayName(number, Locale.getDefault());
		System.out.println("====" + name + "====");
		System.out.println("CP: " + pkm.getCp());
		System.out.println("IV: " + iv + "%");
		System.out.println("Height: " + pkm.getHeightM() + "m");
		System.out.println("Weight: " + pkm.getWeightKg() + "kg");
		System.out.println("Move 1: " + pkm.getMove1());
		System.out.println("Move 2: " + pkm.getMove2());

		// Rename the pokemon to <Name> IV%
		try {
			String newName = String.format("%s%d", name, (int) iv);
			System.out.println("Rename pokemon to: " + newName);
			NicknamePokemonResponse.Result r = pkm.renamePokemon(newName);
			System.out.println("Rename pokemon result: " + r);
		} catch (RequestFailedException e1) {
			System.out.println("failed to rename pokemon: " + e1.getMessage());
		}
		// Set pokemon with IV above 90% as favorite
		if (iv > 90) {
			try {
				System.out.println("IV > 90%  ---> favorite...");
				pkm.setFavoritePokemon(true);
				System.out.println("IV > 90%  ---> favorite...OK");
			} catch (RequestFailedException e) {
				System.out.println("failed to set pokemon to favorite: " + e.getMessage());
			}
		}

		// evolve pkm if tagged as xp provider
		if (pkm.canEvolve() //
				&& Constants.POKEMON_ID_EXP.contains(pkm.getPokemonId())) {
			System.out.println("Evolving pokemon... ");
			try {
				EvolutionResult r = pkm.evolve();
				System.out.println("Evolve pokemon result: " + r.getResult());
			} catch (RequestFailedException e) {
				System.out.println("failed to evolve pokemon: " + e.getMessage());
			}
		}
	}

	public static void displayNearbyPokemons(PokemonGo go) {
		trace("##### Pokémons à proximité #####");
		Set<CatchablePokemon> catchablePokemon = go.getMap().getMapObjects().getPokemon();
		catchablePokemon.forEach(pok -> System.out
				.println(PokeDictionary.getDisplayName((int) pok.getPokemonId().getNumber(), Locale.getDefault())));
	}

	public static void displayNearbyPokestops(PokemonGo go) {
		trace("##### Pokéstop à proximité #####");
		go.getMap().getMapObjects().getPokestops() //
				.forEach(pokestop -> {
					try {
						System.out.println(String.format("%s (%.1f m)", pokestop.getName(), pokestop.getDistance()));
					} catch (RequestFailedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
	}

	public static void displayNearbyGyms(PokemonGo go) {
		trace("##### Arènes à proximité #####");
		go.getMap().getMapObjects().getGyms() //
				.forEach(gym -> {
					try {
						System.out.println(String.format("%s (%.1f m)", gym.getName(), gym.getDistance()));
					} catch (RequestFailedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
	}

	public static void releasePokemons(PokemonGo go) throws RequestFailedException {
		trace("##### Transfert des pokemons #####");
		PokeBank pokebank = go.getInventories().getPokebank();
		List<Pokemon> pokemons = pokebank.getPokemons();

		if (pokemons.isEmpty())
			return;

		// look for pkm to be transfered
		List<Pokemon> transferPokemons = Observable.from(pokemons) //
				.groupBy(Pokemon::getPokemonId) //
				.flatMap(obs -> obs.toList() //
						.map(list -> list.stream() //
								.sorted(Comparator.comparing(Pokemon::getCp).reversed()) //
								// favorites cannot be transfered
								.filter(pkm -> !pkm.isFavorite())
								// don't tranfer pkms in gyms
								.filter(pkm -> !pkm.isDeployed())
								// don't transfer buddy
								.filter(pkm -> !pkm.isBuddy())
								// A tester
								.filter(pkm -> !pkm.isEgg())
								// don't transfer pkm with CP > 2000
								.filter(pkm -> pkm.getCp() < 2000)
								// don't transfer potentially strong pkms
								.filter(pkm -> pkm.getIvInPercentage() < 90.0)
								// IV less restrictive for tagged strong families
								.filter(pkm -> pkm.getIvInPercentage() < 70 //
										|| !Constants.POKEMON_FAMILY_TO_KEEP.contains(pkm.getPokemonFamily()))))
				.reduce(Stream::concat) //
				.map(stream -> stream.collect(Collectors.toList())) //
				.toBlocking() //
				.first();

		System.out.println("Releasing " + transferPokemons.size() + " pokemons...");
		if (!transferPokemons.isEmpty()) {
			Pokemon[] transferArray = transferPokemons.toArray(new Pokemon[transferPokemons.size()]);

			@SuppressWarnings("unused")
			Map<PokemonFamilyId, Integer> responses = pokebank.releasePokemon(transferArray);
			System.out.println("Releasing " + transferPokemons.size() + " pokemons...OK");
		}
	}

	public static void freeInventory(PokemonGo go) throws RequestFailedException {
		trace("##### Nettoyage de l'inventaire #####");
		ItemBag bag = go.getInventories().getItemBag();

		// manage potions
		List<ItemId> potionIds = Arrays.asList(ItemId.ITEM_MAX_POTION, //
				ItemId.ITEM_HYPER_POTION, //
				ItemId.ITEM_SUPER_POTION, //
				ItemId.ITEM_POTION);
		int nbMaxPotions = (int) (bag.getMaxStorage() * 0.2);
		removeItems(bag, potionIds, nbMaxPotions);

		// manage revives
		List<ItemId> reviveIds = Arrays.asList(ItemId.ITEM_MAX_REVIVE, //
				ItemId.ITEM_REVIVE);
		int nbMaxRevives = (int) (bag.getMaxStorage() * 0.2);
		removeItems(bag, reviveIds, nbMaxRevives);

		// manage balls
		List<ItemId> ballIds = Arrays.asList(ItemId.ITEM_MASTER_BALL, //
				ItemId.ITEM_ULTRA_BALL, //
				ItemId.ITEM_GREAT_BALL, //
				ItemId.ITEM_POKE_BALL);
		int nbMaxBalls = (int) (bag.getMaxStorage() * 0.4);
		removeItems(bag, ballIds, nbMaxBalls);

		// manage berries
		removeItem(bag, ItemId.ITEM_NANAB_BERRY, 20); // automatically used by catch algorithm
		removeItem(bag, ItemId.ITEM_PINAP_BERRY, 20); // automatically used by catch algorithm
		removeItem(bag, ItemId.ITEM_GOLDEN_RAZZ_BERRY, 50);
		removeItem(bag, ItemId.ITEM_RAZZ_BERRY, 20); // automatically used by catch algorithm

		// manage evolution items
		removeItem(bag, ItemId.ITEM_DRAGON_SCALE, 3);
		removeItem(bag, ItemId.ITEM_KINGS_ROCK, 3);
		removeItem(bag, ItemId.ITEM_METAL_COAT, 3);
		removeItem(bag, ItemId.ITEM_SUN_STONE, 3);
		removeItem(bag, ItemId.ITEM_UP_GRADE, 3);

	}

	private static void removeItems(ItemBag bag, List<ItemId> orderedIds, int nbMaxTot) throws RequestFailedException {
		for (ItemId id : orderedIds) {
			int nbCurrentID = bag.getItem(id).getCount();
			if (nbCurrentID > 0 && nbCurrentID > nbMaxTot) {
				System.out.println("removing " + (nbCurrentID - nbMaxTot) + " "
						+ PokeDictionary.getDisplayItemName(id, Locale.getDefault()));
				bag.removeItem(id, nbCurrentID - nbMaxTot);
				nbCurrentID = nbMaxTot;
			}
			nbMaxTot -= nbCurrentID;
		}
		;
	}

	private static void removeItem(ItemBag bag, ItemId id, int nbMax) throws RequestFailedException {
		int nbCurrentID = bag.getItem(id).getCount();
		if (nbCurrentID > 0 && nbCurrentID > nbMax) {
			System.out.println("removing " + (nbCurrentID - nbMax) + " "
					+ PokeDictionary.getDisplayItemName(id, Locale.getDefault()));
			bag.removeItem(id, nbCurrentID - nbMax);
		}
	}

	public static void tryIncubateEgg(PokemonGo go) {
		trace("##### Gestion des oeufs");
		Optional<EggPokemon> nextEgg = go.getInventories().getHatchery().getEggs().stream() //
				.filter(egg -> !egg.isIncubate()) //
				.sorted(Comparator.comparing(egg -> egg.getCreationTimeMs())) //
				.findFirst();

		if (nextEgg.isPresent()) {
			Optional<EggIncubator> freeIncubator = go.getInventories().getIncubators().stream() //
					.filter(ei -> !ei.isInUse()) //
					.findFirst();
			if (freeIncubator.isPresent()) {
				System.out.println(String.format("Incubate %.2fkm egg...", nextEgg.get().getEggKmWalkedTarget()));
				try {
					nextEgg.get().incubate(freeIncubator.get());
					System.out.println(String.format("Incubate %.2fkm egg...OK", nextEgg.get().getEggKmWalkedTarget()));
				} catch (RequestFailedException e) {
					e.printStackTrace();
					System.out.println("Failed to incubate egg");
				}
			} else {
				System.out.println("No available incubator!");
			}
		} else {
			System.out.println("No available egg!");
		}
	}

	public static List<Point> generatePoints(List<Point> in) {
		int step_m = 100; // distance max entre chaque point

		return Observable.from(in) //
				.buffer(2, 1) //
				.filter(list -> list.size() == 2) //
				.concatMapIterable(list -> {
					double dist_m = MapUtil.distFrom(list.get(0), list.get(1));
					int nbStep = (int) dist_m / step_m + 1;
					return IntStream.range(0, nbStep) //
							.mapToObj(i -> new Point( //
									list.get(0).getLatitude()
											+ (list.get(1).getLatitude() - list.get(0).getLatitude()) * i / nbStep, //
									list.get(0).getLongitude()
											+ (list.get(1).getLongitude() - list.get(0).getLongitude()) * i / nbStep)) //
							.collect(Collectors.<Point>toList());
				}).toList() //
				.toBlocking() //
				.first();
	}

	public static void evolveAndPowerUpPkm(PokemonGo api) throws RequestFailedException {
		trace("##### Amélioration des pokemons #####");
		PokeBank pokebank = api.getInventories().getPokebank();
		List<Pokemon> pokemons = pokebank.getPokemons();
		Evolutions evolutionMeta = api.getItemTemplates().getEvolutions();

		// look for pkm to be evolved or powered up
		List<Pokemon> pkmToPowerUp = Observable.from(pokemons) //
				.filter(pkm -> Constants.POKEMON_FAMILY_TO_KEEP.contains(pkm.getPokemonFamily())) //
				.groupBy(Pokemon::getPokemonFamily) //
				.flatMap(obs -> obs.toList() //
						.map(list -> list.stream() //
								.sorted(Comparator.comparing(Pokemon::getIvInPercentage).reversed()) //
								.limit(1) // powerup only one of each family
								.filter(pkm -> !pkm.isEgg()) // A tester
								.filter(pkm -> pkm.canPowerUp() || pkm.canEvolve()))) //
				.reduce(Stream::concat) //
				.map(stream -> stream //
						.sorted(Comparator
								.<Pokemon, Integer>comparing(pkm -> pkm
										.getCpFullEvolveAndPowerup(evolutionMeta.getHighest(pkm.getPokemonId()).get(0)))
								.reversed()) //
						.limit(10) // powerup only 10 pkm (except eevee)
						.collect(Collectors.toList())) //
				.toBlocking() //
				.first();

		for (Pokemon pkm_tmp : pkmToPowerUp) {
			Pokemon pkm = pkm_tmp;
			while (pkm.canEvolve()) {
				System.out.println("Evolving "
						+ PokeDictionary.getDisplayName(pkm.getPokemonId().getNumber(), Locale.getDefault()));
				EvolutionResult r = pkm.evolve();
				System.out.println("Evolution result : " + r.getResult());
				if (r.isSuccessful()) {
					Utils.onNewPokemon(api, r.getEvolvedPokemon());
					pkm = r.getEvolvedPokemon();
				}
			}

			while (pkm.canPowerUp() && pkm.getStardustCostsForPowerup() <= 4000) {
				System.out.println("Upgrading "
						+ PokeDictionary.getDisplayName(pkm.getPokemonId().getNumber(), Locale.getDefault()));
				UpgradePokemonResponse.Result r = pkm.powerUp();
				System.out.println("Upgrading result: " + r);
				if (!r.equals(UpgradePokemonResponse.Result.SUCCESS)) {
					break;
				} else {
					System.out.println("New CP: " + pkm.getCp());
				}
			}
		}

		/**
		 * gestion specifique famille evoli (seule famille pour laquelle on monte plus
		 * d'un pokemon)
		 **/
		// find best possessed evolutions
		Map<PokemonId, Pokemon> mapBestEeveeEvolutions = Observable //
				.from(pokemons.stream() //
						.filter(pkm -> pkm.getPokemonFamily() == PokemonFamilyId.FAMILY_EEVEE) //
						.filter(pkm -> pkm.getPokemonId() != PokemonId.EEVEE) //
						.sorted(Comparator.comparing(Pokemon::getIvInPercentage).reversed()) //
						.collect(Collectors.toList()))
				.distinct(pkm -> pkm.getPokemonId()) //
				.toMap(Pokemon::getPokemonId) //
				.toBlocking() //
				.first();
		double minIV = mapBestEeveeEvolutions.values().stream() //
				.mapToDouble(Pokemon::getIvInPercentage) //
				.min() //
				.orElse(0);

		// find eevees that can give better evolutions
		List<Pokemon> eevees = pokemons.stream() //
				.filter(pkm -> pkm.getPokemonId() == PokemonId.EEVEE) //
				.filter(pkm -> pkm.getIvInPercentage() > minIV) //
				.sorted(Comparator.comparing(Pokemon::getIvInPercentage).reversed()) //
				.collect(Collectors.toList());

		// manage eevee evolution
		Map<PokemonId, Pokemon> mapBestEeveeEvolutions2 = new HashMap<>(mapBestEeveeEvolutions);
		for (Pokemon eevee : eevees) {
			if (!eevee.canEvolve()) {
				// keep only evolutions better than current eevee
				List<PokemonId> idToRemove = mapBestEeveeEvolutions2.entrySet().stream() //
						.filter(kv -> kv.getValue().getIvInPercentage() < eevee.getIvInPercentage()) //
						.map(kv -> kv.getKey()) //
						.collect(Collectors.toList());
				idToRemove.forEach(mapBestEeveeEvolutions2::remove);
				break;
			}

			// evolve eevee if it can give a better evolution

			// force evolution if missing
			if (!mapBestEeveeEvolutions2.containsKey(PokemonId.ESPEON)) {
				// mentali
				eevee.renamePokemon("Sakura");
			} else if (!mapBestEeveeEvolutions2.containsKey(PokemonId.UMBREON)) {
				// noctali
				eevee.renamePokemon("Tamao");
			} else if (!mapBestEeveeEvolutions2.containsKey(PokemonId.VAPOREON)) {
				// noctali
				eevee.renamePokemon("Rainer");
			} else if (!mapBestEeveeEvolutions2.containsKey(PokemonId.JOLTEON)) {
				// noctali
				eevee.renamePokemon("Sparky");
			} else if (!mapBestEeveeEvolutions2.containsKey(PokemonId.FLAREON)) {
				// noctali
				eevee.renamePokemon("Pyro");
			}

			if (mapBestEeveeEvolutions2.size() < 5 //
					|| mapBestEeveeEvolutions2.values().stream() //
							.anyMatch(evol -> eevee.getIvInPercentage() > evol.getIvInPercentage())) {
				System.out.println("Evolving "
						+ PokeDictionary.getDisplayName(eevee.getPokemonId().getNumber(), Locale.getDefault()));
				EvolutionResult r = eevee.evolve();
				System.out.println("Evolution result : " + r.getResult());
				if (r.isSuccessful()) {
					Utils.onNewPokemon(api, r.getEvolvedPokemon());
					if (!mapBestEeveeEvolutions2.containsKey(r.getEvolvedPokemon().getPokemonId()) //
							|| mapBestEeveeEvolutions2.get(r.getEvolvedPokemon().getPokemonId()).getIvInPercentage() < r
									.getEvolvedPokemon().getIvInPercentage()) {
						// keep if new evolution or if better than previous
						mapBestEeveeEvolutions2.put(r.getEvolvedPokemon().getPokemonId(), r.getEvolvedPokemon());
					}
				}
			}
		}

		// manage evolution powerup
		List<Pokemon> evolToPowerUp = mapBestEeveeEvolutions2.values().stream() //
				.sorted(Comparator.comparing(Pokemon::getIvInPercentage).reversed()) //
				.collect(Collectors.toList());
		for (Pokemon evol : evolToPowerUp) {
			while (evol.canPowerUp() && evol.getStardustCostsForPowerup() <= 4000) {
				System.out.println("Upgrading "
						+ PokeDictionary.getDisplayName(evol.getPokemonId().getNumber(), Locale.getDefault()));
				UpgradePokemonResponse.Result r = evol.powerUp();
				System.out.println("Upgrading result: " + r);
				if (!r.equals(UpgradePokemonResponse.Result.SUCCESS)) {
					break;
				} else {
					System.out.println("New CP: " + evol.getCp());
				}
			}
		}

	}

	public static boolean healPokemon(PokemonGo api, Pokemon pkm) throws RequestFailedException {

		ItemBag bag = api.getInventories().getItemBag();

		// revive if needed
		if (pkm.isFainted()) {
			if (pkm.revive() == UseItemReviveResponse.Result.ERROR_CANNOT_USE) {
				System.out.println("We have no revives! Cannot revive pokemon.");
				return false;
			}
		}

		// heal
		while (pkm.isInjured()) {
			int missingStamina = pkm.getMaxStamina() - pkm.getStamina();

			UseItemPotionResponse.Result r = null;
			if (missingStamina < 20 //
					&& bag.getItem(ItemId.ITEM_POTION).getCount() > 0) {
				r = pkm.usePotion(ItemId.ITEM_POTION);
			} else if (missingStamina < 50 //
					&& bag.getItem(ItemId.ITEM_SUPER_POTION).getCount() > 0) {
				r = pkm.usePotion(ItemId.ITEM_SUPER_POTION);
			} else if (missingStamina < 200 //
					&& bag.getItem(ItemId.ITEM_HYPER_POTION).getCount() > 0) {
				r = pkm.usePotion(ItemId.ITEM_HYPER_POTION);
			} else if (api.getInventories().getItemBag().getItem(ItemId.ITEM_MAX_POTION).getCount() > 0) {
				r = pkm.usePotion(ItemId.ITEM_POTION);
			} else {
				// cannot heal in one try -> use any potion
				if (pkm.heal() == UseItemPotionResponse.Result.ERROR_CANNOT_USE) {
					System.out.println("We have no potions! Cannot heal pokemon.");
					return false;
				}
			}
			if (r != UseItemPotionResponse.Result.SUCCESS) {
				return false;
			}
		}

		return true;
	}

	public static void trace(String str) {
		System.out.println(String.format("%d - %s", System.currentTimeMillis(), str));
	}

	/**
	 * Trouve l'arène la plus proche avec un fly
	 * 
	 * @param api
	 * @return
	 */
	public static Optional<Gym> checkGymsInArea(PokemonGo api) {
		displayNearbyGyms(api);

		return api.getMap().getMapObjects().getGyms().stream() //
				.filter(gym -> !gym.getFortData().hasRaidInfo()) // cannot attack gym with running raid
				// look for gym with fly
				.filter(gym -> {
					try {
						return gym.getDefendingPokemon().stream() //
								.anyMatch(pkm -> Constants.KNOWN_FLIES.contains(pkm.getPokemon().getOwnerName()));
					} catch (RequestFailedException e1) {
						e1.printStackTrace();
						return false;
					}
				})
				// sort by best pokmemon
				.sorted(Comparator.<Gym, Integer>comparing(gym -> {
					try {
						return gym.getDefendingPokemon().stream() //
								.mapToInt(pkm -> pkm.getCpNow()).max() //
								.getAsInt();
					} catch (RequestFailedException e) {
						e.printStackTrace();
						return Integer.MAX_VALUE;
					}
				}))
				// look for weakest gym
				.findFirst();
	}

}
