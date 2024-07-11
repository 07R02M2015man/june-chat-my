package ru.otus.june.chat.server;

public class User {
    String login;
    String password;
    String username;
    String role;

    protected User(String login, String password, String username, String role) {
        this.login = login;
        this.password = password;
        this.username = username;
        this.role = role;
    }
}

