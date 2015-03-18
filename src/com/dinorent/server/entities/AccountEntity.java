package com.dinorent.server.entities;

import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class AccountEntity extends EntityContainer {
	
	public static final String KIND = "Account";
	
	/**
	 * Account types are a bit mask using one or more of the following constants.
	 */
	public interface AccountType {
		public static final int BASIC = 0;
		public static final int LANDLORD = 1;
		public static final int REALTOR = 2;
		public static final int PROPERTY_MANAGER = 4;
	}
	
	public AccountEntity(String emailAddress, String password) {
		super(KIND);
		setEmailAddress(emailAddress);
		setPassword(password);
		setAccountType(AccountType.BASIC);
	}
	
	public AccountEntity(Entity accountEntity) {
		super(accountEntity);
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

	public String getName() {
		return (String) mEntity.getProperty(Properties.NAME);
	}

	public void setName(String name) {
		mEntity.setProperty(Properties.NAME, name);
	}

	public int getAccountType() {
		return (Integer) mEntity.getProperty(Properties.ACCOUNT_TYPE);
	}

	public void setAccountType(int accountType) {
		mEntity.setProperty(Properties.ACCOUNT_TYPE, accountType);
	}

	public String getPhoneNumber() {
		return (String) mEntity.getProperty(Properties.PHONE_NUMBER);
	}

	public void setPhoneNumber(String phoneNumber) {
		mEntity.setProperty(Properties.PHONE_NUMBER, phoneNumber);
	}
	
	/**
	 * Find (if exists) the account entity matching the given email address.
	 * 
	 * @param datastore The data store to perform the query with.
	 * @param emailAddress The email address to find.
	 * @return The {@link AccountEntity} object representing the account (may contain a null entity if account wasn't
	 * 		found).
	 */
	public static AccountEntity findAccountEntity(DatastoreService datastore, String emailAddress) {
		Query query = new Query(AccountEntity.KIND)
				.setFilter(new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress));
		return new AccountEntity(datastore.prepare(query).asSingleEntity());
	}
}
