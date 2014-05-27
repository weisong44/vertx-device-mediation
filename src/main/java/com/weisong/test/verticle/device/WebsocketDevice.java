package com.weisong.test.verticle.device;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.WebSocket;

import com.weisong.test.message.DeviceRequest;
import com.weisong.test.message.oneway.DeviceStatistics;
import com.weisong.test.message.twoway.ChallengeRequest;
import com.weisong.test.message.twoway.ChallengeResponse;
import com.weisong.test.message.twoway.HealthRequest;
import com.weisong.test.message.twoway.HealthResponse;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.GenericUtil;
import com.weisong.test.util.VertxUtil;

public class WebsocketDevice {
	
	static private class Worker extends Thread {
		
		final private Map<String, Integer> successfulResponses = new HashMap<>();
		final private Map<String, Integer> failedResponses = new HashMap<>();
		
		final private String nodeId = GenericUtil.getRandomMacAddress();
		final private String uri = "/device/" + nodeId;
		
		private Worker() {
			setName("Node " + nodeId);
		}
		
		public void run() {
			while(true) {
				VertxUtil.get().createHttpClient()
					.setPort(8090)
					.exceptionHandler(new ExceptionHandler(this))
					.connectWebsocket(uri, new WebSocketHandler(this));
				synchronized (this) {
					try {
						this.wait();
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						throw new RuntimeException(e); // Bail out!
					}
				}
			}
		}
	}

	@RequiredArgsConstructor
	static private class WebSocketHandler implements Handler<WebSocket> {
		final private Worker worker;
		@Override public void handle(WebSocket ws) {
			ws.dataHandler(new RequestHandler(ws, worker));
			final Long timerId = VertxUtil.get().setPeriodic(1000, new PeriodicNotificationHandler(ws, worker));
			ws.closeHandler(new CloseHandler(worker, timerId));
		}
	}
	
	@RequiredArgsConstructor
	static private class ExceptionHandler implements Handler<Throwable> {
		final private Object lock;
		@Override public void handle(Throwable event) {
			System.err.println("Failed to connect, retry later ...");
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	@RequiredArgsConstructor
	static private class CloseHandler implements Handler<Void> {
		final private Object lock;
		final private Long timeId; 
		@Override public void handle(Void event) {
			VertxUtil.get().cancelTimer(timeId);
			System.err.println("Connection closed, retry later ...");
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	@RequiredArgsConstructor
	static private class RequestHandler implements Handler<Buffer> {
		final private WebSocket ws;
		final private Worker worker;
		@Override public void handle(Buffer data) {
			DeviceRequest request = (DeviceRequest) DeviceMessageUtil.decode(data.toString());
			System.out.println("Received: " + request);
			incStats(worker.successfulResponses, request);
			if(request instanceof ChallengeRequest) {
				handleChallengeRequest((ChallengeRequest) request);
			}
			else if(request instanceof HealthRequest) {
				handleHealthRequest((HealthRequest) request);
			}
			else {
				System.err.println("Unknown message: " + data.toString());
			}
		}

		private void handleChallengeRequest(ChallengeRequest req) {
			ChallengeResponse resp = new ChallengeResponse();
			resp.setRequestId(req.getRequestId());
			resp.setHashedChallengeString(req.getChallengeString());
			DeviceMessageUtil.send(ws, resp);
		}
		
		private void handleHealthRequest(HealthRequest req) {
			HealthResponse resp = new HealthResponse();
			resp.setRequestId(req.getRequestId());
			DeviceMessageUtil.send(ws, resp);
		}
		
		private void incStats(Map<String, Integer> map, DeviceRequest request) {
			String key = request.getClass().getSimpleName();
			Integer count = map.get(key);
			if(count == null) {
				count = 0;
			}
			map.put(key, ++count);
		}
	}
	
	@RequiredArgsConstructor
	static private class PeriodicNotificationHandler implements Handler<Long> {
		final private WebSocket ws;
		final private Worker worker;
		@Override public void handle(Long event) {
			DeviceStatistics stats = new DeviceStatistics();
			stats.setSuccessful(worker.successfulResponses);
			stats.setFailed(worker.failedResponses);
			DeviceMessageUtil.send(ws, stats);
		}
	}
	
	static public void main(String[] args) throws Exception {
		int count = 1;
		if(args.length == 1) {
			count = Integer.valueOf(args[0]);
		}
		
		VertxUtil.init(VertxFactory.newVertx());
		for(int i = 0; i < count; i++) {
			new Worker().start();
		}
		
		// Wait for ever
		synchronized (WebsocketDevice.class) {
			WebsocketDevice.class.wait();
		}
	}
}

