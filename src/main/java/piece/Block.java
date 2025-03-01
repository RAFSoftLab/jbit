package piece;

public class Block {

    int offset;
    int length;
    int downloadState; // 0: not downloaded, 1: downloaded, 2: requested
    int pieceIndex;


    public Block(int offset, int length, int downloadState, int pieceIndex) {
        this.offset = offset;
        this.length = length;
        this.downloadState = downloadState;
        this.pieceIndex = pieceIndex;
    }


    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int getDownloadState() {
        return downloadState;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }


    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setDownloadState(int downloadState) {
        this.downloadState = downloadState;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }
}
