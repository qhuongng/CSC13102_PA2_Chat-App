import java.util.*;
import java.io.*;
import java.net.Socket;
import java.sql.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

public class ClientUI extends JFrame {
    private JFileChooser chooser;
    private boolean isLoggedIn;

    private String username;
    private PrintWriter writer;

    private DefaultListModel<String> messages;
    private JTextArea textInput;

    public ClientUI(String username) {
        super("Chat Application - " + username);

        this.username = username;
        isLoggedIn = false;

        loginDialog(true);

        if (!isLoggedIn) {
            dispose();
            System.exit(0);
        } else {
            messages = new DefaultListModel<>();
            JList<String> messagePane = new JList<>(messages);
            JScrollPane messageScrollPane = new JScrollPane(messagePane);

            textInput = new JTextArea(3, 30);
            textInput.setLineWrap(true);
            textInput.setWrapStyleWord(true);

            JScrollPane textInputScrollPane = new JScrollPane(textInput);

            JButton sendButton = new JButton("Send");
            sendButton.setFocusable(false);

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
                        sendButton.setEnabled(true);
                    } else {
                        sendButton.setEnabled(false);
                    }
                }
            };

            textInput.getDocument().addDocumentListener(fieldListener);
            /* end of adapted snippet */

            JPanel clientListPane = new JPanel(new BorderLayout(10, 10));
            clientListPane.setPreferredSize(new Dimension(320, 600));

            JScrollPane clientListScrollPane = new JScrollPane(clientListPane);

            JPanel chatPane = new JPanel(new BorderLayout(10, 10));
            chatPane.setBorder(new EmptyBorder(0, 10, 0, 10));

            chatPane.add(messageScrollPane, BorderLayout.CENTER);
            chatPane.add(inputPane, BorderLayout.SOUTH);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
            panel.add(clientListScrollPane, BorderLayout.WEST);
            panel.add(chatPane, BorderLayout.CENTER);

            add(panel);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1200, 800);
            setLocationRelativeTo(null);
            setVisible(true);

            new Thread(new Client()).start();
        }
    }

    private class Client implements Runnable {
        private Socket socket;
        private BufferedReader reader;

        @Override
        public void run() {
            try {
                socket = new Socket("localhost", 8888);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = reader.readLine()) != null) {
                    messages.addElement(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loginDialog(boolean isSigningUp) {
        JDialog dialog = new JDialog();
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isLoggedIn == false) {
                    dispose();
                }
            }
        });

        JLabel header = new JLabel();
        header.setFont(new Font(new JLabel().getFont().getName(), Font.BOLD, 32));
        header.setAlignmentX(CENTER_ALIGNMENT);

        JButton okButton = new JButton();
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.setFocusable(false);
        okButton.setEnabled(false);

        JButton switchButton = new JButton();
        switchButton.setForeground(Color.BLUE);
        switchButton.setFocusable(false);
        switchButton.setMargin(new Insets(0, 0, 0, 0));
        switchButton.setContentAreaFilled(false);
        switchButton.setBorderPainted(false);
        switchButton.setOpaque(false);
        switchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        switchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();

                loginDialog(!isSigningUp);
            }
        });

        if (isSigningUp) {
            dialog.setTitle("Create a new account");
            header.setText("Welcome");
            okButton.setText("Sign up");
            switchButton.setText("<html><u>Log in</u></html>");
        } else {
            dialog.setTitle("Log in with an existing account");
            header.setText("Welcome back");
            okButton.setText("Log in");
            switchButton.setText("<html><u>Create account</u></html>");
        }

        // create text fields
        JTextField usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(200, 30));
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(200, 30));
        JPasswordField repeatPasswordField = new JPasswordField();
        repeatPasswordField.setPreferredSize(new Dimension(200, 30));

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
                boolean emptyUsername = (usernameField.getText().isEmpty() || usernameField.getText() == null);
                boolean emptyPassword = (passwordField.getPassword() == null
                        || passwordField.getPassword().length == 0);
                boolean emptyRepeatPassword = (repeatPasswordField.getPassword() == null
                        || repeatPasswordField.getPassword().length == 0);

                if (isSigningUp) {
                    if (!emptyUsername && !emptyPassword && !emptyRepeatPassword) {
                        okButton.setEnabled(true);
                    } else {
                        okButton.setEnabled(false);
                    }
                } else {
                    // the dialog for logging in only has 2 fields
                    if (!emptyUsername && !emptyPassword) {
                        okButton.setEnabled(true);
                    } else {
                        okButton.setEnabled(false);
                    }
                }
            }
        };

        usernameField.getDocument().addDocumentListener(fieldListener);
        passwordField.getDocument().addDocumentListener(fieldListener);
        repeatPasswordField.getDocument().addDocumentListener(fieldListener);
        /* end of adapted snippet */

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // get the text input from text fields
                String username = usernameField.getText().trim();
                char[] password = passwordField.getPassword();
                char[] repeatedPassword = repeatPasswordField.getPassword();

                if (isSigningUp) {
                    dialog.setVisible(false);
                    dialog.dispose();

                    loginDialog(false);
                } else {
                    isLoggedIn = true;

                    dialog.setVisible(false);
                    dialog.dispose();

                    // frame.toFront();
                    // frame.requestFocus();
                }

                // TODO: handle user authentication
            }
        });

        // create labels for the text fields
        JLabel usernameLabel = new JLabel("Username", JLabel.LEFT);
        JLabel passwordLabel = new JLabel("Password", JLabel.LEFT);
        JLabel repeatPasswordLabel = new JLabel("Repeat password", JLabel.LEFT);

        JPanel usernamePane = new JPanel(new BorderLayout(0,
                5));
        usernamePane.add(usernameLabel, BorderLayout.NORTH);
        usernamePane.add(usernameField, BorderLayout.CENTER);

        JPanel passwordPane = new JPanel(new BorderLayout(0,
                5));
        passwordPane.add(passwordLabel, BorderLayout.NORTH);
        passwordPane.add(passwordField, BorderLayout.CENTER);

        JPanel repeatPasswordPane = new JPanel(new BorderLayout(0,
                5));
        repeatPasswordPane.add(repeatPasswordLabel, BorderLayout.NORTH);
        repeatPasswordPane.add(repeatPasswordField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(switchButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(okButton);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(30, 50, 30, 50));

        panel.add(header);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(usernamePane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(passwordPane);

        if (isSigningUp) {
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(repeatPasswordPane);
        }

        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(buttonPanel);

        // add the panel to the dialog window
        dialog.add(panel, BorderLayout.CENTER);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
        String message = textInput.getText();
        messages.addElement("You: " + message + "\n");

        // Send the message to the server
        writer.println(username + ": " + message);

        textInput.setText("");
    }

}