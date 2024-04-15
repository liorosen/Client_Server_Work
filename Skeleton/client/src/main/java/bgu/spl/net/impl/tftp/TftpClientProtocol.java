// package bgu.spl.net.impl.tftp;

// import bgu.spl.net.api.BidiMessagingProtocol;
// import bgu.spl.net.api.MessagingProtocol;
// import bgu.spl.net.srv.Connections;

// import java.io.DataOutputStream;
// import java.io.IOException;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;

// public class TftpClientProtocol implements BidiMessagingProtocol<String> {
//     private static final byte OP_ACK = 0x04;
//     private boolean shouldTerminate = false;

//     @Override
//     public void process(String msg) {
//         shouldTerminate = "bye".equals(msg);
//         System.out.println("[" + LocalDateTime.now() + "]: " + msg);
//         //return createEcho(msg);
//     }

//     private String createEcho(String message) {
//         String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
//         return message + " .. " + echoPart + " .. " + echoPart + " ..";
//     }

//     @Override
//     public void start(int connectionId, Connections<String> connections) {

//     }

//     @Override
//     public boolean shouldTerminate() {
//         return shouldTerminate;
//     }
// }
