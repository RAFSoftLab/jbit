package core.network;

public interface TrackerClient {

    TrackerNetworkResponse connect(TrackerNetworkRequest request);

    void announce();

    void scrape();

    void close();
}
