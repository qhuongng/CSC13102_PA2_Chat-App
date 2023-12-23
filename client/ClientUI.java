import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

public class ClientUI extends JFrame {
    private Socket socket;

    private JFileChooser chooser;
    private ClientLoginUI login;

    private String username;
    private PrintWriter writer;

    // false: offline, true: online
    private Map<String, Boolean> clients;
    private DefaultListModel<String> messages;
    private DefaultListModel<String> clientList;

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
            if (login.isCanceled()) {
                dispose();
                System.exit(0);
            }
        }

        if (!login.isCanceled()) {
            this.username = login.getUsername();

            setTitle("Chat Application Client - " + username);
            initUI();

            new Thread(new Client()).start();
        }
    }

    private void initUI() {
        messages = new DefaultListModel<>();
        clientList = new DefaultListModel<>();

        JList<String> messagePane = new JList<>(messages);
        messagePane.setCellRenderer(new MessageListCellRenderer());

        JScrollPane messageScrollPane = new JScrollPane(messagePane);

        textInput = new JTextArea(3, 30);
        textInput.setLineWrap(true);
        textInput.setWrapStyleWord(true);
        textInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // not needed
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // not needed
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();

                    if (target != "") {
                        sendMessage("text");
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
                sendMessage("text");
            }
        });

        JButton sendFileButton = new JButton("Send file");
        sendFileButton.setFocusable(false);
        sendFileButton.setEnabled(false);

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage("file");
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

        deleteChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // show confirm dialog
                int input = JOptionPane.showConfirmDialog(ClientUI.this, "Do you want to delete this chat?",
                        "Confirm chat delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (input == 0) {
                    // OK
                    messages.clear();

                    writer.println(username + "!deletechat:" + target);

                    // show complete dialog
                    JOptionPane.showConfirmDialog(ClientUI.this,
                            "Chat history deleted successfully.", "Delete complete", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE);
                }
            }
        });

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

                    sendFileButton.setEnabled(true);

                    if (target.contains(";")) {
                        // group chat
                        // format group chat name
                        String[] members = target.split(";");

                        String labelText = "";

                        for (int i = 0; i < members.length - 1; i++) {
                            labelText += members[i] + ", ";
                        }

                        labelText += members[members.length - 1];

                        chatName.setText(labelText);
                    } else {
                        chatName.setText(target);
                    }

                    deleteChatButton.setEnabled(true);

                    // clear the messages screen
                    messages.clear();

                    writer.println(target + "!chatselect");
                }
            }
        });

        JScrollPane clientListScrollPane = new JScrollPane(clientListPane);

        JButton newGroupChatButton = new JButton("New group chat");
        newGroupChatButton.setFocusable(false);
        newGroupChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openClientChooser();
            }
        });

        JPanel clientsPane = new JPanel(new BorderLayout(10, 10));
        clientsPane.add(clientListScrollPane, BorderLayout.CENTER);
        clientsPane.add(newGroupChatButton, BorderLayout.SOUTH);

        JPanel chatPane = new JPanel(new BorderLayout(10, 10));
        chatPane.setBorder(new EmptyBorder(0, 10, 0, 10));

        chatPane.add(chatInfoPane, BorderLayout.NORTH);
        chatPane.add(messageScrollPane, BorderLayout.CENTER);
        chatPane.add(inputPane, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        panel.add(clientsPane, BorderLayout.WEST);
        panel.add(chatPane, BorderLayout.CENTER);

        add(panel);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                writer.println(username + "!disconnect");
                dispose();
                System.exit(0);
            }
        });
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
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

    private void openClientChooser() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Select group members");
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        Set<String> clientNames = clients.keySet();
        clientNames.remove(username);

        JList<String> allClients = new JList<>(clientNames.toArray(new String[clientNames.size()]));
        allClients.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    super.addSelectionInterval(index0, index1);
                }
            }
        });

        JButton okButton = new JButton("Create group chat");
        okButton.setFocusable(false);
        okButton.setEnabled(false);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.util.List<String> selected = allClients.getSelectedValuesList();

                createGroupChat(selected);

                dialog.dispose();
            }
        });

        allClients.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // enable OK button if group chat has at least 3 members (including current
                // user)
                java.util.List<String> selected = allClients.getSelectedValuesList();

                if (selected.size() >= 2) {
                    okButton.setEnabled(true);
                } else {
                    okButton.setEnabled(false);
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(allClients), BorderLayout.CENTER);
        panel.add(okButton, BorderLayout.SOUTH);

        dialog.add(panel);

        dialog.pack();
        dialog.setLocationRelativeTo(ClientUI.this);
        dialog.setVisible(true);
    }

    private void createGroupChat(java.util.List<String> selected) {
        String groupMembers = username + ";";

        for (int i = 0; i < selected.size() - 1; i++) {
            groupMembers += selected.get(i) + ";";
        }

        groupMembers += selected.get(selected.size() - 1) + "!newgroup";

        writer.println(groupMembers);
    }

    private void sendMessage(String messageType) {
        if (messageType.equals("text")) {
            String message = username + ": " + textInput.getText();
            messages.addElement(message);

            // Send the text message to the server
            writer.println(message + "!target:" + target);

            textInput.setText("");
        } else if (messageType.equals("file")) {
            // File sharing logic
            String filePath = openFileChooser(false);

            if (!filePath.isEmpty()) {
                String fileName = new File(filePath).getName();
                try {
                    // Notify the server about the upcoming file transfer
                    writer.println(target + "!file:" + fileName);

                    // Send the file to the server
                    FileInputStream fileStream = new FileInputStream(filePath);
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        socket.getOutputStream().write(buffer, 0, bytesRead);
                    }

                    fileStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Client implements Runnable {
        private BufferedReader reader;

        @Override
        public void run() {
            try {
                socket = new Socket("localhost", 7291);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println(username);

                String message;

                while ((message = reader.readLine()) != null) {
                    System.out.println(username + " received: " + message + "\n");

                    if (message.contains("!all")) {
                        String[] receivedClientString = message.split("\\,");
                        String ownUsername = "";

                        clients.clear();

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
                        clientListPane.repaint();
                    } else if (message.contains("!online")) {
                        Set<String> allClientNames = clients.keySet();

                        // refresh the online status
                        for (String clientName : allClientNames) {
                            if (message.contains(clientName)) {
                                clients.put(clientName, true);
                            } else {
                                clients.put(clientName, false);
                            }
                        }

                        clientListPane.revalidate();
                        clientListPane.repaint();
                    } else if (message.contains("!history")) {
                        String[] chatLines = message.split(";;");

                        for (int i = 0; i < chatLines.length - 1; i++) {
                            messages.addElement(chatLines[i]);
                        }
                    } else if (message.contains("!newgroup")) {
                        String groupName = message.split("!")[0];

                        if (groupName.contains(username)) {
                            clients.put(groupName, false);
                            clientList.addElement(groupName);

                            clientListPane.revalidate();
                            clientListPane.repaint();
                        }
                    } else if (message.contains("!newclient")) {
                        String clientName = message.split("!")[0];

                        if (!clientName.equals(username) && !clients.containsKey(clientName)) {
                            clients.put(clientName, false);
                            clientList.addElement(clientName);

                            clientListPane.revalidate();
                            clientListPane.repaint();
                        }
                    } else if (message.contains("!group:")) {
                        // message structure: <sender>: <body>!group:<target>
                        String[] msgTokens = message.split("!");
                        String msgBody = msgTokens[0];
                        String targetGroup = msgTokens[1].split(":")[1];

                        if (target.equals(targetGroup)) {
                            messages.addElement(msgBody);
                        }
                    } else if (!message.contains("!login") && !message.contains("!signup")) {
                        String senderName = message.split(":")[0];

                        if (target.equals(senderName)) {
                            // private chat
                            messages.addElement(message);
                        }
                    }
                }

                socket.close();
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

            String labelText = "<html><p><b>" + clientName;

            if (index == 0) {
                labelText += " (You)";
            }

            if (clientName.equals(username)
                    || (clients.get(clientName) != null && clients.get(clientName) == true)) {
                labelText += "</b></p><p><font color='green'>• <em>active</em></font><p></html>";
            } else if (clientName.contains(";")) {
                // group chat
                String[] members = clientName.split(";");

                labelText = "<html><p><b>";

                for (int i = 0; i < members.length - 1; i++) {
                    labelText += members[i] + ", ";
                }

                labelText += members[members.length - 1]
                        + "</b></p><p><font color='gray'><em>Group chat</em></font><p></html>";
            } else if (clients.get(clientName) != null && clients.get(clientName) == false) {
                labelText += "<b></p><p><font color='gray'>• <em>offline</em></font><p></html>";
            }

            setText(labelText);

            return this;
        }
    }

    private class MessageListCellRenderer extends JLabel implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String message, int index,
                boolean isSelected, boolean cellHasFocus) {

            setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

            if (message.contains(username)) {
                setForeground(Color.BLUE);
            } else {
                setForeground(Color.RED);
            }

            String[] msgTokens = message.split(":");

            setText("<html><p><b>" + msgTokens[0] + ":</b>" + msgTokens[1] + "</p></html>");

            return this;
        }
    }

    public static void setUIFont(FontUIResource f) {
        java.util.Enumeration<?> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, f);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                setUIFont(new FontUIResource((new JLabel().getFont().getName()), Font.PLAIN, 15));
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            new ClientUI();
        });
    }
}