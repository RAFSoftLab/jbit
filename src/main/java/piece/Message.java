package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public abstract class Message {

    protected final PeerConnection peerConnection;

    public Message(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public abstract boolean parse(ByteBuffer buffer);

    public abstract ByteBuffer create();

}
