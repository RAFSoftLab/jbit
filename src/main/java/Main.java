import core.PeerConnection;
import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;
import core.bencode.TorrentFile;
import core.network.HttpTrackerClient;
import core.network.TrackerManager;
import core.network.TrackerNetworkRequest;
import core.network.TrackerNetworkResponse;
import piece.Handshake;
import piece.HandshakeClient;
import piece.TorrentManager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        try (Bencoder bencoder = new Bencoder(
                new BufferedInputStream(new FileInputStream("src/main/resources/torrentFiles/test5.torrent")))) {

            BencodeDictionary dictionary = bencoder.decode();

            //byte[] encoded = dictionary.encode();

            //Files.write(Paths.get("src/main/resources/torrentFiles/test4.torrent"), encoded);

            //Bencoder bencoder1 = new Bencoder(
             //       new BufferedInputStream(new FileInputStream("src/main/resources/torrentFiles/test3.torrent")));

            //BencodeDictionary decode = bencoder1.decode();

                    //HttpTrackerClient client = new HttpTrackerClient();

            //client.connect(TrackerNetworkRequest.of(new TorrentFile(dictionary)));
            TorrentFile torrentFile = new TorrentFile(dictionary);
            TrackerManager manager = new TrackerManager();
            TrackerNetworkResponse announce = manager.announce(torrentFile);

            TorrentManager torrentManager = new TorrentManager();
            torrentManager.addTorrentPeers(torrentFile, announce.getPeers());
            torrentManager.init();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
