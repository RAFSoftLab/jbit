package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class Cancel extends Message {

    public Cancel(PeerConnection peerConnection) {
        super(peerConnection);
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        System.out.println("cancel");
        return false;
    }

    @Override
    public ByteBuffer create() {
        return null;
    }
}
