package core;

import core.bencode.TorrentFile;
import core.network.Peer;
import piece.TasksContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerConnection {

    private static final int HANDSHAKE_SIZE = 68;
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    private static final int PIECE_SIZE = 16 * 1024;// 16 KB

    private final Peer peer;
    private final TasksContext tasks;
    private final TorrentFile torrentFile;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(PIECE_SIZE * 2);

    private BitSet bitField;

    private int amChoking = 1;
    private int amInterested = 0;
    private int peerChoking = 1;
    private int peerInterested = 0;

    public PeerConnection(Peer peer, TorrentFile torrentFile, SocketChannel channel) {
        this.peer = peer;
        this.torrentFile = torrentFile;
        this.channel = channel;
        this.tasks = new TasksContext(new ConcurrentLinkedQueue<>(), new ArrayBlockingQueue<>(50));
        this.bitField = new BitSet(torrentFile.getInfo().getPiecesStorage().size());
    }

    public ByteBuffer getBuffer() {
        return readBuffer;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public TorrentFile getTorrentFile() {
        return torrentFile;
    }

    public TasksContext getTasks() {
        return tasks;
    }

    public BitSet getBitField() {
        return this.bitField;
    }

    public void setBitField(BitSet bitField) {
        this.bitField = bitField;
    }

    public boolean havePiece(int pieceIndex) {
        if (bitField == null) return false;
        return bitField.get(pieceIndex);
    }


    public void setAmChoking(int amChoking) {
        this.amChoking = amChoking;
    }

    public void setPeerInterested(int peerInterested) {
        this.peerInterested = peerInterested;
    }

    public int getPeerChoking() {
        return this.peerChoking;
    }

    public void setPeerChoking(int peerChoking) {
        this.peerChoking = peerChoking;
    }

    public int getAmInterested() {
        return this.amInterested;
    }

    public void setAmInterested(int amInterested) {
        this.amInterested = amInterested;
    }

    public void sendHandshake(TorrentFile torrentFile, List<Peer> peers) throws IOException {
        Selector selector = Selector.open();

        Map<SocketChannel, Peer> peerMap = new HashMap<>();

        for (Peer peer : peers) {
            try {
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(peer.getAddress(), peer.getPort()));

                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                peerMap.put(socketChannel, peer);

                System.out.println("Attempting to connect to: " + peer.getAddress() + ":" + peer.getPort());
            } catch (IOException e) {
                System.out.println("Failed to initiate connection to: " + peer.getAddress() + ":" + peer.getPort());
            }
        }

        long startTime = System.currentTimeMillis();

        while (!peerMap.isEmpty() && (System.currentTimeMillis() - startTime) < CONNECTION_TIMEOUT_MS) {
            if (selector.select(1000) == 0) continue;

            Iterator<SelectionKey> keyIterator = selector.selectedKeys()
                    .iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isConnectable()) {
                    handleConnect(key, peerMap, torrentFile);
                } else if (key.isReadable()) {
                    handleRead(key, peerMap, torrentFile);
                }
            }
        }

        for (SocketChannel channel : peerMap.keySet()) {
            System.out.println("Closing connection to: " + peerMap.get(channel)
                    .getAddress());
            channel.close();
        }

        selector.close();
    }

    private void handleConnect(SelectionKey key, Map<SocketChannel, Peer> peerMap, TorrentFile torrentFile) {
        SocketChannel channel = (SocketChannel) key.channel();
        Peer peer = peerMap.get(channel);
        try {
            if (channel.finishConnect()) {
                System.out.println("Connected to: " + peer.getAddress() + ":" + peer.getPort());
                key.interestOps(SelectionKey.OP_WRITE);
                sendHandshakeMessage(channel, torrentFile);
                key.interestOps(SelectionKey.OP_READ);
            } else {
                System.out.println("Failed to connect to: " + peer.getAddress());
                peerMap.remove(channel);
                key.cancel();
                channel.close();
            }
        } catch (IOException e) {
            System.out.println("Error during connect: " + peer.getAddress());
            peerMap.remove(channel);
            key.cancel();
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sendHandshakeMessage(SocketChannel channel, TorrentFile torrentFile) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(HANDSHAKE_SIZE);
        buffer.put((byte) 19);
        buffer.put("BitTorrent protocol".getBytes(StandardCharsets.UTF_8)); // pstr
        buffer.put(new byte[8]);

        byte[] infoHash = torrentFile.getInfoHashBytes();
        if (infoHash.length != 20) throw new IOException("Invalid infoHash length!");
        buffer.put(infoHash);

        byte[] peerId = "-JB0001-325123235742".getBytes(StandardCharsets.UTF_8);
        if (peerId.length != 20) throw new IOException("Invalid peerId length!");
        buffer.put(peerId);

        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        System.out.println("Handshake sent to peer.");
    }

    private void handleRead(SelectionKey key, Map<SocketChannel, Peer> peerMap, TorrentFile torrentFile) {
        SocketChannel channel = (SocketChannel) key.channel();
        Peer peer = peerMap.get(channel);
        ByteBuffer responseBuffer = ByteBuffer.allocate(HANDSHAKE_SIZE);

        try {
            int bytesRead = channel.read(responseBuffer);
            if (bytesRead == -1) {
                System.out.println("Peer closed connection: " + peer.getAddress());
                peerMap.remove(channel);
                key.cancel();
                channel.close();
                return;
            }

            responseBuffer.flip();
            if (responseBuffer.remaining() < HANDSHAKE_SIZE) {
                System.out.println("Incomplete handshake from peer: " + peer.getAddress());
                return;
            }

            responseBuffer.get();
            byte[] pstrBytes = new byte[19];
            responseBuffer.get(pstrBytes);
            String pstr = new String(pstrBytes, StandardCharsets.UTF_8);
            byte[] reserved = new byte[8];
            responseBuffer.get(reserved);
            byte[] receivedInfoHash = new byte[20];
            responseBuffer.get(receivedInfoHash);
            byte[] receivedPeerId = new byte[20];
            responseBuffer.get(receivedPeerId);

            System.out.println("Received handshake from: " + peer.getAddress());
            System.out.println("InfoHash matches: " + Arrays.equals(torrentFile.getInfoHashBytes(), receivedInfoHash));
            System.out.println("Received Peer ID: " + new String(receivedPeerId, StandardCharsets.UTF_8));

            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);

        } catch (IOException e) {
            System.out.println("Read error from peer: " + peer.getAddress());
            peerMap.remove(channel);
            key.cancel();
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void setPieceIndex(int pieceIndex) {
        bitField.set(pieceIndex);
    }
}
