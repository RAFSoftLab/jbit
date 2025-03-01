package piece;

import core.bencode.TorrentFile;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Handshake {

    public static final int HANDSHAKE_SIZE = 68;
    private final TorrentFile torrentFile;

    public Handshake(TorrentFile torrent) {
        this.torrentFile = torrent;
    }



    public ByteBuffer create() {

//        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
//        buffer.put((byte) 19);
//        buffer.put("BitTorrent protocol".getBytes(StandardCharsets.UTF_8)); // pstr
//        buffer.put(new byte[8]);
//
//        byte[] infoHash = torrentFile.getInfoHashBytes();
//        if (infoHash.length != 20) throw new RuntimeException("Invalid infoHash length!");
//        buffer.put(infoHash);
//
//        byte[] peerId = "-JB0001-325123235742".getBytes(StandardCharsets.UTF_8);
//        if (peerId.length != 20) throw new RuntimeException("Invalid peerId length!");
//        buffer.put(peerId);
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
        buffer.put((byte) 19);
        buffer.put("BitTorrent protocol".getBytes(StandardCharsets.UTF_8)); // pstr
        buffer.put(new byte[8]);

        byte[] infoHash = torrentFile.getInfoHashBytes();
        if (infoHash.length != 20) throw new RuntimeException("Invalid infoHash length!");
        buffer.put(infoHash);

        byte[] peerId = "-JB0001-325123235742".getBytes(StandardCharsets.UTF_8);
        if (peerId.length != 20) throw new RuntimeException("Invalid peerId length!");
        buffer.put(peerId);
        return buffer;
    }


    public boolean parse(ByteBuffer buffer) {
        buffer.flip();
        if(buffer.remaining() < HANDSHAKE_SIZE) {
            return false;
        }

        buffer.get();
        byte[] pstrBytes = new byte[19];
        buffer.get(pstrBytes);
        String pstr = new String(pstrBytes, StandardCharsets.UTF_8);
        byte[] reserved = new byte[8];
        buffer.get(reserved);
        byte[] receivedInfoHash = new byte[20];
        buffer.get(receivedInfoHash);
        byte[] receivedPeerId = new byte[20];
        buffer.get(receivedPeerId);


        return Arrays.equals(torrentFile.getInfoHashBytes(), receivedInfoHash);


    }
}
