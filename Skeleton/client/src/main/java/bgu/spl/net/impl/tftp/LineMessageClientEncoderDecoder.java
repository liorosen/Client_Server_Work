package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LineMessageClientEncoderDecoder implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    private boolean mode = false;

    @Override
    public byte [] decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if(mode) {
//            if(len == 0 && nextByte != 0)
//                return null;
//            if(len == 1 && nextByte != 3)
//                return null;
            if (len == 517) {
                pushByte(nextByte);
                return popBytes();
            }
            pushByte(nextByte);
            return null; //not a line yet
        }
        else
        {
            if (len == 3) {
                pushByte(nextByte);
                return popString().getBytes();
            }
            pushByte(nextByte);
            return null; //not a line yet
        }
    }

    @Override
    public byte[] encode(byte[] message) {
        return (new String(message) + "\n").getBytes(); //uses utf8 by default
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private byte[] popBytes()
    {
        byte [] copy = Arrays.copyOf(bytes, len);
        len = 0;
        bytes = new byte[1 << 10];
        return copy;
    }
    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, 0, len, Charset.defaultCharset());
        len = 0;
        return result;
    }

    public void switch_mode(boolean state)
    {
        mode = state;
    }
}
