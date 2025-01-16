package core.network;

import core.bencode.TorrentFile;
import core.bencode.UdpTrackerClient;

import java.util.List;
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

        List<String> announceList = torrent.getAnnounceList();
        announceList.addFirst(torrent.getAnnounce());

        for (String url : announceList) {
            try {
                request = new TrackerNetworkRequest.Builder().announceUrl(url)
                        .infoHash(torrent.getInfoHash())
                        .left(3385495)
                        .uploaded(0)
                        .downloaded(0)
                        .compact(1)
                        .build();

                TrackerNetworkResponse response = announce(request);
                requests.put(torrent, request);
                return response;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        throw new RuntimeException("Failed to connect to tracker");
    }

    private TrackerNetworkResponse announce(TrackerNetworkRequest request) {
        return request.getAnnounceUrl()
                .startsWith("http") ? http.connect(request) : udp.connect(request);
    }


}
