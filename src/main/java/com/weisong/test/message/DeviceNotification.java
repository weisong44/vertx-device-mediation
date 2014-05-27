package com.weisong.test.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
abstract public class DeviceNotification extends DeviceMessage {
}
