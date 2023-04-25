package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Platform;

import java.io.InputStream;
import java.net.Socket;

public class Client implements Runnable {
    private Socket clientSocket;
    private Controller controller;
    private String[] users;

    public Client(Socket client, Controller controller) {
        this.clientSocket = client;
        this.controller = controller;
    }

    @Override
    public void run() {
        try{
            InputStream in = clientSocket.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) != -1) {
                String msg = new String(buf, 0, len);
                getMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMessage(String str) {
        try {
            String[] msgArr = str.split(":");
            String TYPE = msgArr[0];
            if(TYPE.equals("Pri")) {
                String sendBy =  msgArr[1];
                String content = str.substring(4+sendBy.length()+1);
                Platform.runLater(() -> controller.sendMessage(sendBy, content));

            } else if(TYPE.equals("Users")) {
                String userList = str.substring(6);
                users = userList.split(",");
                Platform.runLater(() -> controller.updateUsers(users));

            } else if(TYPE.equals("Gro")) {
                String sendBy = msgArr[1];
                String sendToGroup = msgArr[2];
                String[] members = msgArr[3].split(",");
                String content = str.substring(4+sendBy.length()+1+sendToGroup.length()+1+msgArr[3].length()+1);
                Platform.runLater(() -> controller.updateMessage(sendBy, content, sendToGroup, members));

            } else if(TYPE.equals("Mebe")) {
                String sender = msgArr[1];
                String action = msgArr[2];
                if(action.equals("in")) {
                    Platform.runLater(() -> controller.userLogin(sender));
                } else if(action.equals("out")) {
                    Platform.runLater(() -> controller.userLogout(sender));
                }
            } else if(TYPE.equals("Ser")) {
                Platform.runLater(() -> controller.close());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
