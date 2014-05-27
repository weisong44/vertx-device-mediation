package com.weisong.test.message.twoway;

import com.weisong.test.message.DeviceResponse;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class ChallengeResponse extends DeviceResponse {
	private String hashedChallengeString;
}
