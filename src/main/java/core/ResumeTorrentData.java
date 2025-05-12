package core;

import common.TorrentState;
import core.bencode.TorrentFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResumeTorrentData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String torrentInfoHash;
    private final BitSet bitField;
    private final String path;
    private TorrentState state;

    public ResumeTorrentData(TorrentFile torrentFile) {
        this.torrentInfoHash = torrentFile.getInfoHash();
        this.bitField = new BitSet(torrentFile.getInfo()
                                           .getPiecesStorage()
                                           .size());
        this.path = "";
        this.state = TorrentState.START;
    }

    public Set<Integer> getDownloadedPieces() {
        return bitField.stream()
                .filter(bitField::get)
                .boxed()
                .collect(Collectors.toSet());
    }

    public TorrentState getState(){
        return this.state;
    }

    public void setState(TorrentState state){
        this.state = state;
    }

    public void setDownloadedPiece(int pieceIndex) {
        this.bitField.set(pieceIndex);
    }

    public BitSet getBitField() {
        return this.bitField;
    }

    @Override
    public int hashCode() {
        return torrentInfoHash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ResumeTorrentData other && this.torrentInfoHash.equals(other.torrentInfoHash);
    }
}
