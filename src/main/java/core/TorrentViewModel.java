package core;

import core.bencode.Bencoder;
import core.bencode.TorrentFile;
import core.network.TrackerManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import piece.TorrentManager;
import ui.TorrentEntry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

public class TorrentViewModel {

    private final TorrentManager torrentManager;
    private final TrackerManager trackerManager;
    private final ObservableList<TorrentEntry> torrents = FXCollections.observableArrayList();

    public TorrentViewModel() {
        this.torrentManager = TorrentManager.getInstance();
        this.torrentManager.getManagedTorrents()
                .stream()
                .map(TorrentFile::getTorrentEntry)
                .forEach(torrents::add);
        this.trackerManager = new TrackerManager(torrentManager);
       // torrentManager.registerViewModel(this);
    }

    public ObservableList<TorrentEntry> getTorrents() {
        return torrents;
    }

    public void addTorrent(File file) {
        try (Bencoder bencoder = new Bencoder(new BufferedInputStream(new FileInputStream(file)))) {
            TorrentFile torrent = new TorrentFile(bencoder.decode());
            torrentManager.addTorrent(torrent);
            torrents.addFirst(torrent.getTorrentEntry());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getAmountOfPeersByTorrent(TorrentFile torrent){
        return torrentManager.getAmountOfPeersByTorrent(torrent);
    }

    public void removeTorrent(TorrentEntry torrentEntry) {
        torrents.remove(torrentEntry);  
        torrentManager.removeTorrent(torrentEntry.getTorrent());

        //implement torrent removal --clear
    }


    public void addAll(Collection<? extends TorrentFile> torrentFiles) {
        for (TorrentFile torrentFile : torrentFiles) {
            torrentManager.addTorrent(torrentFile);
            torrents.addFirst(torrentFile.getTorrentEntry());
        }
    }

    public void deleteFromDisk(TorrentEntry torrent) {
        torrentManager.deleteFromDisk(torrent.getTorrent());
    }
}
