package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private static final int MAX_MESSAGE_SIZE = 516; // Maximum size of a TFTP message

    private byte[] buffer = new byte[MAX_MESSAGE_SIZE];
    private int length = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if (nextByte == '\n') {
            byte[] message = Arrays.copyOf(buffer, length);
            length = 0;
            return message;
        } else {
            buffer[length++] = nextByte;
            return null;
        }
    }

    @Override
    public byte[] encode(byte[] message) {
        byte[] encodedMessage = new byte[message.length + 1];
        System.arraycopy(message, 0, encodedMessage, 0, message.length);
        encodedMessage[message.length] = '\n'; // Append newline character
        return encodedMessage;
    }
}