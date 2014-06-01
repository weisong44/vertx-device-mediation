package com.weisong.test.verticle.server;

import org.vertx.java.platform.Verticle;

import com.weisong.test.util.VertxUtil;

public class EntryVerticle extends Verticle {
	@Override
	public void start() {
		
		VertxUtil.init(vertx);
		
		container.deployVerticle(WebsocketServerVerticle.class.getName());

		final Class<?>[] workerVerticles = new Class<?>[] {
	        DeviceMessageHandlerVerticle.class
	      , PollingVerticle.class
		};
		for(final Class<?> c : workerVerticles) {
	        container.deployWorkerVerticle(c.getName());
		}
	}
}
