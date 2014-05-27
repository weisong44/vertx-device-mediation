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
	
	static public void send(WebSocket ws, DeviceMessage message) {
		// Strip off address info when sending to device
		DeviceMessage.AddrInfo info = message.createOrGetAddrInfo();
		message.setAddrInfo(null);
		String s = JsonUtil.toJsonString(message);
		message.setAddrInfo(info);
		ws.writeTextFrame(s);
	}
	
	static public <T> EventBus send(String address, DeviceMessage request) {
		return VertxUtil.getEventBus().send(address, encode(request));
	}
	
	static public EventBus sendToDevice(final DeviceRequest request, final Handler<Message<String>> replyHandler) {
		DeviceMessage.AddrInfo info = request.createOrGetAddrInfo();
		request.setAddrInfo(null);
		String s = JsonUtil.toJsonString(request);
		request.setAddrInfo(info);
		if(request.createOrGetAddrInfo().getSocketId() == null) {
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
		return VertxUtil.getEventBus().send(info.getSocketId(), s, replyHandler);
	}
	
	static public Map<String, String> getDeviceNodeToSocketMap() {
		return VertxUtil.get().sharedData().getMap(Addresses.toDeviceTopic);
	}

}
