package tasks;

import core.PeerConnection;
import exceptions.NoAvailableBlock;
import piece.*;
import storage.PieceStorage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DownloadScheduler {

    private static final int MAX_WRITE_TASKS = 30;

    private final PiecePicker picker;
    private final BlockingQueue<WriteEvent> events;

    private Thread thread;
    private volatile boolean running = false;

    public DownloadScheduler(PiecePicker picker) {
        this.picker = picker;
        this.events = new LinkedBlockingQueue<>();
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("DownloadScheduler is already running");
        }
        running = true;
        thread = new Thread(this::createTasks, "DownloadScheduler");
        thread.start();
    }

    private void createTasks() {

        while (running) {
            try {
                WriteEvent event = events.take();
                PeerConnection peerConnection = event.getPeerConnection();
                WriteEvent.EventType eventType = event.getEventType();

                switch (eventType) {
                    case HAVE_BITFIELD -> addInterestedTask(peerConnection);
                    case FREE_SPACE, UNCHOKE -> addRequestTask(peerConnection);
                    case REQUEST -> addUnimplementedTask();
                    case NOT_INTERESTING -> addUnimplementedTask();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addUnimplementedTask() {
    }


    public void addInterestedTask(PeerConnection peerConnection) {

        if (peerConnection.getAmInterested() == 0 && peerConnection.isInteresting()) {
            Message interested = new Interested(peerConnection);
            peerConnection.getTasks()
                    .addTask(new Task(interested.create()
                                              .array(), TaskType.WRITE, peerConnection));

            events.add(new WriteEvent(peerConnection, WriteEvent.EventType.FREE_SPACE));
        }
    }

    public void enqueueEvent(WriteEvent event) {
        events.add(event);
    }

    private void addRequestTask(PeerConnection peerConnection) {
        PieceStorage pieceStorage = picker.find(peerConnection.getTorrentFile());
        while (peerConnection.getTasks()
                .sizeOfWriteTasks() < MAX_WRITE_TASKS) {
            try {
                Message request = new Request(pieceStorage, peerConnection);
                peerConnection.getTasks()
                        .addTask(new Task(request.create()
                                                  .array(), TaskType.WRITE, peerConnection));
            } catch (NoAvailableBlock e) {
                pieceStorage = picker.find(peerConnection.getTorrentFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void listen(Message message) {
        if (message instanceof BitField || message instanceof Have) {
            events.add(new WriteEvent(message.getPeerConnection(), WriteEvent.EventType.HAVE_BITFIELD));
        } else if (message instanceof Unchoke) {
            events.add(new WriteEvent(message.getPeerConnection(), WriteEvent.EventType.UNCHOKE));
        } else if (message instanceof Request) {
            events.add(new WriteEvent(message.getPeerConnection(), WriteEvent.EventType.REQUEST));
        }
    }

    public void stop() {
        running = false;
        thread.interrupt();
    }
}
