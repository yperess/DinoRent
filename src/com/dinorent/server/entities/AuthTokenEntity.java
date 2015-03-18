package com.dinorent.server.entities;

import java.util.Date;

import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class AuthTokenEntity extends EntityContainer {
	
	public static final String KIND = "AuthToken";
	
	public AuthTokenEntity(String emailAddress, Date date) {
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
	
	public void setEmailAddress(String emailAddress) {
		mEntity.setProperty(Properties.EMAIL_ADDRESS, emailAddress);
	}
	
	public String getEmailAddress() {
		return (String) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}
	
	public static AuthTokenEntity findAuthTokenEntity(DatastoreService datastore, String emailAddress) {
		Query query = new Query(AuthTokenEntity.KIND)
				.setFilter(new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress));
		return new AuthTokenEntity(datastore.prepare(query).asSingleEntity());
	}
	
	public static class InvalidAuthTokenException extends Exception {
		public InvalidAuthTokenException() {
			super("Invalid auth token exception");
		}
	}
	
	public static class AuthTokenExpiredException extends Exception {
		public AuthTokenExpiredException() {
			super("Auth token expired exception");
		}
	}
}
