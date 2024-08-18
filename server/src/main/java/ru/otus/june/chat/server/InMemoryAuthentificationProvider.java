package ru.otus.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

public class InMemoryAuthentificationProvider implements AuthentificationProvider {
    private static final Logger logger = LogManager.getLogger(InMemoryAuthentificationProvider.class.getName());;
    private final Server server;
    private List<User> users;

    public InMemoryAuthentificationProvider(Server server) {
        this.server = server;
        try {
            DBAuthenticationProvider.connect();
            users = DBAuthenticationProvider.readDB();
        } catch (Exception e) {
            logger.error("Ошибка чтения базы данных",e);
        }
    }

    @Override
    public void initialize() {
        logger.info("Сервис аутентификации запущен: DB режим");
    }

    private String getUsernameByLoginAndPassowrd(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdmin(String username) {
        for (User u : users) {
            if (u.username.equals(username) && u.role.equals("ADMIN")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) throws SQLException {
        String authUsername = getUsernameByLoginAndPassowrd(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорректный логин/пароль!");
            return false;
        }
        if (server.isUserNameBusy(authUsername)) {
            clientHandler.sendMessage("Пользователь с такой учетной записью уже авторизован!");
            return false;
        }
        if (server.isClientBanned(authUsername)) {
            clientHandler.sendMessage("Указанный пользователь забанен!");
            return false;
        }
        clientHandler.setUsername(authUsername);
        server.subscribe(clientHandler);
        clientHandler.sendMessage("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 2) {
            clientHandler.sendMessage("Логин 3+ символа, пароль 6+ символов, Имя пользователя 2 +");
            return false;
        }
        if (isLoginAlreadyExist(login)) {
            clientHandler.sendMessage("Указанный логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя уже занято");
            return false;
        }
        users.add(new User(login, password, username, "USER"));
        clientHandler.setUsername(username);
        server.subscribe(clientHandler);

        try {
            DBAuthenticationProvider.writeDB(login, password, username, "USER");
        } catch (Exception e) {
            e.printStackTrace();
        }
        clientHandler.sendMessage("/regok " + username);
        return true;
    }

    @Override
    public boolean checkUsers(ClientHandler clientHandler, String username) {
        if (!isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя не существует!");
            return false;
        }
        if (!server.isUserNameBusy(username)) {
            clientHandler.sendMessage("Пользователь с таким именем не подключен!");
            return false;
        }
        if (!isAdmin(clientHandler.getUsername())) {
            clientHandler.sendMessage("Недостаточно прав для выполнения команды.");
            return false;
        }
        if (isAdmin(username)) {
            clientHandler.sendMessage("Указаный пользователь " + username +" является Админом. Нельзя выполнить указанное действие!");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkIsAdmin(ClientHandler clientHandler) {
        if (!isAdmin(clientHandler.getUsername())) {
            clientHandler.sendMessage("Недостаточно прав для выполнения команды.");
            return false;
        }
        return true;
    }

    @Override
    public boolean changeNickName(String newNick, String username) {
        try {
            DBAuthenticationProvider.updateUsernameDB(newNick, username);
            users = DBAuthenticationProvider.readDB();
        } catch (Exception e) {
            logger.error("Ошибка чтения БД:",e);
        }
        return true;
    }


}

