package com.pokego.bot;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.inventory.PokeBank;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.Encounter;
import com.pokegoapi.api.map.pokemon.ThrowProperties;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.settings.PokeballSelector;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse.CatchStatus;
import okhttp3.OkHttpClient;
import rx.Observable;

/**
 * Main basique qui fait le tour des pokestops du centre-ville de Cholet et attrape des pkms au passage
 * 
 * 
 * @deprecated voir CholetCenterMain pour une implémentation plus aboutie 
 */
public class LoginTestApp {

	private static final boolean LOOP = true;

	private static final long MAX_TIME_MIN = 120;

	public static void main(String[] args) {

		// exit after fixed time
		if (MAX_TIME_MIN > 0) {
			Observable.timer(MAX_TIME_MIN, TimeUnit.MINUTES) //
					.subscribe(__ -> {
						System.out.println("exit...");
						System.exit(0);
					});
		}

		try {
			OkHttpClient httpClient = new OkHttpClient();
			
			/** 
			* Google: 
			* You will need to redirect your user to GoogleUserCredentialProvider.LOGIN_URL
			* Afer this, the user must signin on google and get the token that will be show to him.
			* This token will need to be put as argument to login.
			*/
			GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(httpClient);

			// in this url, you will get a code for the google account that is logged
			System.out.println("Please go to " + GoogleUserCredentialProvider.LOGIN_URL);
			System.out.println("Enter authorization code:");
						
			// Ask the user to enter it in the standard input
			Scanner sc = new Scanner(System.in);
			String access = sc.nextLine();
						
			// we should be able to login with this token
			provider.login(access);

			PokemonGo go = new PokemonGo(httpClient);
			HashProvider hasher = new PokeHashProvider(PokeHashKey.from(LoginData.HASH_KEY), true);
			try {
				go.login(provider, hasher);
			} catch (LoginFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidCredentialsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RequestFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Affichage divers
			System.out.println(go.getPlayerProfile().getPlayerData().getUsername());
			System.out.println("banned: " + go.getPlayerProfile().isBanned());
			System.out.println("warned: " + go.getPlayerProfile().isWarned());
			System.out.println("Distance parcourue: " + go.getPlayerProfile().getStats().getKmWalked() + "km");
			System.out.println("Nb pokémons attrapés: " + go.getPlayerProfile().getStats().getPokemonsCaptured());

			System.out.println("##### Liste des pokémons #####");
			go.getInventories().getPokebank().getPokemons().stream()
					.sorted(Comparator.comparing(Pokemon::getCp).reversed())
					.forEach(pok -> System.out.println(String.format("%1$15s\tCP: %2$4d\tIV: %3$4s", //
							PokeDictionary.getDisplayName((int) pok.getPokemonId().getNumber(), Locale.getDefault()), //
							pok.getCp(), //
							pok.getIvInPercentage())));

			// add listener
			// go.addListener(new PokemonListernerImpl());

			// Move to start location (parc du mail)
			go.setLocation(47.059120, -0.880538, Constants.START_ALTITUDE);
			// Wait until map is updated for the current location
			try {
				go.getMap().awaitUpdate();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Affiche les éléments à proximité
			// Utils.displayNearbyPokemons(go);
			Utils.displayNearbyPokestops(go);
			// Utils.displayNearbyGyms(go);

			/** fait le tour des pokestops */
			List<Pokestop> pokestops = go.getMap().getMapObjects().getPokestops().stream() //
					.filter(pokestop -> pokestop.inRange()) //
					.collect(Collectors.toList());

			do {
				for (Pokestop pokestop : pokestops) {
					if (pokestop.canLoot()) {
						System.out.println("Looting pokestop " + pokestop.getName() + "...");
						PokestopLootResult result = pokestop.loot();
						System.out.println("Pokestop loot returned result: " + result.getResult());
					}
				}

				Set<CatchablePokemon> catchablePokemon = go.getMap().getMapObjects().getPokemon();
				System.out.println("Pokemon in area: " + catchablePokemon.size());

				Random random = new Random();
				PokeBank pokebank = go.getInventories().getPokebank();
				ItemBag bag = go.getInventories().getItemBag();
				
				for (CatchablePokemon cp : catchablePokemon) {
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

								// If no item is active, use a razzberry
								int razzberryCount = bag.getItem(ItemId.ITEM_RAZZ_BERRY).getCount();
								if (encounter.getActiveItem() == null && razzberryCount > 0) {
									encounter.useItem(ItemId.ITEM_RAZZ_BERRY);
								}

								// Throw pokeball with random properties
								encounter.throwPokeball(PokeballSelector.SMART, ThrowProperties.random());

								if (encounter.getStatus() == CatchStatus.CATCH_SUCCESS) {
									// Print pokemon stats
									Pokemon pokemon = pokebank.getPokemonById(encounter.getCapturedPokemon());
									if (pokemon != null) {
										double iv = pokemon.getIvInPercentage();
										int number = pokemon.getPokemonId().getNumber();
										String name = PokeDictionary.getDisplayName(number, Locale.ENGLISH);
										System.out.println("====" + name + "====");
										System.out.println("CP: " + pokemon.getCp());
										System.out.println("IV: " + iv + "%");
										System.out.println("Height: " + pokemon.getHeightM() + "m");
										System.out.println("Weight: " + pokemon.getWeightKg() + "kg");
										System.out.println("Move 1: " + pokemon.getMove1());
										System.out.println("Move 2: " + pokemon.getMove2());
										// Rename the pokemon to <Name> IV%
										pokemon.renamePokemon(name + " " + iv + "%");
										// Set pokemon with IV above 90% as favorite
										if (iv > 90) {
											pokemon.setFavoritePokemon(true);
										}
									}
								}
							}
						} else {
							System.out.println("Skipping Pokemon, we have no Pokeballs!");
						}

						// Wait for animation before catching next pokemon
						Thread.sleep(3000 + random.nextInt(1000));
					} else {
						System.out.println("Failed to encounter pokemon: " + encounter.getEncounterResult());
					}
				}

				if (LOOP) {
					System.out.println("###### En attente #######");
					int max = 6;
					int period = 10;
					Observable.interval(period, TimeUnit.SECONDS) //
							.take(max) //
							.toBlocking() //
							.subscribe(l -> System.out.println("remaining: " + (max - l - 1) * period + "s"));
				}

			} while (LOOP);
			
			//go.exit();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
