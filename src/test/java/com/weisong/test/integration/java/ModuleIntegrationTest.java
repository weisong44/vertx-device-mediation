package com.weisong.test.integration.java;

/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertTrue;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.testtools.TestVerticle;

import com.weisong.test.util.VertxUtil;
import com.weisong.test.verticle.server.EntryVerticle;

/**
 * Example Java integration test that deploys the module that this project
 * builds.
 *
 * Quite often in integration tests you want to deploy the same module for all
 * tests and you don't want tests to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {

	@Test
	public void testPing() {
		container.logger().info("in testPing()");
		vertx.eventBus().send("ping-address", "ping!",
				new Handler<Message<String>>() {
					@Override
					public void handle(Message<String> reply) {
						assertEquals("pong!", reply.body());

						/*
						 * If we get here, the test is complete You must always
						 * call `testComplete()` at the end. Remember that
						 * testing is *asynchronous* so we cannot assume the
						 * test is complete by the time the test method has
						 * finished executing like in standard synchronous tests
						 */
						testComplete();
					}
				});
	}

	@Test
	public void testSomethingElse() {
		// Whatever
		testComplete();
	}

	@Override
	public void start() {
		initialize();
		container.deployVerticle(EntryVerticle.class.getName(), new AsyncResultHandler<String>() {
			@Override
			public void handle(AsyncResult<String> asyncResult) {
				assertTrue(asyncResult.succeeded());
				assertNotNull("deploymentID should not be null", asyncResult.result());
				VertxUtil.init(vertx);
				startTests();
			}
		});
	}

}
