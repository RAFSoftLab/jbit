package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class NotInterested extends Message {

    private final int length = 1;
    private final byte id = 3;

    public NotInterested(PeerConnection peerConnection) {
        super(peerConnection);
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        int length = buffer.getInt();
        int id = buffer.get();
        assert  id == 3;
        System.out.println("Not intrested message");
        peerConnection.setPeerInterested(0);
        return false;
    }

    @Override
    public ByteBuffer create() {
        peerConnection.setAmInterested(0);
        System.out.println("Not interested message AM");
        return null;
    }
}
