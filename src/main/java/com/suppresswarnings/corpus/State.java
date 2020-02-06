package com.suppresswarnings.corpus;

import java.util.function.BiFunction;

@FunctionalInterface
public interface State<T> extends BiFunction<String, T, State<T>> {
}
