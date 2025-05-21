import core.PeerConnection;
import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;
import core.bencode.TorrentFile;
import core.network.Peer;
import core.network.TrackerManager;
import piece.TorrentManager;
import storage.PieceStorage;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        try (Bencoder bencoder = new Bencoder(
                new BufferedInputStream(new FileInputStream("src/main/resources/torrentFiles/test6.torrent")))) {

            BencodeDictionary dictionary = bencoder.decode();

            TorrentFile torrentFile = new TorrentFile(dictionary);
            //TrackerNetworkResponse announce = manager.announce(torrentFile);
            TorrentManager torrentManager = new TorrentManager();
            //orrentManager.addTorrent(torrentFile);
            TrackerManager manager = new TrackerManager(torrentManager);

            torrentManager.init();




//            Runnable runnable = () -> {
//                try {
//                    if (torrentFile.isCompleted()){
//                        return;
//                    }
//
//                    List<PeerConnection> torrentConnections = torrentManager.getTorrentConnections(torrentFile);
//                    List<PeerConnection> activeConnections = torrentConnections.stream().filter( conn -> conn.getPeerChoking() == 0).toList();
//                    System.out.println("ACTIVE CONNECTIONS: " + activeConnections.size());
//                    System.out.println("LEFT: " + (torrentFile.getInfo().getPiecesStorage().size() - torrentFile.getDownloadedPieces().size()));
//                    List<Integer> leftPieces = new ArrayList<>();
//                    for (PieceStorage piece : torrentFile.getInfo().getPiecesStorage()){
//                        if (!torrentFile.getDownloadedPieces().contains(piece.getIndex())){
//                            leftPieces.add(piece.getIndex());
//                        }
//                    }
//
//                    System.out.println("LEFT PIECES: " + leftPieces.size());
//
//                    List<PieceStorage> piecesStorage = torrentFile.getInfo()
//                            .getPiecesStorage();
//                    int totalDownloaded = 0;
//
//                    for(PieceStorage piece : piecesStorage){
//                        totalDownloaded+= piece.getAmountOfDownloadedBytes();
//                    }
//                    System.out.println("TOTAL DOWNLOADED: " + totalDownloaded/1024/1024 + " MB");
//
//
//
//                    List<Integer> idx = new ArrayList<>();
//                    for (PieceStorage piece : piecesStorage){
//                        if (!piece.isRequested()){
//                            idx.add(piece.getIndex());
//                        }
//                    }
//                    System.out.println("NOT REQUESTED: " + idx.size());
//                    idx.forEach( i -> System.out.print(i + " "));
//
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            };
//
//            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
//
//            scheduledExecutorService.scheduleAtFixedRate(runnable, 60, 10,
//                                              TimeUnit.SECONDS);



//            Set<Peer> peers = torrentFile.isCompleted() ?  new HashSet<>() : manager.getAllPeers(torrentFile);
//            System.out.println("Adding peers: " + peers.size());
//            torrentManager.addTorrentPeers(torrentFile, new ArrayList<>(peers));
//            torrentManager.establishConnections(torrentFile, new ArrayList<>(peers));
//            torrentManager.init();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}
