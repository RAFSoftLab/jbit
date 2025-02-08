package core.network;

import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class TrackerNetworkResponse {

    private String failureReason;
    private String warningMessage;
    private int interval;
    private long minInterval;
    private String trackerId;
    private int complete;
    private int incomplete;
    private List<Peer> peers = new ArrayList<>();


    public static TrackerNetworkResponse of(String response) {
        return response.startsWith("http") || response.startsWith(
                "udp") ? new TrackerNetworkResponse() : new TrackerRedirectResponse(response);
    }

    public static TrackerNetworkResponse of(byte[] responseBytes) {
        TrackerNetworkResponse response = new TrackerNetworkResponse();
        ByteBuffer buffer = ByteBuffer.wrap(responseBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        System.out.println( buffer.getInt() + " action") ;
        System.out.println(buffer.getInt() + " transactionId received");

        response.interval = buffer.getInt();
        response.complete = buffer.getInt();
        response.incomplete = buffer.getInt();

        while (buffer.remaining() >= 6) {
            int ipInt = buffer.getInt();
            String ip = String.format("%d.%d.%d.%d", (ipInt >> 24) & 0xFF, (ipInt >> 16) & 0xFF, (ipInt >> 8) & 0xFF,
                                      ipInt & 0xFF);

            int port = buffer.getShort() & 0xFFFF;
            response.peers.add(new Peer(ip, port));
        }

        System.out.println(response);

        return response;
    }

    public static TrackerNetworkResponse of(InputStream io) {
        TrackerNetworkResponse response = new TrackerNetworkResponse();

        try (Bencoder bencoder = new Bencoder(io)) {

            BencodeDictionary decode = bencoder.decode();

            response.complete = decode.get("complete", int.class);
            response.incomplete = decode.get("incomplete", int.class);
            response.interval = decode.get("interval", int.class);
            response.minInterval = decode.get("min_interval", int.class);
            response.peers = null;

            BencodeDictionary dict = (BencodeDictionary) decode.get("peers");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }


    public int getInterval() {
        return interval;
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public List<Peer> getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        return "TrackerNetworkResponse{" + "failureReason='" + failureReason + '\'' + ", warningMessage='" + warningMessage + '\'' + ", interval=" + interval + ", minInterval=" + minInterval + ", trackerId='" + trackerId + '\'' + ", complete=" + complete + ", incomplete=" + incomplete + ", peers=" + peers + '}';
    }
}
