import java.util.*;
import java.io.*;
import java.net.*;

public class Server implements Runnable {
    private Map<String, PrintWriter> logins;
    private Map<String, String> clients;
    private Map<String, PrintWriter> onlineClientWriters;
    private ServerSocket serverSocket;
    private final int port = 7291;

    @Override
    public void run() {
        try {
            clients = new HashMap<>();
            loadUserDatabase();
            System.out.println(clients);

            logins = new HashMap<>();
            onlineClientWriters = new HashMap<>();

            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + ".\n");

            while (true) {
                Socket socket = serverSocket.accept();

                new Thread(new SocketHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authRespond(PrintWriter writer, String message, String target) {
        Set<String> loginNames = logins.keySet();
        PrintWriter recvWriter;

        for (String loginName : loginNames) {
            if (loginName.equals(target)) {
                recvWriter = logins.get(loginName);
                recvWriter.println(message);
            }
        }
    }

    public void broadcast(PrintWriter writer, String message, boolean isBroadcastingOnlineList, String target) {
        // get list of clients
        Set<String> usernames = onlineClientWriters.keySet();
        PrintWriter recvWriter;

        for (String username : usernames) {
            if (!target.isEmpty() || target != "") {
                // send message to private chats
                if (username.equals(target)) {
                    recvWriter = onlineClientWriters.get(username);
                    recvWriter.println(message);
                }
            } else {
                // broadcast
                recvWriter = onlineClientWriters.get(username);

                if (isBroadcastingOnlineList) {
                    recvWriter.println(message);
                } else {
                    if (recvWriter != writer) {
                        recvWriter.println(message);
                    }
                }
            }
        }
    }

    public void broadcastOnlineClients(PrintWriter writer) {
        String onlineClients = "";
        Set<String> usernames = onlineClientWriters.keySet();

        for (String username : usernames) {
            onlineClients += username + ",";
        }

        onlineClients += "!online";

        broadcast(writer, onlineClients, true, "");
    }

    public String convertToCsv(String[] credentials) {
        return String.join(",", credentials);
    }

    public boolean writeToUserDatabase(String[] credentials) {
        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter("data/clients.csv", true));

            buffer.write(convertToCsv(credentials));
            buffer.newLine();
            buffer.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean loadUserDatabase() {
        try {
            BufferedReader buffer = new BufferedReader(new FileReader("data/clients.csv"));
            String line = "";

            while ((line = buffer.readLine()) != null) {
                String[] data = line.split("\\,");

                if (data.length != 2)
                    continue;

                String username = data[0];
                String password = data[1];

                clients.put(username, password);
            }

            buffer.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private class SocketHandler implements Runnable {
        private BufferedReader reader;
        private PrintWriter writer;
        private String name;

        public SocketHandler(Socket socket) {
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new PrintWriter(socket.getOutputStream(), true);

                String firstTouch = reader.readLine();

                if (!firstTouch.contains("!authenticate")) {
                    this.name = firstTouch;

                    // print the connect message onto the server
                    System.out.println(this.name + "!connect\n");

                    // broadcast list of online clients to all clients
                    onlineClientWriters.put(name, writer);
                    broadcastOnlineClients(writer);
                } else {
                    this.name = firstTouch.split("!")[0];
                    logins.put(name, writer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;

                while ((message = reader.readLine()) != null) {
                    System.out.println("server: " + message + "\n");

                    // remove the client from the online list if they send the disconnect message
                    if (message.equals(name + "!disconnect")) {
                        System.out.println(message + "\n");
                        onlineClientWriters.remove(name);
                        broadcastOnlineClients(writer);
                    } else if (message.contains("!signup")) {
                        String[] msgTokens = message.split("!");
                        String[] credentials = msgTokens[0].split(",");

                        if (clients.containsKey(credentials[0])) {
                            authRespond(writer, credentials[0] + "!accexists", name);
                        } else {
                            clients.put(credentials[0], credentials[1]);
                            writeToUserDatabase(credentials);
                            authRespond(writer, credentials[0] + "!signupsuccess", name);
                        }
                    } else if (message.contains("!login")) {
                        String[] msgTokens = message.split("!");
                        String[] credentials = msgTokens[0].split(",");

                        if (!clients.containsKey(credentials[0])) {
                            authRespond(writer, credentials[0] + "!noacc", name);
                        } else if (!clients.get(credentials[0]).equals(credentials[1])) {
                            authRespond(writer, credentials[0] + "!wrongpass", name);
                        } else {
                            authRespond(writer, credentials[0] + "!loginsuccess", name);
                        }
                    } else if (message.equals(name + "!donelogin")) {
                        logins.remove(name);
                    } else {
                        // split the messages and targets
                        String[] msgTokens = message.split("!");
                        String target = msgTokens[1].split(":")[1];

                        broadcast(writer, msgTokens[0], false, target);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}