package storage;

import piece.Block;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
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


    public synchronized boolean updateBlock(int offset, byte[] blockData) {
        for (Block block : blocks) {
            if (block.getOffset() == offset && block.getDownloadState() == 2) {
                dataBuffer.position(offset);
                dataBuffer.put(blockData);
                block.setDownloadState(1);
                System.out.println("UPDATED BLOCK WITH OFFSET " + offset + " and state " + block.getDownloadState());
                return true;
            }
        }
        return false;
    }

    public boolean areAllBlocksDownloaded() {
        for (Block block : blocks) {
            if (block.getDownloadState() != 1) {
                if(block.getRequestTime().plusSeconds(3).isBefore(LocalDateTime.now())){
                    block.setDownloadState(0);
                }
                return false;
            }
        }
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

        if (areAllBlocksDownloaded()) {
            System.out.println("Should come here?");
            if(verify()){
              //downloadPiece

            };
        }
        return null;
    }

    public boolean verify() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(dataBuffer.array());
            if (MessageDigest.isEqual(hash, expectedHash)) {
                verified = true;
                System.out.println("HASHES EQUAL");
                return true;
            } else {
                System.out.println("HASHES NOT EQUAL");
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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
