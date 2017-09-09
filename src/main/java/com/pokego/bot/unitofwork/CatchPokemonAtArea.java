package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;

public class CatchPokemonAtArea implements IUnitOfWork {

	private final PokemonGo api;
	
	public CatchPokemonAtArea(PokemonGo api) {
		this.api = api;
	}
	
	@Override
	public void run() throws Exception {
		Utils.catchPkmInArea(api);
	}

}
