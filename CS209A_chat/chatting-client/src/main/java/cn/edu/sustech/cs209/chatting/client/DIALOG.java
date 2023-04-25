package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class DIALOG {
    private String clientUser;
    private String chatName;
    private ObservableList<Message> messages;
    private ObservableList<String> member;
    private List<String> members;

    enum MessageType {
        PRIVATE, GROUP
    }

    public MessageType messageType;

    public DIALOG(String clientUser, String chatName) {
        this.clientUser = clientUser;
        this.chatName = chatName;
        messages = FXCollections.observableArrayList();
        messageType = MessageType.PRIVATE;
    }

    public DIALOG(String clientUser, String chatName, List<String> member) {
        this.clientUser = clientUser;
        this.chatName = chatName;
        this.members = member;
        this.member = FXCollections.observableArrayList();
        this.member.addAll(member);
        messages = FXCollections.observableArrayList();
        messageType = MessageType.GROUP;
    }

    public String getChatName() {
        return chatName;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public ObservableList<String> getMember() {
        return member;
    }


    public void addMessage(Long timestamp, String data) {
        messages.add(new Message(timestamp, clientUser, chatName, data));
    }

    public void getMessages(Long timestamp, String data) {
        messages.add(new Message(timestamp, chatName, clientUser, data));
    }

    public void getMessages(Long timestamp, String data, String sendBy) {
        messages.add(new Message(timestamp, sendBy, chatName, data));
    }
}
