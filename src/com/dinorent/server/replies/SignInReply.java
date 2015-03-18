package com.dinorent.server.replies;

import com.dinorent.server.entities.AccountEntity;
import com.dinorent.server.entities.AuthTokenEntity;
import com.google.api.server.spi.config.ApiResourceProperty;

public class SignInReply extends BaseReply {
	
	private final String mAuthToken;

	public SignInReply(int statusCode) {
		super(statusCode);
		mAuthToken = null;
	}
	
	public SignInReply(int statusCode, AccountEntity account, AuthTokenEntity authToken) {
		super(statusCode);
		mAuthToken = authToken.getKeyString();
	}

	@ApiResourceProperty(name = "authToken")
	public String getAuthToken() {
		return mAuthToken;
	}
}
