package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
        String last_message = "0000";
        while(last_message.getBytes()[0] != 0
                || last_message.getBytes()[1] != 3
                || last_message.getBytes()[2] != 0
                || last_message.getBytes()[3] != 0) {
            while (nextMessage == null) {
                    try {
                        nextMessage = encdec.decodeNextByte((byte) (in.read()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            data += nextMessage ;
            last_message = nextMessage;
            while(last_message.length() < 4)
                last_message+= 0;
            nextMessage = null;
            //send ack packet back to server
        }
        try {
            FileWriter file = new FileWriter("Skeleton/client/target/" + readFilename);
            file.write(data);
            file.close();
            System.out.println("RRQ " + readFilename + " complete");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }



    }


    public static void helpFunc (String[] parts , TftpClient client , BufferedReader reader , String line, BufferedWriter out, BufferedReader in)  {
        switch (parts[0]) {
            //--------------------------------------------------
            //LOGRQ
            case "LOGRQ":
                System.out.print("Enter username: ");
                String username = parts[1];
                client.sendLoginRequest(username,out);

                // Construct the LOGRQ packet
               String packetString = "LOGRQ " + username;
               System.out.println(packetString);

                break;
            //--------------------------------------------------
            //DELRQ
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
            //RRQ
            case "RRQ":

                String readFilename = parts[1];
                System.out.print("Your filename is: " + readFilename);
                try {

                    FileReader file = new FileReader( "Skeleton/client/target/" + readFilename);

                    // File already exists, handle this case appropriately
                    System.out.println("\n" + "Error: File already exists locally");
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
            //WRQ
            case "WRQ":
                System.out.print("Enter filename to write: ");
                String writeFilename = null;
                try {
                    writeFilename = reader.readLine().trim();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                client.sendReadWriteRequest(writeFilename, false , out);
                //get ack/accepted
                if(handle_ack(in))
                    write_to_server(writeFilename,out);
                //write to server
                break;
            //--------------------------------------------------
            // DIRQ
            case "DIRQ":
                client.sendDirListingRequest(out);
                break;
            //--------------------------------------------------

            default:
                System.out.println("Illegal TFTP operation – Unknown Opcode: " + parts[0]);
                try {
                    sendMessage(out, line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            //--------------------------------------------------
        }
    }
    private static byte[] createDataPacket(int blockNumber, byte[] data) {
        byte[] packet = new byte[data.length + 6];
        packet[0] = 0; // Opcode for DATA
        packet[1] = 3; // Opcode value for DATA
        packet[2] = (byte) ((data.length >> 8) & 0xFF); // High byte of data length
        packet[3] = (byte) (data.length & 0xFF); // Low byte of data length
        packet[4] = (byte) ((blockNumber >> 8) & 0xFF); // High byte of block number
        packet[5] = (byte) (blockNumber & 0xFF); // Low byte of block number
        System.arraycopy(data, 0, packet, 6, data.length);
        return packet;
    }

    private static void  sendFile(byte[] data,BufferedWriter out){
        List<byte[]> dataPackets = new ArrayList<>();
        int blockNumber = 1;
        int offset = 0;
        while (offset < data.length) {
            int blockSize = Math.min(data.length - offset, 512);
            byte[] blockData = Arrays.copyOfRange(data, offset, offset + blockSize);
            dataPackets.add(createDataPacket(blockNumber, blockData));
            try {
                out.write(new String(createDataPacket(blockNumber, blockData)));
                out.write("\n");
                out.flush();
            }catch(Exception e)
            {

            }

            //stop and read socket to get ack to continue
            //process(connections.getHandler(connectionId).receiveAckPacket());
            offset += blockSize;
            blockNumber++;
        }
        //TODO: Make sure it is good!
        byte[] backsalshn = new byte [0];
        dataPackets.add(createDataPacket(blockNumber, backsalshn));
        try {
            out.write(new String(createDataPacket(blockNumber, backsalshn)));
            out.write("\n");
            out.flush();
        }catch(Exception e)
        {

        }
    }
    private static void write_to_server(String writeFilename, BufferedWriter out) {
        //open file for reading
        String data = new String();
        try {
            Scanner sc = new Scanner(new File(writeFilename));
            while(sc.hasNextLine())
            {
                data+=sc.nextLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendFile(data.getBytes(),out);
    }

    private static boolean handle_ack(BufferedReader in) {
        //read from in, check if ack.
        //return true if ack
        //return false otherwise
        return true;
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) { // establishes a socket connection to the server using the IP address and port number specified.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));       //  creates an input stream to receive data from the server over the socket connection.
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));       //  creates a BufferedReader object to read input from the keyboard.
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            System.out.println(System.getProperty("user.dir"));
            TftpClient client = new TftpClient();

            Thread keyboardThread = new Thread(() -> {      //  creates a new thread for reading input from the keyboard.
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ", 2);
                        parts[0] = parts[0].toUpperCase();
                        if (parts[0].equals("DISC")) {
                            client.sendDisconnect(out);
                            break;
                        }
                        //String opcode = parts[0].toUpperCase();
                        if(parts.length == 1){
                            System.out.println("Please insert valid command ");
                            continue;
                        }
                        helpFunc(parts, client, reader, line, out , in);
                    }
                    client.sendDisconnect(out);
                    sendMessage(out, line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

//            Thread serverThread = new Thread(() -> {       // creates a new thread for receiving messages from the server.
//                try {
//                    char[] buf = new char[512];
//                    int len;
//                    while ((len = in.read(buf)) != '\0') {
//                        String msg = new String(buf, 0, len);
//                        System.out.println(msg);
//
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });

            keyboardThread.start();
            //serverThread.start();

            keyboardThread.join();
            //serverThread.join();
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
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 7;
        String packet = new String(pack) + " " + username + "\0" +  "\n";
        System.out.println(packet);
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Delete File Request (DELRQ) packet
    public void sendDeleteRequest(String filename , BufferedWriter out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 8;
        String packet = new String(pack) + filename+  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Read Request (RRQ) or Write Request (WRQ) packet
    public void sendReadWriteRequest(String filename, boolean isRead,BufferedWriter out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        if(isRead)
            pack[1] = 1;
        else
            pack[1] = 2;
        String packet = new String(pack) + filename+  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Directory Listing Request (DIRQ) packet
    public void sendDirListingRequest(BufferedWriter out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 6;
        String packet = new String(pack) +  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Disconnect (DISC) packet
    public void sendDisconnect(BufferedWriter out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 10;
        String packet = new String(pack) +  "\0"  +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to send a generic packet (assuming it's already constructed)
    private void sendPacket(byte[] packet, BufferedWriter out) {
        try {
            out.write(new String(packet));
            //out.newLine();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }








}


