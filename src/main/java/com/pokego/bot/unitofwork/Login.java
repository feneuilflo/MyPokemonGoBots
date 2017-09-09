package com.pokego.bot.unitofwork;

import java.util.Comparator;
import java.util.Locale;

import com.pokego.bot.Constants;
import com.pokego.bot.LoginData;
import com.pokego.bot.listener.PlayerListenerImpl;
import com.pokego.bot.listener.PokemonListernerImpl;
import com.pokego.bot.utils.WorkQueue;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Point;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.request.InvalidCredentialsException;
import com.pokegoapi.exceptions.request.LoginFailedException;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import okhttp3.OkHttpClient;

public class Login implements IUnitOfWork {
	
	private final PokemonGo api;
	private final WorkQueue queue;
	
	private final Point startLocation;
	
	private final CredentialProvider credentialProvider;

	public Login(PokemonGo api, CredentialProvider credentialProvider, WorkQueue queue) {
		this.api = api;
		this.queue = queue;
		this.startLocation = new Point(Constants.START_LATITUDE, Constants.START_LONGITUDE);
		this.credentialProvider = credentialProvider;
	}
	
	public Login(PokemonGo api, CredentialProvider credentialProvider, WorkQueue queue, Point start) {
		this.api = api;
		this.queue = queue;
		this.startLocation = start;
		this.credentialProvider = credentialProvider;
	}

	@Override
	public void run() throws Exception {
		HashProvider hasher = new PokeHashProvider(PokeHashKey.from(LoginData.HASH_KEY), true);
		try {
			api.login(credentialProvider, hasher);
		} catch (LoginFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} catch (RequestFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		
		// affichage divers
		System.out.println(api.getPlayerProfile().getPlayerData().getUsername());
		System.out.println("banned: " + api.getPlayerProfile().isBanned());
		System.out.println("warned: " + api.getPlayerProfile().isWarned());
		System.out.println("level: " + api.getPlayerProfile().getLevel() //
				+ String.format("(%d/%d)", //
						api.getPlayerProfile().getStats().getExperience() - api.getPlayerProfile().getStats().getPrevLevelXp(), //
						api.getPlayerProfile().getStats().getNextLevelXp() - api.getPlayerProfile().getStats().getPrevLevelXp()));
		System.out.println("Distance parcourue: " + api.getPlayerProfile().getStats().getKmWalked() + "km");
		System.out.println("Nb pokémons attrapés: " + api.getPlayerProfile().getStats().getPokemonsCaptured());
		System.out.println("Oeufs éclos: " + api.getPlayerProfile().getStats().getEggsHatched());
		System.out.println("Poussière d'étoile: " + api.getPlayerProfile().getCurrency(PlayerProfile.Currency.STARDUST));

		System.out.println("##### Liste des pokémons #####");
		api.getInventories().getPokebank().getPokemons().stream()
				.sorted(Comparator.comparing(Pokemon::getCp).reversed())
				.forEach(pok -> System.out.println(String.format("%1$15s\tCP: %2$4d\tIV: %3$4s\tbonbons:%4$d", //
						PokeDictionary.getDisplayName((int) pok.getPokemonId().getNumber(), Locale.getDefault()), //
						pok.getCp(), //
						pok.getIvInPercentage(), //
						api.getInventories().getCandyjar().getCandies(pok.getPokemonFamily()))));
		
		// add listener
		api.addListener(new PokemonListernerImpl(queue));
		api.addListener(new PlayerListenerImpl(queue));
		
		// Move to start location (47.0603329,-0.8805762)
		api.setLocation(startLocation.getLatitude(), startLocation.getLongitude(), Constants.START_ALTITUDE);
		// Wait until map is updated for the current location
		try {
			api.getMap().awaitUpdate();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
