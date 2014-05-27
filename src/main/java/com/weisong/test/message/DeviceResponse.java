package com.weisong.test.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
abstract public class DeviceResponse extends DeviceMessage {
	private String requestId;
}
