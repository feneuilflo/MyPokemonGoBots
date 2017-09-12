package com.pokego.bot;

import java.util.concurrent.TimeUnit;

import com.pokego.bot.unitofwork.Login;
import com.pokego.bot.unitofwork.Patrol;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;

import okhttp3.OkHttpClient;
import rx.Observable;

public class PatrolCholetCenterMain {


	public static void main(String[] args) {
		// exit after fixed number of tries
		int remainingTries = 10;

		do {
			final OkHttpClient httpClient = new OkHttpClient();
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
			
			//api.addListener(new RequestListener()); 
			
			queue.addWork(new Login(api, credentialProvider, queue, new Point(47.0603329, -0.8805762)));
			
			/** patrouille */
			queue.addWork(new Patrol(api, queue, Constants.ORDERED_POKESTOP_NAMES));
			
			try {
				queue.join();
			} catch (InterruptedException e) {
				System.err.println("interrupted join");
				// nothing to do
			}
			
			api.exit();
			
			if (remainingTries > 1) {
				System.out.println("###### En attente (reboot) #######");
				int max = 6;
				int period = 10;
				Observable.interval(period, TimeUnit.SECONDS) //
						.take(max) //
						.toBlocking() //
						.subscribe(l -> System.out.println("remaining time before reboot: " + (max - l - 1) * period + "s"));
			}
			
		} while (--remainingTries > 0);
	}

}
