package com.gill.graft;

import java.util.List;

/**
 * BaseTest
 *
 * @author gill
 * @version 2023/08/01
 **/
public abstract class BaseTest {

	protected static <T> void print(List<T> targets) {
		System.out.println();
		for (T target : targets) {
			System.out.printf("%s%n", target);
			printSplit();
		}
		System.out.println();
	}

	protected static void printSplit() {
		System.out.println("=========");
	}

	protected static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
