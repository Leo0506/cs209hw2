package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler extends Thread {
    private Socket client;
    private String clientId;
    private Server server;

    public ClientHandler(Socket client, Server server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void run() {
        try (InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {
            String usersStr = String.join(",", server.getClientList().keySet());
            String msgContent = "Users:" + usersStr;
            out.write(msgContent.getBytes());

            byte[] id = new byte[20];
            int clientIdLen = in.read(id);
            clientId = new String(id, 0, clientIdLen).trim();
            server.getClientList().put(clientId, client);

            String userList = "Users:" + String.join(",", server.getClientList().keySet());
            server.getClientList().values().forEach(socket -> {
                try {
                    socket.getOutputStream().write(userList.getBytes());
                } catch (IOException e) {
                    System.out.println("Error");
                }
            });
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                sendMessage(new String(buffer, 0, bytesRead));
            }
        } catch (Exception e) {
            System.out.println("ClientHandler Error.");
        } finally {
            try {
                if (client.equals(server.getClientList().get(clientId))) {
                    server.getClientList().remove(clientId);
                    String msg = "Mebe:" + clientId + ":out";
                    server.getClientList().values().forEach(socket -> {
                        try {
                            socket.getOutputStream().write(msg.getBytes());
                        } catch (IOException e) {
                            System.out.println("Error");
                        }
                    });
                }
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String str) {
        try {
            String regex = "(\\w+):(.*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(str);

            if (matcher.find()) {
                String type = matcher.group(1);
                String body = matcher.group(2);

                switch (type) {
                    case "Pri": {
                        String[] parts = body.split(":");
                        String targetClientId = parts[0];
                        String msgBody = parts[1];
                        String msgContent = "Pri:" + clientId + ":" + msgBody;

                        Socket targetClient = server.getClientList().get(targetClientId);
                        if (targetClient == null) {
                            System.out.println("Target Client Not Found.");
                        }
                        assert targetClient != null;
                        OutputStream out = targetClient.getOutputStream();
                        out.write(msgContent.getBytes());
                        break;
                    }
                    case "Gro": {
                        String[] parts = body.split(":");
                        String[] users = parts[2].split(",");
                        for (String user : users) {
                            if (user.equals(clientId)) {
                                continue;
                            }
                            Socket targetClient = server.getClientList().get(user);
                            if (targetClient == null) {
                                continue;
                            }
                            OutputStream out = targetClient.getOutputStream();
                            out.write(str.getBytes());
                        }
                        break;
                    }
                    case "Mebe:out":
                        server.getClientList().remove(clientId);
                        client.close();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error.");
        }
    }
}