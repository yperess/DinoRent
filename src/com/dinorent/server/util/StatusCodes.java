package com.dinorent.server.util;

public interface StatusCodes {
	public static final int OK = 0;
	
	// Sign-in status codes.
	public static final int ACCOUNT_NOT_FOUND = -1;
	public static final int ACCOUNT_EMAIL_TAKEN = -2;
	public static final int INCORRECT_VERIFICATION_CODE = -3;
	public static final int INCORRECT_USERNAME_OR_PASSWORD = -4;
}
