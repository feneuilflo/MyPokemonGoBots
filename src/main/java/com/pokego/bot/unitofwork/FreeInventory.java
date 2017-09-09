package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;

public class FreeInventory implements IUnitOfWork {
	
	private final PokemonGo api;
	
	public FreeInventory(PokemonGo api) {
		this.api = api;
	}

	@Override
	public void run() throws Exception {
		Utils.freeInventory(api);
	}
	

}
