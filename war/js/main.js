$(document).ready(function() {
	$("html").click(function() {
		dinorent.ui.hideAllContainers(false /* hideLoadingScreen */);
	});
	// Gobble these click events.
	$("#sign-in-email, #sign-in-password, #signInMenu > div").click(function(event) {
		event.stopPropagation();
	});
	$(".MenuIcon").click(function(event) {
		$(".MainMenu").animate({width:'toggle'},350);
		$("#signInMenu").hide();
		event.stopPropagation();
	});
	$(".SignInButton").click(function(event) {
		$(".MainMenu").animate({width:'hide'},350);
		$("#signInMenu").slideToggle();
		event.stopPropagation();
	});
	$("#sign-in").click(function(event) {
		dinorent.ui.setLoadingVisible(true);
		setTimeout(function() {
			dinorent.ui.hideAllContainers(true /* hideLoadingScreen */);
		}, 1000);
	});
	$("#loadingScreen").click(function(event) {
		event.stopPropagation();
	});
	
	// Test methods only.
	$("#foo-button").click(function(event) {
		gapi.client.accounts.foo().execute(function(resp) {
			document.getElementById("foo").innerHTML = resp.content;
		});
		event.stopPropagation();
	});
	$("#bar-button").click(function(event) {
		gapi.client.accounts.bar().execute(function(resp) {
			document.getElementById("foo").innerHTML = resp.content;
		});
		event.stopPropagation();
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

dinorent.ui.hideAllContainers = function(hideLoadingScreen) {
	$("#signInMenu").slideUp();
	$(".MainMenu").animate({width:'hide'},350);
	if (hideLoadingScreen) {
		$("#loadingScreen").hide();
	}
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
		try {
			gapi.client.accounts.foo().execute(function(resp) {
				document.getElementById("accounts").innerHTML = resp.content;
			});
		} catch (e) {
			alert('error caught: ' + e);
		}
	}
	gapi.client.load('accounts', 'v1', callback, dinorent.constants.API_ROOT);
}