package core.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class HttpTrackerClient implements TrackerClient {

    private final HttpClient client;

    public HttpTrackerClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }


    @Override
    public TrackerNetworkResponse connect(TrackerNetworkRequest request) {
        try {
            List<String> s = request.getAnnounceList()
                    .stream()
                    .filter(x -> x.startsWith("http://tracker.opentrackr.org:1337"))
                    .toList();
            System.out.println(URI.create(request.getURL(s.getFirst()))
                                       .toString());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(request.getURL(s.getFirst())))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .GET()
                    .build();

            HttpResponse<InputStream> send = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            return TrackerNetworkResponse.of(new BufferedInputStream(send.body()));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to connect to tracker");
    }

    private boolean sendAnnounceRequest(String url, TrackerNetworkRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(request.getURL(url)))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Tracker Response: " + response.body());
            return true;  // Successfully contacted tracker
        } catch (Exception e) {
            System.err.println("Failed to connect to tracker: " + url);
            e.printStackTrace();
            return false;  // Tracker request failed
        }
    }

    @Override
    public void announce() {

    }

    @Override
    public void scrape() {
        System.out.println("Scraping tracker via HTTP");
    }

    @Override
    public void close() {
        System.out.println("Closing connection to tracker via HTTP");
    }


}
