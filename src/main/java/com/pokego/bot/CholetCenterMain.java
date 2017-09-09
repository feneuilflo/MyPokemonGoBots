package com.pokego.bot;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.pokego.bot.unitofwork.CatchPokemonAtArea;
import com.pokego.bot.unitofwork.FreeInventory;
import com.pokego.bot.unitofwork.Login;
import com.pokego.bot.unitofwork.Loop;
import com.pokego.bot.unitofwork.MoveToAllPokestops;
import com.pokego.bot.unitofwork.ReleasePokemon;
import com.pokego.bot.utils.Utils;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;

import okhttp3.OkHttpClient;
import rx.Observable;

public class CholetCenterMain {

	private static final boolean LOOP = true;
	private static final long MAX_TIME_MIN = 240;

	public static void main(String[] args) {
		// exit after fixed time
		if (MAX_TIME_MIN > 0) {
			Observable.timer(MAX_TIME_MIN, TimeUnit.MINUTES) //
					.subscribe(__ -> {
						System.out.println("exit...");
						System.exit(0);
					});
		}

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
			
			/** transfere les pokemons superflus */
			queue.addWork(new ReleasePokemon(api));

			/** attrape les pokemons a proximité */
			queue.addWork(new CatchPokemonAtArea(api));

			/** gestion des oeufs */
			queue.addWork(() -> Utils.tryIncubateEgg(api)); 

			/** vide l'inventaire */
			queue.addWork(new FreeInventory(api));
			
			/** ameliore les pkm */
			// queue.addWork(new EvolveAndPowerUp(api));

			/** fait le tour des pokestops en boucle */
			queue.addWork(new Loop(queue, Arrays.asList(new MoveToAllPokestops(api, queue))));
			
			try {
				queue.join();
			} catch (InterruptedException e) {
				System.err.println("interrupted join");
				// nothing to do
			}
			
			api.exit();
			
			if (LOOP) {
				System.out.println("###### En attente (reboot) #######");
				int max = 6;
				int period = 10;
				Observable.interval(period, TimeUnit.SECONDS) //
						.take(max) //
						.toBlocking() //
						.subscribe(l -> System.out.println("remaining time before reboot: " + (max - l - 1) * period + "s"));
			}
			
		} while (LOOP);
	}

}
