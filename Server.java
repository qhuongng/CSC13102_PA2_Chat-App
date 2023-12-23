import java.io.*;
import java.net.*;
import java.util.*;

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

    public void broadcast(PrintWriter writer, String message, boolean isBroadcastingToAll, String target) {
        // get list of clients
        Set<String> usernames = onlineClientWriters.keySet();
        PrintWriter recvWriter;

        for (String username : usernames) {
            if (!target.isEmpty() || target != "") {
                if (username.equals(target)) {
                    recvWriter = onlineClientWriters.get(username);
                    recvWriter.println(message);
                }
            } else {
                // broadcast
                recvWriter = onlineClientWriters.get(username);

                if (isBroadcastingToAll) {
                    recvWriter.println(message);
                } else {
                    if (recvWriter != writer) {
                        recvWriter.println(message);
                    }
                }
            }
        }
    }

    public void groupBroadcast(PrintWriter writer, String message, String target) {
        // get list of clients
        Set<String> usernames = onlineClientWriters.keySet();
        PrintWriter recvWriter;

        for (String username : usernames) {
            if (target.contains(username)) {
                recvWriter = onlineClientWriters.get(username);

                if (recvWriter != writer) {
                    recvWriter.println(message + "!group:" + target);
                }
            }
        }
    }

    public void broadcastClients(PrintWriter writer, Map<String, ?> map, String mode, String target) {
        String broadcastMsg = "";
        Set<String> usernames = map.keySet();

        for (String username : usernames) {
            broadcastMsg += username + ",";
        }

        if (mode == "online") {
            broadcastMsg += "!online";
            broadcast(writer, broadcastMsg, true, "");
        } else if (mode == "all") {
            broadcastMsg += "!all";
            broadcast(writer, broadcastMsg, false, target);
        }
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

                if (data.length == 1) {
                    // group chat
                    String username = data[0];
                    clients.put(username, "");
                } else if (data.length == 2) {
                    String username = data[0];
                    String password = data[1];

                    clients.put(username, password);
                }
            }

            buffer.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean writeChatHistory(String sender, String receiver, String message) {
        try {
            // write to sender's file
            File senderChatHistoryFile = new File("data/chats/" + sender + "/" + receiver + ".txt");

            if (!senderChatHistoryFile.exists()) {
                senderChatHistoryFile.createNewFile();
            }

            if (senderChatHistoryFile.exists() && !senderChatHistoryFile.isDirectory()) {
                BufferedWriter buffer = new BufferedWriter(new FileWriter(senderChatHistoryFile, true));

                buffer.write(sender + ":" + message);
                buffer.newLine();

                buffer.close();
            }

            // write to receiver's file
            File receiverChatHistoryFile = new File("data/chats/" + receiver + "/" + sender + ".txt");

            if (!receiverChatHistoryFile.exists()) {
                receiverChatHistoryFile.createNewFile();
            }

            if (receiverChatHistoryFile.exists() && !receiverChatHistoryFile.isDirectory()) {
                BufferedWriter buffer = new BufferedWriter(new FileWriter(receiverChatHistoryFile, true));

                buffer.write(sender + ":" + message);
                buffer.newLine();

                buffer.close();
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean writeGroupChatHistory(String sender, String receiver, String message) {
        try {
            String[] targets = receiver.split(";");

            for (int i = 0; i < targets.length; i++) {
                File chatHistoryFile = new File("data/chats/" + targets[i] + "/" + receiver + ".txt");

                if (!chatHistoryFile.exists()) {
                    chatHistoryFile.createNewFile();
                }

                if (chatHistoryFile.exists() && !chatHistoryFile.isDirectory()) {
                    BufferedWriter buffer = new BufferedWriter(new FileWriter(chatHistoryFile, true));

                    buffer.write(sender + ":" + message);
                    buffer.newLine();

                    buffer.close();
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean clearChatHistory(String sender, String receiver) {
        try {
            File chatHistoryFile = new File("data/chats/" + sender + "/" + receiver + ".txt");

            if (chatHistoryFile.exists() && !chatHistoryFile.isDirectory()) {
                BufferedWriter buffer = new BufferedWriter(new FileWriter(chatHistoryFile));

                buffer.write("");
                buffer.close();

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getChatHistory(String sender, String receiver) {
        try {
            String messages = "";
            File chatHistoryFile = new File("data/chats/" + sender + "/" + receiver + ".txt");

            if (!chatHistoryFile.exists()) {
                chatHistoryFile.createNewFile();
            }

            if (chatHistoryFile.exists() && !chatHistoryFile.isDirectory()) {
                BufferedReader buffer = new BufferedReader(new FileReader(chatHistoryFile));
                String line = "";

                while ((line = buffer.readLine()) != null) {
                    if (!line.equals("") && !line.isEmpty())
                        messages += line + ";;";
                }

                messages += "!history";

                buffer.close();

                return messages;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Cannot retrieve chat history";
    }

    private boolean receiveFileFromClient(String sender, String receiver, InputStream istream, String fname) {
        try {
            String path = "data/files/" + sender + "/" + receiver + "/";
            File directory = new File(path);
            directory.mkdirs();

            try (OutputStream ostream = new FileOutputStream(path + fname)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while (istream.available() > 0 && (bytesRead = istream.read(buffer)) != -1) {
                    ostream.write(buffer, 0, bytesRead);
                }

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    // private void sendFileToClient(Socket socket, String sender, String receiver,
    // String fname) {
    // try {
    // // construct the file path on the server
    // String fpath = "data/files/" + sender + "/" + receiver + "/" + fname;

    // // check if the file exists
    // File file = new File(fpath);

    // if (!file.exists()) {
    // System.out.println("File not found: " + fpath);
    // return;
    // }

    // FileInputStream fistream = new FileInputStream(fpath);
    // byte[] buffer = new byte[1024];
    // int bytesRead;

    // while ((bytesRead = fistream.read(buffer)) != -1) {
    // socket.getOutputStream().write(buffer, 0, bytesRead);
    // }

    // fistream.close();
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }

    private class SocketHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String name;

        public SocketHandler(Socket socket) {
            try {
                this.socket = socket;
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new PrintWriter(socket.getOutputStream(), true);

                String firstTouch = reader.readLine();

                if (!firstTouch.contains("!authenticate")) {
                    this.name = firstTouch;

                    // print the connect message onto the server
                    System.out.println(this.name + "!connect\n");

                    onlineClientWriters.put(name, writer);

                    broadcastClients(writer, clients, "all", name);

                    // broadcast list of online clients to all clients
                    broadcastClients(writer, onlineClientWriters, "online", "");
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
                        onlineClientWriters.remove(name);
                        broadcastClients(writer, onlineClientWriters, "online", "");
                        break;
                    } else if (message.contains("!signup")) {
                        String[] msgTokens = message.split("!");
                        String[] credentials = msgTokens[0].split(",");

                        if (clients.containsKey(credentials[0])) {
                            authRespond(writer, credentials[0] + "!accexists", name);
                        } else {
                            clients.put(credentials[0], credentials[1]);
                            writeToUserDatabase(credentials);
                            // create a chat history folder for the user
                            new File("data/chats/" + credentials[0]).mkdirs();

                            authRespond(writer, credentials[0] + "!signupsuccess", name);
                            broadcast(writer, credentials[0] + "!newclient", true, "");
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
                    } else if (message.contains("!chatselect")) {
                        // extract target name
                        String target = message.split("!")[0];

                        // import chat history
                        String messages = getChatHistory(name, target);
                        broadcast(writer, messages, false, name);
                    } else if (message.contains("!target:")) {
                        // message structure: <sender>: <body>!target:<receiver>
                        String[] msgTokens = message.split("!");
                        String sender = msgTokens[0].split(":")[0];
                        String msgBody = msgTokens[0].split(":")[1];
                        String target = msgTokens[1].split(":")[1];

                        if (!target.contains(";")) {
                            // private chat
                            // send the message to the target client
                            broadcast(writer, msgTokens[0], false, target);

                            // write the message to the chat history file
                            writeChatHistory(sender, target, msgBody);
                        } else {
                            // group chat
                            groupBroadcast(writer, msgTokens[0], target);

                            // write the message to the chat history file
                            writeGroupChatHistory(sender, target, msgBody);
                        }
                    } else if (message.contains("!deletechat:")) {
                        // extract target name
                        String target = message.split(":")[1];

                        clearChatHistory(name, target);
                    } else if (message.contains("!file:")) {
                        // message structure: <receiver>!file:<filename>
                        String receiver = message.split("!")[0];
                        String fname = message.split(":")[1];

                        // read file content from the client
                        InputStream fstream = socket.getInputStream();

                        // handle the file transfer
                        if (receiveFileFromClient(name, receiver, fstream, fname)) {
                            String recvMsg = " sent file [" + fname + "]";

                            broadcast(writer, name + ":" + recvMsg, false, receiver);
                            writeChatHistory(name, receiver, recvMsg);
                        }
                    } else if (message.contains("!download:")) {
                        // message structure: <sender>: sent file [file name]!download:<receiver>
                        String sender = message.split(":")[0];
                        String receiver = message.split(":")[2];
                        String fname = message.split("\\[|\\]")[1];

                        // TODO: handle sending file to client
                        // sendFileToClient(socket, sender, receiver, fname);
                    } else if (message.contains("!newgroup")) {
                        String groupName = message.split("!")[0];

                        clients.put(groupName, "");

                        String[] credentials = { groupName, "" };
                        writeToUserDatabase(credentials);

                        // update the client list on all clients
                        broadcast(writer, groupName + "!newgroup", true, "");
                        broadcastClients(writer, onlineClientWriters, "online", "");
                    } else {
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Server()).start();
    }
}