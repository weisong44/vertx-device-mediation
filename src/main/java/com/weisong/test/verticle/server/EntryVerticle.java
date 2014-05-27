package com.weisong.test.verticle.server;

import org.vertx.java.platform.Verticle;

import com.weisong.test.util.VertxUtil;

public class EntryVerticle extends Verticle {
	@Override
	public void start() {
		
		VertxUtil.init(vertx);
		
		container.deployVerticle(WebsocketServerVerticle.class.getName());
		container.deployWorkerVerticle(DeviceMessageHandlerVerticle.class.getName());
	}
}
