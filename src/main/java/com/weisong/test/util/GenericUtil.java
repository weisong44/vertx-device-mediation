package com.weisong.test.util;

import java.util.Random;

public class GenericUtil {
	
	static private Random random = new Random();
	
	static public String getRandomMacAddress() {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < 6; i++) {
			sb.append(String.format("%02X", random.nextInt(256)));
		}
		return sb.toString();
	}
}
