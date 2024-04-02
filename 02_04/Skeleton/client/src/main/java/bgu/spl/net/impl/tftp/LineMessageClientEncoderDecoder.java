package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LineMessageClientEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    private boolean mode = false;

    @Override
    public String decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if(mode) {
            if (len == 517) {
                pushByte(nextByte);
                return popString();
            }
            pushByte(nextByte);
            return null; //not a line yet
        }
        else
        {
            if (nextByte == '\n') {
                return popString();
            }
            pushByte(nextByte);
            return null; //not a line yet
        }
    }

    @Override
    public byte[] encode(String message) {
        return (message + "\n").getBytes(); //uses utf8 by default
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
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
