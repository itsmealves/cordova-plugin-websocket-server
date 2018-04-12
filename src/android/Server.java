package io.github.itsmealves.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

abstract class Observer {
    public abstract void emit(JSONObject json);
}

class Client {
    private String id;
    private WebSocket connection;

    public Client(WebSocket socket) {
        this.id = UUID.randomUUID().toString();
        this.connection = socket;
    }

    public String getId() {
        return this.id;
    }

    public WebSocket getConnection() {
        return this.connection;
    }

    public String getAddress() {
        return this.getConnection().getRemoteSocketAddress().getAddress().getHostAddress();
    }

    @Override
    public boolean equals(Object obj) {
        Client other = (Client) obj;
        return this.getId().equals(other.getId());
    }

    public String toJsonString() {
        StringBuilder json = new StringBuilder();

        json.append("{");

        json.append("\"id\": \"");
        json.append(this.getId());
        json.append("\",");

        json.append("\"address\": \"");
        json.append(this.getAddress());
        json.append("\",");

        json.append("\"resource\": \"");
        json.append(this.getConnection().getResourceDescriptor());
        json.append("\"");

        json.append("}");

        return json.toString();
    }
}

class WSServer extends WebSocketServer {

    private List<Client> clients;
    private Observer errorObserver;
    private Observer messageObserver;
    private Observer connectionObserver;
    private Observer disconnectionObserver;

	public WSServer( int port ) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
        this.clients = new ArrayList<>();
	}

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        Client client = new Client(conn);
        this.clients.add(client);

        if(this.connectionObserver != null) {
            try {
                String clientJson = client.toJsonString();
                String json = "{\"client\": " + clientJson + "}";

                JSONObject jsonObj = new JSONObject(json);

                this.connectionObserver.emit(jsonObj);
            } catch(JSONException e) {}
        }
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        if(code != 1006) {
            Client client = getClientByConnection(conn);
            if(client != null) {
                this.clients.remove(client);

                if(this.disconnectionObserver != null) {
                    try {
                        Log.d("PluginTest", "observer not null");
                        String clientJson = client.toJsonString();
                        Log.d("PluginTest", clientJson);
                        String json = "{\"client\": " + clientJson + ", " +
                            "\"code\": " + code + ", \"reason\": \"" + reason + "\"}";
                        Log.d("PluginTest", json);

                        JSONObject jsonObj = new JSONObject(json);

                        this.disconnectionObserver.emit(jsonObj);
                    } catch(JSONException e) {
                        Log.d("PluginTest", "json error " + e.getMessage());
                        this.disconnectionObserver.emit(null);
                    }
                }
            }
        }
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
        Log.d("PluginTest", "Message");
        Client client = getClientByConnection(conn);

        if(client != null) {
            Log.d("PluginTest", "client found message " + client.getAddress());

            if(this.messageObserver != null) {
                try {
                    String clientJson = client.toJsonString();
                    String json = "{\"client\": " + clientJson + ", " +
                    "\"message\": \"" + message + "\"}";

                    JSONObject jsonObj = new JSONObject(json);

                    this.messageObserver.emit(jsonObj);
                } catch(JSONException e) {
                    this.messageObserver.emit(null);
                }
            }
        } else {
            Log.d("PluginTest", "client not found message");
        }
	}

	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) {
		this.onMessage(conn, message.toString());
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
        Log.d("PluginTest", "Error following");
        Log.d("PluginTest", ex.getMessage());
        Log.d("PluginTest", conn.getRemoteSocketAddress().getAddress().getHostAddress());
        Client client = getClientByConnection(conn);

        if(this.errorObserver != null) {
            try {
                String error = ex.getMessage();
                String clientJson = client.toJsonString();
                String json = "{\"client\": " + clientJson + ", " +
                "\"error\": \"" + error + "\"}";

                JSONObject jsonObj = new JSONObject(json);

                this.errorObserver.emit(jsonObj);
            } catch(JSONException e) {
                this.errorObserver.emit(null);
            }
        }
	}

	@Override
	public void onStart() {}

    private Client getClientByConnection(WebSocket connection) {
        Client c = null;

        Log.d("PluginTest", "searching");
        String address = connection.getRemoteSocketAddress().getAddress().getHostAddress();
        Log.d("PluginTest", address + " searching");

        for(Client client : clients) {
            if(client.getAddress().equals(address)) {
                c = client;
                break;
            }
        }

        return c;
    }

    private Client getClientByAddress(String address) {
        Client c = null;

        Log.d("PluginTest", "searching");
        Log.d("PluginTest", address + " searching");

        for(Client client : clients) {
            if(client.getAddress().equals(address)) {
                c = client;
                break;
            }
        }

        return c;
    }

    private Client getClientById(String id) {
        Client c = null;

        for(Client client : clients) {
            if(client.getId().equals(id)) {
                c = client;
                break;
            }
        }

        return c;
    }

    public void send(String id, String message) {
        Client client = getClientById(id);
        client.getConnection().send(message);
    }

    public void setErrorObserver(Observer observer) {
        this.errorObserver = observer;
    }

    public void setMessageObserver(Observer observer) {
        this.messageObserver = observer;
    }

    public void setConnectionObserver(Observer observer) {
        this.connectionObserver = observer;
    }

    public void setDisconnectionObserver(Observer observer) {
        this.disconnectionObserver = observer;
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

                callbackContext.success(this.server.getPort() + " rocks!");
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
        } else if(action.equals("send")) {
            if(this.server != null) {
                this.server.send(data.getString(0), data.getString(1));
            }

            callbackContext.success();
            return true;
        } else if(action.equals("broadcast")) {
            if(this.server != null) {
                this.server.broadcast(data.getString(0));
            }

            callbackContext.success();
            return true;
        } else if(action.equals("setErrorObserver")) {
            if(this.server != null) {
                this.server.setErrorObserver(new Observer() {
                    @Override
                    public void emit(JSONObject json) {
                        PluginResult result;

                        if(json == null) {
                            result = new PluginResult(PluginResult.Status.OK, "errorobserver");
                        } else {
                            result = new PluginResult(PluginResult.Status.OK, json);
                        }


                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }

            return true;
        } else if(action.equals("setMessageObserver")) {
            if(this.server != null) {
                this.server.setMessageObserver(new Observer() {
                    @Override
                    public void emit(JSONObject json) {
                        PluginResult result;

                        if(json == null) {
                            result = new PluginResult(PluginResult.Status.OK, "messageObserver");
                        } else {
                            result = new PluginResult(PluginResult.Status.OK, json);
                        }


                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }

            return true;
        } else if(action.equals("setConnectionObserver")) {
            if(this.server != null) {
                this.server.setConnectionObserver(new Observer() {
                    @Override
                    public void emit(JSONObject json) {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, json);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                });
            }

            return true;
        } else if(action.equals("setDisconnectionObserver")) {
            if(this.server != null) {
                Log.d("PluginTest", "set disconection called");
                this.server.setDisconnectionObserver(new Observer() {
                    @Override
                    public void emit(JSONObject json) {
                        PluginResult result;

                        if(json == null) {
                            Log.d("PluginTest", "with null");
                            result = new PluginResult(PluginResult.Status.OK, "disconnectionObserver");
                        } else {
                            Log.d("PluginTest", "with json");
                            result = new PluginResult(PluginResult.Status.OK, json);
                        }


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
