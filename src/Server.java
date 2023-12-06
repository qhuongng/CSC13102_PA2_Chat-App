import java.util.*;
import java.io.*;
import java.net.*;

public class Server implements Runnable {
    private ArrayList<PrintWriter> clientWriters;
    private ServerSocket serverSocket;
    private final int port = 8888;

    @Override
    public void run() {
        try {
            clientWriters = new ArrayList<>();

            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port + ".\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(writer);

                new Thread(new ClientHandler(clientSocket, writer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket, PrintWriter writer) {
            this.clientSocket = socket;

            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = writer;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    // broadcast the message to all connected clients
                    for (PrintWriter clientWriter : clientWriters) {
                        if (clientWriter != writer) {
                            clientWriter.println(message);
                        }
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