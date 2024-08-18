package ru.otus.june.chat;

import javax.crypto.spec.PSource;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean activeClient;

    public Client() throws IOException {
        Scanner scanner = new Scanner(System.in);
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF();
                    String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    String messageTime = "[" + time + "] ";
                    System.out.print(messageTime);
                    if (message.equals("/exitok")) {
                        break;
                    }
                    if (message.startsWith("/authok ")) {
                        System.out.println("Удалось упешно войти в чат под именем пользователя: " + message.split(" ")[1]);
                        continue;
                    }
                    if (message.startsWith("/regok ")) {
                        System.out.println("Удалось упешно пройти регистрацию войти в чат под именем пользователя: " + message.split(" ")[1]);
                        continue;
                    }
                    if (message.equals("/disconnectBanOk")) {
                        out.writeUTF("/disconnect");
                        break;
                    }
                    if (message.equals("/disconnectInactiveOk")) {
                        System.out.println("Вы отключены из-за неактивности");
                        out.writeUTF("/disconnect");
                        break;
                    }
                    if (message.equals("/shutdownOk")) {
                        System.out.println("Сервер остановлен!");
                        out.writeUTF("/disconnect");
                        break;
                    }
                    System.out.println(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();

        while (true) {
            String message = scanner.nextLine();
            out.writeUTF(message);
            if (message.equals("/exit")) {
                break;
            }
        }
    }

    private void disconnect() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Пользователь отключен!");
    }
}

