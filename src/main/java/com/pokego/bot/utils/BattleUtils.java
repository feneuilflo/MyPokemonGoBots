package com.pokego.bot.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pokego.bot.Constants;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Battle.ServerAction;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.request.RequestFailedException;

import POGOProtos.Data.Battle.BattleActionTypeOuterClass.BattleActionType;
import POGOProtos.Data.Battle.BattleParticipantOuterClass.BattleParticipant;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Enums.PokemonMoveOuterClass.PokemonMove;
import POGOProtos.Networking.Responses.GymStartSessionResponseOuterClass.GymStartSessionResponse;
import POGOProtos.Settings.Master.MoveSettingsOuterClass.MoveSettings;

public class BattleUtils {

	/**
	 * Try to fight the closest gym
	 * 
	 * @return true if no error and all flies have been defeated, false otherwise
	 * @throws RequestFailedException
	 */
	
	
	public static boolean battleNearbyGym(PokemonGo api) throws RequestFailedException {
		System.out.println("### looking for gym to fight ###");

		// find the closest gym
		Optional<Gym> opt = api.getMap().getMapObjects().getGyms().stream() //
				.filter(Gym::inRange) //
				.sorted(Comparator.comparing(Gym::getDistance)) //
				.findFirst();
		
		if(!opt.isPresent()) {
			System.out.println("No gym in range");
			return false;
		}
		
		Gym gym = opt.get();
		System.out.println("Found gym " + gym.getName());

		// check gym can be attacked
		if (gym.getFortData().hasRaidInfo() //
				|| gym.getOwnedByTeam() == api.getPlayerProfile().getPlayerData().getTeam()) {
			System.out.println("Cannot attack the closest gym --> return");
			return false;
		}

		// attack as long as there is a fly in the gym
		while (gym.getDefendingPokemon().stream() //
				.anyMatch(pkm -> Constants.KNOWN_FLIES.contains(pkm.getPokemon().getOwnerName()))) {
			System.out.println("### in range defenders: ###");
			try {
				System.out.println(String.format("%s - team: %s", gym.getName(), gym.getOwnedByTeam()));
				gym.getDefendingPokemon()
						.forEach(pkm -> System.out.println(String.format("%s (%s) since %smin - pc: %d/%d", //
								pkm.getPokemon().getPokemonId(), //
								pkm.getPokemon().getOwnerName(), //
								(System.currentTimeMillis() - pkm.getDeployMs()) / 60_000, //
								pkm.getCpNow(), pkm.getCpWhenDeployed())));
			} catch (RequestFailedException e) {
				e.printStackTrace();
			}

			// select attackers
			System.out.println("### selecting attackers: ");
			List<Pokemon> pokemons = api.getInventories().getPokebank().getPokemons();
			List<Pokemon> attackers = pokemons.stream() //
					.sorted(Comparator.comparing(Pokemon::getCp).reversed()) //
					.limit(6) //
					.peek(pkm -> System.out.println(String.format("%s (%d)", pkm.getPokemonId(), pkm.getCp()))) //
					.collect(Collectors.toList());
			
			// heal attackers
			for(Pokemon pkm : attackers) {
				if(pkm.isFainted() || pkm.isInjured()) {
					System.out.println("Healing " + pkm.getPokemonId());
					boolean isHealed = Utils.healPokemon(api, pkm);
					if (!isHealed) {
						System.out.println("Cannot heal attackers -> quit");
						return false;
					}
				}
			}
			
			if (attackers.isEmpty()) {
				System.out.println("No available attackers -> quit");
				return false;
			} else {
				System.out.println("### Battle ###");
				// Create battle object
				Battle battle = gym.battle();

				// Start battle
				FightHandler handler = new FightHandler(attackers.toArray(new Pokemon[6]));
				try {
					battle.start(handler);
					while (battle.isActive()) {
						handleAttack(api, battle);
					}
				} catch (RequestFailedException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// check one defender defeated
				if(!handler.isOneDefenderDefeated()) {
					System.out.println("Crushing defeat -> quit");
					return false;
				}

			}
		}
		
		// sucess !
		return true;

	}

	private static void handleAttack(PokemonGo api, Battle battle) throws InterruptedException {
		int duration;
		PokemonMove specialMove = battle.getActiveAttacker().getPokemon().getMove2();
		MoveSettings moveSettings = api.getItemTemplates().getMoveSettings(specialMove);
		// Check if we have sufficient energy to perform a special attack
		int energy = battle.getActiveAttacker().getEnergy();
		int desiredEnergy = -moveSettings.getEnergyDelta();
		if (energy <= desiredEnergy) {
			System.err.println("attack");
			duration = battle.attack();
		} else {
			System.err.println("special attack");
			duration = battle.attackSpecial();
		}
		// Attack and sleep for the duration of the attack + some extra time
		Thread.sleep(duration + (long) (Math.random() * 10));
	}

	private static class FightHandler implements Battle.BattleHandler {
		private Pokemon[] team;
		
		private boolean oneDefenderDefeated = false;

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
		public void onVictory(PokemonGo api, Battle battle, int deltaPoints, long newPoints) {
			System.out.println("Gym ended with result: Victory!");
			System.out.println("Delta points: " + deltaPoints + ", New points: " + newPoints);
			oneDefenderDefeated = true;
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
			// Dodge all special attacks // FIXME should during damage window
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
				Battle.BattlePokemon attacker, int duration, long damageWindowStart, long damageWindowEnd,
				Battle.ServerAction action) {
			PokemonId attackedPokemon = attacked.getPokemon().getPokemonId();
			PokemonId attackerPokemon = attacker.getPokemon().getPokemonId();
			System.out.println(attackedPokemon + " attacked by " + attackerPokemon + " (" + toIndexName(action) + ")");
		}

		@Override
		public void onAttackedSpecial(PokemonGo api, Battle battle, Battle.BattlePokemon attacked,
				Battle.BattlePokemon attacker, int duration, long damageWindowStart, long damageWindowEnd,
				Battle.ServerAction action) {
			PokemonId attackedPokemon = attacked.getPokemon().getPokemonId();
			PokemonId attackerPokemon = attacker.getPokemon().getPokemonId();
			System.out.println(attackedPokemon + " attacked with special attack by " + attackerPokemon + " ("
					+ toIndexName(action) + ")");
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
			oneDefenderDefeated = true;
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
		 * @param action
		 *            the action containing an index
		 * @return a readable name for the attacker
		 */
		private String toIndexName(Battle.ServerAction action) {
			String name = "Attacker";
			if (action.getAttackerIndex() == -1) {
				name = "Defender";
			}
			return name;
		}
		
		
		public boolean isOneDefenderDefeated() {
			return oneDefenderDefeated;
		}

	}

}