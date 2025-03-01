package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import storage.PieceStorage;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RarestFirstPicker implements PiecePicker {

    private final Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private final Map<TorrentFile, PieceStorage> torrentPieces;
    private final Map<TorrentFile, List<Integer>> alreadyDownloadedPieces;

    public RarestFirstPicker(Map<TorrentFile, List<PeerConnection>> torrentConnections) {
        this.torrentConnections = torrentConnections;
        this.torrentPieces = new ConcurrentHashMap<>();
        this.alreadyDownloadedPieces = new ConcurrentHashMap<>();
    }

    public synchronized PieceStorage find(TorrentFile torrentFile) {
        try {
            PieceStorage piece = torrentPieces.get(torrentFile);
            List<Integer> downloadedPieces = alreadyDownloadedPieces.get(torrentFile);

            if (piece != null && piece.isVerified()) {
                downloadedPieces.add(piece.getIndex());
            }

            if (piece != null && !piece.isVerified()) {
                return piece;
            }

            List<PeerConnection> peers = torrentConnections.get(torrentFile);

            BitSet bitSet = peers.getFirst()
                    .getBitField();

            int size = bitSet.size();

            if (bitSet == null) {
                throw new RuntimeException("BitField should not be null");
            }

            int totalPieces = torrentFile.getInfo()
                    .getPiecesStorage()
                    .size();

            int[] pieces = new int[totalPieces];

            for (PeerConnection peer : peers) {

                for (int i = peer.getBitField()
                        .nextSetBit(0); i >= 0; i = peer.getBitField()
                        .nextSetBit(i + 1)) {
                    pieces[i]++;
                }
            }

            int rarest = Integer.MAX_VALUE;
            int rarestPieceIndex = 0;

            for (int i = 0; i < pieces.length; i++) {
                int pieceAmount = pieces[i];
                int finalI = i;
                if (pieceAmount > 0 && pieceAmount < rarest && downloadedPieces.stream().noneMatch(idx -> idx == finalI)) {
                    rarest = pieceAmount;
                    rarestPieceIndex = i;
                }
            }

            int finalRarestPieceIndex = rarestPieceIndex;
            torrentFile.getInfo()
                    .getPiecesStorage()
                    .stream()
                    .filter(pieceStorage -> pieceStorage.getIndex() == finalRarestPieceIndex)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Piece not found"));

            piece = new PieceStorage(rarestPieceIndex, (int) torrentFile.getInfo()
                    .getPieceLength(), torrentFile.getInfo()
                                             .getPieces()
                                             .getBytes());

            this.torrentPieces.put(torrentFile, piece);

            return piece;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void addPiece(int pieceIndex) {
        // pieceFrequencyMap.put(pieceIndex, pieceFrequencyMap.getOrDefault(pieceIndex, 0) + 1);
    }

    @Override
    public void removePiece(int pieceIndex) {
        //pieceFrequencyMap.remove(pieceIndex);
    }

    @Override
    public int getNextPiece() {
        return 0;
    }


}
