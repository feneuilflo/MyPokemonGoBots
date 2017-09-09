package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;

public class MoveToPokeStop implements IUnitOfWork {

	private final Pokestop destination;
	private final PokemonGo api;
	
	public MoveToPokeStop(PokemonGo api, Pokestop destination) {
		this.api = api;
		this.destination = destination;
	}
	
	@Override
	public void run() throws Exception {
		Utils.moveToPokestop(api, destination);
		Utils.lootPokestop(api, destination);
		Utils.catchPkmInArea(api);
	}

}
