package com.dinorent.server.entities;

import java.util.Date;
import java.util.Random;

import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class PendingAccountEntity extends EntityContainer {
	
	public static final String KIND = "PendingAccount";
	
	public PendingAccountEntity(String emailAddress, String password, Date timestamp) {
		super(KIND);
		setEmailAddress(emailAddress);
		setPassword(password);
		setTimestamp(timestamp);
		setNewVerificationCode();
	}
	
	public PendingAccountEntity(Entity entity) {
		super(entity);
	}
	
	public int getVerificationCode() {
		Object obj = mEntity.getProperty(Properties.VERIFICATION_CODE);
		if (obj instanceof Integer) {
			return ((Integer) obj).intValue();
		} else if (obj instanceof Long) {
			return ((Long) obj).intValue();
		}
		throw new IllegalStateException("Invalid verification code in entity");
	}
	
	public void setNewVerificationCode() {
		Random rand = new Random(System.currentTimeMillis());
		mEntity.setProperty(Properties.VERIFICATION_CODE, rand.nextInt() % 10000);
	}
	
	public String getEmailAddress() {
		return (String) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}
	
	public void setEmailAddress(String emailAddress) {
		mEntity.setProperty(Properties.EMAIL_ADDRESS, emailAddress);
	}
	
	public String getPassword() {
		return (String) mEntity.getProperty(Properties.PASSWORD);
	}
	
	public void setPassword(String password) {
		mEntity.setProperty(Properties.PASSWORD, password);
	}
	
	public Date getTimestamp() {
		return (Date) mEntity.getProperty(Properties.TIMESTAMP);
	}
	
	public void setTimestamp(Date timestamp) {
		mEntity.setProperty(Properties.TIMESTAMP, timestamp);
	}
	
	public static PendingAccountEntity findPendingAccountEntity(DatastoreService datastore, String emailAddress) {
		Query query = new Query(PendingAccountEntity.KIND)
				.setFilter(new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress));
		return new PendingAccountEntity(datastore.prepare(query).asSingleEntity());
	}
}
