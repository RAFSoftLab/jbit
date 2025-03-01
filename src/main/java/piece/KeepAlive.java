package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class KeepAlive extends Message {


    public KeepAlive(PeerConnection peerConnection) {
        super(peerConnection);
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        System.out.println("Keep alive");
        return false;
    }

    @Override
    public ByteBuffer create() {
        System.out.println("Keep alive");
        return null;
    }
}
