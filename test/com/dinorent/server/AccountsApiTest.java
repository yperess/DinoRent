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
import com.dinorent.server.util.StatusCodes;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;

import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountsApiTest {

    private final long BASE_TIME = 1000l;

    private final LocalServiceTestHelper helper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private Date mNow = new Date(BASE_TIME);
    
    private final AccountsApi mAccountsApi = new AccountsApi() {
    	@Override
    	protected Date getDate() {
    		return mNow;
    	}
    };

    @Before
    public void setUp() {
        helper.setUp();
        mNow.setTime(BASE_TIME);
    }

    @After
    public void tearDown() {
        helper.tearDown();
    }

    @Test
    public void testSignIn_noAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	SignInReply reply = mAccountsApi.signIn(emailAddress, password);
    	assertEquals(StatusCodes.ACCOUNT_NOT_FOUND, reply.getStatusCode());
    }
    
    @Test
    public void testSignIn_success() throws EntityNotFoundException {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	insertAccount(emailAddress, password);
    	SignInReply reply = mAccountsApi.signIn(emailAddress, password);
    	
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotNull(reply.getAuthToken());
    	AuthTokenEntity authToken = new AuthTokenEntity(datastore.get(KeyFactory.stringToKey(reply.getAuthToken())));
    	assertNotNull(authToken.getEntity());
    	assertEquals(emailAddress, authToken.getEmailAddress());
    }
    
    @Test
    public void testValidateAuthToken_notFound() {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	final String emailAddress = "test@domain.com";
    	final String authToken = insertAuthToken(emailAddress, mNow);
    	
    	// Now delete the auth token.
    	datastore.delete(KeyFactory.stringToKey(authToken));
    	try {
    		AccountsApi.validateAuthToken(datastore, authToken, mNow);
    		fail();
    	} catch (InvalidAuthTokenException ex) {
    		// Expected path.
    	} catch (AuthTokenExpiredException ex) {
    		fail();
    	}
    }
    
    @Test
    public void testValidateAuthToken_expired() {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	final String emailAddress = "test@domain.com";
    	final String authToken = insertAuthToken(emailAddress, mNow);
    	
    	try {
    		AccountsApi.validateAuthToken(datastore, authToken, new Date(BASE_TIME + AccountsApi.AUTH_TOKEN_LIFE +5l));
    		fail();
    	} catch (InvalidAuthTokenException ex) {
    		fail();
    	} catch (AuthTokenExpiredException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testValidateAuthToken() {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	final String emailAddress = "test@domain.com";
    	final String authToken = insertAuthToken(emailAddress, mNow);
    	final Date testTime = new Date(BASE_TIME + (AccountsApi.AUTH_TOKEN_LIFE / 2));
    	
    	try {
    		AccountsApi.validateAuthToken(datastore, authToken, testTime);
    	} catch (InvalidAuthTokenException ex) {
    		fail();
    	} catch (AuthTokenExpiredException ex) {
        	fail();
    	}
    }
    
    @Test
    public void testCreateAccount_alreadyUsedInAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	insertAccount(emailAddress, password);
    	CreateAccountReply reply = mAccountsApi.createAccount(emailAddress, password);
    	assertEquals(StatusCodes.ACCOUNT_EMAIL_TAKEN, reply.getStatusCode());
    }
    
    @Test
    public void testCreateAccount_alreadyUsedInPendingAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	insertPendingAccount(emailAddress, password, mNow);
    	CreateAccountReply reply = mAccountsApi.createAccount(emailAddress, password);
    	assertEquals(StatusCodes.ACCOUNT_EMAIL_TAKEN, reply.getStatusCode());
    }
    
    @Test
    public void testCreateAccount_successAlreadyUsedInPendingAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	insertPendingAccount(emailAddress, password, mNow);
    	
    	mNow.setTime(BASE_TIME + AccountsApi.PENDING_ACCOUNT_LIFE + 5l);
    	CreateAccountReply reply = mAccountsApi.createAccount(emailAddress, password);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testCreateAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	CreateAccountReply reply = mAccountsApi.createAccount(emailAddress, password);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testValidateAccount_accountNotFound() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	final int verificationCode = 1;
    	ValidateAccountReply reply = mAccountsApi.validateAccount(emailAddress, password, verificationCode);
    	assertEquals(StatusCodes.ACCOUNT_NOT_FOUND, reply.getStatusCode());
    }
    
    @Test
    public void testValidateAccount() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	final int verificationCode = insertPendingAccount(emailAddress, password, mNow);
    	ValidateAccountReply reply = mAccountsApi.validateAccount(emailAddress, password, verificationCode);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	// TODO - verify that the pending account was removed and that the actual account was created.
    }
    
    @Test
    public void testRegenerateValidationCode_accountNotFound() {
    	fail();
    }
    
    @Test
    public void testRegenerateValidationCode() {
    	fail();
    }
    
    @Test
    public void testSignOut_authTokenAlreadyExpired() {
    	fail();
    }
    
    @Test
    public void testSignOut_authTokenNotFound() {
    	fail();
    }
    
    @Test
    public void testSignOut() {
    	fail();
    }
    
    private void insertAccount(String emailAddress, String password) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	AccountEntity accountEntity = new AccountEntity(emailAddress, password);
    	datastore.put(accountEntity.getEntity());
    }
    
    private int insertPendingAccount(String emailAddress, String password, Date timestamp) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	PendingAccountEntity pendingAccountEntity = new PendingAccountEntity(emailAddress, password, timestamp);
    	datastore.put(pendingAccountEntity.getEntity());
    	return pendingAccountEntity.getVerificationCode();
    }
    
    private String insertAuthToken(String emailAddress, Date date) {
    	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    	AuthTokenEntity authToken = new AuthTokenEntity(emailAddress, date);
    	datastore.put(authToken.getEntity());
    	return authToken.getKeyString();
    }
}
