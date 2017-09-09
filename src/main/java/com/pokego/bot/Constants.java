package com.pokego.bot;

import java.util.Arrays;
import java.util.List;

import POGOProtos.Enums.PokemonFamilyIdOuterClass.PokemonFamilyId;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;

public interface Constants {
	
	// Start location
	public static final double START_LATITUDE = 47.0603329;
	public static final double START_LONGITUDE = -0.8805762;
	public static final double START_ALTITUDE = 1.0; // not used by api
	
	// ordered list of pokestops in Cholet's center
	public static final List<String> ORDERED_POKESTOP_NAMES = Arrays.asList( //
			"Impasse des Charuelles", //
			"Hôtel de ville de Cholet", //
			"Earth ", // space at the end
			"Iron Minotorus", //
			"Mars", //
			"Musée D'art et D'histoire", //
			"Le Menhir", //
			"La Tour Du Mail", //
			"Parc Du Mail", //
			"Cholet, Le Camion De Pompiers", //
			"Sun", //
			"La Caverne", //
			"Plaque Du Mail", //
			"Arbre À Girouettes", //
			"Rue des Marteaux", //
			"Mosaïque Mural ", // space at the end
			"Le Fief De Vigne", //
			"Arcades Rougé", //
			"Cholet, Citation D'Heraclite", //
			"Le Mur Aux Livres", //
			"Inscription Claude Roy", //
			"Grand Café", //
			"Manège Travot", //
			"Bureau de poste, Place Travot"
			);
	
	// Best pokemon families
	// only these families will be powered up
	public static final List<PokemonFamilyId> POKEMON_FAMILY_TO_KEEP = Arrays.asList(
			// starters
			PokemonFamilyId.FAMILY_BULBASAUR, //
			PokemonFamilyId.FAMILY_CHARMANDER, //
			PokemonFamilyId.FAMILY_SQUIRTLE, //
			PokemonFamilyId.FAMILY_CHIKORITA, //
			PokemonFamilyId.FAMILY_CYNDAQUIL, //
			PokemonFamilyId.FAMILY_TOTODILE, //
			// very strong
			PokemonFamilyId.FAMILY_LARVITAR, //
			PokemonFamilyId.FAMILY_DRATINI, //
			PokemonFamilyId.FAMILY_MAGIKARP, //
			// strong
			PokemonFamilyId.FAMILY_EEVEE, //
			PokemonFamilyId.FAMILY_GASTLY, //
			PokemonFamilyId.FAMILY_MACHOP, //
			PokemonFamilyId.FAMILY_GEODUDE, //
			PokemonFamilyId.FAMILY_RHYHORN, //
			PokemonFamilyId.FAMILY_TEDDIURSA, //
			PokemonFamilyId.FAMILY_EXEGGCUTE, // noeunoeuf
			PokemonFamilyId.FAMILY_PHANPY, //
			PokemonFamilyId.FAMILY_ABRA, //
			PokemonFamilyId.FAMILY_GROWLITHE, // caninos
			// strong with no evolution
			PokemonFamilyId.FAMILY_LAPRAS, //
			PokemonFamilyId.FAMILY_HERACROSS, //
			PokemonFamilyId.FAMILY_DITTO, //
			PokemonFamilyId.FAMILY_SNORLAX, //
			// defense
			PokemonFamilyId.FAMILY_CHANSEY, //
			PokemonFamilyId.FAMILY_JIGGLYPUFF, //
			PokemonFamilyId.FAMILY_CLEFAIRY, //
			// legendary
			PokemonFamilyId.FAMILY_CELEBI, //
			PokemonFamilyId.FAMILY_HO_OH, //
			PokemonFamilyId.FAMILY_LUGIA, //
			PokemonFamilyId.FAMILY_MEW, //
			PokemonFamilyId.FAMILY_MEWTWO, //
			PokemonFamilyId.FAMILY_ENTEI, //
			PokemonFamilyId.FAMILY_RAIKOU, //
			PokemonFamilyId.FAMILY_SUICUNE, //
			PokemonFamilyId.FAMILY_MOLTRES, //
			PokemonFamilyId.FAMILY_ZAPDOS, //
			PokemonFamilyId.FAMILY_ARTICUNO);
	
	// these pokemons will be evolved when catched if we have enough candies
	public static final List<PokemonId> POKEMON_ID_EXP = Arrays.asList(
			PokemonId.RATTATA, //
			PokemonId.CATERPIE, // chenipan
			PokemonId.SENTRET, // fouinette
			PokemonId.HOOTHOOT, //
			PokemonId.NATU, //
			PokemonId.KRABBY, //
			PokemonId.EKANS, // abo
			PokemonId.GOLDEEN, // poissirène
			PokemonId.MARILL, // 
			PokemonId.ZUBAT, // nosferapti
			PokemonId.SPEAROW, // piafabec
			PokemonId.STARYU, // stari
			PokemonId.PIDGEY, // roucool
			PokemonId.SPINARAK, // mimigal
			PokemonId.LEDYBA // coxy
			);
	

}
