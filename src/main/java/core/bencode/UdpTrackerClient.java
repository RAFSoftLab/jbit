package core.bencode;

import core.network.TrackerClient;
import core.network.TrackerNetworkRequest;
import core.network.TrackerNetworkResponse;

public class UdpTrackerClient implements TrackerClient {


    @Override
    public TrackerNetworkResponse connect(TrackerNetworkRequest request) {
        System.out.println("Connecting to tracker via UDP");






        return null;
    }

    @Override
    public void announce() {
        System.out.println("Announcing to tracker via UDP");
    }

    @Override
    public void scrape() {
        System.out.println("Scraping tracker via UDP");
    }

    @Override
    public void close() {
        System.out.println("Closing connection to tracker via UDP");
    }
}
