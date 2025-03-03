package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import exceptions.NoAvailableBlock;
import storage.PieceStorage;

import java.nio.ByteBuffer;

public class Request extends Message {

    private final int length = 13;
    private final byte id = 6;
    private final RarestFirstPicker picker;

    public Request(RarestFirstPicker picker, PeerConnection peerConnection) {
        super(peerConnection);
        this.picker = picker;
    }


    @Override
    public boolean parse(ByteBuffer buffer) {
        buffer.flip();
        int length = buffer.getInt();
        int id = buffer.get();
        int index = buffer.getInt();
        int begin = buffer.getInt();
        int blockLength = buffer.getInt();
        System.out.println("Requested piece: " + index + " begin: " + begin + " length: " + blockLength);
        return false;
    }

    @Override
    public ByteBuffer create() {
        TorrentFile torrentFile = peerConnection.getTorrentFile();
        PieceStorage pieceStorage = picker.find(torrentFile);

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
        System.out.println("creating request for " + index + " with offset " + block.offset + " and length " + block.length);
        return buffer;
    }
}
