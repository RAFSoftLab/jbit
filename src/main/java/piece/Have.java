package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;

public class Have extends Message {

    private final int length = 5;
    private final byte id = 4;
    private final int pieceIndex;

    public Have(int pieceIndex, PeerConnection peerConnection) {
        super(peerConnection);
        this.pieceIndex = pieceIndex;
    }


    @Override
    public boolean parse(ByteBuffer buffer) {

        int length = buffer.getInt();
        int id = buffer.get();
        int pieceIndex = buffer.getInt();
        System.out.println("Have message received for index: " + pieceIndex);

        peerConnection.setPieceIndex(pieceIndex);
        return false;
    }

    @Override
    public ByteBuffer create() {
        return null;
    }
}
