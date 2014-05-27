package com.weisong.test.util;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;

public class VertxUtil {
	
	static private Vertx vertx;
	
	static public void init(Vertx theVertx) {
		vertx = theVertx;
	}
	
	static private void checkInit() {
		if(vertx == null) {
			throw new RuntimeException("VertxUtil not initialized");
		}
	}
	
	static public Vertx get() {
		checkInit();
		return vertx;
	}
	
	static public EventBus getEventBus() {
		checkInit();
		return vertx.eventBus();
	}
}
