package com.dinorent.server.entities;

import java.util.Date;

import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class AuthTokenEntity extends EntityContainer {
	
	public static final String KIND = "AuthToken";
	
	public AuthTokenEntity(Email emailAddress, Date date) {
		super(KIND);
		setTimestamp(date);
		setEmailAddress(emailAddress);
	}
	
	public AuthTokenEntity(Entity authTokenEntity) {
		super(authTokenEntity);
	}
	
	public void setTimestamp(Date date) {
		mEntity.setProperty(Properties.TIMESTAMP, date);
	}

	public Date getTimestamp() {
		return (Date) mEntity.getProperty(Properties.TIMESTAMP);
	}
	
	public void setEmailAddress(Email emailAddress) {
		mEntity.setProperty(Properties.EMAIL_ADDRESS, emailAddress);
	}
	
	public Email getEmailAddress() {
		return (Email) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}
	
	public static AuthTokenEntity findAuthTokenEntity(DatastoreService datastore, Email emailAddress) {
		Query query = new Query(AuthTokenEntity.KIND)
				.setFilter(new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress));
		return new AuthTokenEntity(datastore.prepare(query).asSingleEntity());
	}
}
