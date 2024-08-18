package ru.otus.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());
    private final Server server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String username;
    private long lastActiveTime;

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.lastActiveTime = System.currentTimeMillis();
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                logger.info("Подключился новый клиент");
                while (true) {
                    String message = in.readUTF();
                    if (message.equals("/exit")) {
                        sendMessage("/exitok");
                        return;
                    }
                    if (message.startsWith("/auth ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (server.getAuthentificationProvider().authenticate(this, elements[1], elements[2])) {
                            break;
                        }
                        continue;
                    }
                    if (message.startsWith("/register ")) {
                        String[] elements = message.split(" ");
                        if (elements.length != 4) {
                            sendMessage("Неверный формат команды /register");
                        }
                        if (server.getAuthentificationProvider().registration(this, elements[1], elements[2], elements[3])) {
                            break;
                        }
                        continue;
                    }
                    sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username '");
                }
                while (true) {
                    this.lastActiveTime = System.currentTimeMillis();
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/exitok");
                            break;
                        } else if (message.startsWith("/disconnect")) {
                            break;
                        } else if (message.startsWith("/w")) {
                            handlePrivateMessage(message);
                            continue;
                        } else if (message.startsWith("/changenick")) {
                            changeNick(message);
                        } else if (message.startsWith("/ban")) {
                            banUser(message);
                            continue;
                        } else if (message.startsWith("/activelist")) {
                            server.listOfActiveClients(this);
                            continue;
                        } else if (message.startsWith("/blocklist")) {
                            String[] commandParts = message.split(" ");
                            if (commandParts.length != 1) {
                                sendMessage("Неверный формат команды. Используйте '/blocklist'");
                                continue;
                            } else if (server.getAuthentificationProvider().checkIsAdmin(this)) {
                                server.listOfBannedClients(this);
                                continue;
                            }
                        } else if (message.startsWith("/shutdown")) {
                            String[] commandParts = message.split(" ");
                            if (commandParts.length != 1) {
                                sendMessage("Неверный формат команды. Используйте '/shutdown'");
                                continue;
                            } else if (server.getAuthentificationProvider().checkIsAdmin(this)) {
                                server.shutdownServer();
                                break;
                            }
                        } else sendMessage("Неверная команда. Повторите попытку.");
                        continue;
                    }
                    server.broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                logger.error("Ошибка ввода/вывода: ", e);
            } catch (SQLException e) {
                logger.error("Ошибка работы с БД: ", e);
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            logger.error("Ошибка отправки сообщения", e);
        }
    }

    private void banUser(String message) {
        String[] elements = message.split(" ");
        if ((elements.length == 1) || (elements.length > 3)) {
            sendMessage("Неверный формат команды. Используйте '/ban username' или '/ban username time'");
        } else if (this.username.equals(elements[1])) {
            sendMessage("Нельзя забанить самого себя!");
        } else if (server.getAuthentificationProvider().checkUsers(this, elements[1])) {
            int banTime = 0;
            if (elements.length == 3) {
                banTime = Integer.parseInt(elements[2]);
            }
            server.blockUser(elements[1], banTime);
        }
    }

    private void handlePrivateMessage(String message) {
        if (message.split(" ").length >= 3) {
            String targetUsername = message.split(" ")[1];
            String content = message.substring(targetUsername.length() + 4);
            content = "Сообщение от " + username + ": " + content;
            server.sendPrivateMessage(targetUsername, content);
        } else {
            server.broadcastMessage("Ошибка сообщения");
        }
    }

    private void changeNick(String message) {
        String[] elements = message.split(" ");
        if (elements.length == 2) {
            server.getAuthentificationProvider().changeNickName(elements[1], this.getUsername());
            sendMessage("Ваш ник изменен на новый: " + elements[1]);
            this.setUsername(elements[1]);
        } else {
            sendMessage("Неверный формат команды. Используйте '/changenick newUsername'");
        }
    }


    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка дисконнекта: входящего потока", e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка дисконнекта: исходящего потока", e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка дисконнекта: закрытия сокета", e);
        }
    }
}


