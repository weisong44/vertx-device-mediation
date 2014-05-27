package com.weisong.test.message.twoway;

import com.weisong.test.message.DeviceRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class ChallengeRequest extends DeviceRequest {
	private String challengeString;
}
