package com.suppresswarnings.corpus;

public interface ContextFactory<T> {
	public Context<T> getInstance(String userid, String text);
}
