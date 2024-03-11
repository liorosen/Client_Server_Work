package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
//import com.sun.security.ntlm.Client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private static final int PORT = 7777;
    private static final String FILES_DIRECTORY = "../Flies/";

    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate = false;
    private static final byte OP_RRQ = 0x01; // Opcode for RRQ packet
    private static final byte OP_WRQ = 0x02;    // Opcode for WRQ packet
    private static final byte OP_DATA = 0x03;  // Opcode for DATA packet
    private static final byte OP_ACK = 0x04; // Opcode for ACK packet
    private static final byte OP_ERROR = 0x05;    // Opcode for ERROR packet
    private static final byte OP_DIRQ = 0x06;  // Opcode for DIRQ packet
    private static final byte OP_LOGRQ = 0x07; // Opcode for LOGRQ packet
    private static final byte OP_DELRQ = 0x08;    // Opcode for DELRQ packet
    private static final byte OP_BCAST = 0x09;  // Opcode for BCAST packet
    private static final byte OP_DISC = 0x0A;  // Opcode for DISC packet
    private static ConcurrentHashMap<Integer, Boolean> loggedInUserIDs = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Boolean> loggedInUserNames = new ConcurrentHashMap<>();

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;
        // Initialize the loggedInUsers maps for this connectionId
        loggedInUserNames.put(Integer.toString(connectionId), false); // Initially, the user is not logged in
        loggedInUserIDs.put(connectionId, false);

    }

    // Sub-Functions implements
    private String extractUsername(byte[] message) {
        int nullTerminatorIndex = 2; // Start at index 2, which is the first byte after the opcode
        while (message[nullTerminatorIndex] != 0) {
            nullTerminatorIndex++; // Move to the next byte until the null terminator is found
        }
        // Convert the bytes representing the username to a string using UTF-8 encoding
        return new String(message, 2, nullTerminatorIndex - 2, StandardCharsets.UTF_8);
    }

    private String extractFilename(byte[] message, int nullTerminatorIndex) {
        // Start at index 2, which is the first byte after the opcode
        while (message[nullTerminatorIndex] != 0) {
            nullTerminatorIndex++; // Move to the next byte until the null terminator is found
        }
        // Convert the bytes representing the filename to a string using UTF-8 encoding
        return new String(message, 2, nullTerminatorIndex - 2, StandardCharsets.UTF_8);
    }

    private short extractOpcode(byte[] message) {
        return (short) ((message[0] << 8) | (message[1] & 0xFF));
    }

    private short extractDelorAddnum(byte[] message) {
        return (short) ((message[2] << 8));
    }

    public static void notifyAllLoggedInClients(String filename, byte[] packet, Connections<byte[]> connections) {
        for (Map.Entry<Integer, Boolean> entry : loggedInUserIDs.entrySet()) {
            int connectionId = entry.getKey();
            boolean isLoggedIn = entry.getValue();

            if (isLoggedIn) {
                connections.send(connectionId, packet);
            }
        }

    }

    private byte[] createDataPacket(int blockNumber, byte[] data) {
        byte[] packet = new byte[data.length + 4];
        packet[0] = 0; // Opcode for DATA
        packet[1] = 3; // Opcode value for DATA
        packet[2] = (byte) ((blockNumber >> 8) & 0xFF); // High byte of block number
        packet[3] = (byte) (blockNumber & 0xFF); // Low byte of block number
        System.arraycopy(data, 0, packet, 4, data.length);
        return packet;
    }

    // Handles Functions
    public byte[] handleLOGRQ(String username) {
        // Check if the username is null or empty
        if (username == null || username.isEmpty()) {
            return handleErrorPacket(0, "Username is invalid"); // Return an ERROR packet if the username is invalid
        }
        // Check if the user is already logged in
        if (loggedInUserNames.containsKey(username)) {
            errorConverter(7);
        }

        // Mark the user as logged in
        loggedInUserNames.put(username, true);

        // Return an ACK packet to indicate successful login
        System.out.println("hi your name is" + username);
        return handleACK(0);
    }

    public static byte[] handleACK(int blockNumber) {
        byte[] packet = new byte[4]; // Opcode (2 bytes) + Block Number (2 bytes)
        packet[0] = 0; // High byte of opcode
        packet[1] = OP_ACK; // Low byte of opcode
        packet[2] = (byte) ((blockNumber >> 8) & 0xFF); // High byte of block number
        packet[3] = (byte) (blockNumber & 0xFF); // Low byte of block number

        return packet;
    }

    private byte[] handleErrorPacket(int errorCode, String errorMessage) {
        byte[] errorCodeBytes = {(byte) ((errorCode >> 8) & 0xFF), (byte) (errorCode & 0xFF)};
        byte[] errorMessageBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        int packetSize = 4 + errorMessageBytes.length + 1; // Opcode (2 bytes) + ErrorCode (2 bytes) + ErrorMessage (variable) + Null terminator (1 byte)
        byte[] packet = new byte[packetSize];

        // Set opcode
        packet[0] = 0;
        packet[1] = OP_ERROR;

        // Set ErrorCode
        System.arraycopy(errorCodeBytes, 0, packet, 2, 2);

        // Set ErrorMessage
        System.arraycopy(errorMessageBytes, 0, packet, 4, errorMessageBytes.length);

        // Set null terminator
        packet[packetSize - 1] = 0;
        connections.send(connectionId,packet);
        return packet;
    }

    private byte[] errorConverter(int errorCode) {
        switch (errorCode) {

            case 1:
                return handleErrorPacket(errorCode, "File not found – RRQ DELRQ of non-existing file.");

            case 2:
                return handleErrorPacket(errorCode, "Access violation – File cannot be written, read or deleted.");

            case 3:
                return handleErrorPacket(errorCode, "Disk full or allocation exceeded – No room in disk.");

            case 4:
                return handleErrorPacket(errorCode, "Illegal TFTP operation – Unknown Opcode.");

            case 5:
                return handleErrorPacket(errorCode, "File already exists – File name exists on WRQ.");

            case 6:
                return handleErrorPacket(errorCode, "User not logged in – Any opcode received before Login completes.");

            case 7:
                return handleErrorPacket(errorCode, "User already logged in – Login username already connected.");

            default:
                return handleErrorPacket(0, "Not defined");

        }
    }

    private void handleBCAST(String filename, int num, Connections<byte[]> connections) {
        // Construct the BCAST packet
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        int packetSize = 4 + filenameBytes.length + 1; // Opcode (2 bytes) + Deleted/Added (1 byte) + Filename (variable) + Null terminator (1 byte)
        byte[] packet = new byte[packetSize];
        packet[0] = 0;
        packet[1] = 9; // OP_BCAST
        packet[2] = (byte) (num);
        System.arraycopy(filenameBytes, 0, packet, 3, filenameBytes.length);
        packet[packetSize - 1] = 0;
        notifyAllLoggedInClients(filename, packet, connections);

    }

    public  byte[] handleDELRQ(String filename, Connections<byte[]> connections) {
        // Convert filename to bytes using UTF-8 encoding
        File file = new File(filename);
        Boolean isFileExists = file.exists();
        if (!isFileExists) {
            // Return an error packet indicating that the file does not exist
            return handleErrorPacket(1, "File not found: " + filename);
        }

        // Delete the file
        boolean isDeleted = file.delete();
        if (isDeleted) {
            handleBCAST(filename, 0, connections);
        } else {
            // Return an error packet indicating that the file could not be deleted
            return handleErrorPacket(2, "Access violation - Cannot delete file: " + filename);
        }
        return handleACK(0);
//        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
//
//        // Calculate packet size
//        int packetSize = 3 + filenameBytes.length + 1; // Opcode (2 bytes) + Filename (variable) + Null terminator (1 byte)
//        byte[] packet = new byte[packetSize];
//
//        // Set opcode
//        packet[0] = 0;
//        packet[1] = OP_DELRQ;
//
//        // Set filename
//        System.arraycopy(filenameBytes, 0, packet, 2, filenameBytes.length);
//
//        // Set null terminator
//        packet[packetSize - 1] = 0;
//
//        return packet;

    }

    public List<byte[]> handleDIRQ() {
        File[] files = new File(FILES_DIRECTORY).listFiles();
        List<byte[]> dataPackets = new ArrayList<>();
        int blockNumber = 1;
        if (files != null) {
            String fileList = "";
            for (File file : files) {
                if (file.isFile()) {
                    fileList += file.getName() + "\0";
                    if (fileList.length() > 512) {
                        dataPackets.add(createDataPacket(blockNumber, fileList.getBytes(StandardCharsets.UTF_8)));
                        fileList = "";
                        blockNumber++;
                    }
                }
            }
            // Add the remaining files to the last data packet
            if (!fileList.isEmpty()) {
                dataPackets.add(createDataPacket(blockNumber, fileList.getBytes(StandardCharsets.UTF_8)));
            }
            for (byte[] packet : dataPackets) {
                connections.send(connectionId, packet);
            }
        }
        return dataPackets;
    }

    public byte[] handleDISC() {
        loggedInUserIDs.remove(connectionId);
        for (Map.Entry<String, Boolean> entry : loggedInUserNames.entrySet()) {
            if (entry.getKey().equals(Integer.toString(connectionId))) {
                loggedInUserNames.remove(entry.getKey(), false);
                break;
            }
        }
        connections.disconnect(connectionId);
        return handleACK(0);

    }

    public List<byte[]> handleDATA(byte[] data) {
        List<byte[]> dataBlocks = new ArrayList<>();
        int dataSize = data.length;
        int currentIndex = 0;
        int blockNumber = 1;

        while (currentIndex < dataSize) {
            int remainingDataSize = dataSize - currentIndex;
            int blockSize = Math.min(512, remainingDataSize); // Determine the size of the current block
            byte[] blockData = new byte[blockSize];

            // Copy the data for the current block
            System.arraycopy(data, currentIndex, blockData, 0, blockSize);
            dataBlocks.add(createDataPacket(blockNumber, blockData)); // Create a data packet for the current block

            currentIndex += blockSize; // Move to the next block
            blockNumber++; // Increment the block number
        }

        return dataBlocks;
    }

    private String uploadFile2Memory(byte[] message){
        String bytess = new String();
        try {
            FileInputStream fis = new FileInputStream(extractFilename(message, message.length - 1));
            int byteRead;
            while ((byteRead = fis.read()) != -1) {
                bytess += byteRead;
            }
        } catch (FileNotFoundException e) {
            errorConverter(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bytess;
    }
//    public byte[] sendSection(byte[] section , byte [] data , int start , int j) {
//       connections.send(connectionId,section);
//        int seclen = Math.min(512, data.length - start);;
//        if(  seclen < 512 && seclen > 0 ){
//            section = new byte[seclen + 1];
//        }
//        else{
//            section = new byte[512];
//        }
//        return section;
//    }
    public byte[] handleRRQ(byte[] message) {
        // First part uploading file to the memory
        String bytes = uploadFile2Memory(message);
        List<byte[]> dataPackets = new ArrayList<>();
        byte [] data = bytes.getBytes();
        // Handle data, send packets of 512 bytes
//        byte [] section  = new byte[512];
//        for (int i = 0, j = 0; i < data.length; i++ ){
//            section[j] = data[i];
//            if(j % 512 == 0) {
//                j = 0;
//                sendSection(section , data , i , j);
//            }
//            else j++;
//        }
        int blockNumber = 1;
        int offset = 0;
        while (offset < data.length) {
            int blockSize = Math.min(data.length - offset, 512);
            byte[] blockData = Arrays.copyOfRange(data, offset, offset + blockSize);
            dataPackets.add(createDataPacket(blockNumber, blockData));
            offset += blockSize;
            blockNumber++;
        }
        byte[] backsalshn = new byte [1];
        backsalshn[0] = '\n';
        // Send data packets to the client
        for (byte[] packet : dataPackets) {
            connections.send(connectionId, packet);
        }
        
        connections.send(connectionId, createDataPacket(blockNumber , backsalshn));
        return data;
    }




    @Override
    public void process(byte[] message) {
        // TODO implement this

        // Check the message opcode
        short opcode = extractOpcode(message);
        if ( opcode != 7){
            errorConverter(6);
        }
        else{
            switch (opcode){
                case 1:
                    handleRRQ(message);
                    break;
                case 2:
                    //handleWRQ(message);
                    break;
                case 3:
                    handleDATA(message);
                    break;
                case 4:
                    handleACK((int) message[3]);
                    break;
                case 5:
                    errorConverter((int) message[3]);
                    break;
                case 6:
                    handleDIRQ();
                    break;
                case 7:
                    handleLOGRQ(extractUsername(message));
                    break;
                case 8:
                    handleDELRQ(extractFilename(message,2),connections);
                    break;
                case 9:
                    handleBCAST(extractFilename(message,3),extractDelorAddnum(message),connections);
                    break;
                case 10:
                    handleDISC();
                    break;

                // Handle other opcodes
                default:
                    // Unknown opcode, send an error message
                    String errorMessage = "Unknown opcode: " + opcode;
                    connections.send(connectionId, errorMessage.getBytes());
                    break;
            }
        }

    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
//        this.connections.disconnect(this.connectionId);
//        loggedInUserIDs.remove(this.connectionId);
//        loggedInUserNames.remove(Integer.toString(connectionId), false);
        return  shouldTerminate;
    }


}
