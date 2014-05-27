package com.weisong.test.message.twoway;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.weisong.test.message.DeviceResponse;

@Getter @Setter @ToString(callSuper = true)
public class HealthResponse extends DeviceResponse {
	
	public enum Status {
		Ok, NOK
	}
	
	private Status status = Status.Ok;
}
