package com.dinorent.server;

import java.util.Date;

import com.dinorent.server.entities.AccountEntity;
import com.dinorent.server.entities.AuthTokenEntity;
import com.dinorent.server.entities.AuthTokenEntity.AuthTokenExpiredException;
import com.dinorent.server.entities.AuthTokenEntity.InvalidAuthTokenException;
import com.dinorent.server.entities.PendingAccountEntity;
import com.dinorent.server.replies.CreateAccountReply;
import com.dinorent.server.replies.SignInReply;
import com.dinorent.server.replies.ValidateAccountReply;
import com.dinorent.server.util.Properties;
import com.dinorent.server.util.StatusCodes;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

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
	
	public class Foo {
		private String mFoo = "foo";
		
		@ApiResourceProperty(name = "content")
		public String getFoo() {
			return mFoo;
		}

		public void setFoo(String foo) {
			mFoo = foo;
		}
	}

    @ApiMethod(name = "foo")
	public Foo foo() {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	Entity entity = datastore.prepare(new Query("Foo")).asSingleEntity();
    	if (entity == null) {
    		entity = new Entity("Foo");
    		entity.setProperty("content", "foo");
    	} else {
    		String content = (String) entity.getProperty("content");
    		entity.setProperty("content", content + ".foo");
    	}
    	datastore.put(entity);
    	Foo foo = new Foo();
    	foo.setFoo(KeyFactory.keyToString(entity.getKey()) + ": " + entity.getProperty("content"));
		return foo;
	}
    
    @ApiMethod(name = "bar")
    public Foo bar() {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	Foo foo = new Foo();
    	foo.setFoo("Entity not found");
    	Entity entity = datastore.prepare(new Query("Foo")).asSingleEntity();
    	if (entity != null) {
    		datastore.delete(entity.getKey());
    		foo.setFoo("Entity removed: " + KeyFactory.keyToString(entity.getKey()));
    	}
    	return foo;
    }
    
    @ApiMethod(name = "signIn")
    public SignInReply signIn(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	
    	// Try to find the account first.
    	Query query = new Query(AccountEntity.KIND)
    			.setFilter(new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, emailAddress));
    	Entity entity = datastore.prepare(query).asSingleEntity();
    	if (entity == null) {
    		// Account wasn't found, return an empty response.
    		return new SignInReply(StatusCodes.ACCOUNT_NOT_FOUND);
    	}
    	
    	AccountEntity account = new AccountEntity(entity);

    	// Create Auth Token and attach to account.
    	AuthTokenEntity authToken = new AuthTokenEntity(emailAddress, getDate());
    	datastore.put(authToken.getEntity());
    	
    	return new SignInReply(StatusCodes.OK, account, authToken);
    }
    
    @ApiMethod(name = "createAccount")
    public CreateAccountReply createAccount(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	PendingAccountEntity pendingAccount;
    	Date now = getDate();
    	
    	// Check that the email is not already in use.
    	if (AccountEntity.findAccountEntity(datastore, emailAddress).getEntity() != null) {
    		// Account was already in use, bail.
    		return new CreateAccountReply(StatusCodes.ACCOUNT_EMAIL_TAKEN);
    	}
		pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, emailAddress);
		if (pendingAccount.getEntity() != null) {
    		// Pending account already exists for this email.
			long age = now.getTime() - pendingAccount.getTimestamp().getTime();
			if (age > PENDING_ACCOUNT_LIFE) {
				// Pending account is very old, delete it and allow user to create a new account with same email.
				datastore.delete(pendingAccount.getEntity().getKey());
			} else {
				return new CreateAccountReply(StatusCodes.ACCOUNT_EMAIL_TAKEN);
			}
		}
    	
    	// Create a pending account.
    	pendingAccount = new PendingAccountEntity(emailAddress, password, getDate());
    	datastore.put(pendingAccount.getEntity());
    	
    	// Send an e-mail to the given email with the verification code.
    	
    	return new CreateAccountReply(StatusCodes.OK);
    }
    
    @ApiMethod(name = "validateAccount")
    public ValidateAccountReply validateAccount(
    		@Named("emailAddress") String emailAddress,
    		@Named("password") String password,
    		@Named("verificationCode") int verificationCode) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	
    	// Find the pending account entity.
    	PendingAccountEntity pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, emailAddress);
    	if (pendingAccount.getEntity() == null) {
    		return new ValidateAccountReply(StatusCodes.ACCOUNT_NOT_FOUND);
    	} else if (pendingAccount.getVerificationCode() != verificationCode) {
    		return new ValidateAccountReply(StatusCodes.INCORRECT_VERIFICATION_CODE);
    	} else if (!pendingAccount.getPassword().equals(password)) {
    		return new ValidateAccountReply(StatusCodes.INCORRECT_USERNAME_OR_PASSWORD);
    	}
    	
    	// Pending account found. Remove it and create a real account.
    	AccountEntity account = new AccountEntity(emailAddress, password);
    	datastore.delete(pendingAccount.getEntity().getKey());
    	datastore.put(account.getEntity());
    	return new ValidateAccountReply(StatusCodes.OK);
    }
    
    /**
     * Overridable for testing purposes.
     * 
     * @return The current date/time in a {@link Date} object.
     */
    protected Date getDate() {
    	return new Date();
    }
    
    public static void validateAuthToken(DatastoreService datastore, String authToken) throws InvalidAuthTokenException,
    		AuthTokenExpiredException {
    	validateAuthToken(datastore, authToken, new Date());
    }
    
    public static void validateAuthToken(DatastoreService datastore, String authToken, Date now) throws
    		InvalidAuthTokenException, AuthTokenExpiredException {
    	if (TextUtils.isEmpty(authToken)) {
    		throw new InvalidAuthTokenException();
    	}
    	
		try {
			AuthTokenEntity authTokenEntity = new AuthTokenEntity(datastore.get(KeyFactory.stringToKey(authToken)));
			Date timestamp = authTokenEntity.getTimestamp();
			// Check how old the entry is, if it's new enough just update the timestamp and keep going. Too old and
			// we fail forcing the user to sign in again.
			if (now.getTime() - timestamp.getTime() > AUTH_TOKEN_LIFE) {
				// Auth expired.
				throw new AuthTokenExpiredException();
			} else {
				// Update auth timestamp.
				authTokenEntity.setTimestamp(now);
				datastore.put(authTokenEntity.getEntity());
			}
		} catch (EntityNotFoundException ex) {
			throw new InvalidAuthTokenException();
		}
    }
}
