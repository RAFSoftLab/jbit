package core.network;

import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;

import java.io.InputStream;

public class TrackerNetworkResponse {

    private String failureReason;
    private String warningMessage;
    private long interval;
    private long minInterval;
    private String trackerId;
    private long complete;
    private long incomplete;
    private String[] peers;


    public static TrackerNetworkResponse of(String response) {
        return response.startsWith("http") || response.startsWith(
                "udp") ? new TrackerNetworkResponse() : new TrackerRedirectResponse(response);
    }


    public static TrackerNetworkResponse of(InputStream io) {
        try (Bencoder bencoder = new Bencoder(io)) {

            BencodeDictionary decode = bencoder.decode();

            TrackerNetworkResponse response = new TrackerNetworkResponse();

            response.complete = decode.get("complete", Long.class);
            response.incomplete = decode.get("incomplete", Long.class);
            response.interval = decode.get("interval", Long.class);
            response.minInterval = decode.get("min_interval", Long.class);
            response.peers = null;
            BencodeDictionary dict = (BencodeDictionary) decode.get("peers");
            System.out.println("ovde");
            System.out.println(dict);

        } catch (Exception e) {
            e.printStackTrace();
        }
        ;


        return null;
    }


}
