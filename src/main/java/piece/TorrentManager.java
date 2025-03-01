package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import core.network.Peer;
import tasks.DownloadScheduler;
import tasks.ReadTaskWorker;
import tasks.Task;
import tasks.TaskType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class TorrentManager {

    private Selector selector;
    private DownloadScheduler downloadScheduler;
    private Map<TorrentFile, List<Peer>> torrentPeers;
    private Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private PiecePicker piecePicker;

    public TorrentManager() {
        try {
            this.selector = Selector.open();
            this.torrentPeers = new ConcurrentHashMap<>();
            this.torrentConnections = new ConcurrentHashMap<>();
            this.piecePicker = new RarestFirstPicker(torrentConnections);
            this.downloadScheduler = new DownloadScheduler((RarestFirstPicker) piecePicker);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() {
        for (TorrentFile torrent : torrentPeers.keySet()) {
            HandshakeClient client = new HandshakeClient(selector, torrent, new Handshake(torrent));
            torrentConnections.put(torrent, client.handshake(torrentPeers.get(torrent)));
            downloadScheduler.addTorrentConnections(torrent, torrentConnections.get(torrent));
        }

        downloadScheduler.scheduleTask();
        executeSelector();
        readTasks();
    }


    public void executeSelector() {
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    while (true) {
                        try {
                            selector.select();
                            Iterator<SelectionKey> iterator = selector.selectedKeys()
                                    .iterator();

                            while (iterator.hasNext()) {
                                SelectionKey key = iterator.next();
                                iterator.remove();

                                if (key.isReadable()) {
                                    SocketChannel channel = (SocketChannel) key.channel();
                                    if (!(key.attachment() instanceof PeerConnection peerConnection)) {
                                        System.out.println("Key not valid");
                                        key.cancel();
                                        channel.close();
                                        continue;
                                    }

                                    System.out.println("Reading from channel");


                                    ByteBuffer buffer = peerConnection.getBuffer();

                                    int read = 0;
                                    try {
                                        read = channel.read(buffer);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        key.cancel();
                                        channel.close();
                                    }


                                    if (read == -1) {
                                        PeerConnection peer = (PeerConnection) key.attachment();
                                        System.out.println("Removing connection");
                                        torrentConnections.get(peer.getTorrentFile()).remove(peer);
                                        key.cancel();
                                        channel.close();
                                    }

                                    buffer.flip();
                                    System.out.println("BUFFER " + buffer.limit() + " Position " + buffer.position());

                                    while (buffer.remaining() >= 4) {
                                        buffer.mark();

                                        int messageLength = buffer.getInt();
                                        //byte id = buffer.get();
                                        //System.out.println("Message length: " + messageLength + " ID: " + id);

                                        if (buffer.remaining() < messageLength) {
                                            System.out.println(
                                                    "Message not complete" + buffer.remaining() + " " + messageLength);
                                            buffer.reset();
                                            break;
                                        }

                                        //read a whole message, make immutable message object and add it to the task queue
                                        byte[] messageBytes = new byte[messageLength + 4];
                                        buffer.reset();
                                        buffer.get(messageBytes);
                                        peerConnection.getTasks()
                                                .addTask(new Task(messageBytes, TaskType.READ, peerConnection));
                                    }

                                    buffer.compact();

                                } else if (key.isWritable()) {
                                    PeerConnection peerConnection = (PeerConnection) key.attachment();
                                    TasksContext tasks = peerConnection.getTasks();
                                    Task task = tasks.getTask(TaskType.WRITE);
                                    if (task != null) {
                                        System.out.println("Wrote task");
                                        ByteBuffer buffer = ByteBuffer.wrap(task.getMessage());
                                        while (buffer.hasRemaining()) {
                                            peerConnection.getChannel()
                                                    .write(buffer);
                                        }
                                    }
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public void addTorrentPeers(TorrentFile torrentFile, List<Peer> peers) {
        this.torrentPeers.put(torrentFile, peers);
    }

    public void readTasks() {
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    while (true) {
                        for (TorrentFile torrent : torrentConnections.keySet()) {
                            List<PeerConnection> peers = torrentConnections.get(torrent);
                            for (PeerConnection peerConnection : peers) {
                                TasksContext tasks = peerConnection.getTasks();
                                Task task = tasks.getTask(TaskType.READ);
                                if (task != null) {
                                    ReadTaskWorker readTaskWorker =
                                            new ReadTaskWorker(task, (RarestFirstPicker) piecePicker);
                                    Executors.newSingleThreadExecutor()
                                            .execute(readTaskWorker);
                                }
                            }
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                });
    }
}
