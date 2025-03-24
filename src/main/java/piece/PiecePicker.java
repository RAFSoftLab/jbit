package piece;

import core.bencode.TorrentFile;
import storage.PieceStorage;

public interface PiecePicker {

    PieceStorage find(TorrentFile torrentFile);

}
