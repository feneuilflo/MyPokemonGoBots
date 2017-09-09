package com.pokego.bot.listener;

import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.PlayerListener;
import com.pokegoapi.api.player.Medal;
import com.pokegoapi.api.player.PlayerProfile;

public class PlayerListenerImpl implements PlayerListener {

	private final WorkQueue queue;
	
	public PlayerListenerImpl(WorkQueue queue) {
		this.queue = queue;
	}
	
	@Override
	public void onLevelUp(PokemonGo api, int oldLevel, int newLevel) {
		System.out.println("PlayerListener: new level (" + newLevel + ")");
		System.out.println("--> exit");
		queue.interrupt();
	}

	@Override
	public void onMedalAwarded(PokemonGo api, PlayerProfile profile, Medal medal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWarningReceived(PokemonGo api) {
		// TODO Auto-generated method stub
		System.err.println("PlayerListener: warning received!");
		System.out.println("--> exit");
		queue.interrupt();
	}
	
	

}
