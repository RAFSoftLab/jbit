package tasks;

import core.PeerConnection;
import piece.*;

import java.nio.ByteBuffer;

public class ReadTaskWorker implements Runnable {

    private final Task task;
    private final RarestFirstPicker rarestFirstPicker;

    public ReadTaskWorker(Task task, RarestFirstPicker rarestFirstPicker) {
        this.task = task;
        this.rarestFirstPicker = rarestFirstPicker;
    }


    @Override
    public void run() {
        Message message = determineMessage(task);
        if(message != null){
            message.parse(ByteBuffer.wrap(task.getMessage()));
        }
    }

    private Message determineMessage(Task task) {
        byte[] message = task.getMessage();
        ByteBuffer buffer = ByteBuffer.wrap(message);
        int length = buffer.getInt();
        byte id = buffer.get();
        PeerConnection peerConnection = task.getPeerConnection();
        System.out.println("Determining message for task with id: " + id + " and length: " + length);

        return switch (id) {
            case 0 -> {
                System.out.println("Task is Choke");
                yield new Choke(peerConnection);
            }
            case 1 -> {
                System.out.println("Task is Unchoke");
                yield new Unchoke(peerConnection);
            }
            case 2 -> {
                System.out.println("Task is Interested");
                yield new Interested(peerConnection);
            }
            case 3 -> {
                System.out.println("Task is NotInterested");
                yield new NotInterested(peerConnection);
            }
            case 4 -> {
                System.out.println("Task is Have");
                yield new Have(0, peerConnection);
            }
            case 5 -> {
                System.out.println("Task is BitField");
                yield new BitField(new byte[0], peerConnection);
            }
            case 6 -> {
                System.out.println("Task is Request");
                yield new Request(rarestFirstPicker, peerConnection);
            }
            case 7 -> {
                System.out.println("Task is Piece");
                yield new Piece(0, 0, new byte[0], peerConnection);
            }
            case 8 -> {
                System.out.println("Task is Cancel");
                yield new Cancel(peerConnection);
            }
            default -> {
                System.out.println("Task with unknown id: " + id);
                yield null;
            }
        };
    }

}
