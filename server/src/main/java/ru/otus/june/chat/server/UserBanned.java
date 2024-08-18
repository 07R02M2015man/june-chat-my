package ru.otus.june.chat.server;

public class UserBanned {
    String nameUser;
    Long banTime;

    protected UserBanned(String nameUser, Long banTime) {
        this.nameUser = nameUser;
        this.banTime = banTime;
    }
}
