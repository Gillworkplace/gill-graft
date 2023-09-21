package com.gill.graft.apis.empty;

import com.gill.graft.apis.MetaStorage;
import com.gill.graft.model.PersistentProperties;

/**
 * EmptyMetaStorage
 *
 * @author gill
 * @version 2023/09/11
 **/
public class EmptyMetaStorage implements MetaStorage {

	public static final EmptyMetaStorage INSTANCE = new EmptyMetaStorage();

	@Override
	public void write(PersistentProperties properties) {

	}

	@Override
	public PersistentProperties read() {
		return new PersistentProperties();
	}
}
