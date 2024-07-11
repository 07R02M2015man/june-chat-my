package ru.otus.june.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBAuthenticationProvider {
    private static Connection connection;
    private static Statement statement;
    public static ResultSet resSet;

    public static void connect() throws ClassNotFoundException, SQLException {

        Class.forName("org.sqlite.JDBC");

//        connection = DriverManager.getConnection("jdbc:sqlite:/Users/tatanamakarenko/Documents/Java. Basic/BDTests/june-chat.db");
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/june-chat", "postgres", "1qaz2wsx");
        System.out.println("База Подключена!");
        statement = connection.createStatement();
    }


    public static List<User> readDB() throws SQLException {
        resSet = statement.executeQuery("SELECT * FROM users_role");
        List<User> users = new ArrayList<>();
        while (resSet.next()) {
            String login = resSet.getString("login");
            String password = resSet.getString("password");
            String username = resSet.getString("user_name");
            String role = resSet.getString("role");
            users.add(new User(login, password, username, role));
        }
        return users;
    }

    public static void writeDB(String login, String password, String username, String role) throws SQLException {
        PreparedStatement prepareStatements = connection.prepareStatement("insert into users (login, password, user_name, role) values (?, ?, ?, ?)");
        prepareStatements.setString(1, login);
        prepareStatements.setString(2, password);
        prepareStatements.setString(3, username);
        prepareStatements.setString(4, role);
        prepareStatements.executeUpdate();
    }

    public static void closeDB() throws SQLException {
//        connection.close();
//        statement.close();
//        resSet.close();

        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (resSet != null) {
                resSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Close connection");
    }

}
