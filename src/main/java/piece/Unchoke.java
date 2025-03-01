package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class Unchoke extends Message {

    private final int length = 1;
    private final byte id = 1;


    public Unchoke(PeerConnection peerConnection) {
        super(peerConnection);
    }


    @Override
    public boolean parse(ByteBuffer buffer) {
        System.out.println("Unchoked message received");
        int length = buffer.getInt();
        byte id = buffer.get();
        assert id == 1;
        peerConnection.setPeerChoking(0);
        return false;
    }

    @Override
    public ByteBuffer create() {
        return null;
    }
}
