package com.pokego.bot.unitofwork;

import java.util.List;
import java.util.Optional;

import com.pokego.bot.utils.BattleUtils;
import com.pokego.bot.utils.Utils;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.map.Point;

public class Patrol implements IUnitOfWork {
	
	private final PokemonGo api;
	private final WorkQueue queue;

	private IUnitOfWork farmingUOW;
	
	
	public Patrol(PokemonGo api, WorkQueue queue, List<String> pokestopNames) {
		this.api = api;
		this.queue = queue;
		this.farmingUOW = new MoveToAllPokestops(api, queue, pokestopNames);
		
	}

	@Override
	public void run() throws Exception {
		Optional<Gym> optGym = Utils.checkGymsInArea(api);
		
		if(optGym.isPresent()) {
			// if a fly is found
			// .. save location
			Point pt = api.getPoint();
			
			// .. move to gym
			if(!optGym.get().inRange()) {
				queue.addWork(new MoveToLocation(api, new Point(optGym.get().getLatitude(), optGym.get().getLongitude())));
			}
			
			// ... and attack
			queue.addWork(() -> {
				if(!BattleUtils.battleNearbyGym(api, optGym.get())) {
					// if we fail to win, do a farming session
					queue.addImmediateWork(farmingUOW);
				}
				
				// go back to saved location
				queue.addImmediateWork(new MoveToLocation(api, pt));
			});
		} else {
			// if no fly is found, do a farming session
			queue.addWork(farmingUOW);
		}
		
		// repeat
		queue.addWork(this);
		
	}
	
	
	

}
