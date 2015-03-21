package com.dinorent.server.entities;

import com.dinorent.server.util.EntityUtils;
import com.dinorent.server.util.Properties;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.PostalAddress;
import com.google.appengine.api.datastore.PhoneNumber;
import com.google.appengine.api.datastore.Email;

/**
 * Container for all the account properties.
 */
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
	
	public AccountEntity(Email emailAddress, String password) {
		super(KIND);
		setEmailAddress(emailAddress);
		setPassword(password);
		setAccountType(AccountType.BASIC);
	}
	
	public AccountEntity(Entity accountEntity) {
		super(accountEntity);
	}

	@ApiResourceProperty(name = "emailAddress")
	public Email getEmailAddress() {
		return (Email) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}

	public void setEmailAddress(Email emailAddress) {
		mEntity.setProperty(Properties.EMAIL_ADDRESS, emailAddress);
	}
	
	@ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
	public String getPassword() {
		return (String) mEntity.getProperty(Properties.PASSWORD);
	}

	public void setPassword(String password) {
		mEntity.setProperty(Properties.PASSWORD, password);
	}

	@ApiResourceProperty(name = "name")
	public String getName() {
		return (String) mEntity.getProperty(Properties.NAME);
	}

	public void setName(String name) {
		mEntity.setProperty(Properties.NAME, name);
	}

	@ApiResourceProperty(name = "accountType")
	public int getAccountType() {
		return EntityUtils.getIntProperty(mEntity, Properties.ACCOUNT_TYPE);
	}

	public void setAccountType(int accountType) {
		mEntity.setProperty(Properties.ACCOUNT_TYPE, accountType);
	}

	@ApiResourceProperty(name = "phoneNumber")
	public PhoneNumber getPhoneNumber() {
		return (PhoneNumber) mEntity.getProperty(Properties.PHONE_NUMBER);
	}

	public void setPhoneNumber(PhoneNumber phoneNumber) {
		mEntity.setProperty(Properties.PHONE_NUMBER, phoneNumber);
	}
	
	@ApiResourceProperty(name = "address")
	public PostalAddress getAddress() {
		return (PostalAddress) mEntity.getProperty(Properties.ADDRESS);
	}
	
	public void setAddress(PostalAddress address) {
		mEntity.setProperty(Properties.ADDRESS, address);
	}
	
	/**
	 * Find (if exists) the account entity matching the given email address.
	 * 
	 * @param datastore The data store to perform the query with.
	 * @param emailAddress The {@link Email} address to find.
	 * @return The {@link AccountEntity} object representing the account (may contain a null entity if account wasn't
	 * 		found).
	 */
	public static AccountEntity findAccountEntity(DatastoreService datastore, Email emailAddress) {
		Query query = new Query(AccountEntity.KIND).setFilter(EntityUtils.getFilterPredicate(emailAddress));
		return new AccountEntity(datastore.prepare(query).asSingleEntity());
	}
}
