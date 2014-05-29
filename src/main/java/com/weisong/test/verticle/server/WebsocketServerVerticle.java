package com.weisong.test.verticle.server;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.weisong.test.message.DeviceMessage;
import com.weisong.test.message.twoway.ChallengeRequest;
import com.weisong.test.message.twoway.ChallengeResponse;
import com.weisong.test.util.Addresses;
import com.weisong.test.util.DeviceMessageUtil;
import com.weisong.test.util.VertxUtil;

public class WebsocketServerVerticle extends Verticle {

	final static private Pattern urlPattern = Pattern.compile("/device/(\\w+)");
	
	private Logger logger;
	
	@RequiredArgsConstructor
	static private class Context {
		final private String socketId;
		final private String nodeId;
		private DeviceAuthStatus authStatus = DeviceAuthStatus.NotAuthenticated;
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
				ws.reject();
				return;
			}

			final Context ctx = new Context(ws.textHandlerID(),  m.group(1));

			ws.closeHandler(new CloseHandler(ctx));
			ws.dataHandler(new DeviceMessageHandler(ws, ctx));
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
				logger.info(String.format("unregistering connection %s [%s]", ctx.nodeId, ctx.socketId));
			}
		}
	};

	@RequiredArgsConstructor
	private class DeviceMessageHandler implements Handler<Buffer> {
		final private ServerWebSocket ws; 
		final private Context ctx; 
		@Override public void handle(final Buffer data) {
			if(shouldReject(data)) {
				ws.reject();
			}
			else {
				DeviceMessage dmsg = DeviceMessageUtil.decode(data);
				dmsg.createOrGetAddrInfo().setNodeId(ctx.nodeId);
				dmsg.createOrGetAddrInfo().setSocketId(ctx.socketId);
				DeviceMessageUtil.send(Addresses.fromDeviceTopic, dmsg);
			}
		}
		
		private boolean shouldReject(Buffer data) {
			if(ctx.authStatus == DeviceAuthStatus.Failed) {
				return true;
			}
			else if(ctx.authStatus == DeviceAuthStatus.NotAuthenticated) {
				DeviceMessage msg = DeviceMessageUtil.decode(data);
				return msg instanceof ChallengeResponse == false;
			}
			return false;
		}
	}
	
	@RequiredArgsConstructor
	private class AuthenticationHandler implements Handler<Long> {
		final private Context ctx; 
		@Override public void handle(Long timeId) {
			ChallengeRequest req = new ChallengeRequest();
			req.setChallengeString("abcd");
			req.createOrGetAddrInfo().setSocketId(ctx.socketId);
			DeviceMessageUtil.sendToDevice(req, new Handler<Message<String>>() {
				@Override public void handle(Message<String> reply) {
					ctx.authStatus = DeviceAuthStatus.Successful;
					Map<String, String> map = DeviceMessageUtil.getDeviceNodeToSocketMap();
					if(map.containsKey(ctx.nodeId)) {
						logger.warn(String.format("Mapping existing: %s -> %s", ctx.nodeId, ctx.socketId));
					}
					map.put(ctx.nodeId, ctx.socketId);
					logger.info(String.format("registering new connection %s [%s]", ctx.nodeId, ctx.socketId));
				}
			});
		}
	}
}