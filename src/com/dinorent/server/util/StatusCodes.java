package com.dinorent.server.util;

import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;

public class StatusCodes {
	public static final int OK = 0;
	
	// Error messages.
	public static class InvalidAuthTokenException extends BadRequestException {
		public InvalidAuthTokenException() {
			super("Invalid auth token.");
		}
	}
	
	public static class EmailTakenException extends BadRequestException {
		public EmailTakenException() {
			super("Email already in use.");
		}
	}
	
	public static class IncorrectVerificationCodeException extends BadRequestException {
		public IncorrectVerificationCodeException() {
			super("Incorrect verification code.");
		}
	}
	
	public static class SessionExpiredException extends UnauthorizedException {
		public SessionExpiredException() {
			super("Session expired.");
		}
	}
	
	public static class AccountNotFoundException extends NotFoundException {
		public AccountNotFoundException() {
			super("Account not found.");
		}
	}
	
	public static class GenericInternalError extends InternalServerErrorException {
		public GenericInternalError() {
			super("Internal error.");
		}
	}
}
