package com.example.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

abstract class MessageObserver {
    public abstract void onMessage(String id, String message);
}


class WSServer extends WebSocketServer {

    private MessageObserver observer;

	public WSServer( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        // TODO: add onConnection callback
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        // TODO: add onDisconnection callback
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
        if(this.observer != null) {
            this.observer.onMessage("123", message);
        }
	}
	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) {
		this.onMessage(conn, message.toString());
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		// TODO: add onError callback
	}

	@Override
	public void onStart() {}

    public void setMessageObserver(MessageObserver observer) {
        this.observer = observer;
    }
}


public class Server extends CordovaPlugin {

    private WSServer server;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (action.equals("start")) {
            try {
                int port = data.getInt(0);
                this.server = new WSServer(port);
                this.server.start();

                callbackContext.success(this.server.getPort());
                return true;
            } catch(UnknownHostException e) {
                callbackContext.error(e.getMessage());
                return false;
            }
        } else if(action.equals("stop")) {
            try {
                this.server.stop(1000);
                callbackContext.success();
                return true;
            } catch(InterruptedException e) {
                callbackContext.error(e.getMessage());
                return false;
            }
        } else if(action.equals("setMessageObserver")) {
            if(this.server != null) {
                this.server.setMessageObserver(new MessageObserver() {
                    @Override
                    public void onMessage(String id, String message) {
                        JSONObject jsonObj = null;

                        try {
                            String json = "{\"id\": \"" + id + "\", " +
                            "\"message\": \"" + message + "\"}";

                            jsonObj = new JSONObject(json);
                        } catch(JSONException e) {}

                        PluginResult result = new PluginResult(PluginResult.Status.OK, jsonObj);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }

            return true;
        } else {
            return false;
        }
    }
}
