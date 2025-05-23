package piece;

import core.PeerConnection;
import storage.PieceStorage;

import java.nio.ByteBuffer;

public class Piece extends Message {

    private final int length;
    private final byte id = 7;
    private final int index;
    private final int begin;
    private final byte[] block;

    public Piece(int index, int begin, byte[] block, PeerConnection peerConnection) {
        super(peerConnection);
        this.index = index;
        this.begin = begin;
        this.block = block;
        this.length = 9 + block.length;
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        int length = buffer.getInt();
        int id = buffer.get();
        int index = buffer.getInt();
        int begin = buffer.getInt();
        assert buffer.remaining() == length - 9;
        byte[] block = new byte[buffer.remaining()];
        buffer.get(block);
        PieceStorage pieceStorage = peerConnection.getTorrentFile()
                .getInfo()
                .getPiecesStorage()
                .get(index);
        assert pieceStorage.getIndex() == index;
        pieceStorage.updateBlock(begin, block);
        peerConnection.updateLastPieceReceived();
        return false;
    }

    @Override
    public ByteBuffer create() {
        peerConnection.updateLastPieceReceived();
        System.out.println("not uploading yet");
        return null;
    }
}
