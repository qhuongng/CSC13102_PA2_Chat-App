import java.util.*;
import java.io.*;
import java.net.*;

public class Server implements Runnable {
    private Map<String, PrintWriter> clients;
    private ServerSocket serverSocket;
    private final int port = 7291;

    @Override
    public void run() {
        try {
            clients = new HashMap<>();

            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + ".\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(PrintWriter writer, String message, boolean isBroadcastingOnlineList, String target) {
        // get list of clients
        Set<String> usernames = clients.keySet();
        PrintWriter recvWriter;

        for (String username : usernames) {
            if (!target.isEmpty() || target != "") {
                // send message to private chats
                if (username.equals(target)) {
                    recvWriter = clients.get(username);
                    recvWriter.println(message);
                }
            } else {
                // broadcast
                recvWriter = clients.get(username);

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
        Set<String> usernames = clients.keySet();

        for (String username : usernames) {
            onlineClients += username + ",";
        }

        onlineClients += "!online";

        broadcast(writer, onlineClients, true, "");
    }

    private class ClientHandler implements Runnable {
        private BufferedReader reader;
        private PrintWriter writer;
        private String username;

        public ClientHandler(Socket socket) {
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new PrintWriter(socket.getOutputStream(), true);

                // read username
                String connectMsg = reader.readLine();
                this.username = connectMsg;
                clients.put(username, writer);

                // prints the connect message onto the server
                System.out.println(connectMsg + "!connect\n");

                // broadcast list of online clients to all clients
                broadcastOnlineClients(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;

                while ((message = reader.readLine()) != null) {
                    // remove the client from the online list if they send the disconnect message
                    if (message.equals(username + "!disconnect")) {
                        System.out.println(message + "\n");
                        clients.remove(username);
                        broadcastOnlineClients(writer);
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