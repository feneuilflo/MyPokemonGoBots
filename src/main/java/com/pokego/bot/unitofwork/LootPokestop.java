package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;

public class LootPokestop implements IUnitOfWork {

	private final PokemonGo api;
	private final Pokestop pokestop;

	public LootPokestop(PokemonGo api, Pokestop pokestop) {
		this.api = api;
		this.pokestop = pokestop;
	}

	@Override
	public void run() throws Exception {
		Utils.lootPokestop(api, pokestop);
	}

}
