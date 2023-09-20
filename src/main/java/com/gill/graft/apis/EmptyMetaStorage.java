package com.gill.graft.apis;


import com.gill.graft.model.PersistentProperties;

/**
 * EmptyMetaStorage
 *
 * @author gill
 * @version 2023/09/11
 **/
public class EmptyMetaStorage implements MetaStorage {

	@Override
	public void write(PersistentProperties properties) {

	}

	@Override
	public PersistentProperties read() {
		return new PersistentProperties();
	}
}
