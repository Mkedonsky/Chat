package com.geekbrains.chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {


    @FXML
    TextArea textArea;

    @FXML
    TextField msgField, loginField;

    @FXML
    PasswordField passField;

    @FXML
    HBox loginBox;

    @FXML
    ListView<String> clientsList;

    private Network network;
    private String nickname;

    public boolean setAuthenticated(boolean authenticated) {
        loginBox.setVisible(!authenticated);
        loginBox.setManaged(!authenticated);
        msgField.setVisible(authenticated);
        msgField.setManaged(authenticated);
        clientsList.setVisible(authenticated);
        clientsList.setManaged(authenticated);
        return authenticated;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        clientsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                msgField.setText("/w " + clientsList.getSelectionModel().getSelectedItem() + " ");
                msgField.requestFocus();
                msgField.selectEnd();
            }
        });
    }


    public void tryToConnect() {

        try {
            if (network != null && network.isConnected()) {
                return;
            }
            setAuthenticated(false);
            network = new Network(8189);
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = network.readMsg();
                        if (msg.startsWith("/authok ")) { // /authok nick1
                            nickname = msg.split(" ")[1];
                            textArea.appendText("Вы зашли в чат под ником: " + nickname + "\n");
                            setAuthenticated(true);
                            timeCapculeLoad();

                            break;
                        }

                        textArea.appendText(msg + "\n");
                    }
                    while (true) {
                        String msg = network.readMsg();
                        if (msg.startsWith("/")) {
                            if (msg.equals("/end_confirm")) {
                                textArea.appendText("Завершено общение с сервером\n");
                                break;
                            }
                            if (msg.startsWith("/set_nick_to ")) {
                                nickname = msg.split(" ")[1];
                                textArea.appendText("Ваш новый ник: " + nickname + "\n");
                                continue;
                            }

                            if (msg.startsWith("/clients_list ")) { // '/clients_list user1 user2 user3'
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    String[] tokens = msg.split(" ");
                                    for (int i = 1; i < tokens.length; i++) {
                                        if (!nickname.equals(tokens[i])) {
                                            clientsList.getItems().add(tokens[i]);
                                        }
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(msg + "\n");
                        }
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "Соединение с серверов разорвано", ButtonType.OK);
                        alert.showAndWait();
                    });
                } finally {
                    timeCapcule();
                    network.close();
                    setAuthenticated(false);
                    nickname = null;
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Невозможно подключиться к серверу", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            network.sendMsg(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось отправить сообщение, проверьте сетевое подключение", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        try {
            tryToConnect();
            network.sendMsg("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException e) {

            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось отправить сообщение, проверьте сетевое подключение", ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void timeCapcule() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(" history\\" + nickname + ".txt", true))) {
            writer.write(textArea.getText());
        } catch (IOException ignored) {
        }
    }


    private void timeCapculeLoad()  {
        File tcl = new File(" history\\" + nickname + ".txt");
        if (!tcl.exists()) textArea.appendText("История чата не найдена" + System.lineSeparator());
        if (tcl.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tcl))){
                    String tclString;
                    List<String> tclStrings = new ArrayList<>();
                    while ((tclString = bufferedReader.readLine()) == null) break;
                    tclStrings.add(tclString);

                if (tclStrings.size() <= 100) {
                for (String str : tclStrings) {
                    textArea.appendText(str + System.lineSeparator());
                  }
                }
                if (tclStrings.size() > 100) {
                    int firstIndex = tclStrings.size() - 100;
                    for (int counter = firstIndex - 1; counter < tclStrings.size(); counter++) {
                        textArea.appendText(tclStrings.get(counter) + System.lineSeparator());
                    }
                }

            }catch (IOException e){
                e.getStackTrace();
            }
        }
    }
}






