package com.pokego.bot.unitofwork;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pokego.bot.Constants;
import com.pokego.bot.utils.Tuple;
import com.pokego.bot.utils.Utils;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.exceptions.request.RequestFailedException;

public class MoveToAllPokestops implements IUnitOfWork {

	private final PokemonGo api;
	private final WorkQueue queue;

	private List<Pokestop> pokestops;

	public MoveToAllPokestops(PokemonGo api, WorkQueue queue) {
		this.api = api;
		this.queue = queue;
	}

	@Override
	public void run() throws Exception {
		if (pokestops == null) {
			Map<String, Pokestop> mapPoketops = api.getMap().getMapObjects().getPokestops().stream() //
					.<Tuple<String, Pokestop>>map(pkstop -> {
						try {
							return Tuple.create(pkstop.getName(), pkstop);
						} catch (RequestFailedException e) {
							System.err.println("error in MoveToAllPokestops: " + e.getMessage());
							queue.interrupt();
							throw new RuntimeException(e);						}
					}) //
					.filter(t2 -> Constants.ORDERED_POKESTOP_NAMES.contains(t2.getA())) //
					.collect(Collectors.toMap(Tuple::getA, Tuple::getB));
			pokestops = Constants.ORDERED_POKESTOP_NAMES.stream().sequential() //
					.peek(name -> {
						if (!mapPoketops.containsKey(name)) {
							System.err.println("Unknown pokestop: " + name);
						}
					}) //
					.filter(name -> mapPoketops.containsKey(name)) //
					.map(name -> mapPoketops.get(name)) //
					.collect(Collectors.toList());
		}
		
		if(pokestops.isEmpty()) {
			// bug sometimes...
			// --> exit
			System.err.println("no pokestop around --> exit");
			queue.interrupt();
		} else {
			for (Pokestop pokestop : pokestops) {
				queue.addImmediateWork(new MoveToPokeStop(api, pokestop));
			}
		}
		
		
	}

}
