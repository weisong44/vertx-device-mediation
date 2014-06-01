package com.weisong.test.verticle.device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.WebSocket;

import com.weisong.test.message.DeviceRequest;
import com.weisong.test.message.oneway.DeviceStatistics;
import com.weisong.test.message.twoway.ChallengeCompleteRequest;
import com.weisong.test.message.twoway.ChallengeCompleteResponse;
import com.weisong.test.message.twoway.ChallengeRequest;
import com.weisong.test.message.twoway.ChallengeResponse;
import com.weisong.test.message.twoway.HealthRequest;
import com.weisong.test.message.twoway.HealthResponse;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.GenericUtil;
import com.weisong.test.util.VertxUtil;

public class WebsocketDevice {

    static private AtomicLong requestCount = new AtomicLong();
    static private AtomicLong notifCount = new AtomicLong();
    
    static private class Printer extends Thread {
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000L);
                    System.out.println(String.format("Requests: % 4d, Notification: % 4d", 
                            requestCount.longValue(), notifCount.longValue()));
                    requestCount.set(0L);
                    notifCount.set(0L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    static private class Worker extends Thread {

        final private String host;
        final private int port;
        
        final private Map<String, Integer> successfulResponses = new HashMap<>();
        final private Map<String, Integer> failedResponses = new HashMap<>();

        final private String nodeId = GenericUtil.getRandomMacAddress();
        final private String uri = "/device/" + nodeId;
        
        @Getter @Setter private Boolean authenticated;

        private Worker(String host, int port) {
            this.host = host;
            this.port = port;
            setName("Node " + nodeId);
        }

        public void run() {
            while (true) {
                setAuthenticated(false);
                VertxUtil.get().createHttpClient()
                    .setHost(host).setPort(port)
                    .exceptionHandler(new ExceptionHandler(this))
                    .connectWebsocket(uri, new WebSocketHandler(this));
                synchronized (this) {
                    try {
                        this.wait();
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e); // Bail out!
                    }
                }
            }
        }
    }

    @RequiredArgsConstructor
    static private class WebSocketHandler implements Handler<WebSocket> {
        final private Worker worker;

        @Override
        public void handle(WebSocket ws) {
            ws.dataHandler(new RequestHandler(ws, worker));
            final Long timerId = VertxUtil.get().setPeriodic(1000, new PeriodicNotificationHandler(ws, worker));
            ws.closeHandler(new CloseHandler(worker, timerId));
        }
    }

    @RequiredArgsConstructor
    static private class ExceptionHandler implements Handler<Throwable> {
        final private Object lock;

        @Override
        public void handle(Throwable event) {
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

        @Override
        public void handle(Void event) {
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

        @Override
        public void handle(Buffer data) {
            DeviceRequest request = (DeviceRequest) DeviceMessageUtil.decode(data);
            requestCount.incrementAndGet();
            //System.out.println("Received: " + request);
            incStats(worker.successfulResponses, request);
            if (request instanceof ChallengeRequest) {
                handleRequest((ChallengeRequest) request);
            }
            else if (request instanceof ChallengeCompleteRequest) {
                handleRequest((ChallengeCompleteRequest) request);
            }
            else if (request instanceof HealthRequest) {
                handleRequest((HealthRequest) request);
            }
            else {
                System.err.println("Unknown message: " + data.toString());
            }
        }

        private void handleRequest(ChallengeRequest req) {
            ChallengeResponse resp = new ChallengeResponse();
            resp.setRequestId(req.getRequestId());
            resp.setHashedChallengeString(req.getChallengeString());
            DeviceMessageUtil.send(ws, resp);
        }

        private void handleRequest(ChallengeCompleteRequest req) {
            ChallengeCompleteResponse resp = new ChallengeCompleteResponse();
            resp.setRequestId(req.getRequestId());
            worker.setAuthenticated(true);
            DeviceMessageUtil.send(ws, resp);
        }

        private void handleRequest(HealthRequest req) {
            HealthResponse resp = new HealthResponse();
            resp.setRequestId(req.getRequestId());
            DeviceMessageUtil.send(ws, resp);
        }

        private void incStats(Map<String, Integer> map, DeviceRequest request) {
            String key = request.getClass().getSimpleName();
            Integer count = map.get(key);
            if (count == null) {
                count = 0;
            }
            map.put(key, ++count);
        }
    }

    @RequiredArgsConstructor
    static private class PeriodicNotificationHandler implements Handler<Long> {
        final private WebSocket ws;
        final private Worker worker;

        @Override
        public void handle(Long event) {
            if(worker.getAuthenticated() == false) {
                return;
            }
            DeviceStatistics stats = new DeviceStatistics();
            stats.setSuccessful(worker.successfulResponses);
            stats.setFailed(worker.failedResponses);
            DeviceMessageUtil.send(ws, stats);
            notifCount.incrementAndGet();
        }
    }

    static public void main(String[] args) throws Exception {
        
        WebsocketDeviceOptions opts = new WebsocketDeviceOptions(args);
        
        VertxUtil.init(VertxFactory.newVertx());
        new Printer().start();
        for (int i = 0; i < opts.getConnections(); i++) {
            new Worker(opts.getHost(), opts.getPort()).start();
        }

        // Wait for ever
        synchronized (WebsocketDevice.class) {
            WebsocketDevice.class.wait();
        }
    }
}
