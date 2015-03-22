package com.dinorent.server.entities;

import com.dinorent.server.util.EntityUtils;
import com.dinorent.server.util.Properties;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

public class PasswordResetEntity extends EntityContainer {
	
	public static final String KIND = "PasswordReset";
	
	private static final int VERIFICATION_CODE_CEILING = 10000;

	public PasswordResetEntity(Email email, int verificationCode) {
		super(KIND);
		setEmailAddress(email);
		setVerificationCode(verificationCode);
	}

	public PasswordResetEntity(Entity entity) {
		super(entity);
	}
	
	public Email getEmailAddress() {
		return (Email) mEntity.getProperty(Properties.EMAIL_ADDRESS);
	}
	
	public void setEmailAddress(Email email) {
		mEntity.setProperty(Properties.EMAIL_ADDRESS, email);
	}
	
	public int getVerificationCode() {
		return EntityUtils.getIntProperty(mEntity, Properties.VERIFICATION_CODE);
	}
	
	public void setVerificationCode(int verificationCode) {
		mEntity.setProperty(Properties.VERIFICATION_CODE, verificationCode % VERIFICATION_CODE_CEILING);
	}

	/**
	 * Get the {@link PasswordResetEntity} associated with the given {@link Email} if one exists. If not the returned
	 * object will contain a null {@link Entity}.
	 * 
	 * @param datastore The datastore service to perform the operation.
	 * @param email The {@link Email} to search for.
	 * @return A {@link PasswordResetEntity} associated with the email.
	 */
	public static PasswordResetEntity findPasswordResetEntity(DatastoreService datastore, Email email) {
		Query query = new Query(KIND).setFilter(EntityUtils.getFilterPredicate(email));
		return new PasswordResetEntity(datastore.prepare(query).asSingleEntity());
	}
}
