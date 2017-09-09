package com.pokego.bot.utils;


public class Tuple<A, B> {
	
	private final A a;
	
	private final B b;

	private Tuple(A a, B b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public static <A, B> Tuple<A, B> create(A a, B b) {
		return new Tuple<>(a, b);
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}

}
