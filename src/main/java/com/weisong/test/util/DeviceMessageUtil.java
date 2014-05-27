package com.weisong.test.util;

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.WebSocket;

import com.weisong.test.message.DeviceMessage;
import com.weisong.test.message.DeviceRequest;

public class DeviceMessageUtil {
	static public String encode(DeviceMessage message) {
		return JsonUtil.toJsonString(message);
	}
	
	static public DeviceMessage decode(String s) {
		return JsonUtil.toObject(s, DeviceMessage.class);
	}
	
	static public WebSocket send(final WebSocket ws, final DeviceMessage message) {
		// Strip off address info when sending to device
		DeviceMessage.AddrInfo info = message.getAddrInfo();
		try {
			message.setAddrInfo(null);
			String s = JsonUtil.toJsonString(message);
			ws.writeTextFrame(s);
		}
		finally {
			message.setAddrInfo(info);
		}
		return ws;
	}
	
	static public EventBus send(String address, DeviceMessage request) {
		return VertxUtil.getEventBus().send(address, encode(request));
	}
	
	static public EventBus sendToDevice(final DeviceRequest request, final Handler<Message<String>> replyHandler) {
		String jsonString = null;
		DeviceMessage.AddrInfo info = request.createOrGetAddrInfo();
		try {
			request.setAddrInfo(null);
			jsonString = JsonUtil.toJsonString(request);
		}
		finally {
			request.setAddrInfo(info);
		}
		
		if(request.getAddrInfo().getSocketId() == null) {
			info.setSocketId(getDeviceNodeToSocketMap().get(info.getNodeId()));
		}
		if(replyHandler != null) {
			VertxUtil.getEventBus().registerHandler(request.getRequestId(), replyHandler);
			VertxUtil.get().setTimer(5000, new Handler<Long>() {
				@Override public void handle(Long event) {
					VertxUtil.getEventBus().unregisterHandler(request.getRequestId(), replyHandler);
				}
			});
		}
		return VertxUtil.getEventBus().send(info.getSocketId(), jsonString, replyHandler);
	}
	
	static public Map<String, String> getDeviceNodeToSocketMap() {
		return VertxUtil.get().sharedData().getMap(Addresses.toDeviceTopic);
	}

}
