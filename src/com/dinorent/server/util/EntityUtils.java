package com.dinorent.server.util;

import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class EntityUtils {

	/**
	 * Get an integer value from a given {@link Entity} keyed by a property. The value must be either an integer or a
	 * long value.
	 * 
	 * @param entity The {@link Entity} to search.
	 * @param property The property key to search the entity for.
	 * @return The integer value.
	 * @throws IllegalStateException If the property was not found or was not an int/long.
	 */
	public static int getIntProperty(Entity entity, String property) {
		Object obj = entity.getProperty(property);
		if (obj instanceof Integer) {
			return ((Integer) obj).intValue();
		} else if (obj instanceof Long) {
			return ((Long) obj).intValue();
		}
		throw new IllegalStateException("Invalid property format in entity: " + property + " is not a number");
	}
	
	/**
	 * Get a filter for a given email address.
	 * 
	 * @param email The {@link Email} to search for.
	 * @return A {@link FilterPredicate} to filter a query with.
	 */
	public static FilterPredicate getFilterPredicate(Email email) {
		return new FilterPredicate(Properties.EMAIL_ADDRESS, FilterOperator.EQUAL, email);
	}
	
	private EntityUtils() {
		// Not instantiable.
	}

}
