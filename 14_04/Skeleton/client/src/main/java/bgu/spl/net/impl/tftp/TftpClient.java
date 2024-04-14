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

    //private static final TftpClientProtocol protocol = new TftpClientProtocol();
    private static final  LineMessageClientEncoderDecoder encdec = new  LineMessageClientEncoderDecoder();

    static Boolean connected=true;
    static Boolean shouldDisc=false;
    static String readFileName="";
    static String writeFileName="";
    static Boolean isWritingToServer=false;
    static Boolean isReadingFromServer=false;

    private static boolean getBytesLastMessgae( byte [] last_message){
        if(last_message[0] != 0
                || last_message[1] != 3
                || last_message[2] != 0
                || last_message[3] != 0){return true;}
        return false;
    }

    private static byte[] helperRRQ(byte[] nextMessage, byte[] data, int counter, int blockNum, DataOutputStream out)
    {
        if((short) ((((short) (nextMessage[4] & 0xFF)) << 8 | (short) (nextMessage[5] & 0xFF)))==1)
        {
            System.out.println("The file " + readFileName + " accepted");
            data=new byte[0];
            counter=0;
            blockNum=1;
        }
            short dataLength=(short) (((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF));
            byte[] new_data=new byte[data.length+dataLength];
            System.arraycopy(data, 0, new_data, 0, data.length);
            System.arraycopy(nextMessage, 6, new_data, data.length, dataLength);
            data = new_data;
            counter=data.length;
            sendACK(out, blockNum);
            
        
        if((short) (((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF))<512)
        {
            isReadingFromServer=false;
            fileWriting(readFileName, data);
            System.out.println("RRQ " + readFileName + " complete");
            readFileName="";
            data=new byte[0];
            blockNum=1;
            counter=0;
        }

        return data;
    }


    private static void fileWriting(String readFileName, byte[] data){
        //try (FileOutputStream fos = new FileOutputStream("client/" + readFileName)) {
        try (FileOutputStream fos = new FileOutputStream(readFileName)) {
            fos.write(data);
        }catch (IOException ex) {
            System.out.println("Error");
        }
    }

    private static int helperWRQ(byte[] nextMessage, byte[] dataToServer, int bufferWRQ, int counterWRQ, DataOutputStream out)
    {
        int blockSize = Math.min(dataToServer.length - bufferWRQ, 512);
        byte[] blockData = Arrays.copyOfRange(dataToServer, bufferWRQ, bufferWRQ + blockSize);
        byte[] temp = createDataPacket(counterWRQ, blockData);
        //counterWRQ++;
        bufferWRQ+=blockSize;
        if(blockData.length<512)
        {
            counterWRQ=1;
            bufferWRQ=0;
            isWritingToServer=false;
        }
        sendPacket(temp, out);

        return bufferWRQ;
    }

    private static byte[] helperDIRQ(byte[] nextMessage, byte[] data, int counter, int blockNum, DataOutputStream out)
    {
        if((short) ((((short) (nextMessage[4] & 0xFF)) << 8 | (short) (nextMessage[5] & 0xFF)))==1)
        {
            data=new byte[0];
            counter=0;
            blockNum=1;
        }
            short dataLength=(short) (((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF));
            byte[] new_data=new byte[data.length+dataLength];
            System.arraycopy(data, 0, new_data, 0, data.length);
            System.arraycopy(nextMessage, 6, new_data, data.length, dataLength);
            data = new_data;
            counter=data.length;
            sendACK(out, blockNum);
            
        
        if((short) (((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF))<512)
        {
            filesPrint(data);
            data=new byte[0];
            blockNum=1;
            counter=0;
        }

        return data;
    }

    private static void filesPrint(byte[] data) {
        //String readFileName="";
        byte[] readFileName = new byte[data.length];
        int counter=0;
        for(int i=0;i<data.length;i++)
        {
            if(data[i] == 0)
            {
                System.out.println(new String(readFileName));
                readFileName = new byte[data.length];
                counter=0;
            }
            else
            {
                readFileName[counter]=data[i];
                counter++;
            }
        }
    }

    public static void helpFunc (String[] parts , TftpClient client , BufferedReader reader , String line, DataOutputStream out, DataInputStream in)  {
        switch (parts[0]) {
            //--------------------------------------------------
            //LOGRQ
            case "LOGRQ":
                //System.out.print("Enter username:");
                String username = parts[1];
                sendLoginRequest(username,out);
                // Construct the LOGRQ packet
               //String packetString = "LOGRQ " + username;
               //System.out.println(packetString);

               break;
            //--------------------------------------------------
            //DELRQ
            case "DELRQ":
                String deletereadFileName = parts[1];
                sendDeleteRequest(deletereadFileName,out);

                break;
            //--------------------------------------------------
            //RRQ
            case "RRQ":
                String _readFileName = parts[1];
                readFileName=_readFileName;
                //System.out.println("Your readFileName is: " + _readFileName);
                try {

                    //FileReader file = new FileReader( "client/" + _readFileName);
                    FileReader file = new FileReader(_readFileName);

                    // File already exists, handle this case appropriately
                    System.out.println("Error: File already exists locally");
                    file.close();
                    return;

                } catch (FileNotFoundException e) {

                    // File doesn't exist locally, request it from the client
                    sendReadWriteRequest(_readFileName, true, out);
                    //read until file complete.
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //System.out.println("The file " + _readFileName + " accepted");
                break;
            //--------------------------------------------------
            //WRQ
            case "WRQ":
                String _writeFileName = parts[1];
                writeFileName=_writeFileName;
                try {
                    //FileReader file = new FileReader( "client/" + _writeFileName);
                    FileReader file = new FileReader(_writeFileName);
                    // File does exist locally, request it from the client
                    sendReadWriteRequest(_writeFileName, false, out);


                } catch (FileNotFoundException e) {

                    
                    // File does not exists, handle this case appropriately
                    System.out.println("Error: File does not exists locally");
                    return;
                }
                //System.out.println("The file " + readFileName + " is being uploaded");
                break;
            //--------------------------------------------------
            // DIRQ
            case "DIRQ":
                sendDirListingRequest(out);
                //helperDIRQ(in);
                break;
            //--------------------------------------------------

            default:
                System.out.println("Illegal TFTP operation - Unknown Opcode: " + parts[0]);
                try {
                    sendMessage(out, line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            //--------------------------------------------------
        }
    }

    private static void handleBCAST(byte[] packet) {
        if(packet[1]==9)
        {
            int startIndex = 3; // Starting index of the part you want to convert
            byte endByte = 10; // Byte indicating the end of the part you want to convert

            // Find the index of the endByte
            int endIndex = startIndex;
            while (endIndex < packet.length && packet[endIndex] != endByte) {
                endIndex++;
            }
            String file = new String(packet, startIndex, endIndex - startIndex, StandardCharsets.UTF_8);

            if(packet[2]==1)
            {
                System.out.println("The file: " + file + " was added to the server");
            }
            else if(packet[2]==0)
            {
                System.out.println("The file: " + file + " was deleted from the server");
            }
        }
    }

    private static boolean getACK(byte[] packet) {
        //check if ack or error
        //if ack of exit
        //if error, error
        //nextMessage
        short ackNum = (short) (((short) (packet[2] & 0xFF)) << 8 | (short) (packet[3] & 0xFF));
        System.out.println("Recieved ACK " + ackNum);
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
                            //client.sendDisconnect(out);
                            //sendDisconnect(out);
                            break;
                        }

                        //String opcode = parts[0].toUpperCase();
                        // if(parts.length == 1){
                        //     System.out.println("Please insert valid command ");
                        //     continue;
                        // }
                        helpFunc(parts, client, reader, line, out , in);
                    }
                    sendDisconnect(out);
                    sendMessage(out, line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread serverMessageThread = new Thread(() -> {
                try {
                    byte[] data= null;
                    byte[] dataToServer=null;
                    int counterWRQ=0;
                    int counter=0;
                    int blockNum=1;
                    int bufferWRQ=0;
                    while (!shouldDisc) {
                        byte[] nextMessage = null;
                        encdec.switch_mode(true);
                        try {
                            while (in.available() > 0) {
                                nextMessage = encdec.decodeNextByte((byte) in.read());
                            }
                        } finally {
                            while(nextMessage==null)
                                nextMessage = encdec.decodeNextByte((byte) '\n');
                            encdec.switch_mode(false);
                        }
///////////////////////////////////////////////////////////////////////////////////////////////////////
                        if(nextMessage[1]==3)
                        {
                            if((short) ((((short) (nextMessage[4] & 0xFF)) << 8 | (short) (nextMessage[5] & 0xFF)))==1)
                                blockNum=1;
                            if(isReadingFromServer)
                            {
                                data = helperRRQ(nextMessage, data, counter, blockNum, out);
                                blockNum++;
                            }
                            else
                            {
                                data = helperDIRQ(nextMessage, data, counter, blockNum, out);
                                blockNum++;
                            }
                        }
///////////////////////////////////////////////////////////////////////////////////////////////////////
                        if(nextMessage[1]==4)
                        {
                            getACK(nextMessage);
                            if(isWritingToServer)
                            {
                                if((short) ((((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF)))==0)
                                {
                                    //String filePath = "client/" + writeFileName;
                                    String filePath =writeFileName;
                                    try {
                                        FileInputStream fileReader =new FileInputStream(filePath);
                                        byte[] fileContent = new byte[fileReader.available()];
                                        fileReader.read(fileContent);
                                        fileReader.close();
                                        dataToServer=fileContent;
                                            
                                    } catch (FileNotFoundException e) {
                                        throw new RuntimeException(e);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    System.out.println("The file " + readFileName + " is being uploaded");
                                    counterWRQ=1;
                                    bufferWRQ = 0;
                                }
                                bufferWRQ = helperWRQ(nextMessage, dataToServer, bufferWRQ, counterWRQ, out);
                                counterWRQ++;
                            }
                            else if((short) ((((short) (nextMessage[2] & 0xFF)) << 8 | (short) (nextMessage[3] & 0xFF)))>0)
                            {
                                System.out.println("WRQ " + writeFileName + " complete");
                                writeFileName="";
                            }
                            if(!connected)
                            {
                                shouldDisc=true;
                            }
                        }
///////////////////////////////////////////////////////////////////////////////////////////////////////                                 
                        if(nextMessage[1]==5)
                        {
                            handleERROR(nextMessage);
                        }
///////////////////////////////////////////////////////////////////////////////////////////////////////
                        if(nextMessage[1]==9)
                        {
                            handleBCAST(nextMessage);
                        } 
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            keyboardThread.start();
            serverMessageThread.start();

            keyboardThread.join();
            serverMessageThread.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void handleERROR(byte[] packet) {
        int startIndex = 4; // Starting index of the part you want to convert
        byte endByte = 10; // Byte indicating the end of the part you want to convert

        // Find the index of the endByte
        int endIndex = startIndex;
        while (endIndex < packet.length && packet[endIndex] != endByte) {
            endIndex++;
        }
        String error = new String(packet, startIndex, endIndex - startIndex, StandardCharsets.UTF_8);
        System.out.println(error);
        isWritingToServer=false;
        isReadingFromServer=false;
    }

    private static void sendMessage(DataOutputStream out, String message) throws IOException {
        out.write(message.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // Method to construct and send a Login Request (LOGRQ) packet
    public static void sendLoginRequest(String username , DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 7;
        String packet = new String(pack) + "" + username + "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Delete File Request (DELRQ) packet
    public static void sendDeleteRequest(String readFileName , DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 8;
        String packet = new String(pack) + readFileName+  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Read Request (RRQ) or Write Request (WRQ) packet
    public static void sendReadWriteRequest(String readFileName, boolean isRead,DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        if(isRead)
        {
            isReadingFromServer=true;
            pack[1] = 1;
        }
        else
        {
            isWritingToServer=true;
            pack[1] = 2;
        }
            String packet = new String(pack) + readFileName+  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Directory Listing Request (DIRQ) packet
    public static void sendDirListingRequest(DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 6;
        String packet = new String(pack) +  "\0" +  "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
    }

    // Method to construct and send a Disconnect (DISC) packet
    public static void sendDisconnect(DataOutputStream out) {
        byte[] pack = new byte[2];
        pack[0] = 0;
        pack[1] = 10;
        String packet = new String(pack) + "\0" + "\n";
        sendPacket(packet.getBytes(StandardCharsets.UTF_8), out);
        connected=false;
        //sendPacket(pack, out);
    }

    public static void sendACK(DataOutputStream out, int blockNum) {
        byte[] pack = new byte[4];
        pack[0] = 0;
        pack[1] = 4;
        pack[2] = (byte) ((blockNum >> 8) & 0xFF);
        pack[3] = (byte) (blockNum & 0xFF);
        //String packet = new String(pack);
        System.out.println("Sent Ack " + blockNum);
        sendPacket(pack, out);
    }

    // Method to send a generic packet (assuming it's already constructed)
    private static void sendPacket(byte[] packet, DataOutputStream out) {
        try {
            out.write(packet);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}


