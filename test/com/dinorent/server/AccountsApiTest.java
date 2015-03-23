package com.dinorent.server;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;

import java.util.Date;
import java.util.Random;

import com.dinorent.server.entities.AccountEntity;
import com.dinorent.server.entities.AccountEntity.AccountType;
import com.dinorent.server.entities.AuthTokenEntity;
import com.dinorent.server.entities.PasswordResetEntity;
import com.dinorent.server.entities.PendingAccountEntity;
import com.dinorent.server.replies.BaseReply;
import com.dinorent.server.replies.GetAccountReply;
import com.dinorent.server.replies.SignInReply;
import com.dinorent.server.util.StatusCodes;
import com.dinorent.server.util.StatusCodes.AccountNotFoundException;
import com.dinorent.server.util.StatusCodes.EmailTakenException;
import com.dinorent.server.util.StatusCodes.GenericInternalError;
import com.dinorent.server.util.StatusCodes.IncorrectVerificationCodeException;
import com.dinorent.server.util.StatusCodes.InvalidAuthTokenException;
import com.dinorent.server.util.StatusCodes.SessionExpiredException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PhoneNumber;
import com.google.appengine.api.datastore.PostalAddress;
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
    	
    	@Override
    	protected void sendVerificationEmail(PendingAccountEntity pendingAccount) {
    		// Do nothing.
    	}
    	
    	@Override
    	protected void sendVerificationEmail(PasswordResetEntity passwordReset) {
    		// Do nothing.
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
    	try {
    		mAccountsApi.signIn(emailAddress, password);
    		fail();
    	} catch (StatusCodes.AccountNotFoundException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testSignIn_success() throws EntityNotFoundException, AccountNotFoundException {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertAccount(email, password);
    	SignInReply reply = mAccountsApi.signIn(email.getEmail(), password);
    	
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotNull(reply.getAuthToken());
    	AuthTokenEntity authToken = new AuthTokenEntity(getDatastore().get(
    			KeyFactory.stringToKey(reply.getAuthToken())));
    	assertNotNull(authToken.getEntity());
    	assertEquals(email, authToken.getEmailAddress());
    }
    
    @Test
    public void testValidateAuthToken_notFound() throws SessionExpiredException, AccountNotFoundException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	
    	// Now delete the auth token.
    	datastore.delete(KeyFactory.stringToKey(authToken));
    	try {
    		AccountsApi.validateAuthToken(datastore, authToken, mNow);
    		fail();
    	} catch (StatusCodes.InvalidAuthTokenException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testValidateAuthToken_expired() throws InvalidAuthTokenException, AccountNotFoundException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	
		try {
			AccountsApi.validateAuthToken(datastore, authToken, new Date(BASE_TIME + AccountsApi.AUTH_TOKEN_LIFE +5l));
    		fail();
		} catch (SessionExpiredException e) {
			// Expected path
		}
    }
    
    @Test
    public void testValidateAuthToken() throws InvalidAuthTokenException, AccountNotFoundException,
    		SessionExpiredException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	final Date testTime = new Date(BASE_TIME + (AccountsApi.AUTH_TOKEN_LIFE / 2));
    	
		AccountsApi.validateAuthToken(datastore, authToken, testTime);
    }
    
    @Test
    public void testCreateAccount_alreadyUsedInAccount() {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertAccount(email, password);
    	try {
    		mAccountsApi.createAccount(email.getEmail(), password);
    		fail();
    	} catch (EmailTakenException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testCreateAccount_alreadyUsedInPendingAccount() {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertPendingAccount(email, password, mNow);
    	try {
    		mAccountsApi.createAccount(email.getEmail(), password);
    		fail();
    	} catch (EmailTakenException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testCreateAccount_successAlreadyUsedInPendingAccount() throws EmailTakenException {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertPendingAccount(email, password, mNow);
    	
    	mNow.setTime(BASE_TIME + AccountsApi.PENDING_ACCOUNT_LIFE + 5l);
    	BaseReply reply = mAccountsApi.createAccount(email.getEmail(), password);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testCreateAccount() throws EmailTakenException {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	BaseReply reply = mAccountsApi.createAccount(emailAddress, password);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testValidateAccount_accountNotFound() throws IncorrectVerificationCodeException {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	final int verificationCode = 1;
    	try {
    		mAccountsApi.validateAccount(emailAddress, password, verificationCode);
    		fail();
    	} catch (AccountNotFoundException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testValidateAccount() throws AccountNotFoundException, IncorrectVerificationCodeException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	final int verificationCode = insertPendingAccount(email, password, mNow);
    	
    	SignInReply reply = mAccountsApi.validateAccount(email.getEmail(), password, verificationCode);
    	AccountEntity accountEntity = AccountEntity.findAccountEntity(datastore, email);
    	
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertEquals(0, datastore.prepare(new Query(PendingAccountEntity.KIND)).countEntities(withLimit(1)));
    	assertNotNull(accountEntity.getEntity());
    	assertEquals(email, accountEntity.getEmailAddress());
    	assertEquals(password, accountEntity.getPassword());
    }
    
    @Test
    public void testDeleteAccount() throws AccountNotFoundException, InvalidAuthTokenException,
    		SessionExpiredException {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertAccount(email, password);
    	final String authToken = insertAuthToken(email, mNow);
    	mAccountsApi.deleteAccount(authToken);
    }
    
    @Test
    public void testRegenerateValidationCode_accountNotFound() {
    	final String emailAddress = "test@domain.com";
    	final String password = "12345";
    	try {
    		mAccountsApi.regenerateValidationCode(emailAddress, password);
    		fail();
    	} catch (AccountNotFoundException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testRegenerateValidationCode() throws AccountNotFoundException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	final int verificationCode = insertPendingAccount(email, password, mNow);
    	mNow.setTime(BASE_TIME + 5l);
    	BaseReply reply = mAccountsApi.regenerateValidationCode(email.getEmail(), password);
    	PendingAccountEntity pendingAccount = PendingAccountEntity.findPendingAccountEntity(datastore, email, password);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotEquals(verificationCode, pendingAccount.getVerificationCode());
    }
    
    @Test
    public void testSignOut_authTokenAlreadyExpired() {
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	mNow.setTime(BASE_TIME + AccountsApi.AUTH_TOKEN_LIFE + 5l);
    	BaseReply reply = mAccountsApi.signOut(authToken);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testSignOut_authTokenNotFound() {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	// Need to delete the auth token first.
    	datastore.delete(KeyFactory.stringToKey(authToken));
    	BaseReply reply = mAccountsApi.signOut(authToken);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testSignOut() {
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	mNow.setTime(BASE_TIME + (AccountsApi.AUTH_TOKEN_LIFE / 2));
    	BaseReply reply = mAccountsApi.signOut(authToken);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    }
    
    @Test
    public void testGetAccount_accountNotFound() throws InvalidAuthTokenException, SessionExpiredException {
    	final Email email = new Email("test@domain.com");
    	final String authToken = insertAuthToken(email, mNow);
    	try {
    		mAccountsApi.getAccount(authToken);
    		fail();
    	} catch (StatusCodes.AccountNotFoundException ex) {
    		// Expected path.
    	}
    }
    
    @Test
    public void testGetAccount() throws AccountNotFoundException, InvalidAuthTokenException, SessionExpiredException {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertAccount(email, password);
    	final String authToken = insertAuthToken(email, mNow);
    	GetAccountReply reply = mAccountsApi.getAccount(authToken);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertEquals(email, reply.getAccountEntity().getEmailAddress());
    	assertEquals(password, reply.getAccountEntity().getPassword());
    }
    
    @Test
    public void testModifyAccount() throws AccountNotFoundException, InvalidAuthTokenException,
    		SessionExpiredException {
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	final String names[] = new String[] {"Name 1", "Name 2"};
    	final int accountTypes[] = new int[] { AccountType.BASIC, AccountType.LANDLORD };
    	final PhoneNumber phoneNumbers[] = new PhoneNumber[] { new PhoneNumber("1234567890"),
    			new PhoneNumber("0123456789") };
    	final PostalAddress addresses[] = new PostalAddress[] {
    			new PostalAddress("123 Fake Street, Gotham City Michigan, 48227"),
    			new PostalAddress("124 Fake Street, Gotham City Michigan, 48227")
    	};
    	insertAccount(email, password, names[0], accountTypes[0], phoneNumbers[0], addresses[0]);
    	final String authToken = insertAuthToken(email, mNow);

    	DatastoreService datastore = getDatastore();
    	GetAccountReply reply = mAccountsApi.modifyAccount(authToken, password, names[1], accountTypes[1],
    			phoneNumbers[1].getNumber(), addresses[1].getAddress());
    	AccountEntity accountEntity = AccountEntity.findAccountEntity(datastore, email);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertEquals(names[1], accountEntity.getName());
    	assertEquals(names[1], reply.getAccountEntity().getName());
    	assertEquals(accountTypes[1], accountEntity.getAccountType());
    	assertEquals(accountTypes[1], reply.getAccountEntity().getAccountType());
    	assertEquals(phoneNumbers[1], accountEntity.getPhoneNumber());
    	assertEquals(phoneNumbers[1], reply.getAccountEntity().getPhoneNumber());
    	assertEquals(addresses[1], accountEntity.getAddress());
    	assertEquals(addresses[1], reply.getAccountEntity().getAddress());
    }
    
    @Test
    public void testResetPasswordRequest_accountNotFound() {
    	final Email email = new Email("test@domain.com");
    	try {
			mAccountsApi.resetPasswordRequest(email.getEmail());
			fail();
		} catch (AccountNotFoundException e) {
			// Expected path.
		}
    }
    
    @Test
    public void testResetPasswordRequest_overrideValidationCode() throws AccountNotFoundException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	final int verificationCode = insertPasswordResetEntity(email, new Random(mNow.getTime()).nextInt());
    	insertAccount(email, password);
    	mNow.setTime(BASE_TIME + 5l);
    	BaseReply reply = mAccountsApi.resetPasswordRequest(email.getEmail());
    	PasswordResetEntity passwordReset = PasswordResetEntity.findPasswordResetEntity(datastore, email);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotEquals(verificationCode, passwordReset.getVerificationCode());
    }
    
    @Test
    public void testResetPasswordRequest() throws AccountNotFoundException {
    	DatastoreService datastore = getDatastore();
    	final Email email = new Email("test@domain.com");
    	final String password = "12345";
    	insertAccount(email, password);
    	BaseReply reply = mAccountsApi.resetPasswordRequest(email.getEmail());
    	PasswordResetEntity passwordReset = PasswordResetEntity.findPasswordResetEntity(datastore, email);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotNull(passwordReset.getEntity());
    }
    
    @Test
    public void testResetPassword_accountNotFound() throws GenericInternalError, IncorrectVerificationCodeException {
    	final Email email = new Email("test@domain.com");
    	final int verificationCode = 1000;
    	final String newPassword = "54321";
    	try {
			mAccountsApi.resetPassword(email.getEmail(), verificationCode, newPassword);
			fail();
		} catch (AccountNotFoundException e) {
			// Expected path.
		}
    }
    
    @Test
    public void testResetPassword_resetEntityNotFound() throws AccountNotFoundException,
    		IncorrectVerificationCodeException {
    	final Email email = new Email("test@domain.com");
    	final String oldPassword = "12345";
    	final int verificationCode = 1000;
    	final String newPassword = "54321";
    	insertAccount(email, oldPassword);
    	try {
			mAccountsApi.resetPassword(email.getEmail(), verificationCode, newPassword);
			fail();
		} catch (GenericInternalError e) {
			// Expected path.
		}
    }
    
    @Test
    public void testResetPassword_incorrectVerificationCode() throws AccountNotFoundException, GenericInternalError {
    	final Email email = new Email("test@domain.com");
    	final String oldPassword = "12345";
    	final int verificationCode = insertPasswordResetEntity(email, new Random(mNow.getTime()).nextInt());
    	final String newPassword = "54321";
    	insertAccount(email, oldPassword);
    	try {
			mAccountsApi.resetPassword(email.getEmail(), verificationCode + 1, newPassword);
			fail();
		} catch (IncorrectVerificationCodeException e) {
			// Expected path.
		}
    }
    
    @Test
    public void testResetPassword() throws AccountNotFoundException, GenericInternalError,
    		IncorrectVerificationCodeException {
    	final Email email = new Email("test@domain.com");
    	final String oldPassword = "12345";
    	final int verificationCode = insertPasswordResetEntity(email, new Random(mNow.getTime()).nextInt());
    	final String newPassword = "54321";
    	insertAccount(email, oldPassword);
    	BaseReply reply = mAccountsApi.resetPassword(email.getEmail(), verificationCode, newPassword);
    	AccountEntity account = AccountEntity.findAccountEntity(getDatastore(), email);
    	assertEquals(StatusCodes.OK, reply.getStatusCode());
    	assertNotNull(account.getEntity());
    	assertEquals(newPassword, account.getPassword());
    }
    
    private void insertAccount(Email email, String password) {
    	insertAccount(email, password, null /* name */, null /* accountType */, null /* phoneNumber */,
    			null /* address */);
    }
    
    private void insertAccount(Email email, String password, String name, Integer accountType, PhoneNumber phoneNumber,
    		PostalAddress address) {
    	DatastoreService datastore = getDatastore();
    	AccountEntity accountEntity = new AccountEntity(email, password);
    	if (name != null) {
    		accountEntity.setName(name);
    	}
    	if (accountType != null) {
    		accountEntity.setAccountType(accountType);
    	}
    	if (phoneNumber != null) {
    		accountEntity.setPhoneNumber(phoneNumber);
    	}
    	if (address != null) {
    		accountEntity.setAddress(address);
    	}
    	datastore.put(accountEntity.getEntity());
    }
    
    private int insertPendingAccount(Email email, String password, Date timestamp) {
    	DatastoreService datastore = getDatastore();
    	PendingAccountEntity pendingAccountEntity = new PendingAccountEntity(email, password, timestamp);
    	datastore.put(pendingAccountEntity.getEntity());
    	return pendingAccountEntity.getVerificationCode();
    }
    
    private String insertAuthToken(Email email, Date date) {
    	DatastoreService datastore = getDatastore();
    	AuthTokenEntity authToken = new AuthTokenEntity(email, date);
    	datastore.put(authToken.getEntity());
    	return authToken.getKeyString();
    }
    
    private int insertPasswordResetEntity(Email email, int verificationCode) {
    	DatastoreService datastore = getDatastore();
    	PasswordResetEntity passwordReset = new PasswordResetEntity(email, verificationCode);
    	datastore.put(passwordReset.getEntity());
    	return passwordReset.getVerificationCode();
    }
    
    private final DatastoreService getDatastore() {
    	return DatastoreServiceFactory.getDatastoreService();
    }
}
