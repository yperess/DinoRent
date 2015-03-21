package com.dinorent.server;

import java.util.Date;
import java.util.Random;

import com.dinorent.server.entities.AccountEntity;
import com.dinorent.server.entities.AuthTokenEntity;
import com.dinorent.server.entities.PasswordResetEntity;
import com.dinorent.server.entities.PendingAccountEntity;
import com.dinorent.server.replies.BaseReply;
import com.dinorent.server.replies.GetAccountReply;
import com.dinorent.server.replies.SignInReply;
import com.dinorent.server.util.StatusCodes;
import com.dinorent.server.util.StatusCodes.AccountNotFoundException;
import com.dinorent.server.util.StatusCodes.EmailTakenException;
import com.dinorent.server.util.StatusCodes.InvalidAuthTokenException;
import com.dinorent.server.util.StatusCodes.SessionExpiredException;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PhoneNumber;
import com.google.appengine.api.datastore.PostalAddress;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.apache.http.util.TextUtils;

@Api (
	version = "v1",
	description = "Accounts API",
	name = "accounts"
)
public class AccountsApi {
	
	/** Lifespan of an Auth token (5 minutes in milliseconds). */
	public static final long AUTH_TOKEN_LIFE = 5l * 60l * 1000l;
	
	/** Lifespan of a pending account (180 days in milliseconds). */
	public static final long PENDING_ACCOUNT_LIFE = 180l * 24l * 60l * 1000l;
    
	/**
	 * Sign into an existing account.
	 * 
	 * @param emailAddress The email address of the account.
	 * @param password The password of the account.
	 * @return {@link SignInReply} with the account and auth token.
	 * @throws StatusCodes.AccountNotFoundException If the account was not found or the password did not match.
	 */
    @ApiMethod(name = "signIn")
    public SignInReply signIn(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password) throws StatusCodes.AccountNotFoundException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	
    	// Try to find the account first.
    	AccountEntity account = AccountEntity.findAccountEntity(datastore, email);
    	if (account.getEntity() == null) {
    		// Account wasn't found, return an empty response.
    		throw new StatusCodes.AccountNotFoundException();
    	}

    	// Create Auth Token and attach to account.
    	AuthTokenEntity authToken = new AuthTokenEntity(email, getDate());
    	datastore.put(authToken.getEntity());
    	
    	return new SignInReply(StatusCodes.OK, account, authToken);
    }
    
    /**
     * Create a new account. If all goes well, this method will send an email with a verification code to complete the
     * sign-up flow via the {@link #validateAccount(String, String, int)} method.
     * 
     * @param emailAddress The email address to create the account for.
     * @param password The password to use for the account.
     * @return {@link BaseReply} with status code {@link StatusCodes#OK} if everything went OK.
     * @throws EmailTakenException If the requested email address was already taken.
     */
    @ApiMethod(name = "createAccount")
    public BaseReply createAccount(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password) throws EmailTakenException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	PendingAccountEntity pendingAccount;
    	Date now = getDate();
    	
    	// Check that the email is not already in use.
    	if (AccountEntity.findAccountEntity(datastore, email).getEntity() != null) {
    		// Account was already in use, bail.
    		throw new StatusCodes.EmailTakenException();
    	}
    	// We want any pending accounts with that email address so use null for password.
		pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, email, null /* password */);
		if (pendingAccount.getEntity() != null) {
    		// Pending account already exists for this email.
			long age = now.getTime() - pendingAccount.getTimestamp().getTime();
			if (age > PENDING_ACCOUNT_LIFE) {
				// Pending account is very old, delete it and allow user to create a new account with same email.
				datastore.delete(pendingAccount.getEntity().getKey());
			} else {
	    		throw new StatusCodes.EmailTakenException();
			}
		}
    	
    	// Create a pending account.
    	pendingAccount = new PendingAccountEntity(email, password, getDate());
    	datastore.put(pendingAccount.getEntity());
    	
    	// Send an e-mail to the given email with the verification code.
    	sendVerificationEmail(pendingAccount);
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * Validate a pending account. For this method to succeed it must be called after
     * {@link #createAccount(String, String)} and use the same email/password along with the verification code that was
     * sent to the user's email address.
     * 
     * @param emailAddress The email address of the account to validate.
     * @param password The account's password.
     * @param verificationCode The verification code sent to the user during the creation step.
     * @return {@link SignInReply} with the account's properties if succeeded.
     * @throws StatusCodes.AccountNotFoundException If the pending account wasn't found.
     * @throws StatusCodes.IncorrectVerificationCodeException If the verification code was incorrect.
     */
    @ApiMethod(name = "validateAccount")
    public SignInReply validateAccount(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password,
    		@Named("verificationCode") int verificationCode) throws StatusCodes.AccountNotFoundException,
    				StatusCodes.IncorrectVerificationCodeException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	
    	// Find the pending account entity.
    	PendingAccountEntity pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, email, password);
    	if (pendingAccount.getEntity() == null) {
    		throw new StatusCodes.AccountNotFoundException();
    	} else if (pendingAccount.getVerificationCode() != verificationCode) {
    		throw new StatusCodes.IncorrectVerificationCodeException();
    	}
    	
    	// Pending account found. Remove it and create a real account.
    	AccountEntity account = new AccountEntity(email, password);
    	datastore.delete(pendingAccount.getEntity().getKey());
    	datastore.put(account.getEntity());
    	return signIn(emailAddress, password);
    }
    
    /**
     * Delete the currently signed-in account.
     * 
     * @param authToken The auth token given to the user upon a successful sign-in.
     * @return {@link BaseReply} with status code {@link StatusCodes#OK} if everything went OK.
     * @throws AccountNotFoundException If the auth token points to an invalid account.
     * @throws InvalidAuthTokenException If the auth token is malformed.
     * @throws SessionExpiredException If the auth token expired.
     */
    @ApiMethod(name = "deleteAccount")
    public BaseReply deleteAccount(@Named("authToken") String authToken) throws AccountNotFoundException,
    		InvalidAuthTokenException, SessionExpiredException {
    	GetAccountReply account = getAccount(authToken);
    	getDatastore().delete(account.getAccountEntity().getEntity().getKey());
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * Create a new validation code for a pending account. For this method to succeed, an account must have been created
     * using the {@link #createAccount(String, String)} method and not yet validated via
     * {@link #validateAccount(String, String, int)}.
     * 
     * @param emailAddress The email address of the account.
     * @param password The password set during the account creation.
     * @return {@link BaseReply} with status code {@link StatusCodes#OK} if everything went OK.
     * @throws StatusCodes.AccountNotFoundException If the account was not found in the pending accounts table.
     */
    @ApiMethod(name = "regenerateValidationCode")
    public BaseReply regenerateValidationCode(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password) throws StatusCodes.AccountNotFoundException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	PendingAccountEntity pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, email, password);
    	if (pendingAccount.getEntity() == null) {
    		throw new StatusCodes.AccountNotFoundException();
    	}
    	pendingAccount.setVerificationCode(new Random(getDate().getTime()).nextInt());
    	datastore.put(pendingAccount.getEntity());
    	sendVerificationEmail(pendingAccount);
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * Sign out an account, never fails unless the auth token is malformed.
     * 
     * @param authToken The auth token given to the user upon a successful sign-in.
     * @return {@link BaseReply} with status code {@link StatusCodes#OK}.
     */
    @ApiMethod(name = "signOut")
    public BaseReply signOut(@Named("authToken") String authToken) {
    	getDatastore().delete(KeyFactory.stringToKey(authToken));
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * Get the account properties.
     * 
     * @param authToken The auth token given to the user upon a successful sign-in.
     * @return A {@link GetAccountReply} with the account properties.
     * @throws AccountNotFoundException If the account to change wasn't found.
     * @throws InvalidAuthTokenException If the auth token is not a valid token.
     * @throws SessionExpiredException If the auth token expired.
     */
    @ApiMethod(name = "getAccount")
    public GetAccountReply getAccount(@Named("authToken") String authToken) throws AccountNotFoundException,
    		InvalidAuthTokenException, SessionExpiredException {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	AuthTokenEntity authTokenEntity = validateAuthToken(datastore, authToken, getDate());
    	AccountEntity account = AccountEntity.findAccountEntity(datastore, authTokenEntity.getEmailAddress());
    	if (account.getEntity() == null) {
    		throw new StatusCodes.AccountNotFoundException();
    	}
    	return new GetAccountReply(account);
    }
    
    /**
     * Override one or more properties of an account.
     * 
     * @param authToken The auth token given to the user upon a successful sign-in.
     * @param password The new password to set, null if no change.
     * @param name The new name associated with the account, null if no change.
     * @param accountType The new account type, null if no change.
     * @param phoneNumber The new phone number, null if no change.
     * @param address The new address, null if no change.
     * @return A {@link GetAccountReply} with the new account properties.
     * @throws AccountNotFoundException If the account to change wasn't found.
     * @throws InvalidAuthTokenException If the auth token is not a valid token.
     * @throws SessionExpiredException If the auth token expired.
     */
    @ApiMethod(name = "modifyAccount")
    public GetAccountReply modifyAccount(
    		@Named("authToken") String authToken,
    		@Nullable @Named("password") String password,
    		@Nullable @Named("name") String name,
    		@Nullable @Named("accountType") Integer accountType,
    		@Nullable @Named("phoneNumber") String phoneNumber,
    		@Nullable @Named("address") String address) throws AccountNotFoundException, InvalidAuthTokenException,
    				SessionExpiredException {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	GetAccountReply reply = getAccount(authToken);
    	if (reply.getStatusCode() != StatusCodes.OK) {
    		return reply;
    	}
    	
    	// Make the account modifications.
    	AccountEntity account = reply.getAccountEntity();
    	if (password != null) {
    		account.setPassword(password);
    	}
    	if (name != null) {
    		account.setName(name);
    	}
    	if (accountType != null) {
    		account.setAccountType(accountType);
    	}
    	if (phoneNumber != null) {
    		account.setPhoneNumber(new PhoneNumber(phoneNumber));
    	}
    	if (address != null) {
    		account.setAddress(new PostalAddress(address));
    	}
    	
		datastore.put(account.getEntity());
    	return reply;
    }
    
    /**
     * Initiates the password reset steps. When this method is called and completes successfully, a verification code
     * will be associated with the account and emailed to the account holder. The account holder will then be able to
     * use the code to create a new password via the {@link #resetPassword(String, int, String)} method.
     * 
     * @param emailAddress The email address to reset the password for.
     * @return A {@link BaseReply} with status code {@link StatusCodes#OK} if everything went OK.
     * @throws AccountNotFoundException If the email is not associated with an account.
     */
    @ApiMethod(name = "resetPasswordRequest")
    public BaseReply resetPasswordRequest(@Named("emailAddress") String emailAddress) throws AccountNotFoundException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	
    	// Check that the account exists.
    	AccountEntity account = AccountEntity.findAccountEntity(datastore, email);
    	if (account.getEntity() == null) {
    		throw new StatusCodes.AccountNotFoundException();
    	}
    	
    	// Check if a reset entity already exists.
    	int verificationCode = new Random(getDate().getTime()).nextInt();
    	PasswordResetEntity passwordReset = PasswordResetEntity.findPasswordResetEntity(datastore, email);
    	if (passwordReset.getEntity() == null) {
    		// Create a new reset entity.
    		passwordReset = new PasswordResetEntity(email, verificationCode); 
    	} else {
    		// Update the reset entity with a new verification code.
    		passwordReset.setVerificationCode(verificationCode);
    	}
    	datastore.put(passwordReset.getEntity());
    	
    	sendVerificationEmail(passwordReset);
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * Resets the password associated with a given account. This method must only be called after a successful call to
     * {@link #resetPasswordRequest(String)}.
     * 
     * @param emailAddress The email address to reset the password to.
     * @param verificationCode The verification code generated by {@link #resetPasswordRequest(String)} and sent to the
     * 			  correct email address.
     * @param password The new password to set for the account.
     * @return {@link BaseReply} with a status code {@link StatusCodes#OK} if everything was successful.
     * @throws StatusCodes.AccountNotFoundException If the email is not associated with an existing account.
     * @throws StatusCodes.GenericInternalError If there is no record of the account having requested a password reset.
     * @throws StatusCodes.IncorrectVerificationCodeException If the verification code used is incorrect.
     */
    @ApiMethod(name = "resetPassword")
    public BaseReply resetPassword(
    		@Named("emailAddress") String emailAddress,
    		@Named("verificationCode") int verificationCode,
    		@Named("password") String password) throws StatusCodes.AccountNotFoundException,
    				StatusCodes.GenericInternalError, StatusCodes.IncorrectVerificationCodeException {
    	Email email = new Email(emailAddress);
    	DatastoreService datastore = getDatastore();
    	
    	// Check that the account exists.
    	AccountEntity account = AccountEntity.findAccountEntity(datastore, email);
    	if (account.getEntity() == null) {
    		throw new StatusCodes.AccountNotFoundException();
    	}
    	
    	// Find the reset entity.
    	PasswordResetEntity passwordReset = PasswordResetEntity.findPasswordResetEntity(datastore, email);
    	if (passwordReset.getEntity() == null) {
    		throw new StatusCodes.GenericInternalError();
    	} else if (passwordReset.getVerificationCode() != verificationCode) {
    		throw new StatusCodes.IncorrectVerificationCodeException();
    	}
    	
    	// Everything is fine, update the password.
    	account.setPassword(password);
    	datastore.put(account.getEntity());
    	return new BaseReply(StatusCodes.OK);
    }
    
    /**
     * @return An instance of the {@link DatastoreService}.
     */
    protected final DatastoreService getDatastore() {
    	return DatastoreServiceFactory.getDatastoreService();
    }
    
    /**
     * Overridable for testing purposes.
     * 
     * @return The current date/time in a {@link Date} object.
     */
    protected Date getDate() {
    	return new Date();
    }
    
    /**
     * Overridable for testing purposes. Send an email with the verification code to the user.
     * 
     * @param pendingAccount The pending account that should receive the verification code email.
     */
    protected void sendVerificationEmail(PendingAccountEntity pendingAccount) {
    	// TODO - send an email with the verification code.
    }
    
    /**
     * Overridable for testing purposes. Send an email with the verification code to the user.
     * 
     * @param passwordReset The password reset entity containing the email that should receive the verification code.
     */
    protected void sendVerificationEmail(PasswordResetEntity passwordReset) {
    	// TODO - send an email with the verification code.
    }
    
    public static AuthTokenEntity validateAuthToken(DatastoreService datastore, String authToken, Date now) throws
    		StatusCodes.InvalidAuthTokenException, StatusCodes.SessionExpiredException {
    	if (TextUtils.isEmpty(authToken)) {
    		throw new StatusCodes.InvalidAuthTokenException();
    	}
    	
		try {
			AuthTokenEntity authTokenEntity = new AuthTokenEntity(datastore.get(KeyFactory.stringToKey(authToken)));
			Date timestamp = authTokenEntity.getTimestamp();
			// Check how old the entry is, if it's new enough just update the timestamp and keep going. Too old and
			// we fail forcing the user to sign in again.
			if (now.getTime() - timestamp.getTime() > AUTH_TOKEN_LIFE) {
				// Auth expired, delete the entity.
				datastore.delete(authTokenEntity.getEntity().getKey());
				throw new StatusCodes.SessionExpiredException();
			} else {
				// Update auth timestamp.
				authTokenEntity.setTimestamp(now);
				datastore.put(authTokenEntity.getEntity());
			}
			return authTokenEntity;
		} catch (EntityNotFoundException ex) {
			throw new StatusCodes.InvalidAuthTokenException();
		}
    }
    
    public static AccountEntity getAccountFromAuthToken(DatastoreService datastore, String authToken) throws
    		ServiceException {
    	return getAccountFromAuthToken(datastore, authToken, new Date());
    }
    
    public static AccountEntity getAccountFromAuthToken(DatastoreService datastore, String authToken, Date date) throws
    		ServiceException {
    	AuthTokenEntity authTokenEntity = validateAuthToken(datastore, authToken, date);
    	AccountEntity account = AccountEntity.findAccountEntity(datastore, authTokenEntity.getEmailAddress());
    	if (account.getEntity() == null) {
    		return null;
    	}
    	return account;
    }
}
