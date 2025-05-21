package piece;

import core.PeerConnection;
import core.SessionLoader;
import core.bencode.TorrentFile;
import core.network.Peer;
import storage.PieceStorage;
import tasks.*;
import util.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TorrentManager {

    private final Selector selector;
    private final Map<TorrentFile, List<Peer>> torrentPeers;
    private final Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private final Set<TorrentFile> managedTorrents;
    private final ReadTaskWorker readTaskWorker;
    private final DownloadScheduler downloadScheduler;
    private final FileUtils fileUtils;
    private final HandshakeClient handshakeClient;
    private final SessionLoader sessionLoader;

    public TorrentManager() {
        try {
            this.selector = Selector.open();
            this.torrentPeers = new ConcurrentHashMap<>();
            this.torrentConnections = new ConcurrentHashMap<>();
            this.downloadScheduler = new DownloadScheduler(new RarestFirstPicker(torrentConnections));
            this.readTaskWorker = new ReadTaskWorker(downloadScheduler);
            this.managedTorrents = new HashSet<>();
            this.fileUtils = new FileUtils(managedTorrents);
            this.handshakeClient = new HandshakeClient(selector);
            this.sessionLoader = new SessionLoader();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void init() {
        List<TorrentFile> torrentFiles = sessionLoader.onLoad();
        managedTorrents.addAll(torrentFiles);
        torrentFiles.forEach(t-> torrentConnections.put(t, new ArrayList<>()));
        executeSelector();
        readTaskWorker.start();
        downloadScheduler.start();
        fileUtils.init();

        readTasks();
    }

    public Map<TorrentFile, List<PeerConnection>> getTorrentPeers() {
        return this.torrentConnections;
    }


    public void establishConnections(TorrentFile torrentFile, List<Peer> peers){
        if(!managedTorrents.contains(torrentFile)){
            throw new IllegalArgumentException(String.format("Torrent with hash: %s not managed", torrentFile.getInfoHash()));
        }

        List<Peer> activePeers = torrentConnections.get(torrentFile)
                .stream()
                .map(PeerConnection::getPeer)
                .toList();
        peers.removeAll(activePeers);

        addTorrentConnections(torrentFile,handshakeClient.handshake(peers, torrentFile));
    }


    public void executeSelector() {
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    while (true) {
                        try {
                            selector.select();
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        Iterator<SelectionKey> iterator = selector.selectedKeys()
                                .iterator();


                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            iterator.remove();
                            try {

                                if (key.isReadable()) {
                                    SocketChannel channel = (SocketChannel) key.channel();
                                    if (!(key.attachment() instanceof PeerConnection peerConnection)) {
                                        System.out.println("Key not valid");
                                        key.cancel();
                                        channel.close();
                                        continue;
                                    }


                                    ByteBuffer buffer = peerConnection.getBuffer();

                                    int read = 0;
                                    try {
                                        read = channel.read(buffer);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        key.cancel();
                                        channel.close();
                                        torrentConnections.get(peerConnection.getTorrentFile())
                                                .remove(peerConnection);
                                    }


                                    if (read == -1) {
                                        PeerConnection peer = (PeerConnection) key.attachment();
                                        System.out.println("Removing connection");
                                        torrentConnections.get(peer.getTorrentFile())
                                                .remove(peer);
                                        key.cancel();
                                        channel.close();
                                    }

                                    buffer.flip();

                                    while (buffer.remaining() >= 4) {
                                        buffer.mark();

                                        int messageLength = buffer.getInt();
                                        //byte id = buffer.get();
                                        //System.out.println("Message length: " + messageLength + " ID: " + id);

                                        if (buffer.remaining() < messageLength) {
                                            buffer.reset();
                                            break;
                                        }

                                        //read a whole message, make immutable message object and add it to the task queue
                                        byte[] messageBytes = new byte[messageLength + 4];
                                        buffer.reset();
                                        buffer.get(messageBytes);
                                        Task task = new Task(messageBytes, TaskType.READ, peerConnection);
                                        peerConnection.getTasks()
                                                .addTask(task);
                                        readTaskWorker.addTask(task);

                                    }

                                    buffer.compact();

                                } else if (key.isWritable()) {

                                    PeerConnection peerConnection = (PeerConnection) key.attachment();
                                    TasksContext tasks = peerConnection.getTasks();
                                    Task task = tasks.getTask(TaskType.WRITE);
                                    if (task != null) {
                                        ByteBuffer buffer = ByteBuffer.wrap(task.getMessage());
                                        while (buffer.hasRemaining()) {
                                            peerConnection.getChannel()
                                                    .write(buffer);
                                        }
                                        downloadScheduler.enqueueEvent(
                                                new WriteEvent(peerConnection, WriteEvent.EventType.FREE_SPACE));
                                    }
                                }
                            } catch (IOException e) {
                                try {
                                    key.channel()
                                            .close();
                                } catch (Exception ignored) {}
                                key.cancel();
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    public void addTorrentPeers(TorrentFile torrentFile, List<Peer> peers) {
        this.torrentPeers.put(torrentFile, peers);
    }

    public void addTorrent(TorrentFile torrentFile){
        this.managedTorrents.add(torrentFile);
        this.torrentConnections.put(torrentFile, new ArrayList<>());
    }

    public List<PeerConnection> getTorrentConnections(TorrentFile torrentFile) {
        return torrentConnections.get(torrentFile);
    }

    public void addTorrentConnections(TorrentFile torrent, List<PeerConnection> connections){
        torrentConnections.get(torrent).addAll(connections);
    }
    public void readTasks() {

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try {
                        LocalDateTime now = LocalDateTime.now();
                        for (TorrentFile torrentFile : managedTorrents) {

                            List<PieceStorage> piecesStorage = torrentFile.getInfo()
                                    .getPiecesStorage();
                            piecesStorage.forEach(PieceStorage::clearStale);

                            List<PeerConnection> peerConnections = new ArrayList<>(torrentConnections.get(torrentFile));

                            for (PeerConnection peerConnection : peerConnections) {
                                if (now.minusSeconds(60)
                                        .isAfter(peerConnection.getLastPieceReceived())) {
                                    try {
                                        System.out.println("Removing IDLE connection");
                                        peerConnection.getPeerChannel()
                                                .close();
                                        torrentConnections.get(torrentFile)
                                                .remove(peerConnection);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error in read tasks");
                        e.printStackTrace();
                    }
                }, 0, 10, TimeUnit.SECONDS);


    }
}
