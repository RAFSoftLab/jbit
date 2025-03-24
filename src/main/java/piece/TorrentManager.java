package piece;

import core.PeerConnection;
import core.bencode.TorrentFile;
import core.network.Peer;
import storage.PieceStorage;
import tasks.DownloadScheduler;
import tasks.ReadTaskWorker;
import tasks.Task;
import tasks.TaskType;

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

    private Selector selector;
    private DownloadScheduler downloadScheduler;
    private Map<TorrentFile, List<Peer>> torrentPeers;
    private Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private PiecePicker piecePicker;
    private ReadTaskWorker readTaskWorker;

    public TorrentManager() {
        try {
            this.selector = Selector.open();
            this.torrentPeers = new ConcurrentHashMap<>();
            this.torrentConnections = new ConcurrentHashMap<>();
            this.piecePicker = new RarestFirstPicker(torrentConnections);
            this.downloadScheduler = new DownloadScheduler((RarestFirstPicker) piecePicker);
            this.readTaskWorker = new ReadTaskWorker((RarestFirstPicker) piecePicker);
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
                                    }
                                }
                            } catch (IOException e) {
                                try{key.channel().close();} catch (Exception ignored){}
                                key.cancel();
                                e.printStackTrace();
                            }
                        }


                    }
                });
    }

    public void handshake(TorrentFile torrentFile, List<Peer> peers) {
        HandshakeClient client = new HandshakeClient(selector, torrentFile, new Handshake(torrentFile));
        List<Peer> activePeers = torrentConnections.get(torrentFile)
                .stream()
                .map(PeerConnection::getPeer)
                .toList();
        peers.removeAll(activePeers);
        List<PeerConnection> handshake = client.handshake(peers);
        System.out.println("ADDING TOTAL OF " + handshake.size() + " PEERS");
        torrentConnections.get(torrentFile)
                .addAll(handshake);
    }

    public void addTorrentPeers(TorrentFile torrentFile, List<Peer> peers) {
        this.torrentPeers.put(torrentFile, peers);
    }

    public List<PeerConnection> getTorrentConnections(TorrentFile torrentFile) {
        return torrentConnections.get(torrentFile);
    }

    public void readTasks() {

      readTaskWorker.start();

      Executors.newSingleThreadScheduledExecutor()
              .scheduleAtFixedRate(()->{
                  try {
                      LocalDateTime now = LocalDateTime.now();
                      for (TorrentFile torrentFile : torrentPeers.keySet()) {

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
                  }catch (Exception e){
                      System.out.println("Error in read tasks");
                      e.printStackTrace();
                  }
                  }, 0, 10, TimeUnit.SECONDS);



    }
}
