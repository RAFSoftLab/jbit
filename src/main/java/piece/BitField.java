package piece;

import core.PeerConnection;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitField extends Message {

    private final int length;
    private final byte id = 5;
    private final byte[] bitField;

    public BitField(byte[] bitField, PeerConnection peerConnection) {
        super(peerConnection);
        this.bitField = bitField;
        this.length = bitField.length + 1;
    }


    @Override
    public boolean parse(ByteBuffer buffer) {
        buffer.getInt();
        buffer.get();

        int pieces = peerConnection.getTorrentFile()
                .getInfo()
                .getPiecesStorage()
                .size();
        BitSet bitSet = new BitSet(pieces);
        int byteIndex = 0;

        while (buffer.hasRemaining()) {
            byte currentByte = buffer.get();

            for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                int pieceIndex = byteIndex * 8 + bitIndex;
                if (pieceIndex >= pieces) {
                    break;
                }
                if ((currentByte & (1 << (7 - bitIndex))) != 0) {
                    bitSet.set(pieceIndex);
                }
            }
            byteIndex++;
        }
        peerConnection.setBitField(bitSet);
        return true;

    }


    @Override
    public ByteBuffer create() {
        int length = peerConnection.getTorrentFile()
                .getInfo()
                .getPiecesStorage()
                .size();

        BitSet bitSet = new BitSet(length);
        ByteBuffer buffer = ByteBuffer.allocate(length + 5);
        buffer.putInt(length + 1);
        buffer.put(id);
        buffer.put(bitSet.toByteArray());
        return buffer;
    }
}
