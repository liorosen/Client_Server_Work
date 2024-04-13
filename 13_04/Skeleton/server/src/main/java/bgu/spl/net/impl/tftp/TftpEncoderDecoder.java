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
        if (nextByte == '\n' && len>1) {
            return popString().getBytes();
        }
        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(byte[] message) {
        // byte[] encodedMessage = new byte[message.length + 1];
        // System.arraycopy(message, 0, encodedMessage, 0, message.length);
        //encodedMessage[message.length] = '\n'; // Append newline character
        return message;
    }
}