package com.pokego.bot.unitofwork;

import java.util.List;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.api.map.fort.Pokestop;

public class MoveToLocation implements IUnitOfWork {
	
	private final PokemonGo api;
	private final Point destination;
	
	private List<Pokestop> pokestops;
	
	public MoveToLocation(PokemonGo api, Point destination) {
		this.api = api;
		this.destination = destination;
	}

	@Override
	public void run() throws Exception {
		System.out.println("### Moving to " + destination);
		Utils.moveToPoint(api, destination);
		Utils.catchPkmInArea(api);
		
		if(pokestops == null) {
			pokestops = Utils.lookForPokestop(api);
		}
		
		for(Pokestop pokestop : pokestops) {
			if(pokestop.canLoot()) {
				Utils.lootPokestop(api, pokestop);
			}
		}
	}
	
	

}
