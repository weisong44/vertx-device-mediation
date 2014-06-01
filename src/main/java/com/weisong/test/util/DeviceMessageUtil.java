package com.weisong.test.util;

import java.util.Map;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.WebSocketBase;
import org.vertx.java.core.impl.DefaultFutureResult;

import com.weisong.test.message.DeviceMessage;
import com.weisong.test.message.DeviceRequest;

public class DeviceMessageUtil {
    
	static public String encode(DeviceMessage dmsg) {
		return JsonUtil.toJsonString(dmsg);
	}
	
    static public DeviceMessage decode(String s) {
        return JsonUtil.toObject(s, DeviceMessage.class);
    }
    
    static public DeviceMessage decode(Buffer data) {
        return decode(data.toString());
    }
    
    static public DeviceMessage decode(Message<String> message) {
        return decode(message.body());
    }
    
	static public WebSocketBase<?> send(final WebSocketBase<?> ws, final DeviceMessage message) {
		// Strip off address info when sending to device
		DeviceMessage.AddrInfo info = message.getAddrInfo();
		try {
			message.setAddrInfo(null);
			String s = JsonUtil.toJsonString(message);
			ws.writeTextFrame(s);
		}
		finally {
			message.setAddrInfo(info);
		}
		return ws;
	}
	
	static public EventBus send(String address, DeviceMessage request) {
		return VertxUtil.getEventBus().send(address, encode(request));
	}
	
	static public EventBus sendToDevice(final DeviceRequest request, final Handler<AsyncResult<Message<String>>> replyHandler) {
		String jsonString = null;
		final DeviceMessage.AddrInfo info = request.createOrGetAddrInfo();
		try {
			request.setAddrInfo(null);
			jsonString = JsonUtil.toJsonString(request);
		}
		finally {
			request.setAddrInfo(info);
		}
		
        if(replyHandler == null) {
            return VertxUtil.getEventBus().send(info.getSocketId(), jsonString);
        }
		
        final String finalJsonString = jsonString;

        // Set destination in request
        if(request.getAddrInfo().getSocketId() == null) {
            info.setSocketId(getDeviceNodeToSocketMap().get(info.getNodeId()));
        }

	    // Create the result object
        final DefaultFutureResult<Message<String>> result = new DefaultFutureResult<>();
        result.setHandler(replyHandler);
        
        // Create the intermediate handler
        final Handler<Message<String>> handler = new Handler<Message<String>>() {
            @Override public void handle(Message<String> msg) {
                result.setResult(msg); // This will call replyHandler.handle()
            }
        };
        
        // Register the handler for the request Id
        VertxUtil.getEventBus().registerHandler(request.getRequestId(), handler, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> regResult) {
                if(regResult.succeeded()) {
                    // Set the timer to unregister the handler after timeout
                    VertxUtil.get().setTimer(5000, new Handler<Long>() {
                        @Override public void handle(Long event) {
                            VertxUtil.getEventBus().unregisterHandler(
                                    request.getRequestId(), handler, new Handler<AsyncResult<Void>>() {
                                @Override
                                public void handle(AsyncResult<Void> unregResult) {
                                    if(unregResult.failed()) {
                                        System.out.println("Failed to unregister handler " + request.getRequestId());
                                    }
                                }
                            });
                            if(result.succeeded() == false) {
                                result.setFailure(new Exception("Timed out!"));
                            }
                        }
                    });
                    VertxUtil.getEventBus().send(info.getSocketId(), finalJsonString);
                }
                else {
                    result.setFailure(new Exception("Failed to register handler"));
                }
            }
        });
		
		return VertxUtil.getEventBus();
	}
	
    static public Map<String, String> getDeviceNodeToSocketMap() {
        return VertxUtil.get().sharedData().getMap(Addresses.toDeviceTopic);
    }
}
