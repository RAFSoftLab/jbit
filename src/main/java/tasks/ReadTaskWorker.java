package tasks;

import core.PeerConnection;
import piece.*;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ReadTaskWorker {

    private final BlockingQueue<Task> tasks;
    private final ExecutorService executorService;
    private final DownloadScheduler downloadScheduler;

    private Thread consumerThread;
    private volatile boolean running = false;

    public ReadTaskWorker(DownloadScheduler downloadScheduler) {
        this.tasks = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.downloadScheduler = downloadScheduler;
    }


    public synchronized void start() {
        if (running) {
            throw new IllegalStateException("ReadTaskWorker is already running");
        }
        running = true;
        consumerThread = new Thread(this::consumeTasks, "ReadTaskWorker");
        consumerThread.start();
    }

    private void consumeTasks() {
        while (running) {
            try {
                Task task = tasks.take();
                executorService.execute(() -> {
                    Message message = determineMessage(task);
                    if (message != null) {
                        message.parse(ByteBuffer.wrap(task.getMessage()));
                        downloadScheduler.listen(message);
                    }
                });

            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
                Thread.currentThread()
                        .interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized void stop() {
        running = false;

        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        executorService.shutdown();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    private Message determineMessage(Task task) {

        if (task == null) {
            return null;
        }

        byte[] message = task.getMessage();
        ByteBuffer buffer = ByteBuffer.wrap(message);
        buffer.getInt();
        byte id = buffer.get();
        PeerConnection peerConnection = task.getPeerConnection();

        return switch (id) {
            case 0 -> new Choke(peerConnection);
            case 1 -> new Unchoke(peerConnection);
            case 2 -> new Interested(peerConnection);
            case 3 -> new NotInterested(peerConnection);
            case 4 -> new Have(0, peerConnection);
            case 5 -> new BitField(peerConnection);
            case 6 -> new Request(peerConnection);
            case 7 -> new Piece(0, 0, new byte[0], peerConnection);
            case 8 -> new Cancel(peerConnection);
            default -> null;
        };
    }

}
