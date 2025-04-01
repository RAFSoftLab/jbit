package core;

import core.bencode.TorrentFile;
import core.network.Peer;
import piece.TasksContext;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerConnection {

    private static final int PIECE_SIZE = 16 * 1024;// 16 KB

    private final Peer peer;
    private final TasksContext tasks;
    private final TorrentFile torrentFile;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(PIECE_SIZE * 2);
    private LocalDateTime lastPieceReceived;
    private LocalDateTime lastPieceSent;

    private BitSet bitField;

    private int amChoking = 1;
    private int amInterested = 0;
    private int peerChoking = 1;
    private int peerInterested = 0;

    public PeerConnection(Peer peer, TorrentFile torrentFile, SocketChannel channel) {
        this.peer = peer;
        this.torrentFile = torrentFile;
        this.channel = channel;
        this.tasks = new TasksContext(new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
        this.bitField = new BitSet(torrentFile.getInfo()
                                           .getPiecesStorage()
                                           .size());
        this.lastPieceReceived = LocalDateTime.now();
    }

    public ByteBuffer getBuffer() {
        return readBuffer;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public TorrentFile getTorrentFile() {
        return torrentFile;
    }

    public TasksContext getTasks() {
        return tasks;
    }

    public BitSet getBitField() {
        return this.bitField;
    }

    public void setBitField(BitSet bitField) {
        this.bitField = bitField;
    }

    public boolean havePiece(int pieceIndex) {
        return bitField.get(pieceIndex);
    }


    public void updateLastPieceReceived() {
        lastPieceReceived = LocalDateTime.now();
    }

    public void updateLastPieceSent() {
        lastPieceSent = LocalDateTime.now();
    }

    public LocalDateTime getLastPieceReceived() {
        return lastPieceReceived;
    }


    public SocketChannel getPeerChannel() {
        return this.channel;
    }

    public LocalDateTime getLastPieceSent() {
        return lastPieceSent;
    }

    public Peer getPeer() {
        return peer;
    }


    public void setAmChoking(int amChoking) {
        this.amChoking = amChoking;
    }

    public void setPeerInterested(int peerInterested) {
        this.peerInterested = peerInterested;
    }

    public int getPeerChoking() {
        return this.peerChoking;
    }

    public void setPeerChoking(int peerChoking) {
        this.peerChoking = peerChoking;
    }

    public int getAmInterested() {
        return this.amInterested;
    }

    public void setAmInterested(int amInterested) {
        this.amInterested = amInterested;
    }

    public boolean isInteresting(){
        return torrentFile.getInfo().getDownloadedPieces().stream()
                .anyMatch(pieceIndex -> bitField.get(pieceIndex));
    }

    public void setPieceIndex(int pieceIndex) {
        bitField.set(pieceIndex);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PeerConnection pc && pc.peer.equals(this.peer) && pc.torrentFile.equals(this.torrentFile);
    }
}
