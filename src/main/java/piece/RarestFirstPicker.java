package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import storage.PieceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RarestFirstPicker implements PiecePicker {

    private final Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private final Map<TorrentFile, PieceStorage> torrentPieces;

    public RarestFirstPicker(Map<TorrentFile, List<PeerConnection>> torrentConnections) {
        this.torrentConnections = torrentConnections;
        this.torrentPieces = new ConcurrentHashMap<>();
    }

    @Override
    public PieceStorage find(TorrentFile torrentFile) {
        try {
            PieceStorage currentPiece = torrentPieces.get(torrentFile);
            List<PeerConnection> peers = new ArrayList<>(torrentConnections.get(torrentFile));
            List<PieceStorage> pieceStorages = torrentFile.getInfo()
                    .getPiecesStorage();

            int totalPieces = pieceStorages.size();
            int[] pieces = new int[totalPieces];

            for (PeerConnection peer : peers) {

                for (int i = peer.getBitField()
                        .nextSetBit(0); i >= 0; i = peer.getBitField()
                        .nextSetBit(i + 1)) {
                    pieces[i]++;
                }
            }

            if (currentPiece != null && pieces[currentPiece.getIndex()] > 0 && !currentPiece.isVerified() && !currentPiece.areAllBlockRequested()) {
                return currentPiece;
            }

            int rarest = -1;
            int rarestPieceIndex = -1;

            for (int i = 0; i < pieces.length; i++) {
                int pieceAmount = pieces[i];
                if (!pieceStorages.get(i)
                        .isVerified() && !pieceStorages.get(i)
                        .areAllBlockRequested() && pieceAmount > 0 && pieceAmount > rarest) {
                    rarest = pieceAmount;
                    rarestPieceIndex = i;
                }
            }


            if (rarestPieceIndex != -1) {
                currentPiece = pieceStorages.get(rarestPieceIndex);

                this.torrentPieces.put(torrentFile, currentPiece);
            }


            return currentPiece;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
