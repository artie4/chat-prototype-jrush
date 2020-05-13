package app;


import app.model.Message;
import app.utils.ConsoleHelper;
import app.utils.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Server {

    private static final Map<String, Connection> connectionMap = new ConcurrentHashMap();

    public static void main(String[] args) {
        int portNumber = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(portNumber);) {
            System.out.println("Server startet on port - " + portNumber);
            while (true) {
                Socket accept = serverSocket.accept();
                new Handler(accept).start();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void sendBroadcastMessage(Message message) {
        connectionMap.forEach((user, connection) -> {
            try {
                connection.send(message);
            } catch (IOException e) {
                System.out.println("failed");
            }
        });
    }

    private static class Handler extends Thread {
        private final Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = null;
            ConsoleHelper.writeMessage(socket.getRemoteSocketAddress().toString());
            try(Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);

            } catch (IOException | ClassNotFoundException exception) {
                ConsoleHelper.writeMessage("Error happens");
            } finally {
                if (userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                    ConsoleHelper.writeMessage(MessageFormat
                            .format("Connection with {0} aborted", socket.getRemoteSocketAddress()));
                }
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String userName;
            connection.send(new Message(MessageType.NAME_REQUEST));
            Message message = connection.receive();
            userName = message.getData();
            if (!message.getType().equals(MessageType.USER_NAME)
                    || userName == null || userName.isEmpty()
                    || connectionMap.containsKey(userName)) {
                return serverHandshake(connection);
            } else {
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED, "Username accepted"));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                String user = entry.getKey();
                if (!user.equalsIgnoreCase(userName)) {
                    Message message = new Message(MessageType.USER_ADDED, user);
                    connection.send(message);
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName)
                throws IOException, ClassNotFoundException {
            while (true) {
                Message receivedMessage = connection.receive();
                if (MessageType.TEXT.equals(receivedMessage.getType())) {
                    sendBroadcastMessage(new Message(MessageType.TEXT,
                            MessageFormat.format("{0}: {1}", userName, receivedMessage.getData())));
                } else {
                    ConsoleHelper.writeMessage("wrong message type");
                }
            }
        }

    }
}
