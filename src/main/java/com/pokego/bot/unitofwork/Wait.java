package com.pokego.bot.unitofwork;

import java.util.concurrent.TimeUnit;

import rx.Observable;

public class Wait implements IUnitOfWork {

	private final int time_min;
	
	public Wait(int time_min) {
		this.time_min = time_min;
	}
	
	@Override
	public void run() throws Exception {
		if(time_min <= 0) {
			return;
		}
		
		System.out.println("###### En attente #######");
		int period_s = 10;
		int max = time_min * 60 / period_s;
		Observable.interval(period_s, TimeUnit.SECONDS) //
				.take(max) //
				.toBlocking() //
				.subscribe(l -> System.out.println("remaining: " + (max - l - 1) * period_s + "s"));
	}

}
