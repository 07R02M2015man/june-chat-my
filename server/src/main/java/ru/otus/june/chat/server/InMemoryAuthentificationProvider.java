package ru.otus.june.chat.server;

import java.util.List;

public class InMemoryAuthentificationProvider implements AuthentificationProvider {
    private Server server;
    private List<User> users;

    public InMemoryAuthentificationProvider(Server server) {
        this.server = server;
//        this.users = new ArrayList<>();
//        this.users.add(new User("login1", "pass1", "user1", "USER"));
//        this.users.add(new User("login2", "pass2", "user2", "USER"));
//        this.users.add(new User("login3", "pass3", "user3", "USER"));
//        this.users.add(new User("admin", "admin", "admin", "ADMIN"));
        try {
            DBAuthenticationProvider.connect();
            users = DBAuthenticationProvider.readDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        System.out.println("Сервис аутентификации запущен: DB режим");
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
    public synchronized boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassowrd(login, password);
        if (authUsername == null) {
            clientHandler.sendMessage("Некорректный логин/пароль ");
            return false;
        }
        if (server.isUserNameBusy(authUsername)) {
            clientHandler.sendMessage("Указанная учетная запись уже занята");
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
    public boolean checkKickUser(ClientHandler clientHandler, String username) {
        if (!isUsernameAlreadyExist(username)) {
            clientHandler.sendMessage("Указанное имя пользователя не существует!");
            return false;
        }
        if (!isAdmin(clientHandler.getUsername())) {
            clientHandler.sendMessage("Недостаточно прав для отключения пользователя.");
            return false;
        }
        if (!server.isUserNameBusy(username)) {
            clientHandler.sendMessage("Пользователь с таким именем не подключен!");
            return false;
        }
        System.out.println(clientHandler.getUsername());
        System.out.println(username);
        if (clientHandler.getUsername().equals(username)){
            clientHandler.sendMessage("Нельзя удалить самого себя!");
            return false;
        }
        return true;
    }



}

