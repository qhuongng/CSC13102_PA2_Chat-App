import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

public class ClientLoginUI {
    private String username;
    private String password;

    private JFrame parentFrame;
    private JDialog dialog;
    private JLabel errorLabel;

    private PrintWriter writer;

    private boolean isLoggedIn;
    private boolean isCanceled;

    public ClientLoginUI(JFrame parentFrame) {
        this.isLoggedIn = false;
        this.isCanceled = false;
        this.parentFrame = parentFrame;

        new Thread(new ClientLogin()).start();
    }

    public void openDialog(boolean isSigningUp) {
        dialog = new JDialog();
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isLoggedIn == false) {
                    isCanceled = true;
                    dialog.dispose();
                }
            }
        });

        JLabel header = new JLabel();
        header.setFont(new Font(new JLabel().getFont().getName(), Font.BOLD, 32));
        header.setAlignmentX(JFrame.CENTER_ALIGNMENT);

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

                openDialog(!isSigningUp);
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

        // create an error message label
        errorLabel = new JLabel();
        errorLabel.setForeground(Color.RED);
        errorLabel.setAlignmentX(JFrame.CENTER_ALIGNMENT);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // get the text input from text fields
                username = usernameField.getText().trim();
                password = new String(passwordField.getPassword());
                String repeatedPassword = new String(repeatPasswordField.getPassword());

                if (isSigningUp && !password.equals(repeatedPassword)) {
                    errorLabel.setText("Password fields do not match.");
                } else {
                    if (isSigningUp) {
                        writer.println(username + "," + password + "!signup");
                    } else {
                        writer.println(username + "," + password + "!login");
                    }
                }
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
        panel.add(errorLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
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
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    private class ClientLogin implements Runnable {
        private BufferedReader reader;
        private Socket socket;
        private int socketNo;

        @Override
        public void run() {
            try {
                Random random = new Random();
                socketNo = random.nextInt(900) + 100;

                socket = new Socket("localhost", 7291);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println(socketNo + "!authenticate");

                String message;

                while ((message = reader.readLine()) != null) {
                    System.out.println("login socket: " + message + "\n");

                    if (message.equals(username + "!accexists")) {
                        // username already exists
                        SwingUtilities.invokeLater(() -> errorLabel.setText("Username is already taken."));
                    } else if (message.equals(username + "!signupsuccess")) {
                        SwingUtilities.invokeLater(() -> {
                            dialog.dispose(); // dispose of the current dialog

                            openDialog(false);
                        });
                    } else if (message.equals(username + "!noacc")) {
                        // username is not registered
                        SwingUtilities.invokeLater(() -> errorLabel.setText("Username is not registered."));
                    } else if (message.equals(username + "!wrongpass")) {
                        // password is incorrect
                        SwingUtilities.invokeLater(() -> errorLabel.setText("Incorrect password."));
                    } else if (message.contains(username + "!loginsuccess")) {
                        isLoggedIn = true;
                        writer.println(socketNo + "!donelogin");
                        SwingUtilities.invokeLater(dialog::dispose);
                    }
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isCanceled() {
        return this.isCanceled;
    }

    public boolean isLoggedIn() {
        return this.isLoggedIn;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password.toString();
    }
}
