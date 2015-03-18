package com.dinorent.server.replies;

import com.google.api.server.spi.config.ApiResourceProperty;

public class BaseReply {

	private final int mStatusCode;
	
	public BaseReply(int statusCode) {
		mStatusCode = statusCode;
	}

	@ApiResourceProperty(name = "statusCode")
	public int getStatusCode() {
		return mStatusCode;
	}
}
