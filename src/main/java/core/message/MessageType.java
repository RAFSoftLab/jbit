package core.message;

public enum MessageType {
    KEEP_ALIVE(-1),
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7),
    CANCEL(8);

    private int massageId;

    MessageType(int massageId) {
        this.massageId = massageId;
    }

    public static MessageType getMessageType(int id) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.massageId == id) {
                return messageType;
            }
        }
        return null;

    }
    }
