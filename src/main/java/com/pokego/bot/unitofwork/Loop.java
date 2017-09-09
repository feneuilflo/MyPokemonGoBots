package com.pokego.bot.unitofwork;

import java.util.List;

import com.pokego.bot.utils.WorkQueue;

public class Loop implements IUnitOfWork {

	private final WorkQueue queue;
	private final List<IUnitOfWork> works;
	
	public Loop(WorkQueue queue, List<IUnitOfWork> works) {
		this.queue = queue;
		this.works = works;
	}
	
	@Override
	public void run() throws Exception {
		// add units of work
		works.forEach(queue::addWork);
		
		// loop
		queue.addWork(new Loop(queue, works));
	}
	
	

}
