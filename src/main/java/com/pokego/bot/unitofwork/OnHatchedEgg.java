package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.pokemon.Pokemon;

public class OnHatchedEgg implements IUnitOfWork {

	private final PokemonGo api;
	private final Pokemon pkm;

	public OnHatchedEgg(PokemonGo api, Pokemon pkm) {
		this.api = api;
		this.pkm = pkm;
	}

	@Override
	public void run() throws Exception {
		// rename / favorite
		Utils.onNewPokemon(api, pkm);
		
		// look for new egg
		Utils.tryIncubateEgg(api);
	}
}
