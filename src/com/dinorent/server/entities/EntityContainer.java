package com.dinorent.server.entities;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;

public class EntityContainer {
	
	protected final Entity mEntity;
	
	/**
	 * Create a new entity container with no properties.
	 * 
	 * @param entityKind The kind of entity to create.
	 */
	public EntityContainer(String entityKind) {
		mEntity = new Entity(entityKind);
	}
	
	/**
	 * Create a new container with an existing entity.
	 * 
	 * @param entity The entity to wrap.
	 */
	public EntityContainer(Entity entity) {
		mEntity = entity;
	}
	
	/**
	 * Get the data-store entity.
	 * 
	 * @return The data-store entity contained in this object.
	 */
	final public Entity getEntity() {
		return mEntity;
	}
	
	/**
	 * Get the web safe string representation of the entity's key.
	 * 
	 * @return The string representation of the entity's key.
	 */
	final public String getKeyString() {
		return KeyFactory.keyToString(mEntity.getKey());
	}
}
