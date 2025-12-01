package org.example.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogWebSocketServer extends WebSocketServer {

    private final CopyOnWriteArrayList<WebSocket> clients = new CopyOnWriteArrayList<>();

    public LogWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        clients.add(webSocket);
        System.out.println("New client connected. Total clients: " + clients.size());
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        clients.remove(webSocket);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.err.println("WebSocket error: " + e.getMessage());
        if (webSocket != null) {
            clients.remove(webSocket);
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully");
    }

    public void broadcast(String msg) {
        for (WebSocket webSocket : clients) {
            if (webSocket.isOpen()) {
                try {
                    webSocket.send(msg);
                } catch (Exception e) {
                    System.err.println("Failed to send to client: " + e.getMessage());
                }
            }
        }
    }
}