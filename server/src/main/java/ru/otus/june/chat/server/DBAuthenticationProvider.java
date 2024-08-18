package ru.otus.june.chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBAuthenticationProvider {
    private static final Logger logger = LogManager.getLogger(DBAuthenticationProvider.class.getName());
    private static Connection connection;
    private static Statement statement;
    public static ResultSet resSet;

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/june-chat", "postgres", "1qaz2wsx");
        logger.info("База Подключена!");
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
    public static List<UserBanned> readBanUsers() throws SQLException {
        resSet = statement.executeQuery("SELECT * FROM ban_users");
        List<UserBanned> usersBanned = new ArrayList<>();
        while (resSet.next()) {
            String nameUser = resSet.getString("user_name");
            Long banTime = resSet.getLong("ban_time");
            usersBanned.add(new UserBanned(nameUser, banTime));
        }
        return usersBanned;
    }

    public static void writeBanUsers(String username, Long banTime) throws SQLException {
        PreparedStatement prepareStatements = connection.prepareStatement("insert into ban_users (user_name, ban_time) values (?, ?)");
        prepareStatements.setString(1, username);
        prepareStatements.setLong(2, banTime);
        prepareStatements.executeUpdate();
    }

    public static void deleteBanUsers(String username) throws SQLException {
        PreparedStatement prepareStatements = connection.prepareStatement("delete from ban_users where user_name=?");
        prepareStatements.setString(1, username);
        prepareStatements.executeUpdate();
    }

    public static void updateUsernameDB(String newlogin, String login) throws SQLException {
        PreparedStatement prepareStatements = connection.prepareStatement("UPDATE users_role SET user_name=? WHERE user_name=?");
        prepareStatements.setString(1, newlogin);
        prepareStatements.setString(2, login);
        prepareStatements.executeUpdate();
    }

    public static void closeDB() throws SQLException {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии соединения с БД", e);
        }
        try {
            if (resSet != null) {
                resSet.close();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии соединения с БД", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии соединения с БД", e);
        }
        logger.info("Close connection");
    }

}
