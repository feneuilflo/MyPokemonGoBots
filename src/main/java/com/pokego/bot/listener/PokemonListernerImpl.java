package com.pokego.bot.listener;

import java.util.Locale;

import com.pokego.bot.unitofwork.OnHatchedEgg;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.listener.PokemonListener;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.pokemon.HatchedEgg;
import com.pokegoapi.util.PokeDictionary;

import POGOProtos.Enums.EncounterTypeOuterClass.EncounterType;
import POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;

public class PokemonListernerImpl implements PokemonListener {

	private final WorkQueue queue;

	public PokemonListernerImpl(WorkQueue queue) {
		this.queue = queue;
	}

	@Override
	public boolean onEggHatch(PokemonGo api, HatchedEgg hatchedEgg) {
		System.out.println("#### Hatched egg !");
		System.out.println(String.format("Received a %s with %d candies", //
				PokeDictionary.getDisplayName(hatchedEgg.getPokemon().getPokemonId().getNumber(), Locale.getDefault()), //
				hatchedEgg.getCandy()));

		queue.addImmediateWork(new OnHatchedEgg(api, hatchedEgg.getPokemon()));

		return true;
	}

	@Override
	public void onEncounter(PokemonGo api, long encounterId, CatchablePokemon pokemon, EncounterType encounterType) {
		// Not used by api ?
		System.err.println("onEncounter");
	}

	@Override
	public boolean onCatchEscape(PokemonGo api, CatchablePokemon pokemon, Pokeball pokeball, int throwCount) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onBuddyFindCandy(PokemonGo api, PokemonFamilyId family, int candyCount) {
		// TODO Auto-generated method stub

	}

}
