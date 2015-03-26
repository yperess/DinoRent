$(document).ready(function() {
	// Save any original state needed:
	$("#signUpCard").data("originalHeight", parseInt($("#signUpCard").css("height"), 10));
	$("#passwordRecoverCard").data("originalHeight", parseInt($("#passwordRecoverCard").css("height"), 10));
	
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
		$("#passwordRecoverCard").hide();
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
	$("#sign-in-help").click(function(event) {
		$("#signInCard").animate({width:'hide'}, 350);
		$("#passwordRecoverCard").animate({width:'show'}, 350);
		event.stopPropagation();
	});
	$("#password-recover").click(function(event) {
		event.stopPropagation();
		$("#passwordRecoverCard .ErrorMessage").hide();
		dinorent.ui.updateSignUpCardHeight($("#passwordRecoverCard"));
		// Validate the inputs.
		var email = $("#password-recover-email").val();
		var numErrors = 0;
		if (!dinorent.utils.isEmailValid(email)) {
			numErrors++;
			$(".ErrorMessage[for=password-recover-email]").show();
		}
		if (numErrors != 0) {
			dinorent.ui.updateSignUpCardHeight($("#passwordRecoverCard"));
			dinorent.ui.setLoadingVisible(false);
			return;
		}
		// TODO - call the password recovery API.
		dinorent.ui.hideAllContainers(true /* hideLoadingScreen */);
	});
	$("#sign-up").click(function(event) {
		event.stopPropagation();
		$("#signUpCard .ErrorMessage").hide();
		dinorent.ui.updateSignUpCardHeight($("#signUpCard"));
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
			dinorent.ui.updateSignUpCardHeight($("#signUpCard"));
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

/** Object version of the url query. */
dinorent.constants.URL_QUERY = (function(query) {
	/* parse the query */
	var x = query.replace(/;/g, '&').split('&'), i, name, t;
	/* query changes from string version of query to object */
	for (query={}, i=0; i<x.length; i++) {
		t = x[i].split('=', 2);
		name = decodeURI(t[0]);
		if (!query[name]) {
			query[name] = [];
		}
		if (t.length > 1) {
			query[name][query[name].length] = decodeURI(t[1]);
		} else {
			// nonstandard - sets variables with no value to true.
			query[name][query[name].length] = true;
		}
	}
	return query;
})(location.search.substring(1).replace(/\+/g, ' '));


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
	dinorent.ui.resetSignUpCard();
	if (hideLoadingScreen) {
		$("#loadingScreen").hide();
	}
}

/**
 * Updates the sign-up card height to dynamically match its content.
 * TODO - consider making this a generic function that takes an element and calculates its children's height plus
 * its padding.
 */
dinorent.ui.updateSignUpCardHeight = function(card) {
	var childHeight = 0;
	card.children().each(function() {
		childHeight += $(this).outerHeight(true /* includeMargins */);
	});
	var height = Math.max(childHeight, card.data("originalHeight"));
	card.height(height);
}

/**
 * Clear the sign-up card input fields.
 */
dinorent.ui.resetSignUpCard = function() {
	$("#signUpCard input[type=email], #signUpCard input[type=password]").val("");
	$("#signUpCard .ErrorMessage").hide();
	$("#signUpCard").animate({height: $("#signUpCard").data("originalHeight")}, 350);
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