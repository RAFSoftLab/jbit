package storage;

import piece.Block;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PieceStorage {

    private final int index;
    private final Block[] blocks;
    private final ByteBuffer dataBuffer;
    private final byte[] expectedHash;
    private boolean verified;
    private final AtomicInteger blocksCompleted = new AtomicInteger(0);
    private final AtomicBoolean requested = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);


    public PieceStorage(int index, int pieceLength, byte[] expectedHash) {
        this.index = index;
        this.expectedHash = expectedHash;
        this.dataBuffer = ByteBuffer.allocate(pieceLength);
        this.verified = false;

        int numberOfBlocks = (int) Math.ceil((double) pieceLength / 16384);
        this.blocks = new Block[numberOfBlocks];
        int offset = 0;

        for (int i = 0; i < numberOfBlocks; i++) {
            int blockLength = Math.min(16384, pieceLength - offset);
            blocks[i] = new Block(offset, blockLength, 0, index);
            offset += blockLength;
        }
    }

    public int getAmountOfDownloadedBytes(){
        return blocksCompleted.get() * 16384;
    }

    public synchronized boolean updateBlock(int offset, byte[] blockData) {

        int blockIndex = offset / 16384;
        Block block = blocks[blockIndex];

        if(block.getOffset() != offset){
            return false;
        }

        if(block.getDownloadState() == 1){
            return false;
        }

        block.setDownloadState(1);
        dataBuffer.position(offset);
        dataBuffer.put(blockData);
        int i = blocksCompleted.incrementAndGet();

        if(i == blocks.length){
            System.out.println("ALL BLOCKS DOWNLOADED");
            verify();
        }

        blocks[blockIndex] = block;

       return true;

    }

    public boolean isFinished(){
        return finished.get();
    }

    public boolean isRequested(){
        return requested.get();
    }

    public boolean areAllBlockRequested(){
        if(requested.get()) return true;

        for (Block block : blocks) {
            if (block.getDownloadState() == 0) {
                return false;
            }
        }
        requested.set(true);
        return true;
    }



    public synchronized Block getNextBlock() {

        for (Block block : blocks) {
            if (block.getDownloadState() == 0) {
                block.setDownloadState(2);
                block.setRequestTime(LocalDateTime.now());
                return block;
            }
        }

        return null;
    }

    public boolean verify() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(dataBuffer.array());
            if (MessageDigest.isEqual(hash, expectedHash)) {
                verified = true;
                finished.set(true);
                System.out.println("HASHES EQUAL");
                return true;
            } else {
                System.out.println("HASHES NOT EQUAL");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public void setVerified(boolean verified){
        this.verified = verified;
    }


    public synchronized void clearStale(){
        if (!finished.get() && isRequested()) {
            for (Block block : blocks) {
                if (block.getDownloadState() == 2) {
                    block.setDownloadState(0);
                    requested.set(false);
                }
            }
        }
    }

    public byte[] getData(){
        return dataBuffer.array();
    }

    public boolean isVerified() {
        return verified;
    }


    public int getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(index);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PieceStorage piece && piece.index == index;
    }
}
