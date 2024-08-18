package ru.otus.june.chat.server;

import java.sql.SQLException;

public interface AuthentificationProvider {
    void initialize();

    boolean authenticate(ClientHandler clientHandler, String login, String password) throws SQLException;

    boolean registration(ClientHandler clientHandler, String login, String password, String username);

    boolean checkUsers(ClientHandler clientHandler, String login);

    boolean checkIsAdmin(ClientHandler clientHandler);

    boolean changeNickName(String newNick, String username);

    //boolean checkIsBanned(ClientHandler clientHandler, String username);
}

