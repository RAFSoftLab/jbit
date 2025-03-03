package core.network;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

public class UdpTrackerClient implements TrackerClient {

    private static final int CONNECT = 0;
    private static final int ANNOUNCE = 1;
    private static final long MAGIC_CONSTANT = 0x41727101980L;
    private static final int TIMEOUT_BASE = 15;
    private static final int TIMEOUT_MS = 120000;
    private static Instant lastAnnounce;

    private final DatagramSocket socket;
    private final Random random;


    public UdpTrackerClient() {
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1000);
            this.random = new Random();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create UDP socket");
        }
    }

    @Override
    public TrackerNetworkResponse announce(TrackerNetworkRequest request) {

        int attempts = 0;
        try {
            String url = request.getURL();
            String[] split = url.split(":");
            String host = split[1].substring(2);
            int port = Integer.parseInt(split[2].split("/")[0]);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);

            long connectionId = connect(host, port);
            int transactionId = random.nextInt();

            ByteBuffer announceBuffer = ByteBuffer.allocate(98);
            announceBuffer.order(ByteOrder.BIG_ENDIAN);

            announceBuffer.putLong(connectionId);
            announceBuffer.putInt(ANNOUNCE);
            announceBuffer.putInt(transactionId);
            announceBuffer.put(request.getInfoHashBytes());
            announceBuffer.put(request.peer_id.getBytes());
            announceBuffer.putLong(request.downloaded);
            announceBuffer.putLong(request.left);
            announceBuffer.putLong(request.uploaded);
            announceBuffer.putInt(2);
            announceBuffer.putInt(0);
            announceBuffer.putInt(new SecureRandom().nextInt());
            announceBuffer.putInt(-1);
            announceBuffer.putShort((short) request.port);

            while (++attempts <= 8) {
                try {
                    socket.send(new DatagramPacket(announceBuffer.array(), announceBuffer.array().length, address));
                    byte[] response = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(response, response.length);
                    socket.receive(responsePacket);
                    if (isInvalidResponse(response, transactionId, ANNOUNCE)) continue;

                    return TrackerNetworkResponse.of(response);

                } catch (SocketTimeoutException timeoutException) {
                    if (Instant.now()
                            .isAfter(lastAnnounce.plusMillis(TIMEOUT_MS))) {

                        announceBuffer.putLong(0, connect(host, port));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to connect to tracker via UDP");

    }

    private boolean isInvalidResponse(byte[] response, int transactionId, int action) {
        if (action == ANNOUNCE && response.length < 20) {
            return true;
        } else if (action == CONNECT && response.length < 16) {
            return true;
        }

        ByteBuffer responseBuffer = ByteBuffer.wrap(response);
        int actionResponse = responseBuffer.getInt();
        int transactionIdResponse = responseBuffer.getInt();
        return transactionId != transactionIdResponse || actionResponse != action;
    }

    private long connect(String ip, int port) {

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(ip), port);
            int transactionId = random.nextInt();
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(MAGIC_CONSTANT);
            buffer.putInt(CONNECT);
            buffer.putInt(transactionId);

            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, address);
            byte[] response = new byte[16];

            int attempts = 1;
            while (attempts <= 8) {
                try {
                    socket.setSoTimeout(TIMEOUT_BASE * (int) Math.pow(2, attempts) * 1000);
                    socket.send(packet);
                    socket.receive(new DatagramPacket(response, response.length));
                } catch (SocketTimeoutException timeoutException) {
                    attempts++;
                    continue;
                }

                ByteBuffer responseBuffer = ByteBuffer.wrap(response);
                if (isInvalidResponse(response, transactionId, CONNECT)) continue;
                lastAnnounce = Instant.now();
                return responseBuffer.getLong(8);
            }
            System.out.println("DOSO OVDE");
            throw new RuntimeException("Failed to connect to tracker via UDP");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to tracker via UDP");
        }
    }


    @Override
    public void scrape() {
        System.out.println("Scraping tracker via UDP");
    }
}
