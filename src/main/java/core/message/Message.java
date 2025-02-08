package core.message;

public class Message {

    private final int lengthPrefix;
    private final MessageType type;
    private final byte[] payload;

    public Message(int lengthPrefix, byte messageId, byte[] payload) {
        this.lengthPrefix = lengthPrefix;
        this.type = MessageType.getMessageType(messageId & 0xFF);
        this.payload = payload;
    }






}
