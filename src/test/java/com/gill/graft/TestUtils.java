package com.gill.graft;

import java.lang.reflect.Field;

/**
 * TestUtils
 *
 * @author gill
 * @version 2023/09/20
 **/
public class TestUtils {

	public static <T> T getField(Object target, String fieldName) {
		Field field = null;
		try {
			field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
