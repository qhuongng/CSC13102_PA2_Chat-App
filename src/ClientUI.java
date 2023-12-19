import java.util.*;
import java.io.*;
import java.net.Socket;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

public class ClientUI extends JFrame {
    private JFileChooser chooser;
    private ClientLoginUI login;

    private String username;
    private PrintWriter writer;

    // false: offline, true: online
    private Map<String, Boolean> clients;
    private DefaultListModel<String> messages;

    // when clicked into a chat, load targets with the users in that chat
    // server will only broadcast the message to users in targets
    private String target;

    private JList<String> clientListPane;
    private JTextArea textInput;

    public ClientUI() {
        super();

        this.login = new ClientLoginUI(this);
        this.clients = new HashMap<>();
        this.target = "";

        login.openDialog(true);

        while (login.isLoggedIn() == false) {
            if (login.isClosed()) {
                dispose();
                break;
            }
        }

        if (!login.isClosed()) {
            this.username = login.getUsername();
            setTitle(username);

            // create a chat history folder for the user
            // will not do anything if the folder already exists
            new File("data/chats/" + username).mkdirs();

            initUI();

            new Thread(new Client()).start();
        }
    }

    private void initUI() {
        messages = new DefaultListModel<>();
        JList<String> messagePane = new JList<>(messages);
        JScrollPane messageScrollPane = new JScrollPane(messagePane);

        textInput = new JTextArea(3, 30);
        textInput.setLineWrap(true);
        textInput.setWrapStyleWord(true);
        textInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();

                    if (target != "") {
                        sendMessage();
                    }
                }
            }
        });

        JScrollPane textInputScrollPane = new JScrollPane(textInput);

        JButton sendButton = new JButton("Send");
        sendButton.setFocusable(false);
        sendButton.setEnabled(false);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JButton sendFileButton = new JButton("Send file");
        sendFileButton.setFocusable(false);
        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFileChooser(false);
            }
        });

        JPanel inputPane = new JPanel(new BorderLayout(10, 10));
        inputPane.add(sendFileButton, BorderLayout.WEST);
        inputPane.add(textInputScrollPane, BorderLayout.CENTER);
        inputPane.add(sendButton, BorderLayout.EAST);

        /**
         * Adapted from @bobasti on StackOverflow:
         * https://stackoverflow.com/a/35973147
         * 
         * Add a DocumentListener to JTextFields to enable a button if all fields
         * are filled.
         */
        DocumentListener fieldListener = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                boolean emptyInput = (textInput.getText().isEmpty() || textInput.getText() == null);

                if (!emptyInput) {
                    if (target != "") {
                        sendButton.setEnabled(true);
                    }
                } else {
                    sendButton.setEnabled(false);
                }
            }
        };

        textInput.getDocument().addDocumentListener(fieldListener);
        /* end of adapted snippet */

        JButton deleteChatButton = new JButton("Delete chat");
        deleteChatButton.setFocusable(false);
        deleteChatButton.setEnabled(false);

        JLabel chatName = new JLabel("Select a chat to start...");
        chatName.setFont(new Font(new JLabel().getFont().getName(), Font.BOLD, 20));

        JPanel chatInfoPane = new JPanel();
        chatInfoPane.setLayout(new BoxLayout(chatInfoPane, BoxLayout.X_AXIS));

        chatInfoPane.add(chatName);
        chatInfoPane.add(Box.createHorizontalGlue());
        chatInfoPane.add(deleteChatButton);

        clientListPane = new JList<>();
        clientListPane.setPreferredSize(new Dimension(320, 600));
        clientListPane.setCellRenderer(new ClientListCellRenderer());
        clientListPane.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    target = clientListPane.getSelectedValue();
                    chatName.setText(clientListPane.getSelectedValue());
                    deleteChatButton.setEnabled(true);

                    // import chat history
                    importChatHistory(username, target);
                }
            }
        });

        JScrollPane clientListScrollPane = new JScrollPane(clientListPane);

        JPanel chatPane = new JPanel(new BorderLayout(10, 10));
        chatPane.setBorder(new EmptyBorder(0, 10, 0, 10));

        chatPane.add(chatInfoPane, BorderLayout.NORTH);
        chatPane.add(messageScrollPane, BorderLayout.CENTER);
        chatPane.add(inputPane, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        panel.add(clientListScrollPane, BorderLayout.WEST);
        panel.add(chatPane, BorderLayout.CENTER);

        add(panel);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public boolean writeChatHistory(String sender, String receiver, String message) {
        try {
            File chatHistoryFile = new File("data/chats/" + sender + "/" + receiver + ".txt");

            if (!chatHistoryFile.exists()) {
                chatHistoryFile.createNewFile();
            }

            else if (chatHistoryFile.exists() && !chatHistoryFile.isDirectory()) {
                BufferedWriter buffer = new BufferedWriter(new FileWriter(chatHistoryFile, true));

                buffer.write(message);
                buffer.newLine();

                buffer.close();

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean importChatHistory(String sender, String receiver) {
        // clear the messages screen
        messages.clear();

        try {
            File chatHistoryFile = new File("data/chats/" + sender + "/" + receiver + ".txt");

            if (!chatHistoryFile.exists()) {
                chatHistoryFile.createNewFile();
            }

            else if (chatHistoryFile.exists() && !chatHistoryFile.isDirectory()) {
                BufferedReader buffer = new BufferedReader(new FileReader(chatHistoryFile));
                String line = "";

                while ((line = buffer.readLine()) != null) {
                    messages.addElement(line);
                }

                buffer.close();

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String openFileChooser(boolean isDirOnly) {
        chooser = new JFileChooser("C:/");

        JPanel pane = new JPanel();
        String path = "";

        if (!isDirOnly) {
            chooser.setDialogTitle("Select a file to send");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showDialog(pane, "Select file") == JFileChooser.APPROVE_OPTION) {
                path = chooser.getSelectedFile().getAbsolutePath();
            }
        } else {
            chooser.setDialogTitle("Select save destination");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showDialog(pane, "Select folder") == JFileChooser.APPROVE_OPTION) {
                path = chooser.getSelectedFile().getAbsolutePath();
            }
        }

        return path;
    }

    private void sendMessage() {
        String messageBody = textInput.getText();
        String ownMessage = "You: " + messageBody;

        messages.addElement(ownMessage);

        // send the message to the server
        writer.println(username + ": " + messageBody + "!target:" + target);

        // write the message to the chat history file
        writeChatHistory(username, target, ownMessage);

        textInput.setText("");
    }

    private class Client implements Runnable {
        private BufferedReader reader;
        private Socket socket;

        @Override
        public void run() {
            try {
                socket = new Socket("localhost", 7291);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println(username);

                String message;

                while ((message = reader.readLine()) != null) {
                    if (message.contains("!all")) {
                        String[] receivedClientString = message.split("\\,");
                        String ownUsername = "";

                        clients.clear();
                        DefaultListModel<String> clientList = new DefaultListModel<>();

                        for (int i = 0; i < receivedClientString.length - 1; i++) {
                            if (receivedClientString[i].equals(username)) {
                                ownUsername = receivedClientString[i];
                                clients.put(receivedClientString[i], true);
                            } else {
                                clientList.addElement(receivedClientString[i]);
                                clients.put(receivedClientString[i], false);
                            }
                        }

                        // add the current client on top of the list
                        clientList.add(0, ownUsername);

                        clientListPane.setModel(clientList);
                        clientListPane.revalidate();
                    } else if (message.contains("!online")) {
                        // update online status client list
                        String[] receivedOnlineClientString = message.split("\\,");

                        for (int i = 0; i < receivedOnlineClientString.length - 1; i++) {
                            if (!receivedOnlineClientString[i].equals(username)) {
                                clients.put(receivedOnlineClientString[i], true);
                            }
                        }

                        clientListPane.revalidate();
                    } else if (!message.contains("!login") && !message.contains("!signup")) {
                        if (target != "") {
                            messages.addElement(message);
                        }

                        // retrieve the sender's name to write to corresponding history file
                        String[] msgTokens = message.split(":");

                        // write the message to the chat history file
                        writeChatHistory(username, msgTokens[0], message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientListCellRenderer extends JLabel implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String clientName, int index,
                boolean isSelected, boolean cellHasFocus) {

            setOpaque(true);

            if (isSelected) {
                setBackground(new Color(230, 247, 255));
            } else {
                setBackground(Color.WHITE);
            }

            Border padding = new EmptyBorder(20, 10, 20, 10);
            Border border = new MatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(border, padding));

            if (index == 0) {
                clientName += " (You)";
            }

            String labelText = "<html><p><b>" + clientName;

            if (clientName.contains("(You)") || (clients.get(clientName) != null && clients.get(clientName) == true)) {
                labelText += "</b></p><p><font color='green'>• <em>active</em></font><p></html>";
            } else if ((clients.get(clientName) != null && clients.get(clientName) == false)) {
                labelText += "<b></p><p><font color='gray'>• <em>offline</em></font><p></html>";
            }

            setText(labelText);

            return this;
        }
    }
}