package com.weisong.test.message.oneway;

import java.util.Map;

import com.weisong.test.message.DeviceNotification;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class DeviceStatistics extends DeviceNotification {
	private Map<String, Integer> successful;
	private Map<String, Integer> failed;
}
