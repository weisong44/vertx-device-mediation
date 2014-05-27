package com.weisong.test.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.weisong.test.message.oneway.DeviceStatistics;
import com.weisong.test.message.twoway.ChallengeRequest;
import com.weisong.test.message.twoway.ChallengeResponse;
import com.weisong.test.message.twoway.HealthRequest;
import com.weisong.test.message.twoway.HealthResponse;

@Getter @Setter @ToString
@JsonInclude(Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
      @JsonSubTypes.Type(value=ChallengeRequest.class, name="challenge-request")
    , @JsonSubTypes.Type(value=ChallengeResponse.class, name="challenge-response")
    , @JsonSubTypes.Type(value=HealthRequest.class, name="health-request")
    , @JsonSubTypes.Type(value=HealthResponse.class, name="health-response")
    , @JsonSubTypes.Type(value=DeviceStatistics.class, name="device-statistics")
})
abstract public class DeviceMessage {
	
	@Getter @Setter @ToString
	static public class AddrInfo {
		private String nodeId;
		private String socketId;
	}
	
	private AddrInfo addrInfo;
	
	public AddrInfo createOrGetAddrInfo() {
		if(addrInfo == null) {
			addrInfo = new AddrInfo();
		}
		return addrInfo;
	}
}
