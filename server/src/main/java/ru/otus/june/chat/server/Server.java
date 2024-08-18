package ru.otus.june.chat.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    private final int port;
    private final List<ClientHandler> clients;
    private final AuthentificationProvider authentificationProvider;
    private ScheduledExecutorService executor;
    private List<UserBanned> usersBanned;

    public AuthentificationProvider getAuthentificationProvider() {
        return authentificationProvider;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authentificationProvider = new InMemoryAuthentificationProvider(this);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен на порту: {}", port);
            authentificationProvider.initialize();
            startInactiveClientsCheck();
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (Exception e) {
            logger.error("Ошибка запуска сервера", e);
        }
    }

    public synchronized boolean isUserNameBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized void listOfActiveClients(ClientHandler clientHandler) {
        String message = "";
        for (ClientHandler c : clients) {
            message += c.getUsername() + " ";
        }
        clientHandler.sendMessage("Активные пользователи: " + message);
    }


    public synchronized void sendPrivateMessage(String targetUsername, String content) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(targetUsername)) {
                c.sendMessage(content);
            }
        }
    }

    public void startInactiveClientsCheck() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::removeInactiveClients, 0, 5, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(this::removeBannedUser, 0, 1, TimeUnit.MINUTES);
    }

    public void removeInactiveClients() {
        List<ClientHandler> inactiveClients = new ArrayList<>();
        if (!clients.isEmpty()) {
            logger.info("Проверка активности клиентов");
            for (ClientHandler client : clients) {
                if (System.currentTimeMillis() - client.getLastActiveTime() > 20 * 60 * 1000) {
                    inactiveClients.add(client);
                }
            }
            for (ClientHandler inactiveClient : inactiveClients) {
                logger.info("Клиент " + inactiveClient.getUsername() + " неактивен более 20 минут. Автоматически отключен");
                clients.remove(inactiveClient);
                inactiveClient.sendMessage("/disconnectInactiveOk");
            }
        }
    }

    public void removeBannedUser() {
        try {
            usersBanned = DBAuthenticationProvider.readBanUsers();
        } catch (Exception e) {
            logger.error("Ошибка чтения БД", e);
        }
        long currentTime = System.currentTimeMillis();
        if (!usersBanned.isEmpty()) {
            for (UserBanned c : usersBanned) {
                String username = c.nameUser;
                long banEndTime = c.banTime;
                if (banEndTime != 0 && currentTime >= banEndTime) {
                    logger.info("Проверка временной блокировки клиента");
                    logger.info("Пользователь {} разблокирован", username);
                    try {
                        DBAuthenticationProvider.deleteBanUsers(username);
                    } catch (Exception e) {
                        logger.error("Ошибка удаления из БД", e);
                    }
                }
            }
        }
    }

    public void blockUser(String username, long banTime) {
        long banEndTime;
        if (!(banTime == 0)) {
            banEndTime = System.currentTimeMillis() + banTime * 60000;
        } else
            banEndTime = banTime;
        try {
            DBAuthenticationProvider.writeBanUsers(username, banEndTime);
        } catch (Exception e) {
            logger.error("Ошибка записи в БД", e);
        }
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                if (banTime == 0) {
                    client.sendMessage("Вы забанены навсегда.");
                    client.sendMessage("/disconnectBanOk");
                    clients.remove(client);
                } else {
                    client.sendMessage("Вы забанены администратором на " + banTime + " минут(ы).");
                    client.sendMessage("/disconnectBanOk");
                }
                broadcastMessage("Пользователь " + username + " был заблокирован администратором.");
            }
        }
    }

    public synchronized void listOfBannedClients(ClientHandler clientHandler) throws SQLException {
        List<UserBanned> usersBanned = DBAuthenticationProvider.readBanUsers();
        String message = " ";
        if (usersBanned.isEmpty()) {
            clientHandler.sendMessage("Забаненных пользователей нет");
        } else {
            for (UserBanned c : usersBanned) {
                message += c.nameUser + " ";
            }
            clientHandler.sendMessage("Список забаненных пользователей: " + message);
        }
    }

    public boolean isClientBanned(String username) throws SQLException {
        usersBanned = DBAuthenticationProvider.readBanUsers();
        for (UserBanned c : usersBanned) {
            if (c.nameUser.equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void shutdownServer() {
        logger.info("Сервер успешно остановлен.");
        for (ClientHandler client : clients) {
            client.sendMessage("/shutdownOk");
        }
        System.exit(0);
    }
}

