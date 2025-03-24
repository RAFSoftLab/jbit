package core.network;

import core.bencode.TorrentFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager {

    private final TrackerClient http;
    private final TrackerClient udp;

    private final Map<TorrentFile, TrackerNetworkRequest> requests = new ConcurrentHashMap<>();

    public TrackerManager() {
        this.http = new HttpTrackerClient();
        this.udp = new UdpTrackerClient();
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
