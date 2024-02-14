# Java Chat App

This program is a project assignment from the course **CSC13102 - Java Application Development (21KTPM2)** at VNU-HCM, University of Science.

The program consists of a server capable of handling multiple clients. Users can register and log into their clients, see all other online clients, as well as take part in private and group conversations. Chat history is saved on the server and can be deleted by the user themselves. File sharing feature is still under development.

The demo for the program can be viewed on [YouTube](https://www.youtube.com/watch?v=U2nA9pSIzrg) (in Vietnamese).

## How to use

### Initializing the server

Using a terminal, navigate to the directory containing the file `Server.jar` and enter the line below:

```
java -jar Server.jar
```

The terminal's output should be "Server started on port 7291", indicating that the server initialization is successful. This terminal window will act as the server, displaying messages received from clients, as well as potential errors and exceptions during its runtime.

Please ensure the `Server.jar` file is in the same directory as the `data` folder before running.

### Initializing the client(s)

With another terminal, navigate to the directory containing the file `ClientUI.jar` and enter the line below:

```
java -jar ClientUI.jar
```

The client initialization is successful if the login dialog with the "Welcome" header appears. At this point, users can choose to log in, or switch to the signup dialog by clicking the link on the dialog.

To have multiple clients running, repeat the same steps using multiple terminals. These terminal windows will display certain messages received from the server and other clients, as well as potential errors and exceptions during runtime.
