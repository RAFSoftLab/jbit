package tasks;

import core.PeerConnection;
import core.bencode.TorrentFile;
import exceptions.NoAvailableBlock;
import piece.Interested;
import piece.Message;
import piece.RarestFirstPicker;
import piece.Request;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DownloadScheduler {

    private static final int MAX_WRITE_TASKS = 30;
    private final Map<TorrentFile, List<PeerConnection>> torrentConnections;
    private final RarestFirstPicker picker;

    public DownloadScheduler(RarestFirstPicker picker) {
        this.torrentConnections = new ConcurrentHashMap<>();
        this.picker = picker;
    }

    public void addTorrentConnections(TorrentFile torrentFile, List<PeerConnection> peerConnections) {
        torrentConnections.put(torrentFile,peerConnections);
    }


    public void scheduleTask() {

        Runnable runnable = () -> {
            try {
                for (TorrentFile torrent : torrentConnections.keySet()) {
                    List<PeerConnection> peers = torrentConnections.get(torrent);


                    for (PeerConnection peerConnection : peers) {

                        if (peerConnection.getPeerChoking() == 0 && peerConnection.getAmInterested() == 1) {
                            try {
                                Request request = new Request(picker, peerConnection);
                                peerConnection.getTasks()
                                        .addTask(new Task(request.create()
                                                                  .array(), TaskType.WRITE, peerConnection));
                            }catch (NoAvailableBlock e){
                                continue;
                            }
                        }

                        if (peerConnection.getAmInterested() == 0) {
                            Message interested = new Interested(peerConnection);
                            peerConnection.getTasks()
                                    .addTask(new Task(interested.create()
                                                              .array(), TaskType.WRITE, peerConnection));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Executors.newScheduledThreadPool(10)
                .scheduleAtFixedRate(runnable, 0, 500, TimeUnit.MILLISECONDS);

    }


}
