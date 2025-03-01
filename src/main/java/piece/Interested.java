package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class Interested extends Message {

    private final int length = 1;
    private final byte id = 2;

    public Interested(PeerConnection peerConnection) {
        super(peerConnection);
    }

    @Override
    public ByteBuffer create() {
        peerConnection.setAmInterested(1);
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.putInt(length);
        buffer.put(id);
        System.out.println("Sending interested message");
        return buffer;
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte id = buffer.get();
        assert id == 2;
        peerConnection.setPeerInterested(1);
        return false;
    }
}
