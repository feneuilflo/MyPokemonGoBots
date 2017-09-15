package com.pokego.bot.listener;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.TutorialListener;
import com.pokegoapi.api.player.Avatar;
import com.pokegoapi.api.player.PlayerAvatar;
import com.pokegoapi.api.player.PlayerGender;
import com.pokegoapi.api.pokemon.StarterPokemon;

import POGOProtos.Enums.TeamColorOuterClass.TeamColor;

public class TutorialListenerImpl implements TutorialListener {
	
	private final String name;
	private final StarterPokemon starter;
	private final TeamColor team;
	
	public TutorialListenerImpl(String name, StarterPokemon starter, TeamColor team) {
		super();
		this.name = name;
		this.starter = starter;
		this.team = team;
	}

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

	@Override
	public TeamColor chooseTeamColor(PokemonGo api) {
		return TeamColor.YELLOW;
	}

}
