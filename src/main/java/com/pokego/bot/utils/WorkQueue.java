package com.pokego.bot.utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.pokego.bot.unitofwork.IUnitOfWork;
import com.pokegoapi.exceptions.AsyncPokemonGoException;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class WorkQueue implements Runnable {

	private final Thread asyncWorkingThread;
	private final List<IUnitOfWork> workList = Collections.synchronizedList(new ArrayList<>());
	
	private boolean active = true;

	
	public WorkQueue() {
		asyncWorkingThread = new Thread(this, "Async WorkingQueue Thread");
		asyncWorkingThread.setDaemon(true);
		asyncWorkingThread.start();
	}
	
	public void addWork(IUnitOfWork work) {
		workList.add(work);
	}
	
	public void addImmediateWork(IUnitOfWork work) {
		workList.add(0, work);
	}
	
	
	@Override
	public void run() {
		while (active) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new AsyncPokemonGoException("System shutdown", e);
			}

			if (!workList.isEmpty()) {
				IUnitOfWork work = workList.remove(0);
				Subscription subs = Subscriptions.empty();
				try {
					subs = Observable.timer(10, TimeUnit.MINUTES) //
						.subscribe(l -> {
							System.err.println("Timeout on work " + work.getClass().getSimpleName());
							asyncWorkingThread.interrupt();
						});
					work.run();
					subs.unsubscribe();
				} catch (Exception e) {
					System.err.println("Error while running work " + work.getClass().getSimpleName());
					e.printStackTrace();
					active = false;
					subs.unsubscribe();
				}
			}
		}
		
		System.out.println("WorkQueue - end of working thread");
	}
	
	public void join() throws InterruptedException {
		asyncWorkingThread.join();
	}

	public void interrupt() {
		asyncWorkingThread.interrupt();
	}
	
	public void exit() {
		active = false;
	}

}
