package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private static final int MAX_MESSAGE_SIZE = 512; // Maximum size of a TFTP message

    private byte[] buffer = new byte[MAX_MESSAGE_SIZE];
    private int length = 0;
    private byte[] bytes = new byte[MAX_MESSAGE_SIZE]; // 1KB
    private int len = 0;
    private short opCode = -1;
    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0;
        return result;
    }
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (nextByte == '\n') {
            return popString().getBytes();
        }
        pushByte(nextByte);
        return null; //not a line yet
//        if ( (nextByte == 0 && length>0) || buffer[1]==4 || (nextByte==6 && length==1) || (nextByte==10 && length==1)) {
//            buffer[length++] = nextByte;
//            if(buffer[1]!=4)
//            {
//                byte[] message = Arrays.copyOf(buffer, length);
//                length = 0;
//                buffer = new byte[MAX_MESSAGE_SIZE];
//                return message;
//            }
//            else
//            {
//                if(length==4)
//                {
//                    byte[] message = Arrays.copyOf(buffer, length);
//                    length = 0;
//                    buffer = new byte[MAX_MESSAGE_SIZE];
//                    return message;
//                }
//                // else if(buffer[1]==6)
//                // {
//                //     byte[] message = Arrays.copyOf(buffer, length);
//                //     length = 0;
//                //     buffer = new byte[MAX_MESSAGE_SIZE];
//                //     return message;
//                // }
//                return null;
//            }
//        } else {
//            buffer[length++] = nextByte;
//            return null;
//        }
//            if (len == 0) {
//                // The first two bytes represent the opcode
//                bytes[0] = nextByte;
//                len++;
//            } else if (len == 1) {
//                bytes[1] = nextByte;
//                //opCode = (short) ((bytes[0] << 8) | (bytes[1] & 0xFF));
//                //TODO: Check the byte to short
//                opCode = (short) (((short) bytes[0] & 0xFF) << 8 | (short) (bytes[1] & 0xFF));
//                len++;
//            } else {
//                //bytes[len++] = nextByte;
//                switch (opCode) { // Check the opcode
//                    case 1: //RRQ packet
//                    case 2: //WRQ packet
//                        if (nextByte == 0) { // Null byte indicates end of filename
        // bytes[len++] = nextByte;
        // byte[] message = Arrays.copyOf(bytes, len);
        // len = 0;
        // bytes = new byte[MAX_MESSAGE_SIZE];
        // return message;
//                        }
//                        // } else {
//                        //     bytes[len++] = nextByte; // Append the byte to the buffer
//                        // }
//                        break;
//                    case 3: // Data packet TODO: Check again
//                        if (len >= 6) { // At least 6 bytes for block number
//                            // Calculate packet length based on data size
//                            int dataSize = ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
//                            int packetLength = 6 + dataSize;
//                            if (len >= packetLength || nextByte == 0) { // Check if we've received all data bytes or encountered a null byte
//                                //int blockNumber = (((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF));
//                                //byte[] data = Arrays.copyOfRange(bytes, 6, len); // Extract the data bytes
//                                byte[] packetData = new byte[packetLength];
//                                System.arraycopy(bytes, 0, packetData, 0, packetLength); // Copy the complete packet data
//                                len = 0; // Reset buffer length for next packet
//                                return packetData;
//                            }
//                        }
//                        break;
//                    case 4: // ACK packet
//                        if (len >= 4) { // At least 4 bytes for block number
//                            //int blockNumber = ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
//                            byte[] packetData = new byte[4]; // ACK packet has a fixed length of 4 bytes
//                            packetData[0] = 0; // Most significant byte of opcode
//                            packetData[1] = 4; // Least significant byte of opcode
//                            packetData[2] = bytes[2]; // Copy block number (MSB)
//                            packetData[3] = bytes[3]; // Copy block number (LSB)
//                            len = 0; // Reset buffer length for next packet
//                            return packetData;
//                        }
//                        break;
//                    case 5: // ERROR packet
//                        if (len >= 4) { // At least 4 bytes for error code
//                            //int errorCode = ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
//                            int messageLength = 0;
//                            for (int i = 4; i < len; i++) {
//                                if (bytes[i] == 0) { // Null byte indicates end of error message
//                                    messageLength = i - 4; // Calculate length of error message
//                                    break;
//                                }
//                            }
//                            if (messageLength > 0) { // If error message exists
//                                //String errorMessage = new String(Arrays.copyOfRange(bytes, 4, 4 + messageLength), StandardCharsets.UTF_8);
//                                byte[] packetData = new byte[5 + messageLength]; // ERROR packet length = 5 + length of error message
//                                packetData[0] = 0; // Most significant byte of opcode
//                                packetData[1] = 5; // Least significant byte of opcode
//                                packetData[2] = bytes[2]; // Copy error code (MSB)
//                                packetData[3] = bytes[3]; // Copy error code (LSB)
//                                System.arraycopy(bytes, 4, packetData, 4, messageLength); // Copy error message bytes
//                                packetData[packetData.length - 1] = 0; // Null terminator for the error message
//                                len = 0; // Reset buffer length for next packet
//                                return packetData;
//                            }
//                        }
//                        break;
//                    case 6: // DIRQ packet
//                        if (len >= 2) {
//                            byte[] packetData = new byte[2]; // DIRQ packet length = 2 (opcode bytes)
//                            packetData[0] = 0; // Most significant byte of opcode
//                            packetData[1] = 6; // Least significant byte of opcode
//                            len = 0; // Reset buffer length for next packet
//                            return packetData;
//                        }
//                        break;
//                    case 7: // LOGRQ packet
//                        if (len >= 3 && nextByte == 0) { // Check if the opcode is followed by at least one character and a null byte
//                            String username = new String(Arrays.copyOfRange(bytes, 2, len), StandardCharsets.UTF_8);
//                            byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
//                            byte[] packetData = new byte[3 + usernameBytes.length]; // Opcode (2 bytes) + username bytes + null terminator
//                            packetData[0] = 0; // Most significant byte of opcode
//                            packetData[1] = 7; // Least significant byte of opcode
//                            System.arraycopy(usernameBytes, 0, packetData, 2, usernameBytes.length); // Copy username bytes
//                            packetData[packetData.length - 1] = 0; // Null terminator for the username
//                            len = 0; // Reset buffer length for next packet
//                            return packetData;
//                        } else {
//                            bytes[len++] = nextByte; // Append the byte to the buffer
//                        }
//                        break;
//                    case 8: // DELRQ packet
//                        if (len >= 3 && nextByte == 0) { // Check if the opcode is followed by at least one character and a null byte
//                            String fileName = new String(Arrays.copyOfRange(bytes, 2, len - 1), StandardCharsets.UTF_8);
//                            byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
//                            byte[] packetData = new byte[3 + fileNameBytes.length]; // Opcode (2 bytes) + filename bytes + null terminator
//                            packetData[0] = 0; // Most significant byte of opcode
//                            packetData[1] = 8; // Least significant byte of opcode
//                            System.arraycopy(fileNameBytes, 0, packetData, 2, fileNameBytes.length); // Copy filename bytes
//                            packetData[packetData.length - 1] = 0; // Null terminator for the filename
//                            len = 0; // Reset buffer length for next packet
//                            return packetData;
//                        }
//                        break;
//                    case 9: // BCAST packet
//                        if (nextByte == 0) { // Check if the opcode is followed by at least one character and a null byte
//                            String message = new String(Arrays.copyOfRange(bytes, 2, len - 1), StandardCharsets.UTF_8);
//                            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
//                            byte[] packetData = new byte[2 + messageBytes.length + 1]; // Opcode (2 bytes) + message bytes + null terminator
//                            packetData[0] = 0; // Most significant byte of opcode
//                            packetData[1] = 9; // Least significant byte of opcode
//                            System.arraycopy(messageBytes, 0, packetData, 2, messageBytes.length); // Copy message bytes
//                            packetData[packetData.length - 1] = 0; // Null terminator for the message
//                            len = 0; // Reset buffer length for next packet
//                            return packetData;
//                        } else {
//                            bytes[len++] = nextByte; // Append the byte to the buffer
//                        }
//                        break;
//                    case 10: // DISC packet
//                        byte[] packetData = new byte[2]; // Opcode (2 bytes)
//                        packetData[0] = 0; // Most significant byte of opcode
//                        packetData[1] = 10; // Least significant byte of opcode
//                        len = 0; // Reset buffer length for next packet
//                        return packetData;
//                    default:
//                        return null;
//                    // Handle other packet types here
//                }
//                return null; // Continue reading bytes
//            }
//            return null;

    }

    @Override
    public byte[] encode(byte[] message) {
        // byte[] encodedMessage = new byte[message.length + 1];
        // System.arraycopy(message, 0, encodedMessage, 0, message.length);
        //encodedMessage[message.length] = '\n'; // Append newline character
        return message;
    }
}