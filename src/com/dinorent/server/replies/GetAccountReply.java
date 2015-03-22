package com.dinorent.server.replies;

import com.dinorent.server.entities.AccountEntity;
import com.dinorent.server.util.StatusCodes;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;

public class GetAccountReply extends BaseReply {
	
	@ApiResourceProperty(name = "account")
	private final AccountEntity mAccountEntity;

	public GetAccountReply(int statusCode) {
		super(statusCode);
		mAccountEntity = null;
	}
	
	public GetAccountReply(AccountEntity entity) {
		super(StatusCodes.OK);
		mAccountEntity = entity;
	}

	@ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
	public AccountEntity getAccountEntity() {
		return mAccountEntity;
	}
}
