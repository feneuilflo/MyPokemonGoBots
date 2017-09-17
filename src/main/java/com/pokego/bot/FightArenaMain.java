package com.pokego.bot;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pokego.bot.unitofwork.Login;
import com.pokego.bot.unitofwork.Stop;
import com.pokego.bot.utils.BattleUtils;
import com.pokego.bot.utils.Utils;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;

import okhttp3.OkHttpClient;

public class FightArenaMain {
	
	public static void main(String[] args) {
		OkHttpClient httpClient = Utils.provideHttpClient();

		final PokemonGo api = new PokemonGo(httpClient);
		final WorkQueue queue = new WorkQueue();
		
		CredentialProvider credentialProvider = null;
		try {
			credentialProvider = //
					// new GoogleUserCredentialProvider(httpClient, LoginData.SACHA_DONT_FLY_GOOGLE_REFRESH_TOKEN);
					new PtcCredentialProvider(httpClient, LoginData.PTC_LOGIN, LoginData.PTC_PWD);
		} catch (LoginFailedException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (InvalidCredentialsException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		queue.addWork(new Login(api, credentialProvider, queue, new Point(Constants.START_LATITUDE, Constants.START_LONGITUDE)));
		
		AtomicBoolean result = new AtomicBoolean(false);
		queue.addWork(() -> {
			Optional<Gym> optGym = Utils.findClosestGym(api);
			
			if(optGym.isPresent()) {
				if(!optGym.get().inRange()) {
					Utils.moveToPoint(api, new Point(optGym.get().getLatitude(), optGym.get().getLongitude()));
				}
				
				result.set(BattleUtils.battleNearbyGym(api, optGym.get()));
			} else {
				System.out.println("No gym to fight !");
			}
		});
		
		queue.addWork(new Stop(queue));
		
		
		try {
			queue.join();
		} catch (InterruptedException e) {
			System.err.println("interrupted join");
			// nothing to do
		}
		
		api.exit();
	}

}
