package com.pokego.bot.unitofwork;

import com.pokego.bot.utils.WorkQueue;

public class Stop implements IUnitOfWork {

	private final WorkQueue queue;
	
	public Stop(WorkQueue queue) {
		this.queue = queue;
	}
	
	@Override
	public void run() throws Exception {
		queue.exit();
	}

}
