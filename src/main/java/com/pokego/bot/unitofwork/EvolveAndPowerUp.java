package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;

public class EvolveAndPowerUp implements IUnitOfWork {
	
private final PokemonGo api;
	
	public EvolveAndPowerUp(PokemonGo api) {
		this.api = api;
	}

	@Override
	public void run() throws Exception {
		Utils.evolveAndPowerUpPkm(api);
	}

}
