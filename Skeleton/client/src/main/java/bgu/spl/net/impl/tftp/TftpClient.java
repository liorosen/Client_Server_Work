package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.MessagingProtocol;

import java.io.*;
import java.rmi.server.Skeleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    private static final int PORT = 7777;
//    private static String directoryPath = "C:/Users/lioro/IdeaProjects/ServerSpl3/Skeleton/server/Flies";
    private static final String SERVER_IP = "localhost";

    private static final TftpClientProtocol protocol = new TftpClientProtocol();
    private static final  LineMessageClientEncoderDecoder encdec = new  LineMessageClientEncoderDecoder();

    private static void helperRRQ(String readFilename, BufferedReader in){
        String data = new String();
        String fin = "\n";
        String nextMessage = null;

        try {
            nextMessage = encdec.decodeNextByte((byte) (in.read()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        while (nextMessage == null) {
            protocol.process(nextMessage);
            try {
                FileWriter file = new FileWriter(readFilename);
                file.write(data);
                file.close();
                System.out.println("RRQ " + readFilename + " complete");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public static void helpFunc (String opcode , TftpClient client , BufferedReader reader , String line, BufferedWriter out, BufferedReader in)  {
        switch (opcode) {
            //--------------------------------------------------
            case "LOGRQ":
                System.out.print("Enter username: ");
                String username = null;
                try {
                    username = reader.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                client.sendLoginRequest(username,out);
                System.out.println("Wait for answer");
                break;
            //--------------------------------------------------
            case "DELRQ":
                System.out.print("Enter filename to delete: ");
                String deleteFilename = null;
                try {
                    deleteFilename = reader.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                client.sendDeleteRequest(deleteFilename,out);
                break;
            //--------------------------------------------------
            case "RRQ":
                System.out.print("Enter filename to read: ");
                String readFilename = null;
                try {
                    readFilename = reader.readLine().trim();
                    FileReader file = new FileReader(readFilename);

                    // File already exists, handle this case appropriately
                    System.out.println("Error: File already exists locally");
                    file.close();
                    return;

                } catch (FileNotFoundException e) {

                    // File doesn't exist locally, request it from the client
                    client.sendReadWriteRequest(readFilename, true, out);

                    //read until file complete.
                     helperRRQ(readFilename, in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                break;
            //--------------------------------------------------
            case "WRQ":
                System.out.print("Enter filename to write: ");
                String writeFilename = null;
                try {
                    writeFilename = reader.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                client.sendReadWriteRequest(writeFilename, false , out);
                break;
            //--------------------------------------------------
            case "DIRQ":
                client.sendDirListingRequest(out);
                break;

            default:
                System.out.println("Illegal TFTP operation – Unknown Opcode: " + opcode);
                try {
                    sendMessage(out, line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            //--------------------------------------------------
        }
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) { // establishes a socket connection to the server using the IP address and port number specified.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));       //  creates an input stream to receive data from the server over the socket connection.
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));       //  creates a BufferedReader object to read input from the keyboard.
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            TftpClient client = new TftpClient();

            Thread keyboardThread = new Thread(() -> {      //  creates a new thread for reading input from the keyboard.
                try {
                    String line;
                    while ((line = reader.readLine().toUpperCase()) != null) {
                        if (line.equals("DISC")) {
                            client.sendDisconnect(out);
                            break;
                        }
                        String[] parts = line.split(" ", 2);
                        String opcode = parts[0].toUpperCase();
                        helpFunc(opcode, client, reader, line, out , in);
                    }
                    client.sendDisconnect(out);
                    sendMessage(out, line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread serverThread = new Thread(() -> {       // creates a new thread for receiving messages from the server.
                try {
                    char[] buf = new char[512];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        String msg = new String(buf, 0, len);
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            keyboardThread.start();
            serverThread.start();

            keyboardThread.join();
            serverThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(BufferedWriter out, String message) throws IOException {
        out.write(new String(message.getBytes(StandardCharsets.UTF_8)));
        out.flush();
    }

    // Method to construct and send a Login Request (LOGRQ) packet
    public void sendLoginRequest(String username , BufferedWriter out) {
        String packet = "LOGRQ " + username + "\0";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Delete File Request (DELRQ) packet
    public void sendDeleteRequest(String filename , BufferedWriter out) {
        String packet = "DELRQ " + filename + "\0";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Read Request (RRQ) or Write Request (WRQ) packet
    public void sendReadWriteRequest(String filename, boolean isRead,BufferedWriter out) {
        String opcode = isRead ? "RRQ" : "WRQ";
        String packet = opcode + " " + filename + "\0";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Directory Listing Request (DIRQ) packet
    public void sendDirListingRequest(BufferedWriter out) {
        String packet = "DIRQ\0";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Disconnect (DISC) packet
    public void sendDisconnect(BufferedWriter out) {
        String packet = "DISC\0";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to send a generic packet (assuming it's already constructed)
    private void sendPacket(byte[] packet, BufferedWriter out) {
        try {
            out.write(new String(packet));
            out.newLine();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }








}


