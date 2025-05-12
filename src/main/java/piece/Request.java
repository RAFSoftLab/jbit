package piece;

import core.PeerConnection;
import exceptions.NoAvailableBlock;
import storage.PieceStorage;

import java.nio.ByteBuffer;

public class Request extends Message {

    private final int length = 13;
    private final byte id = 6;

    private final PieceStorage pieceStorage;

    public Request(PieceStorage pieceStorage, PeerConnection peerConnection) {
        super(peerConnection);
        this.pieceStorage = pieceStorage;
    }

    public Request(PeerConnection peerConnection) {
        super(peerConnection);
        this.pieceStorage = null;
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        buffer.flip();
        int length = buffer.getInt();
        int id = buffer.get();
        if(length != this.length && id != this.id) {
            throw new IllegalStateException("Invalid message");
        }
        int index = buffer.getInt();
        int begin = buffer.getInt();
        int blockLength = buffer.getInt();
        return false;
    }

    @Override
    public ByteBuffer create() {
        if(pieceStorage == null ){
            throw new RuntimeException("No piece available");
        }
        int index = pieceStorage.getIndex();
        Block block = pieceStorage.getNextBlock();

        if (block == null) {
            throw new NoAvailableBlock(String.format("No available block for piece with index %d", index));
        }

        ByteBuffer buffer = ByteBuffer.allocate(length + 4);
        buffer.putInt(length);
        buffer.put(id);
        buffer.putInt(index);
        buffer.putInt(block.offset);
        buffer.putInt(block.length);
        return buffer;
    }
}
