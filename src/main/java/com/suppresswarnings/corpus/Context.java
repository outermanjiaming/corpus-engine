package com.suppresswarnings.corpus;

import java.util.Random;
import java.util.function.Function;

public abstract class Context<T> implements Function<String, String>{
	T handler;
	State<Context<T>> state;
	StringBuffer buffer;
	Random random = new Random();
	public Context(T handler) {
		this.handler = handler;
		this.buffer = new StringBuffer();
	}
	public T handler() {
		return handler;
	}
	public void state(State<Context<T>> state) {
		this.state = state;
	}
	public State<Context<T>> state() {
		return this.state;
	}
	public int random(int bound) {
		return random.nextInt(bound);
	}
	public String output() {
		String out = buffer.toString();
		buffer.setLength(0);
		return out;
	}
	public void output(String output) {
		if(this.buffer.length() > 0) {
			this.buffer.append(System.lineSeparator());
		}
		this.buffer.append(output);
	}
	
	@Override
	public String apply(String input) {
		state = state.apply(input, this);
		return output();
	}
	
	public void log(String info) {
		System.out.println(info);
	}
}
