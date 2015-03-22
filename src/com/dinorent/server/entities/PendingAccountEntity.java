package com.dinorent.server.entities;

import java.util.Date;
import java.util.Random;

import org.apache.http.util.TextUtils;

import com.dinorent.server.util.EntityUtils;
import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class PendingAccountEntity extends EntityContainer {
	
	public static final String KIND = "PendingAccount";
	
	private static final int VERIFICATION_CODE_CEILING = 10000;
	
	public PendingAccountEntity(Email emailAddress, String password, Date timestamp) {
		super(KIND);
		setEmailAddress(emailAddress);
		setPassword(password);
		setTimestamp(timestamp);
		setVerificationCode(new Random(timestamp.getTime()).nextInt(VERIFICATION_CODE_CEILING));
	}
	
	public PendingAccountEntity(Entity entity) {
		super(entity);
	}
	
	public int getVerificationCode() {
		return EntityUtils.getIntProperty(mEntity, Properties.VERIFICATION_CODE);
	}
	
	public void setVerificationCode(int verificationCode) {
		mEntity.setProperty(Properties.VERIFICATION_CODE, verificationCode % VERIFICATION_CODE_CEILING);
	}
	
	public Email getEmailAddress() {
		return (Email) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}
	
	public void setEmailAddress(Email emailAddress) {
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
	
	/**
	 * Find a pending account entity with an optional password filter. If the filter is used (non-empty and non-null)
	 * then it will be used to find the pending account entity. Otherwise the matching pending account entity will be
	 * matched against the email only.
	 * 
	 * @param datastore The datastore object used for the query.
	 * @param emailAddress The email address to search for.
	 * @param password The optional password associated with the account.
	 * @return A {@link PendingAccountEntity} with a non-null {@link Entity} object if the account was found. 
	 */
	public static PendingAccountEntity findPendingAccountEntity(DatastoreService datastore, Email emailAddress,
			String password) {
		Filter filter = null;
		FilterPredicate emailFilter = new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress);
		if (TextUtils.isEmpty(password)) {
			filter = emailFilter;
		} else {
			FilterPredicate passwordFilter = new FilterPredicate(Properties.PASSWORD, FilterOperator.EQUAL, password);
			filter = CompositeFilterOperator.and(emailFilter, passwordFilter);
		}
		
		Query query = new Query(PendingAccountEntity.KIND).setFilter(filter);
		return new PendingAccountEntity(datastore.prepare(query).asSingleEntity());
	}
}
