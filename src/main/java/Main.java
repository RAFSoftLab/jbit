import core.PeerConnection;
import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;
import core.bencode.TorrentFile;
import core.network.TrackerManager;
import core.network.TrackerNetworkResponse;
import piece.TorrentManager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
            Runnable runnable = () -> {
                try {
                    List<PeerConnection> torrentConnections = torrentManager.getTorrentConnections(torrentFile);
                    List<PeerConnection> activeConnections = torrentConnections.stream().filter( conn -> conn.getPeerChoking() == 1).toList();
                    System.out.println("ACTIVE CONNECTIONS: " + activeConnections.size());
                    if(activeConnections.size() < 5){
                        TrackerNetworkResponse announce1 = manager.announce(torrentFile);
                        torrentManager.handshake(torrentFile, announce1.getPeers());
                        System.out.println("ADDING NEW PEERS");
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            };

            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

            scheduledExecutorService.scheduleAtFixedRate(runnable, 10, 10,
                                              TimeUnit.SECONDS);

            torrentManager.addTorrentPeers(torrentFile, announce.getPeers());
            torrentManager.init();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
