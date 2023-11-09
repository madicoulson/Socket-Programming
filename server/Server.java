/*
 * CS 4065 - Computer Networks & Networked Computing
 * University of Cincinnati - Fall 2023
 * Project #2: Bulletin Board
 * Project Team: Preston Buterbaugh, Madilyn Coulson, Chloe Belletti
 * Bulletin Board Server
 */

package server;

import java.io.*;
import java.net.*;
import java.util.*;
import server.Message;
import server.User;
public class Server implements Runnable {
    //Static server data structures
    public static ArrayList<User> connectedUsers = new ArrayList<User>();
    public static ArrayList<Message> messages = new ArrayList<Message>();
    
    //Field for the client connection socket (used in threads)
    private Socket socket;

    //Constructor for use when starting new threads
    public Server(Socket clientSocket) {
        this.socket = clientSocket;
    }

    //Main function - Listens for connection requests and then 
    public static void main(String[] args) throws Exception {
        //Create server socket
        ServerSocket serverSocket = new ServerSocket(6789);

        //Print message to show that server program has successfully started
        System.out.println("Server started");

        //Loop infinetely until server is closed
        while(true) {
            Socket clientSocket = serverSocket.accept();
            Server clientConnection = new Server(clientSocket);
            System.out.println("New client connected with address " + clientSocket.getRemoteSocketAddress());
            Thread thread = new Thread(clientConnection);
            thread.start();
        }
    }

    //Run function - runs threads
    public void run() {
        try {
            waitForClientData();
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private void waitForClientData() throws Exception {

        //Get line of text sent from client
        String dataLine = readFromClient(socket);

        //Loop until the text "exit" is sent
        while(!dataLine.equals("exit")) {
            //Split the input line into the command and any following arguments
            StringTokenizer tokens = new StringTokenizer(dataLine);
            String command = tokens.nextToken();
            ArrayList<String> args = new ArrayList<String>(tokens.countTokens() - 1);
            while(tokens.hasMoreTokens()) {
                args.add(tokens.nextToken());
            }

            //Take action based on what command was entered
            if(command.equals("join")) {
                addUser(args.get(0));
            }
            else if (command.equals("users")) {
                sendUserList();
            }
            else if(command.equals("leave")){
                removeUser(args.get(0));
            }

            dataLine = readFromClient(socket);
        }
        
        //Close the socket
        System.out.println(socket.getRemoteSocketAddress() + " disconnected");
        socket.close();
    }

    //Function for user joining the group
    public void addUser(String username) throws Exception {
        System.out.println(username + " issued join command");

        //Gather information to send to the new user formatted as JSON string
        String payload = "{\"users\": [";
        
        //Get a list of all currently connected users
        for(User user : connectedUsers) {
            payload = payload + "\"" + user.getUsername() + "\",";
        }
        if(connectedUsers.size() > 0) {
            payload = payload.substring(0, payload.length() - 1); //Remove trailing comma
        }
        payload += "], \"messages\": [";

        //Get last two messages sent
        if(messages.size() >= 2) {
            payload = payload + messages.get(messages.size() - 2).toJsonString() + ",";
        }
        if(messages.size() >= 1) {
            payload = payload + messages.get(messages.size() - 1).toJsonString();
        }
        payload = payload + "]}\n";

        //Send payload data to client
        sendToClient(socket, payload);
        
        //Inform all other connected clients that the user has joined
        String message = username + " joined the group\n";
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }

        //Add the newly connected user to the server's list of users
        User newUser = new User(username, socket);
        connectedUsers.add(newUser);
    }

    public void createPost() {}

    //Function for outputting list of users.
    public void sendUserList() throws Exception{
        //Get a list of all currently connected users
        String userList = "";
        for(User users : connectedUsers) {
            userList = userList + users.getUsername() + ",";
        }
        userList = userList.substring(0, userList.length() - 1) + "\n"; //Replace trailing comma with newline

        //Store the list of usernames in a message
        String message = "Current list of users: " + userList;
        //Inform all connected clients of the list of users
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }
    }

    //Function for removing user from the group
    public void removeUser(String username) throws Exception{
        
        User user = null;
        //Ensure that the user is part of the connected users
        for(User users:connectedUsers) {
            if (users.getUsername() == username) {
                user = users;
                break;
            }
        }

        if (user == null) {
            throw new IllegalArgumentException("User not found in group.");
        }
        
        //Store the username into the message
        String message = user.getUsername() + " left the group";

        //Print that the user is leaving the group
        System.out.println(user.getUsername() + " is leaving the group. ");
        
        //Remove the user from connectUsers
        connectedUsers.remove(user);
      
        //Gather information to send to the new user formatted as JSON string
        String payload = "{\"users\": [";
        
        //Get a list of all currently connected users after removing user
        for(User users : connectedUsers) {
            payload = payload + "\"" + users.getUsername() + "\",";
        }
        if(connectedUsers.size() > 0) {
            payload = payload.substring(0, payload.length() - 1); //Remove trailing comma
        }
        payload += "]";

        //Send user list to client
        sendToClient(socket, payload);

        //Inform all other connected clients that the user has left the group
        for(User connectedUser : connectedUsers) {
            sendToClient(connectedUser.getSocket(), message);
        }
    }

    public void retrieveMessage() {}

    //Static helper functions
    public static String readFromClient(Socket clientSocket) throws Exception {
        BufferedReader streamFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        return streamFromClient.readLine();
    }

    public static void sendToClient(Socket clientSocket, String data) throws Exception {
        DataOutputStream streamToClient = new DataOutputStream(clientSocket.getOutputStream());
        streamToClient.writeBytes(data);
    }
}