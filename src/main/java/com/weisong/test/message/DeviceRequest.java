package com.weisong.test.message;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
abstract public class DeviceRequest extends DeviceMessage {
	private String requestId = UUID.randomUUID().toString();
}
