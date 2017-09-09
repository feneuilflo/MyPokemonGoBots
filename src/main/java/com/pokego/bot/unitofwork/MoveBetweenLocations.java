package com.pokego.bot.unitofwork;

import java.util.List;
import java.util.stream.Collectors;

import com.pokego.bot.utils.Utils;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Point;

public class MoveBetweenLocations implements IUnitOfWork {

	private final PokemonGo api;
	private final WorkQueue queue;

	private List<Point> points;
	private List<IUnitOfWork> uows;

	public MoveBetweenLocations(PokemonGo api, WorkQueue queue, List<Point> points) {
		this.api = api;
		this.queue = queue;
		this.points = points;
	}
	
	@Override
	public void run() throws Exception {
		if(uows == null) {
			List<Point> allPoints = Utils.generatePoints(points);
			uows = allPoints.stream() //
					.map(pt -> new MoveToLocation(api, pt)) //
					.collect(Collectors.toList());
		}
		
		for(IUnitOfWork uow : uows) {
			queue.addWork(uow);
		}
		
	}

}
