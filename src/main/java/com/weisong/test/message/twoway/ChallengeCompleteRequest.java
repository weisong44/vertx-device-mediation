package com.weisong.test.message.twoway;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.weisong.test.message.DeviceRequest;

@Getter @Setter @ToString(callSuper = true)
public class ChallengeCompleteRequest extends DeviceRequest {
    private Boolean successful;
}
