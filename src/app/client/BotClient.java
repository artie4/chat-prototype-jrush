package app.client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random()*100);
    }

    public static void main(String[] args) {
        new BotClient().run();
    }

    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            System.out.println(message);
            if (message.contains(": ")) {
                String[] tokens = message.split(": ");
                String username = tokens[0];
                String text = tokens[1];
                String responseTemplate = "Информация для %s: %s";
                switch (text) {
                    case "дата": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("d.MM.YYYY")));
                        break;
                    }
                    case "день": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("d")));
                        break;
                    }
                    case "месяц": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("MMMM")));
                        break;
                    }
                    case "год": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("YYYY")));
                        break;
                    }
                    case "время": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("H:mm:ss")));
                        break;
                    }
                    case "час": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("H")));
                        break;
                    }
                    case "минуты": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("m")));
                        break;
                    }
                    case "секунды": {
                        sendTextMessage(String.format(responseTemplate, username,
                                currentDateWithFormat("s")));
                        break;
                    }
                }
            }
        }

        private String currentDateWithFormat(String pattern) {
            return new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
        }
    }
}
