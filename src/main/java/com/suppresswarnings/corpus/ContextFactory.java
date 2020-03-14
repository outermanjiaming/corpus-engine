package com.suppresswarnings.corpus;

public interface ContextFactory<T> {
	Context<T> getInstance(String userid, String text);
}
