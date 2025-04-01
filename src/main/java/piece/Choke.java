package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class Choke extends Message {

    private final int length = 1;
    private final byte id = 0;

    public Choke(PeerConnection peerConnection) {
        super(peerConnection);
    }

    @Override
    public ByteBuffer create() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        System.out.println("Choke");
        buffer.putInt(length);
        buffer.put(id);
        return buffer;
    }

    //Note: check if checking is mandatory due to switch case already checking id
    @Override
    public boolean parse(ByteBuffer buffer) {
        int length = buffer.getInt();

        if (length != this.length) {
            throw new RuntimeException("Invalid length");
        }

        byte id = buffer.get();

        if (id != this.id) {
            throw new RuntimeException("Invalid id");
        }

        this.peerConnection.setPeerChoking(1);
        return true;

    }
}
