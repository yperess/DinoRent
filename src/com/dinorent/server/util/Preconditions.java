package com.dinorent.server.util;

public class Preconditions {
	
	/**
	 * Tests to make sure that a given argument isn't null.
	 * 
	 * @param arg The argument to test.
	 * @return The parameter argument if it wasn't null.
	 */
	public static <T> T checkNotNull(T arg) {
		if (arg == null) {
			throw new IllegalArgumentException();
		}
		return arg;
	}
}
