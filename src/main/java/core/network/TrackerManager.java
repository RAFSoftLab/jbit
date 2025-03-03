package core.network;

import core.bencode.TorrentFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager {

    private final TrackerClient http;
    private final TrackerClient udp;

    private final Map<TorrentFile, TrackerNetworkRequest> requests = new ConcurrentHashMap<>();

    public TrackerManager() {
        this.http = new HttpTrackerClient();
        this.udp = new UdpTrackerClient();
    }


    public TrackerNetworkResponse announce(TorrentFile torrent) {

        TrackerNetworkRequest request = requests.get(torrent);

        if (request != null) {
            return announce(request);
        }

        request = TrackerNetworkRequest.of(torrent);
        request.resolveNextUrl();

        try {
            TrackerNetworkResponse response = announce(request);
            requests.put(torrent, request);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            request.resolveNextUrl();
            announce(torrent);

        }


        throw new RuntimeException("Failed to connect to tracker");
    }

    private TrackerNetworkResponse announce(TrackerNetworkRequest request) {
        return request.getUrl()
                .startsWith("http") ? http.announce(request) : udp.announce(request);
    }


}
