package com.weisong.test.verticle.server;

import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import com.weisong.test.message.DeviceMessage;
import com.weisong.test.message.DeviceResponse;
import com.weisong.test.util.Addresses;
import com.weisong.test.util.DefaultAsyncResultHandler;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.VertxUtil;

public class DeviceMessageHandlerVerticle extends Verticle {
	
	private AsyncResultHandler<Void> asyncResultHandler = new DefaultAsyncResultHandler<Void>();
	
	private Handler<Message<String>> fromDeviceHandler = new Handler<Message<String>>() {
		@Override public void handle(Message<String> message) {
			DeviceMessage dmsg = DeviceMessageUtil.decode(message);
			if(dmsg instanceof DeviceResponse) {
				DeviceResponse resp = (DeviceResponse) dmsg;
				DeviceMessageUtil.send(resp.getRequestId(), resp);
			}
			else {
				//container.logger().info(String.format("Notification: %s", dmsg.getAddrInfo().getNodeId()));
			}
		}
	};
	
	@Override
	public void start() {
		VertxUtil.init(vertx);
		VertxUtil.getEventBus().registerHandler(Addresses.fromDeviceTopic, fromDeviceHandler, asyncResultHandler);
	}

	@Override
	public void stop() {
		VertxUtil.getEventBus().unregisterHandler(Addresses.fromDeviceTopic, fromDeviceHandler, asyncResultHandler);
	}
}
