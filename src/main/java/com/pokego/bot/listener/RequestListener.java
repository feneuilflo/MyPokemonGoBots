package com.pokego.bot.listener;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.RequestInterceptor;
import com.pokegoapi.main.ServerRequest;
import com.pokegoapi.main.ServerRequestEnvelope;
import com.pokegoapi.main.ServerResponse;

public class RequestListener implements RequestInterceptor {

	@Override
	public boolean shouldRemove(PokemonGo api, ServerRequest request, ServerRequestEnvelope envelope) {
		return false;
	}

	@Override
	public ServerRequest adaptRequest(PokemonGo api, ServerRequest request, ServerRequestEnvelope envelope) {
		System.out.println("RequestListener: adaptRequest " + request.getType());
		return null;
	}

	@Override
	public void handleResponse(PokemonGo api, ServerResponse response, ServerRequestEnvelope request) {
		System.out.println("RequestListener: handleResponse ");
	}

}
