package com.pokego.bot;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Battle.ServerAction;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import POGOProtos.Data.Battle.BattleActionTypeOuterClass.BattleActionType;
import POGOProtos.Data.Battle.BattleParticipantOuterClass.BattleParticipant;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;
import POGOProtos.Networking.Responses.GymStartSessionResponseOuterClass.GymStartSessionResponse;
import POGOProtos.Networking.Responses.UseItemPotionResponseOuterClass.UseItemPotionResponse;
import POGOProtos.Networking.Responses.UseItemReviveResponseOuterClass.UseItemReviveResponse;
import POGOProtos.Settings.Master.MoveSettingsOuterClass.MoveSettings;
import okhttp3.OkHttpClient;

/**
 * Main de test pour les combats d'arène
 * <p>
 * danger : instable
 */
public class TestGym {

	public static void main(String[] args) {
		final OkHttpClient httpClient = new OkHttpClient();
		final PokemonGo api = new PokemonGo(httpClient);
		HashProvider hasher = new PokeHashProvider(PokeHashKey.from(LoginData.HASH_KEY), true);

		CredentialProvider credentialProvider = null;
		try {
			credentialProvider = //
					// new GoogleUserCredentialProvider(httpClient, LoginData.SACHA_DONT_FLY_GOOGLE_REFRESH_TOKEN);
					new PtcCredentialProvider(httpClient, LoginData.PTC_LOGIN, LoginData.PTC_PWD);
			api.login(credentialProvider, hasher);
		} catch (LoginFailedException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (InvalidCredentialsException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (RequestFailedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Affichage divers
		System.out.println(api.getPlayerProfile().getPlayerData().getUsername());
		System.out.println("banned: " + api.getPlayerProfile().isBanned());
		System.out.println("warned: " + api.getPlayerProfile().isWarned());
		System.out.println("Distance parcourue: " + api.getPlayerProfile().getStats().getKmWalked() + "km");
		System.out.println("Nb pokémons attrapés: " + api.getPlayerProfile().getStats().getPokemonsCaptured());

		// Move to start location
		// api.setLocation(47.05948478361122, -0.8813792467117311, Constants.START_ALTITUDE); // porte du mail
		api.setLocation(47.0603329, -0.8805762, Constants.START_ALTITUDE); // caisse d'épargne
		// api.setLocation(47.06081864086622,-0.8815991878509521, Constants.START_ALTITUDE); // notre dame

		// Wait until map is updated for the current location
		try {
			api.getMap().awaitUpdate();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Affiche les éléments à proximité
		// Utils.displayNearbyPokemons(go);
		// Utils.displayNearbyPokestops(go);
		Utils.displayNearbyGyms(api);

		// affiche plus de details
		System.out.println("### details ###");
		api.getMap().getMapObjects().getGyms().stream() //
				.forEach(gym -> {
					try {
						System.out.println(String.format("%s - team: %s - raid boss: %s", gym.getName(),
								gym.getOwnedByTeam().toString(), //
								gym.getFortData().hasRaidInfo() //
										? String.format("%s (%dmin)", //
												gym.getFortData().getRaidInfo().getRaidPokemon().getPokemonId(), //
												(gym.getFortData().getRaidInfo().getRaidEndMs()
														- System.currentTimeMillis()) / 60_000) //
										: "aucun"));
					} catch (RequestFailedException e) {
						e.printStackTrace();
					}
				});

		// 
		System.out.println("### in range defenders: ###");
		Gym gym = api.getMap().getMapObjects().getGyms().stream() //
				.filter(Gym::inRange) //
				.findFirst() //
				.get();
		try {
			System.out.println(String.format("%s - team: %s", gym.getName(), gym.getOwnedByTeam()));
			gym.getDefendingPokemon().forEach(pkm -> System.out.println(String.format("%s (%s) since %smin - pc: %d/%d", //
					pkm.getPokemon().getPokemonId(), //
					pkm.getPokemon().getOwnerName(), //
					(System.currentTimeMillis() - pkm.getDeployMs()) / 60_000, //
					pkm.getCpNow(), pkm.getCpWhenDeployed())));
		} catch (RequestFailedException e) {
			e.printStackTrace();
		}
		
		// heal and select attackers
		List<Pokemon> pokemons = api.getInventories().getPokebank().getPokemons();
		List<Pokemon> attackers = pokemons.stream() //
				.sorted(Comparator.comparing(Pokemon::getCp).reversed()) //
				.map(pkm -> {
					try {
						healPokemonFull(pkm);
					} catch (RequestFailedException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					
					return pkm;
				}) //
				.filter(pkm -> !pkm.isFainted()) //
				.filter(pkm -> !pkm.isInjured()) //
				.limit(6) //
				.collect(Collectors.toList());
		
		if(attackers.isEmpty()) {
			System.out.println("No available attackers");
		} else {
			System.out.println("### Battle ###");
			//Create battle object
			Battle battle = gym.battle();
			
			//Start battle
			try {
				battle.start(new FightHandler(attackers.toArray(new Pokemon[6])));
				while (battle.isActive()) {
					handleAttack(api, battle);
				}
			} catch (RequestFailedException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}

	}
	
	
	private static void handleAttack(PokemonGo api, Battle battle) throws InterruptedException {
		int duration;
		PokemonMove specialMove = battle.getActiveAttacker().getPokemon().getMove2();
		MoveSettings moveSettings = api.getItemTemplates().getMoveSettings(specialMove);
		//Check if we have sufficient energy to perform a special attack
		int energy = battle.getActiveAttacker().getEnergy();
		int desiredEnergy = -moveSettings.getEnergyDelta();
		if (energy <= desiredEnergy) {
			System.err.println("attack");
			duration = battle.attack();
		} else {
			System.err.println("special attack");
			duration = battle.attackSpecial();
		}
		//Attack and sleep for the duration of the attack + some extra time
		Thread.sleep(duration + (long) (Math.random() * 10));
	}

	private static void healPokemonFull(Pokemon pokemon) throws RequestFailedException {
		//Continue healing the pokemon until fully healed
		while (pokemon.isInjured() || pokemon.isFainted()) {
			System.out.println("Healing " + pokemon.getPokemonId());
			if (pokemon.isFainted()) {
				if (pokemon.revive() == UseItemReviveResponse.Result.ERROR_CANNOT_USE) {
					System.out.println("We have no revives! Cannot revive pokemon.");
					break;
				}
			} else {
				if (pokemon.heal() == UseItemPotionResponse.Result.ERROR_CANNOT_USE) {
					System.out.println("We have no potions! Cannot heal pokemon.");
					break;
				}
			}
		}
	}

	private static class FightHandler implements Battle.BattleHandler {
		private Pokemon[] team;

		FightHandler(Pokemon[] team) {
			this.team = team;
		}

		@Override
		public Pokemon[] createTeam(PokemonGo api, Battle battle) {
			return team;
		}

		@Override
		public void onStart(PokemonGo api, Battle battle, GymStartSessionResponse.Result result) {
			System.out.println("Battle started with result: " + result);
			try {
				System.out.println("Defender count: " + battle.getGym().getDefendingPokemon().size());
				System.out.println("attacker: " + battle.getActiveAttacker().getPokemon().getPokemonId());
				System.out.println("defender: " + battle.getActiveDefender().getPokemon().getPokemonId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onVictory(PokemonGo api, Battle battle) {
			System.out.println("Gym ended with result: Victory!");
		}

		@Override
		public void onDefeated(PokemonGo api, Battle battle) {
			System.out.println("Gym ended with result: Defeated");
		}

		@Override
		public void onTimedOut(PokemonGo api, Battle battle) {
			System.out.println("Gym battle timed out!");
		}

		@Override
		public void onActionStart(PokemonGo api, Battle battle, Battle.ServerAction action) {
			//Dodge all special attacks
			if (action.getType() == BattleActionType.ACTION_SPECIAL_ATTACK) {
				System.out.println("Dodging special attack!");
				battle.dodge();
			}
			System.out.println(toIndexName(action) + " performed " + action.getType());
		}

		@Override
		public void onActionEnd(PokemonGo api, Battle battle, Battle.ServerAction action) {
		}

		@Override
		public void onDamageStart(PokemonGo api, Battle battle, ServerAction action) {
		}

		@Override
		public void onDamageEnd(PokemonGo api, Battle battle, ServerAction action) {
		}

		@Override
		public void onPlayerJoin(PokemonGo api, Battle battle, BattleParticipant joined, Battle.ServerAction action) {
			System.out.println(joined.getTrainerPublicProfile().getName() + " joined this battle!");
		}

		@Override
		public void onPlayerLeave(PokemonGo api, Battle battle, BattleParticipant left, Battle.ServerAction action) {
			System.out.println(left.getTrainerPublicProfile().getName() + " left this battle!");
		}

		@Override
		public void onAttacked(PokemonGo api, Battle battle, Battle.BattlePokemon attacked,
							   Battle.BattlePokemon attacker, int duration,
							   long damageWindowStart, long damageWindowEnd, Battle.ServerAction action) {
			PokemonId attackedPokemon = attacked.getPokemon().getPokemonId();
			PokemonId attackerPokemon = attacker.getPokemon().getPokemonId();
			System.out.println(attackedPokemon + " attacked by " + attackerPokemon + " (" + toIndexName(action) + ")");
		}

		@Override
		public void onAttackedSpecial(PokemonGo api, Battle battle, Battle.BattlePokemon attacked,
									  Battle.BattlePokemon attacker, int duration,
									  long damageWindowStart, long damageWindowEnd, Battle.ServerAction action) {
			PokemonId attackedPokemon = attacked.getPokemon().getPokemonId();
			PokemonId attackerPokemon = attacker.getPokemon().getPokemonId();
			System.out.println(attackedPokemon
					+ " attacked with special attack by " + attackerPokemon + " (" + toIndexName(action) + ")");
		}

		@Override
		public void onException(PokemonGo api, Battle battle, Exception exception) {
			System.err.println("Exception while performing battle:");
			exception.printStackTrace();
		}

		@Override
		public void onInvalidActions(PokemonGo api, Battle battle) {
			System.err.println("Sent invalid actions!");
		}

		@Override
		public void onAttackerHealthUpdate(PokemonGo api, Battle battle, int lastHealth, int health, int maxHealth) {
			System.out.println("Attacker: " + health + " / " + maxHealth);
		}

		@Override
		public void onDefenderHealthUpdate(PokemonGo api, Battle battle, int lastHealth, int health, int maxHealth) {
			System.out.println("Defender: " + health + " / " + maxHealth);
		}

		@Override
		public void onAttackerSwap(PokemonGo api, Battle battle, Battle.BattlePokemon newAttacker) {
			System.out.println("Attacker change: " + newAttacker.getPokemon().getPokemonId());
		}

		@Override
		public void onDefenderSwap(PokemonGo api, Battle battle, Battle.BattlePokemon newDefender) {
			System.out.println("Defender change: " + newDefender.getPokemon().getPokemonId());
		}

		@Override
		public void onFaint(PokemonGo api, Battle battle, Battle.BattlePokemon pokemon, int duration,
							Battle.ServerAction action) {
			System.out.println(toIndexName(action) + " fainted!");
		}

		@Override
		public void onDodge(PokemonGo api, Battle battle, Battle.BattlePokemon pokemon, int duration,
							Battle.ServerAction action) {
			System.out.println(toIndexName(action) + " dodged!");
		}

		/**
		 * Converts the attacker index to a readable name
		 *
		 * @param action the action containing an index
		 * @return a readable name for the attacker
		 */
		private String toIndexName(Battle.ServerAction action) {
			String name = "Attacker";
			if (action.getAttackerIndex() == -1) {
				name = "Defender";
			}
			return name;
		}

		
	}

}
