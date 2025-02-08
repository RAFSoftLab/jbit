package core.network;

public interface TrackerClient {

    TrackerNetworkResponse announce(TrackerNetworkRequest request);

    void scrape();
}
