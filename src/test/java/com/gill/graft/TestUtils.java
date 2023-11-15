package com.gill.graft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TestUtils
 *
 * @author gill
 * @version 2023/09/20
 **/
public class TestUtils {

	public static <T> T getField(Object target, String fieldName) {
		try {
			List<Field> fields = new ArrayList<>();
			Class<?> clazz = target.getClass();
			while (clazz != Object.class) {
				fields.addAll(Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList()));
				clazz = clazz.getSuperclass();
			}
			for (Field field : fields) {
				if (fieldName.equals(field.getName())) {
					field.setAccessible(true);
					return (T) field.get(target);
				}
			}
			throw new NoSuchFieldException();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setField(Object target, String fieldName, Object property) {
		try {
			List<Field> fields = new ArrayList<>();
			Class<?> clazz = target.getClass();
			while (clazz != Object.class) {
				fields.addAll(Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList()));
				clazz = clazz.getSuperclass();
			}
			for (Field field : fields) {
				if (fieldName.equals(field.getName())) {
					field.setAccessible(true);
					field.set(target, property);
					return;
				}
			}
			throw new NoSuchFieldException();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
