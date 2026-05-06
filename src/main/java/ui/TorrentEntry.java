package ui;

import core.bencode.TorrentFile;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TorrentEntry {

    private final TorrentFile torrent;

    private final StringProperty name;
    private final StringProperty size;
    private final DoubleProperty progress;
    private final StringProperty downSpeed;
    private final StringProperty upSpeed;
    private final StringProperty eta;
    private final StringProperty peers;
    private final StringProperty state;

    public TorrentEntry(TorrentFile torrent) {
        this.torrent = torrent;
        this.name = new SimpleStringProperty(torrent.getInfo().getName());
        this.size = new SimpleStringProperty(torrent.getInfo().getTotalSize());
        this.progress = new SimpleDoubleProperty(0);
        this.downSpeed = new SimpleStringProperty("0");
        this.upSpeed = new SimpleStringProperty("3");
        this.eta = new SimpleStringProperty("1 day");
        this.peers = new SimpleStringProperty("24");
        this.state = new SimpleStringProperty(torrent.getMetaData().getState().toString());
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty downSpeedProperty() {
        return downSpeed;
    }

    public StringProperty upSpeedProperty() {
        return upSpeed;
    }

    public StringProperty etaProperty() {
        return eta;
    }

    public StringProperty peersProperty() {
        return peers;
    }

    public StringProperty stateProperty() {
        return state;
    }

    public TorrentFile getTorrent() {
        return torrent;
    }


    @Override
    public int hashCode() {
        return torrent.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TorrentEntry entry && this.torrent.equals(entry.torrent);
    }
}
