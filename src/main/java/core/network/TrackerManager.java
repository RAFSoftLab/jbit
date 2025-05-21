package core.network;

import common.TorrentState;
import core.PeerConnection;
import core.bencode.TorrentFile;
import piece.TorrentManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrackerManager {

    private final TrackerClient http;
    private final TrackerClient udp;
    private final ScheduledExecutorService scheduler;
    private final TorrentManager torrentManager;
    private final Map<TorrentFile, TrackerNetworkRequest> requests = new ConcurrentHashMap<>();

    public TrackerManager(TorrentManager torrentManager) {
        this.http = new HttpTrackerClient();
        this.udp = new UdpTrackerClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.torrentManager = torrentManager;
        execute();
    }

    private void execute() {
        scheduler.schedule(() -> {
            try {

                Map<TorrentFile, List<PeerConnection>> torrentPeers = torrentManager.getTorrentPeers();

                for (TorrentFile torrentFile : torrentPeers.keySet()) {
                    if (torrentFile.getMetaData().getState() == TorrentState.DOWNLOADING) {
                        List<PeerConnection> peerConnections = torrentPeers.get(torrentFile);

                        if (peerConnections.size() < 3) {
                            Set<Peer> peers = getAllPeers(torrentFile);
                            torrentManager.establishConnections(torrentFile, new ArrayList<>(peers));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10, TimeUnit.SECONDS);
    }

    public Set<Peer> getAllPeers(TorrentFile torrentFile) {

        int size = torrentFile.getAnnounceList()
                .size();
        Set<Peer> peers = new HashSet<>();

        for (int i = 0; i <= size; i++) {
            TrackerNetworkResponse announceResponse = announce(torrentFile);
            if (announceResponse == null) {
                continue;
            }

            peers.addAll(announceResponse.getPeers());
            if (peers.size() >= 100) {
                return peers;
            }
        }

        System.out.println("PEERS GATHERED: " + peers.size());

        return peers;
    }


    public TrackerNetworkResponse announce(TorrentFile torrent) {

        TrackerNetworkRequest request = requests.get(torrent);

        if (request == null) {
            request = TrackerNetworkRequest.of(torrent);
            requests.put(torrent, request);
        }

        try {
            request.resolveNextUrl();
            TrackerNetworkResponse announce = announce(request);

            System.out.println("Doing for " + request.getUrl());
            return announce;

        } catch (Exception e) {
            return null;
        }

    }

    private TrackerNetworkResponse announce(TrackerNetworkRequest request) {
        return request.getUrl()
                .startsWith("http") ? http.announce(request) : udp.announce(request);
    }


}
