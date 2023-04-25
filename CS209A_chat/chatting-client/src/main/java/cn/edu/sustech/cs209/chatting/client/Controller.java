package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    public TextArea inputArea;

    @FXML
    public Label online_number;
    @FXML
    ListView<String> onlineList = new ListView<>();
    @FXML
    ListView<String> groupList;
    private ObservableList<String> groupItems;
    public Label curUser;
    private Socket clientSocket;
    ObservableList<String> users;
    DIALOG d;
    @FXML
    ListView<String> chatList;
    @FXML
    ListView<Message> chatContentList;
    private String chosen;
    @FXML
    public Label groupInfo;
    private ObservableList<DIALOG> dialogs;
    private ObservableList<String> items;
    private ObservableList<Message> messageItem;
    private String username;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        users = FXCollections.observableArrayList();
        groupItems = FXCollections.observableArrayList();
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText("Please Log in");
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            username = input.get();
        }
        try {
            clientSocket = new Socket("127.0.0.1", 8880);
            InputStream in = clientSocket.getInputStream();
            byte[] buf = new byte[1024];
            int len = in.read(buf);
            String userList = new String(buf, 0, len);
            if(userList.startsWith("Users:")){
                userList = userList.substring(6);
                users.addAll(userList.split(","));
            }
            while (users != null && users.stream().anyMatch(user -> user.equals(username))) {
                dialog = new TextInputDialog();
                dialog.setContentText("Try again. Username already exists");
                Optional<String> reInput = dialog.showAndWait();
                if (reInput.isPresent() && !reInput.get().isEmpty()) {
                    username = reInput.get();
                } else {
                    Platform.exit();
                }
            }
            curUser.setText("current username: " + username);
            clientSocket.getOutputStream().write(username.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Platform.exit();
        }

        Thread msgListener = new Thread(new Client(clientSocket, this));
        msgListener.setDaemon(true);
        msgListener.start();
        chatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                chosen = newValue;
                d = dialogs.get(items.indexOf(newValue));
                messageItem = d.getMessages();
                chatContentList.setItems(FXCollections.observableArrayList());
                chatContentList.getItems().clear();
                chatContentList.setItems(messageItem);
                chatContentList.scrollTo(messageItem.size() - 1);
                if(d.messageType == DIALOG.MessageType.GROUP) {
                    groupItems = d.getMember();
                    groupList.setItems(FXCollections.observableArrayList());
                    groupList.getItems().clear();
                    groupList.setItems(groupItems);

                } else {
                    groupList.setItems(FXCollections.emptyObservableList());
                }
            }
        });
        dialogs = FXCollections.observableArrayList();
        items = FXCollections.observableArrayList();
        chatList.setItems(items);
        chatContentList.setCellFactory(new MessageCellFactory());
        groupList.setItems(groupItems);
        groupList.setCellFactory(new userCellFactory());
    }


    @FXML
    public void showOnlineList(){
        onlineList.setItems(users);
        Stage stage = new Stage();
        stage.setTitle("Check the online users");
        stage.setWidth(300);
        stage.setHeight(300);
        HBox box = new HBox(50);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(onlineList);
        stage.setScene(new Scene(box));
        stage.showAndWait();
    }

    @FXML
    public void createPrivateChat() {
        Stage stage = new Stage();
        stage.setTitle("Create Private Chat");
        stage.setWidth(300);
        stage.setHeight(200);

        Label label = new Label("Select a user:");
        ToggleGroup group = new ToggleGroup();

        List<RadioButton> radioButtons = users.stream()
                .filter(u -> !u.equals(username))
                .map(u -> {
                    RadioButton rb = new RadioButton(u);
                    rb.setToggleGroup(group);
                    return rb;
                })
                .toList();

        VBox vbox = new VBox(10, label);
        vbox.getChildren().addAll(radioButtons);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            RadioButton selectedRB = (RadioButton) group.getSelectedToggle();
            if (selectedRB != null) {
                String u = selectedRB.getText();
                if (!items.contains(u)) {
                    items.add(u);
                    dialogs.add(new DIALOG(username, u));
                }
                chosen = u;
                stage.close();
            }
        });

        HBox hbox = new HBox(10, okBtn);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(20));

        BorderPane borderPane = new BorderPane(vbox);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.showAndWait();

        if (chosen != null) {
            chatList.getSelectionModel().select(chosen);
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat(){
        Stage stage = new Stage();
        stage.setTitle("Create Group Chat");
        stage.setWidth(300);
        stage.setHeight(350);

        Label userLabel = new Label("Select users to add to the group:");
        VBox userBox = new VBox(10, userLabel);

        List<CheckBox> checkBoxList = new ArrayList<>();

        users.stream()
                .filter(user -> !user.equals(username))
                .forEach(user -> {
                    CheckBox checkBox = new CheckBox(user);
                    checkBoxList.add(checkBox);
                    userBox.getChildren().add(checkBox);
                });

        TextArea groupNameField = new TextArea();
        groupNameField.setPromptText("Enter group name");
        groupNameField.setWrapText(true);
        groupNameField.setPrefRowCount(2);
        VBox nameBox = new VBox(10, new Label("Group name:"), groupNameField);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            List<String> selectedUsers = checkBoxList.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());
            selectedUsers.add(username);
            String groupName = groupNameField.getText().trim();
            if(selectedUsers.size() < 3 || groupName.isEmpty() ) {
                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Warning");
                dialog.setContentText("Please make sure the group must have at least 3 members and the group can't be empty");
                ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().add(okButton);
                dialog.showAndWait();
                return;
            }
            stage.close();
            for (DIALOG dd : dialogs) {
                if (dd.getChatName().equals(groupName)) {
                    chosen = groupName;
                    chatList.getSelectionModel().select(chosen);
                    return;
                }
            }
            items.add(groupName);
            dialogs.add(new DIALOG(username, groupName, selectedUsers));
            chosen = groupName;
            chatList.getSelectionModel().select(chosen);
        });

        HBox btnBox = new HBox(10, okBtn);
        btnBox.setAlignment(Pos.CENTER);
        VBox box = new VBox(10, userBox, nameBox, btnBox);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        stage.setScene(new Scene(box));
        stage.showAndWait();
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void Send() throws IOException {
        String input = inputArea.getText().trim();
        if (input.isEmpty() || d == null) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String sendTo = d.getChatName();

        String msgContent;
        if (d.messageType == DIALOG.MessageType.GROUP) {
            String sendToGroup = sendTo;
            String sendToParticipant = String.join(",", d.getMember());
            String sendBy = username;
            msgContent = String.format("Gro:%s:%s:%s:%s", sendBy, sendToGroup, sendToParticipant, input);
        } else {
            msgContent = String.format("Pri:%s:%s", sendTo, input);
        }
        clientSocket.getOutputStream().write(msgContent.getBytes());

        dialogs.get(items.indexOf(chosen)).addMessage(timestamp, input);
        inputArea.setText("");
    }


    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    ObservableList<Message> chatContentItem = getListView().getItems(); // 获取当前的chatContentItem
                    if (!chatContentItem.contains(msg)) { // 检查新的消息是否在chatContentItem中
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);

                    nameLabel.setStyle("-fx-background-color: grey ; -fx-border-width: 5px;");
                    nameLabel.setStyle(username);

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private static class userCellFactory implements Callback<ListView<String>, ListCell<String>> {
        @Override
        public ListCell<String> call(ListView<String> param) {
            return new ListCell<String>() {

                @Override
                public void updateItem(String user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || Objects.isNull(user)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(user);
                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    wrapper.setAlignment(Pos.TOP_LEFT);
                    wrapper.getChildren().addAll(nameLabel);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }


    public void sendMessage(String sendBy, String content) {
        int index = items.indexOf(sendBy);
        if (index < 0) {
            items.add(sendBy);
            dialogs.add(new DIALOG(username, sendBy));
            index = items.size() - 1;
        }
        dialogs.get(index).getMessages(System.currentTimeMillis(), content);
        try{
            InputStream is = getClass().getResourceAsStream("ring.mp3");
            assert is != null;
            BufferedInputStream bis = new BufferedInputStream(is);
            Player player = new Player(bis);
            player.play();
        }   catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateUsers(String[] new_users) {
        users.setAll(new_users);
        Arrays.stream(new_users)
                .filter(user -> !items.contains(user) && !user.equals(username))
                .forEach(user -> {
                    items.add(user);
                    dialogs.add(new DIALOG(username, user));
                });
        online_number.setText("online number: " + users.size());
    }

    public void userLogin(String user) {
        if(!items.contains(user) && !user.equals(username)) {
            users.add(user);
            items.add(user);
            dialogs.add(new DIALOG(username, user));
        }
    }

    public void userLogout(String user) {
        if (!user.equals(username)) {
            users.remove(user);
        }
    }



    public void updateMessage(String sendBy, String content, String sendToGroup, String[] participant) {
        if(!items.contains(sendToGroup)) {
            items.add(sendToGroup);
            dialogs.add(new DIALOG(username, sendToGroup, Arrays.stream(participant).toList()));
        }
        Long timestamp = System.currentTimeMillis();
        dialogs.get(items.indexOf(sendToGroup)).getMessages(timestamp, content, sendBy);
        try {
            InputStream is = getClass().getResourceAsStream("ring.mp3");
            assert is != null;
            BufferedInputStream bis = new BufferedInputStream(is);
            Player player = new Player(bis);
            player.play();
        }   catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }


    public void close() {
        updateUsers(new String[]{username});
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Server Close");
        dialog.setHeaderText("Sorry, the server has been closed.");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
        System.exit(0);
    }

}
