package tasks;

import core.PeerConnection;

public class Task {

    private final byte[] message;
    private final TaskType taskType;
    private final PeerConnection peerConnection;

    public Task(byte[] message, TaskType taskType, PeerConnection peerConnection) {
        this.message = message;
        this.taskType = taskType;
        this.peerConnection = peerConnection;
    }


    public byte[] getMessage() {
        return message;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }
}
