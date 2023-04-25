package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Server {

    public final static int DEFAULT_PORT_NUMBER = 8880;
    private int portNumber;
    private ServerSocket serverSocket = null;

    private HashMap<String, Socket> clientList;
    public Server() {
        this.portNumber = DEFAULT_PORT_NUMBER;
        this.clientList = new HashMap<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(this.portNumber);
            System.out.println("Server started on port " + portNumber);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Socket> getClientList() {
        return clientList;
    }

}
