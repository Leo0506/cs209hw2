package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args)  {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            List<Socket> clients = new ArrayList<>(server.getClientList().values());
            for (int i = 0; i < clients.size(); i++) {
                Socket socket = clients.get(i);
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write("Ser:".getBytes());
                    socket.close();
                    if (i == clients.size() - 1) {
                        server.getClientList().clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        server.start();
    }
}
