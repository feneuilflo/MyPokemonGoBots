package com.pokego.bot;

import java.util.Comparator;
import java.util.Locale;

import com.pokego.bot.utils.Utils;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.TutorialListener;
import com.pokegoapi.api.player.Avatar;
import com.pokegoapi.api.player.PlayerAvatar;
import com.pokegoapi.api.player.PlayerGender;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.pokemon.StarterPokemon;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.request.RequestFailedException;
import com.pokegoapi.util.Log;
import com.pokegoapi.util.PokeDictionary;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;

import okhttp3.OkHttpClient;

/**
 * Main de création de compte
 * 
 * TODO : renseigner le nom, le lgin et le pwd (cf lignes marquées en "to do")
 *
 */
public class CreatePTCAccountMain {

	/**
	 * Goes through the tutorial with custom responses.
	 *
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		OkHttpClient http = new OkHttpClient();
		final PokemonGo api = new PokemonGo(http);
		try {
			// Add listener to listen for all tutorial related events, must be registered
			// before login is called,
			// otherwise it will not be used
			api.addListener(new TutorialListener() {
				@Override
				public String claimName(PokemonGo api, String lastFailure) {
					// Last attempt to set a codename failed, set a random one by returning null
					if (lastFailure != null) {
						System.out.println("Codename \"" + lastFailure + "\" is already taken. Using random name.");
						return null;
					}
					System.out.println("Selecting codename");
					return ""; // TODO a renseigner
				}

				@Override
				public StarterPokemon selectStarter(PokemonGo api) {
					// Catch Charmander as your starter pokemon
					System.out.println("Selecting starter pokemon");
					return StarterPokemon.SQUIRTLE;
				}

				@Override
				public PlayerAvatar selectAvatar(PokemonGo api) {
					System.out.println("Selecting player avatar");
					return new PlayerAvatar( //
							PlayerGender.FEMALE, //
							Avatar.Skin.YELLOW.id(), //
							Avatar.Hair.BLACK.id(), //
							Avatar.FemaleShirt.BLUE.id(), //
							Avatar.FemalePants.BLACK_PURPLE_STRIPE.id(),
							Avatar.FemaleHat.BLACK_YELLOW_POKEBALL.id(), //
							Avatar.FemaleShoes.BLACK_YELLOW_STRIPE.id(), //
							Avatar.Eye.GREEN.id(), //
							Avatar.FemaleBackpack.GRAY_BLACK_YELLOW_POKEBALL.id());
				}
			});
			HashProvider hasher = new PokeHashProvider(PokeHashKey.from(LoginData.HASH_KEY), true);
			api.login(new PtcCredentialProvider(http, "", ""), hasher); // TODO a renseigner

			// Affichage divers
			System.out.println(api.getPlayerProfile().getPlayerData().getUsername());
			System.out.println("banned: " + api.getPlayerProfile().isBanned());
			System.out.println("warned: " + api.getPlayerProfile().isWarned());
			System.out.println("Distance parcourue: " + api.getPlayerProfile().getStats().getKmWalked() + "km");
			System.out.println("Nb pokémons attrapés: " + api.getPlayerProfile().getStats().getPokemonsCaptured());

			System.out.println("##### Liste des pokémons #####");
			api.getInventories().getPokebank().getPokemons().stream()
					.sorted(Comparator.comparing(Pokemon::getCp).reversed())
					.forEach(pok -> System.out.println(String.format("%1$15s\tCP: %2$4d\tIV: %3$4s", //
							PokeDictionary.getDisplayName((int) pok.getPokemonId().getNumber(), Locale.getDefault()), //
							pok.getCp(), //
							pok.getIvInPercentage())));

			// add listener
			// go.addListener(new PokemonListernerImpl());

			// Move to start location (47.0603329,-0.8805762)
			api.setLocation(Constants.START_LATITUDE, Constants.START_LONGITUDE, Constants.START_ALTITUDE);
			// Wait until map is updated for the current location
			try {
				api.getMap().awaitUpdate();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Affiche les éléments à proximité
			Utils.displayNearbyPokemons(api);
			Utils.displayNearbyPokestops(api);
			Utils.displayNearbyGyms(api);
		} catch (RequestFailedException e) {
			Log.e("Main", "Failed to login!", e);
		}
	}

}
