package core.message;

import java.nio.ByteBuffer;

public class MessageParser {


    public Message parseMessage(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int lengthPrefix = buffer.getInt();
        byte id = buffer.get();
        byte[] payload = new byte[lengthPrefix - 1];

        return new Message(lengthPrefix, id, payload);

    }


}
