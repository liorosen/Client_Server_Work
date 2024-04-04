package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    //private ConnectionsImpl  connectionHandlers;

    //private int conId;



    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol , ConnectionsImpl  connectionHandlers) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;

    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {

                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    protocol.process(nextMessage);

                }
                //}
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try {
            if (msg != null) {
                out.write(encdec.encode(msg));
                out.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    //TODO: BIG ADDITION CHECK FREQUENTLY THIS AND IT'S INTERFACE
    public byte[] receiveDataPacket() {
        byte[] buffer = new byte[518]; // Maximum size of a data packet (opcode + block number + data)
        try {
            int bytesRead = in.read(buffer); // Read data from the input stream

            if (bytesRead <= 0) {
                // Handle end of stream or error
                return null;
            }

            return Arrays.copyOf(buffer, bytesRead); // Return the received data packet
        } catch (IOException ex) {
            ex.printStackTrace(); // or any other handling logic
            return null; // Return null or throw a custom exception, depending on your error handling strategy
        }
    }

    public byte[] receiveAckPacket() {
        byte[] buffer = new byte[4];
        try {
            int bytesRead = in.read(buffer); // Read data from the input stream

            if (bytesRead <= 0) {
                // Handle end of stream or error
                return null;
            }

            return Arrays.copyOf(buffer, bytesRead); // Return the received data packet
        } catch (IOException ex) {
            ex.printStackTrace(); // or any other handling logic
            return null; // Return null or throw a custom exception, depending on your error handling strategy
        }
    }
}