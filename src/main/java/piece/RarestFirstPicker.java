package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import storage.PieceStorage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RarestFirstPicker implements PiecePicker {

    private final Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private final Map<TorrentFile, PieceStorage> torrentPieces;

    public RarestFirstPicker(Map<TorrentFile, List<PeerConnection>> torrentConnections) {
        this.torrentConnections = torrentConnections;
        this.torrentPieces = new ConcurrentHashMap<>();
    }

    public synchronized PieceStorage find(TorrentFile torrentFile) {
        try {
            PieceStorage currentPiece = torrentPieces.get(torrentFile);
            List<PeerConnection> peers = torrentConnections.get(torrentFile);
            Set<Integer> downloadedPieces = torrentFile.getInfo()
                    .getDownloadedPieces();

            if (currentPiece != null && currentPiece.isVerified()) {
                downloadedPieces.add(currentPiece.getIndex());
            }

            if (currentPiece != null && !currentPiece.isVerified()) {
                return currentPiece;
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
                if (pieceAmount > 0 && pieceAmount < rarest && !downloadedPieces.contains(i)) {
                    rarest = pieceAmount;
                    rarestPieceIndex = i;
                }
            }

            currentPiece = torrentFile.getInfo()
                    .getPiecesStorage()
                    .get(rarestPieceIndex);

            this.torrentPieces.put(torrentFile, currentPiece);

            return currentPiece;

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
