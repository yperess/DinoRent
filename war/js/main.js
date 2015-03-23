$(document).ready(function() {
	$("html").click(function() {
		dinorent.ui.hideAllContainers(false /* hideLoadingScreen */);
	});
	// Gobble these click events.
	$("input[type!=submit], #signInMenu > div").click(function(event) {
		event.stopPropagation();
	});
	$(".MenuIcon").click(function(event) {
		$(".MainMenu").animate({width:'toggle'},350);
		$("#signInMenu").hide();
		event.stopPropagation();
	});
	$(".SignInButton").click(function(event) {
		$("#signInCard").show();
		$("#signUpCard").hide();
		$(".MainMenu").animate({width:'hide'},350);
		$("#signInMenu").slideToggle();
		event.stopPropagation();
	});
	$("#sign-in").click(function(event) {
		dinorent.ui.setLoadingVisible(true);
		// TODO - call the sign-in API.
		setTimeout(function() {
			dinorent.ui.hideAllContainers(true /* hideLoadingScreen */);
		}, 1000);
	});
	$("#loadingScreen").click(function(event) {
		event.stopPropagation();
	});
	$("#sign-in-create-account").click(function(event) {
		$("#signInCard").animate({width:'hide'}, 350);
		$("#signUpCard").animate({width:'show'}, 350);
		event.stopPropagation();
	});
	$("#signUpCard").on("show", function() {
		dinorent.ui.updateSignUpCardHeight();
	});
	$("#sign-up").click(function(event) {
		event.stopPropagation();
		$(".ErrorMessage").hide();
		dinorent.ui.updateSignUpCardHeight();
		dinorent.ui.setLoadingVisible(true);
		// Validate the inputs.
		var email = $("#sign-up-email").val();
		var password = $("#sign-up-password").val();
		var passwordConfirm = $("#sign-up-password-confirm").val();
		var numErrors = 0;
		if (!dinorent.utils.isEmailValid(email)) {
			numErrors++;
			$(".ErrorMessage[for=sign-up-email]").show();
		}
		if (!dinorent.utils.isPasswordValid(password)) {
			numErrors++;
			$(".ErrorMessage[for=sign-up-password]").show();
		}
		if (password != passwordConfirm) {
			numErrors++;
			$(".ErrorMessage[for=sign-up-password-confirm]").show();
		}
		if (numErrors != 0) {
			dinorent.ui.updateSignUpCardHeight();
			dinorent.ui.setLoadingVisible(false);
			return;
		}
		// TODO - call the create account API.
		dinorent.ui.hideAllContainers(true /* hideLoadingScreen */);
	});
});

/** DinoRent global namespace. */
var dinorent = dinorent || {};

/** Constants namespace. */
dinorent.constants = dinorent.constants || {};

/** Current client state namespace. */
dinorent.state = dinorent.state || {};

/** UI namespace. */
dinorent.ui = dinorent.ui || {};

/** Utils namespace. */
dinorent.utils = dinorent.utils || {};

/** Constant for the API root. */
dinorent.constants.API_ROOT = "https://dino-rent.appspot.com/_ah/api";

/**
 * Show or hide the loading progress bar.
 * 
 * @param visible Boolean value telling if the loading screen should be visible.
 */
dinorent.ui.setLoadingVisible = function(visible) {
	if (visible) {
		$("#loadingScreen").show();
	} else {
		$("#loadingScreen").hide();
	}
}

/**
 * Hide all the floating containers such as the main menu, sign-in screen, etc. There is one exception, the loading
 * screen gets a special treatment and is guarded with a parameter boolean.
 * 
 * @param hideLoadingScreen @type {boolean} tells if the loading screen should also be hidden.
 */
dinorent.ui.hideAllContainers = function(hideLoadingScreen) {
	$("#signInMenu").slideUp();
	$(".MainMenu").animate({width:'hide'},350);
	if (hideLoadingScreen) {
		$("#loadingScreen").hide();
	}
}

/**
 * Updates the sign-up card height to dynamically match its content.
 * TODO - consider making this a generic function that takes an element and calculates its children's height plus
 * its padding.
 */
dinorent.ui.updateSignUpCardHeight = function() {
	var headerHeight = $("#signUpCard > h1").outerHeight(true /* includeMargins */);
	var formHeight = $("#signUpCard > form").outerHeight(true /* includeMargins */);
	$("#signUpCard").height(headerHeight + formHeight);
}

/**
 * Check if a given email string is a valid email address.
 * 
 * @param email @type {string} value of the email to validate.
 * @return @type {boolean} telling if the email appears valid.
 */
dinorent.utils.isEmailValid = function(email) {
  var regex = /^([a-zA-Z0-9_.+-])+\@(([a-zA-Z0-9-])+\.)+([a-zA-Z0-9]{2,4})+$/;
  return regex.test(email);
}

/**
 * Check if a given password is valid. Valid passwords are:
 * <ul>
 *   <li> 8+ characters.</li>
 *   <li> Are entirely alpha numeric.</li>
 * </ul>
 * 
 * @param password @type {string} value of the password to validate.
 * @return @type {boolean} telling if the password is valid.
 */
dinorent.utils.isPasswordValid = function(password) {
  var regex = /^[a-zA-Z-0-9]{8,}$/;
  if (typeof password == 'undefined') {
	return false;
  } else if (password.length < 8) {
	return false;
  }
  return regex.test(password);
}

/**
 * Whether or not the apis have been initialized.
 * @type {boolean}
 */
dinorent.state.apisInitialized = false;

dinorent.initApis = function() {
	var callback = function() {
		dinorent.state.apisInitialized = true;
		dinorent.ui.setLoadingVisible(false);
		// test the api.
//		try {
//			gapi.client.accounts.foo().execute(function(resp) {
//				document.getElementById("accounts").innerHTML = resp.content;
//			});
//		} catch (e) {
//			alert('error caught: ' + e);
//		}
	}
	gapi.client.load('accounts', 'v1', callback, dinorent.constants.API_ROOT);
}