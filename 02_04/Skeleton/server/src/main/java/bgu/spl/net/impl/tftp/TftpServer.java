package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;

import java.util.function.Supplier;

public class TftpServer extends BaseServer<byte[]> {

    public TftpServer(int port, Supplier<BidiMessagingProtocol<byte[]>> protocolFactory, Supplier<MessageEncoderDecoder<byte[]>> encdecFactory) {
        super(port, protocolFactory, encdecFactory);
    }

    @Override
    protected void execute(BlockingConnectionHandler<byte[]> handler) {
        // The handler will start automatically and execute the communication logic
        // between the server and the client according to the provided protocol.
        handler.run();
    }

    public static void main(String[] args) {
        int port = Integer.parseInt("7777"); // Get the port number from command-line arguments

        // Create a new TFTP server with the specified port
        TftpServer server = new TftpServer(port,
                TftpProtocol::new, // Protocol factory
                TftpEncoderDecoder::new // Encoder/decoder factory
        );

        // Start the server
        server.serve();
    }
}
