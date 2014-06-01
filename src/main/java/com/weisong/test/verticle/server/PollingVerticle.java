package com.weisong.test.verticle.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import com.weisong.test.message.twoway.HealthRequest;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.VertxUtil;

public class PollingVerticle extends Verticle {
	
    final static private AtomicLong successfulCounter = new AtomicLong();
    final static private AtomicLong failedCounter = new AtomicLong();
    final static private AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    final static private AtomicLong maxLatency = new AtomicLong(Long.MIN_VALUE);
    final static private AtomicLong totalLatency = new AtomicLong();
    
    static private class Printer extends Thread {
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000L);
                    if(successfulCounter.longValue() > 0L) {
                        long avgLatency = totalLatency.longValue() / successfulCounter.longValue();
                        System.out.println(String.format("Polling response: % 4d % 4d% 4dms [% 4dms - % 4dms]", 
                                successfulCounter.longValue(), failedCounter.longValue(),
                                avgLatency, minLatency.longValue(), maxLatency.longValue()));
                    }
                    successfulCounter.set(0L);
                    failedCounter.set(0L);
                    minLatency.set(Long.MAX_VALUE);
                    maxLatency.set(Long.MIN_VALUE);
                    totalLatency.set(0L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
	private Handler<Long> pollingHandler = new Handler<Long>() {
		@Override public void handle(Long event) {
		    final Map<String, Long> tsMap = new HashMap<>();
			for(final String nodeId : DeviceMessageUtil.getDeviceNodeToSocketMap().keySet()) {
				HealthRequest request = new HealthRequest();
				request.createOrGetAddrInfo().setNodeId(nodeId);
				tsMap.put(nodeId, System.currentTimeMillis());
				DeviceMessageUtil.sendToDevice(request, new Handler<AsyncResult<Message<String>>>() {
					@Override public void handle(AsyncResult<Message<String>> result) {
					    if(result.succeeded()) {
					        successfulCounter.incrementAndGet();
					        Long st = tsMap.get(nodeId);
					        if(st != null) {
					            Long t = System.currentTimeMillis() - st;
					            minLatency.set(Math.min(t, minLatency.longValue()));
					            maxLatency.set(Math.max(t, maxLatency.longValue()));
					            totalLatency.set(totalLatency.longValue() + t);
					        }
					        /*
                            DeviceResponse response = (DeviceResponse) DeviceMessageUtil.decode(result.result());
                            container.logger().info(String.format("Response: %s", response.getAddrInfo().getNodeId()));
					         */
					    }
					    else {
					        failedCounter.incrementAndGet();
	                        container.logger().warn("Polling failed: " + result.cause());
					    }
					}
				});
			}
		}
	};

	@Override
	public void start() {
		VertxUtil.init(vertx);
		vertx.setPeriodic(1000, pollingHandler);
		new Printer().start();
	}
}
