package app.client;

import app.Connection;
import app.model.Message;
import app.utils.ConsoleHelper;
import app.utils.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private volatile boolean clientConnected = false;

    protected Connection connection;

    protected String getServerAddress() {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage(e.getMessage());
            clientConnected = false;
        }
    }

    public void run() {
        try {
            SocketThread socketThread = getSocketThread();
            socketThread.setDaemon(true);
            socketThread.start();
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException iex) {
            ConsoleHelper.writeMessage(iex.getMessage());
        }
        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
            String clientMessage;
            while (clientConnected) {
                clientMessage = ConsoleHelper.readString();
                if ("exit".equalsIgnoreCase(clientMessage)) {
                    clientConnected = false;
                    break;
                }
                if (shouldSendTextFromConsole()) {
                    sendTextMessage(clientMessage);
                }
            }
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
    }

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("%s joined the chat", userName));
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("%s leaving the chat", userName));
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            for (; ; ) {
                Message receive = connection.receive();
                MessageType messageType = receive.getType();
                if (MessageType.NAME_REQUEST.equals(messageType)) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                } else if (MessageType.NAME_ACCEPTED.equals(messageType)) {
                    notifyConnectionStatusChanged(true);
                    break;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            for (; ; ) {
                Message receive = connection.receive();
                MessageType messageType = receive.getType();
                if (MessageType.TEXT.equals(messageType)) {
                    processIncomingMessage(receive.getData());
                } else if (MessageType.USER_ADDED.equals(messageType)) {
                    informAboutAddingNewUser(receive.getData());
                } else if (MessageType.USER_REMOVED.equals(messageType)) {
                    informAboutDeletingNewUser(receive.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try {
                Socket socket = new Socket(serverAddress, serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException exception) {
                notifyConnectionStatusChanged(false);
            }
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }

}
