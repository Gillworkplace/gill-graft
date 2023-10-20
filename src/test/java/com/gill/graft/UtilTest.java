package com.gill.graft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import com.gill.graft.proto.RaftConverter;

import cn.hutool.core.util.RandomUtil;

/**
 * UtilTest
 *
 * @author gill
 * @version 2023/10/19
 **/
public class UtilTest {

	@RepeatedTest(100)
	public void testIntCodec() {
		int num = RandomUtil.randomInt(0, Integer.MAX_VALUE);
		byte[] bytes = RaftConverter.intEncode(num);
		Assertions.assertEquals(num, RaftConverter.intDecode(bytes));
	}
}
