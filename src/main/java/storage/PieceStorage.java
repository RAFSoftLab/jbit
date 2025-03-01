package storage;

import piece.Block;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class PieceStorage {

    private final int index;
    private final List<Block> blocks;
    private final ByteBuffer dataBuffer;
    private final byte[] expectedHash;
    private boolean verified;


    public PieceStorage(int index, int pieceLength, byte[] expectedHash) {
        this.index = index;
        this.expectedHash = expectedHash;
        this.dataBuffer = ByteBuffer.allocate(pieceLength);
        this.verified = false;
        int offset = 0;
        this.blocks = new ArrayList<>();
        while (offset < pieceLength) {
            int blockLength = Math.min(16384, pieceLength - offset);
            blocks.add(new Block(offset, blockLength, 0, index));
            offset += blockLength;
        }
    }


    public synchronized void updateBlock(int offset, byte[] blockData) {
        for (Block block : blocks) {
            if (block.getOffset() == offset && block.getDownloadState() == 0) {
                dataBuffer.position(offset);
                dataBuffer.put(blockData);
                block.setDownloadState(1);
                break;
            }
        }
    }

    public boolean areAllBlocksDownloaded() {
        for (Block block : blocks) {
            if (block.getDownloadState() != 1) {
                return false;
            }
        }
        return true;
    }

    public boolean isNextBlockAvailable(){
        for (Block block : blocks) {
            if (block.getDownloadState() == 0) {
                return true;
            }
        }
        return false;
    }

    public synchronized Block getNextBlock() {

        for (Block block : blocks) {
            if (block.getDownloadState() == 0) {
                block.setDownloadState(2);
                return block;
            }
        }
        if(areAllBlocksDownloaded()){
            verify();
        }
        return null;
    }

    public void verify(){
        byte[] data = dataBuffer.array();
        byte[] hash = new byte[20];

        System.arraycopy(data, 0, hash, 0, 20);
        if (MessageDigest.isEqual(hash, expectedHash)) {
            verified = true;
        }
    }

    public boolean isVerified(){
        return verified;
    }

    public synchronized boolean isComplete() {
        for (Block block : blocks) {
            if (block.getDownloadState() != 1) {
                return false;
            }
        }
        return true;
    }

    public int getIndex() {
        return index;
    }

}
