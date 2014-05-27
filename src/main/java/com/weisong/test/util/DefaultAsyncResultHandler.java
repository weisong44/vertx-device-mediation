package com.weisong.test.util;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;

public class DefaultAsyncResultHandler<T> implements AsyncResultHandler<T> {
	@Override
	public void handle(AsyncResult<T> result) {
		if(result.failed()) {
			System.err.println("Action failed");
			if(result.cause() != null) {
				result.cause().printStackTrace();
			}
		}
	}
}
