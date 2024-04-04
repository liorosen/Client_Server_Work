package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    private static final int PORT = 7777;

    private static final String SERVER_IP = "localhost";

    private static final TftpClientProtocol protocol = new TftpClientProtocol();
    private static final  LineMessageClientEncoderDecoder encdec = new  LineMessageClientEncoderDecoder();

    private static boolean getBytesLastMessgae( byte [] last_message){
        if(last_message[0] != 0
                || last_message[1] != 3
                || last_message[2] != 0
                || last_message[3] != 0){return true;}
        return false;
    }

    private static void helperRRQ(String readFilename, DataInputStream in){
        byte [] data = new byte[0];
        byte [] nextMessage = null;
        byte [] last_message = {0,0,0,0};
        nextMessage = null;
        encdec.switch_mode(true);
        while(getBytesLastMessgae(last_message)) {
            while (nextMessage == null) {
                    try {
                        nextMessage = encdec.decodeNextByte((byte) (in.read()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            getACK(in);
            encdec.switch_mode(true);

            int ilength = (int)(nextMessage[3]) + ((int)(nextMessage[2])<<8);
            if(ilength > 518)
            {
                System.out.println(nextMessage);
            }
            System.out.println(nextMessage[2]);
            System.out.println(nextMessage[3]);
            System.out.println(ilength);
            byte [] new_data = new byte[data.length+ilength];
            System.arraycopy(data,0,new_data,0,data.length);
            System.arraycopy(nextMessage,6,new_data,data.length,ilength);
            data = new_data;
            last_message = nextMessage;
            nextMessage = null;
            //send ack packet back to server
        }
        fileWriting(readFilename , data);
        encdec.switch_mode(false);
    }


    private static void fileWriting(String filename, byte[] data){
        try (FileOutputStream fos = new FileOutputStream("Skeleton/client/target/" + filename)) {
            fos.write(data);
        }catch (IOException ex) {
            System.out.println("Error");
        }
    }

    public static void helpFunc (String[] parts , TftpClient client , BufferedReader reader , String line, DataOutputStream out, DataInputStream in)  {
        switch (parts[0]) {
            //--------------------------------------------------
            //LOGRQ
            case "LOGRQ":
                //System.out.print("Enter username:");
                String username = parts[1];
                client.sendLoginRequest(username,out);
                getACK(in);
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
                System.out.println("The file " + readFilename + " accepted");
                break;
            //--------------------------------------------------
            //WRQ
            case "WRQ":
                String fileName = parts[1];
                client.sendReadWriteRequest( fileName, false , out);
                //get ack/accepted
                if(getACK(in))
                    write_to_server(fileName,out);
                //write to server
                System.out.println("The file " + fileName + " uploaded");
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

    private static boolean getACK(DataInputStream in) {
        byte[] nextMessage = null;
        encdec.switch_mode(false);
        while (nextMessage == null) { // for ack
            try {
                nextMessage = encdec.decodeNextByte((byte) (in.read()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        //check if ack or error
        //if ack of exit
        //if error, error
        //nextMessage
        return true;
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

    private static void  sendFile(byte[] data,DataOutputStream out){
        List<byte[]> dataPackets = new ArrayList<>();
        int blockNumber = 1;
        int offset = 0;
        while (offset < data.length) {
            int blockSize = Math.min(data.length - offset, 512);
            byte[] blockData = Arrays.copyOfRange(data, offset, offset + blockSize);
            dataPackets.add(createDataPacket(blockNumber, blockData));
            try {
                out.write(createDataPacket(blockNumber, blockData));
                out.write("\n".getBytes());
                out.flush();
            }catch(Exception e){}

            //stop and read socket to get ack to continue
            //process(connections.getHandler(connectionId).receiveAckPacket());
            offset += blockSize;
            blockNumber++;
        }
//        //TODO: Make sure it is good!
//        byte[] backsalshn = new byte [0];
//        dataPackets.add(createDataPacket(blockNumber, backsalshn));
//        try {
//            out.write(createDataPacket(blockNumber, backsalshn));
//            out.write("\n".getBytes());
//            out.flush();
//        }catch(Exception e)
//        {

        //}
    }
    private static void write_to_server(String writeFilename, DataOutputStream out) {
        //open file for reading
        byte[] data = new byte[1 << 10] ;
        byte ch = 0;
        int i = 0;
        writeFilename = "Skeleton/client/" + writeFilename;
        try(FileInputStream sc = new FileInputStream(writeFilename)){
            while((ch = (byte) sc.read()) != -1 )
            {
                data[i] = ch;
                i++;
                if (i >= data.length) {
                    data = Arrays.copyOf(data,  i* 2);
                }
            }
            data = Arrays.copyOf(data, i);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendFile(data,out);
    }


    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) { // establishes a socket connection to the server using the IP address and port number specified.
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));       //  creates an input stream to receive data from the server over the socket connection.
//            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));       //  creates a BufferedReader object to read input from the keyboard.
            //System.out.println(System.getProperty("user.dir"));
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

            keyboardThread.start();
            keyboardThread.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(DataOutputStream out, String message) throws IOException {
        out.write(message.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // Method to construct and send a Login Request (LOGRQ) packet
    public void sendLoginRequest(String username , DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 7;
        String packet = new String(pack) + "" + username + "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Delete File Request (DELRQ) packet
    public void sendDeleteRequest(String filename , DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 8;
        String packet = new String(pack) + filename+  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Read Request (RRQ) or Write Request (WRQ) packet
    public void sendReadWriteRequest(String filename, boolean isRead,DataOutputStream out) {
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
    public void sendDirListingRequest(DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 6;
        String packet = new String(pack) +  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Disconnect (DISC) packet
    public void sendDisconnect(DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 10;
        String packet = new String(pack) +  "\0"  +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to send a generic packet (assuming it's already constructed)
    private void sendPacket(byte[] packet, DataOutputStream out) {
        try {
            out.write(packet);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}


