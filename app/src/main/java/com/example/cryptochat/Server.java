package com.example.cryptochat;

import android.util.Log;

import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {


    private WebSocketClient client;
    private Map<Long, String> names = new ConcurrentHashMap<>();
    private Consumer<Pair<String, String>> onMessageReceived;
    private Consumer<String> usersConnect;
    private Consumer<Integer> countUsersOnline;

    public Server(Consumer<Pair<String, String>> onMessageReceived,
                  Consumer<String> usersConnect,
                  Consumer<Integer> countUsersOnline) {
        this.onMessageReceived = onMessageReceived;
        this.usersConnect = usersConnect;
        this.countUsersOnline = countUsersOnline;
    }

    public void connect() {
        URI addr = null;
        try {
            addr = new URI("ws://78.111.182.163:8881");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;

        }
        client = new WebSocketClient(addr) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("SERVER", "Connected to server");
            }

            @Override
            public void onMessage(String json) {
                int type = Protocol.getType(json);
                if (type == Protocol.MESSAGE) {
                    displayIncoming(Protocol.unpackMessage(json));
                }
                if (type == Protocol.USER_STATUS) {
                    updateStatus(Protocol.unpackStatus(json));
                }
                Log.i("SERVER", "Got message: " + json);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("SERVER", "Connection closed");
            }

            @Override
            public void onError(Exception ex) {
                Log.e("SERVER", "onError", ex);
            }
        };
        client.connect();
    }

    public void disconnect() {
        client.close();
    }

    public void sendMessage(String text) {
        Protocol.Message mess = new Protocol.Message(text);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packMessage(mess));
        }
    }

    public void sendName(String name) {
        Protocol.UserName userName = new Protocol.UserName(name);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packName(userName));
        }
    }

    private void updateStatus(Protocol.UserStatus status) {
        Protocol.User u = status.getUser();
        if (status.isConnected()) {
            names.put(u.getId(), u.getName());
            usersConnect.accept(u.getName());
        } else {
            names.remove(u.getId());
        }
        countUsersOnline.accept(names.size());
    }

    private void displayIncoming(Protocol.Message message) {
        String name = names.get(message.getSender());
        if (name == null) {
            name = "Unnamed";
        }
        String text = message.getEncodedText();
        onMessageReceived.accept(new Pair<>(name, text));
    }
}
