package com.weisong.test.verticle.server;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.weisong.test.message.DeviceMessage;
import com.weisong.test.message.twoway.ChallengeCompleteRequest;
import com.weisong.test.message.twoway.ChallengeCompleteResponse;
import com.weisong.test.message.twoway.ChallengeRequest;
import com.weisong.test.message.twoway.ChallengeResponse;
import com.weisong.test.util.Addresses;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.VertxUtil;

public class WebsocketServerVerticle extends Verticle {

	final static private Pattern urlPattern = Pattern.compile("/device/(\\w+)");
	
	private Logger logger;
	
	static private class Context {
	    final private ServerWebSocket ws;
		final private String socketId;
		final private String nodeId;
		private DeviceAuthStatus authStatus = DeviceAuthStatus.NotAuthenticated;
		private Context(ServerWebSocket ws, String nodeId) {
		    this.ws = ws;
		    this.socketId = ws.textHandlerID();
		    this.nodeId = nodeId;
		}
	}
	
	@Override
	public void start() {
		logger = container.logger();
		VertxUtil.init(vertx);
		vertx.createHttpServer()
			.websocketHandler(new WebSocketHandler())
			.listen(8090);
	}
	
	@RequiredArgsConstructor
	private class WebSocketHandler implements Handler<ServerWebSocket> {
		@Override public void handle(final ServerWebSocket ws) {
			
			final Matcher m = urlPattern.matcher(ws.path());
			if (!m.matches()) {
				rejectOrClose(ws);
				return;
			}

			final Context ctx = new Context(ws,  m.group(1));

			ws.closeHandler(new CloseHandler(ctx));
			ws.dataHandler(new DeviceMessageHandler(ctx));
			vertx.setTimer(200, new AuthenticationHandler(ctx));

		}
	}

	@RequiredArgsConstructor
	private class CloseHandler implements Handler<Void> {
		final private Context ctx; 
		@Override public void handle(final Void event) {
			Map<String, String> nodeToSocketMap = DeviceMessageUtil.getDeviceNodeToSocketMap();
			if(nodeToSocketMap.containsKey(ctx.nodeId)) {
				nodeToSocketMap.remove(ctx.nodeId);
				logger.info(String.format("unregistering (%d) %s [%s]", 
				        nodeToSocketMap.size(), ctx.nodeId, ctx.socketId));
			}
		}
	};

	@RequiredArgsConstructor
	private class DeviceMessageHandler implements Handler<Buffer> {
		final private Context ctx; 
		@Override public void handle(final Buffer data) {
			if(ctx.authStatus == DeviceAuthStatus.Failed) {
				rejectOrClose(ctx.ws);
				return;
			}
			else if(ctx.authStatus == DeviceAuthStatus.NotAuthenticated) {
                DeviceMessage dmsg = DeviceMessageUtil.decode(data);
                if(dmsg instanceof ChallengeResponse == false && dmsg instanceof ChallengeCompleteResponse == false) {
                    System.out.println(String.format("Device %s not authenticated, drop message", ctx.nodeId));
                    return;
                }
			}
			
			DeviceMessage dmsg = DeviceMessageUtil.decode(data);
			dmsg.createOrGetAddrInfo().setNodeId(ctx.nodeId);
			dmsg.createOrGetAddrInfo().setSocketId(ctx.socketId);
			//System.out.println(String.format("Received: %s %s", dmsg.getAddrInfo().getNodeId(), dmsg.getClass().getSimpleName()));
			DeviceMessageUtil.send(Addresses.fromDeviceTopic, dmsg);
		}
	}
	
	@RequiredArgsConstructor
	private class AuthenticationHandler implements Handler<Long> {
		final private Context ctx;
		private int retryCount;
		@Override public void handle(final Long timerId) {
			ChallengeRequest req = new ChallengeRequest();
			req.setChallengeString("abcd");
			req.createOrGetAddrInfo().setSocketId(ctx.socketId);
			DeviceMessageUtil.sendToDevice(req, new Handler<AsyncResult<Message<String>>>() {
				@Override public void handle(AsyncResult<Message<String>> result) {
				    if(result.succeeded()) {
				        completeChallenge(timerId);
				    }
				    else {
                        handleTimeout(timerId);
				    }
				}
			});
		}
		
		private void completeChallenge(final Long timerId) {
		    ChallengeCompleteRequest req = new ChallengeCompleteRequest();
		    req.setSuccessful(true);
            req.createOrGetAddrInfo().setSocketId(ctx.socketId);
            DeviceMessageUtil.sendToDevice(req, new Handler<AsyncResult<Message<String>>>() {
                @Override public void handle(AsyncResult<Message<String>> result) {
                    if(result.succeeded()) {
                        ctx.authStatus = DeviceAuthStatus.Successful;
                        Map<String, String> map = DeviceMessageUtil.getDeviceNodeToSocketMap();
                        if(map.containsKey(ctx.nodeId)) {
                            logger.warn(String.format("Mapping existing: %s -> %s", ctx.nodeId, ctx.socketId));
                        }
                        map.put(ctx.nodeId, ctx.socketId);
                        logger.info(String.format("registering (%d) %s [%s]", 
                                map.size(), ctx.nodeId, ctx.socketId));
                    }
                    else {
                        handleTimeout(timerId);
                    }
                }
            });
		}
		
		private void handleTimeout(Long timerId) {
            if(++retryCount < 5) {
                // Timed out, retry
                logger.warn(String.format("Deivce %s set authentication timed out, retry %d", 
                        ctx.nodeId, retryCount));
                AuthenticationHandler.this.handle(timerId);
            }
            else {
                logger.warn(String.format("Deivce %s authentication timed out %d times, close!!!", 
                        ctx.nodeId, retryCount));
                ctx.ws.close();
            }
		}
	}
	
	private void rejectOrClose(ServerWebSocket ws) {
        try {
            ws.reject();
        }
        catch (Exception e) {
            container.logger().warn("Failed to reject client, closing connection.");
            ws.close();
        }
	}
}