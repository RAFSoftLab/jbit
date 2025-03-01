package piece;

import core.PeerConnection;

import java.util.concurrent.BlockingQueue;

public class TaskQueue {

    private BlockingQueue<PeerConnection> queue;


    public TaskQueue(BlockingQueue<PeerConnection> queue) {
        this.queue = queue;
    }

}
