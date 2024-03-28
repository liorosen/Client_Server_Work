package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.UserInfosPro;
//import com.sun.security.ntlm.Client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private static final int PORT = 7777;
    private static final String FILES_DIRECTORY = "Skeleton/server/Flies/";

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
    private UserInfosPro userSys = new UserInfosPro();

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;

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
        // Convert bytes to integers before shifting
//        int highByte = Byte.toUnsignedInt(message[0]) << 8;
//        int lowByte = Byte.toUnsignedInt(message[1]);
//
//        // Combine the high and low bytes using bitwise OR
//        return (short) (highByte | lowByte);
        return (short) (((short) (message[0] & 0xFF)) << 8 | (short) (message[1] & 0xFF));
    }

    private short extractDelorAddnum(byte[] message) {
        return (short) ((message[2] << 8));
    }

    public void notifyAllLoggedInUsers(String filename, byte[] packet, Connections<byte[]> connections) {
        for (Map.Entry<String,Integer> entry : userSys) {
            int connectionId = entry.getValue();
            connections.send(connectionId, packet);
        }
    }

    private byte[] createDataPacket(int blockNumber, byte[] data) {
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

    // Handles Functions
    public byte[] handleLOGRQ(String username) {
        // Check if the username is null or empty
        if (username == null || username.isEmpty()) {
            return handleErrorPacket(0, "Username is invalid"); // Return an ERROR packet if the username is invalid
        }
        // Check if the user is already logged in
        if (userSys.searchUserByID(connectionId)) {
            errorConverter(7);
            return null;
        }

        // Mark the user as logged in
        userSys.addUser(connectionId,username);

        // Return an ACK packet to indicate successful login
        System.out.println("hi your name is " + username + " accepted");
        return handleACK(0);
    }

    public byte[] handleACK(int blockNumber) {
        byte[] packet = new byte[4]; // Opcode (2 bytes) + Block Number (2 bytes)
        packet[0] = 0; // High byte of opcode
        packet[1] = OP_ACK; // Low byte of opcode
        packet[2] = (byte) ((blockNumber >> 8) & 0xFF); // High byte of block number
        packet[3] = (byte) (blockNumber & 0xFF); // Low byte of block number
        connections.send(connectionId,packet);
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

    private byte[] handleBCAST(String filename, int num, Connections<byte[]> connections) {
        // Construct the BCAST packet
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        int packetSize = 3 + filenameBytes.length + 1; // Opcode (2 bytes) + Deleted/Added (1 byte) + Filename (variable) + Null terminator (1 byte)
        byte[] packet = new byte[packetSize];
        packet[0] = 0;
        packet[1] = 9; // OP_BCAST
        packet[2] = (byte) (num);
        System.arraycopy(filenameBytes, 0, packet, 3, filenameBytes.length);
        packet[packetSize - 1] = 0;
        notifyAllLoggedInUsers(filename, packet, connections);
        return packet;
    }

    public  byte[] handleDELRQ(String filename, Connections<byte[]> connections) {
        // Convert filename to bytes using UTF-8 encoding
        File file = new File(FILES_DIRECTORY+filename);
        Boolean isFileExists = file.exists();
        if (!isFileExists) {
            // Return an error packet indicating that the file does not exist
            return errorConverter(1);
        }

        // Delete the file
        boolean isDeleted = file.delete();
        handleACK(0);
        if (isDeleted) {
            return handleBCAST(filename, 0, connections);
        } else {
            // Return an error packet indicating that the file could not be deleted
            return handleErrorPacket(2, "Access violation - Cannot delete file: " + filename);
        }

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

    public void handleDISC() {
        handleACK(0);
        userSys.deleteUserByID(connectionId);
        connections.disconnect(connectionId);
        shouldTerminate = true;
        //return handleACK(0);

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


    public void handleRRQ(byte[] message) {
        String fileName = extractFilename(message,2 );
        if(!isFileExist(fileName.toLowerCase())){
            errorConverter(1);
        }
        sendFile(uploadFiletoMemory(fileName));
    }

    private void sendFile(byte[] data) {
        List<byte[]> dataPackets = new ArrayList<>();
        int blockNumber = 1;
        int offset = 0;
        while (offset < data.length) {
            int blockSize = Math.min(data.length - offset, 512);
            byte[] blockData = Arrays.copyOfRange(data, offset, offset + blockSize);
            dataPackets.add(createDataPacket(blockNumber, blockData));
            connections.send(connectionId, createDataPacket(blockNumber, blockData));
            connections.send(connectionId,"\n".getBytes());

            //stop and read socket to get ack to continue
            //process(connections.getHandler(connectionId).receiveAckPacket());
            offset += blockSize;
            blockNumber++;
        }
//        if(dataPackets.get(dataPackets.size()-1).length==518)
//        {
            //TODO: Make sure it is good!
        byte[] backsalshn = new byte [0];
        //backsalshn[0] = '0';
        dataPackets.add(createDataPacket(blockNumber, backsalshn));
        connections.send(connectionId, createDataPacket(blockNumber, backsalshn));
        connections.send(connectionId,"\n".getBytes());
            //process(connections.getHandler(connectionId).receiveAckPacket());
//        }

        // Send data packets to the client
        // int counterBlock=1;
        // for (byte[] packet : dataPackets) {
        //     connections.send(connectionId, packet);
        //     handleACK(counterBlock);
        //     counterBlock++;
        // }

        //connections.send(connectionId, createDataPacket(blockNumber , backsalshn));

    }

    private boolean isFileExist(String fileName) {
        try {
            FileReader fileReader = new FileReader( FILES_DIRECTORY  + fileName);
            return true;
        } catch (FileNotFoundException e) {
            return  false;
        }
    }

    private byte[] uploadFiletoMemory(String fileName) {
        try {
            //Scanner fileReader = new Scanner( new File(FILES_DIRECTORY  + fileName));
            // String fileContent = new String();
            // while(fileReader.hasNext()){
            //     fileContent += fileReader.next();
            // }
            // return fileContent.getBytes();
            // StringBuilder fileContent = new StringBuilder();
            // while (fileReader.hasNextLine()) {
            //     fileContent.append(fileReader.nextLine()).append("\n"); // Read entire line including newline
            // }
            // return fileContent.toString().getBytes();
            InputStream inputStream = new FileInputStream(FILES_DIRECTORY + fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024]; // Adjust buffer size as needed
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
            // } catch (FileNotFoundException e) {
            //     throw new RuntimeException(e);
            // }
        } catch (IOException e) {
            //TODO: Error that the file isn't found!
            throw new RuntimeException(e);
        }
    }

    public void handleWRQ(byte[] message) {
        // Extract the filename from the WRQ message
        String filename = extractFilename(message, 2);
        if(doesFileExist(filename))
        {
            //TODO: Check correction again
            errorConverter(5);
            return;
        }
        String bytes = new String();
        List<byte[]> dataPackets = new ArrayList<>();
        List<byte[]> ackPackets = new ArrayList<>();
        byte [] data = bytes.getBytes();
        // Create a FileOutputStream to write the received data to the file
        try (FileOutputStream fos = new FileOutputStream(FILES_DIRECTORY + filename)) {
            // Initialize variables for receiving data
            handleACK(0);
            int blockNumber = 1;
            int offset = 0;
            byte[] dataPacket2 = connections.getHandler(connectionId).receiveDataPacket();
            // Continue receiving data until an empty data packet is received
            while (true) {
                offset+=dataPacket2.length-6;
                dataPackets.add(dataPacket2);
                //int blockSize = Math.min(data.length - offset, 512);
                //byte[] blockData = Arrays.copyOfRange(data, offset, offset + blockSize);
                //byte[] dataPacket = (blockNumber, blockData); // Implement this method to receive data packets
                // // Check if the received data packet is empty (end of file)

                // // Write the data from the data packet to the file
                // fos.write(dataPacket, 4, dataPacket.length - 4); // Skip the first 4 bytes (opcode and block number)

                // // Create an ACK packet for the received data packet
                // byte[] ackPacket = handleACK(0);
                // ackPackets.add(ackPacket); // Store the ACK packet for later sending

                // // Increment the block number for the next data packet
                // offset += blockSize;
                // blockNumber++;

                if (dataPacket2.length < 518) {
                    break;
                }
                handleACK(blockNumber);
                blockNumber++;
                dataPacket2 = connections.getHandler(connectionId).receiveDataPacket();
            }
            byte[] finalData=new byte[offset];
            int counter=0;
            for (byte[] dataPart : dataPackets) {
                System.arraycopy(dataPart, 6, finalData, counter, dataPart.length-6);
                counter+=dataPart.length-6;
            }

            // Write the data from the data packet to the file
            fos.write(finalData, 0, finalData.length); // Skip the first 6 bytes (opcode and block number)
//            // Send all ACK packets for the received data packets
//            for (byte[] ackPacket : dataPackets) {
//                // Send the ACK packet to the client (replace this with actual logic)
//                connections.send(connectionId, ackPacket);
//            }

            // Send a final ACK packet to acknowledge the end of file transfer
            byte[] finalAckPacket = handleACK(blockNumber);
            //connections.send(connectionId, finalAckPacket); // Send the final ACK packet

            handleBCAST(filename, 1, connections);
        } catch (IOException e) {
            e.printStackTrace(); // Handle file I/O errors
        }
    }

    private static boolean doesFileExist(String fileName) {
        File directory = new File(FILES_DIRECTORY);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        System.out.println(new String(message));
        // Check the message opcode
        short opcode = extractOpcode(message);
        switch (opcode){
            case 1:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleRRQ(message);
                break;
            case 2:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleWRQ(message);
                break;
            case 3:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleDATA(message);
                break;
            case 4:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleACK((int) message[3]);
                break;
            case 5:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    errorConverter((int) message[3]);
                break;
            case 6:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleDIRQ();
                break;
            case 7:
                // if (!userSys.searchUserByID(connectionId)){
                //     errorConverter(7);
                // }
                handleLOGRQ(new String(Arrays.copyOfRange(message, 2, message.length)));
                break;
            case 8:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleDELRQ(extractFilename(message,2),connections);
                break;
            case 9:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
                    handleBCAST(extractFilename(message,3),extractDelorAddnum(message),connections);
                break;
            case 10:
                if (!userSys.searchUserByID(connectionId)){
                    errorConverter(6);
                }
                else
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

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
//        this.connections.disconnect(this.connectionId);
//        loggedInUserIDs.remove(this.connectionId);
//        loggedInUserNames.remove(Integer.toString(connectionId), false);
        return  shouldTerminate;
    }


}