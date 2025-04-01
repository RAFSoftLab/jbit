package tasks;

import core.PeerConnection;

public class WriteEvent {

    private final PeerConnection peer;
    private final EventType eventType;

    public WriteEvent(PeerConnection peer, EventType eventType) {
        this.peer = peer;
        this.eventType = eventType;
    }

    public PeerConnection getPeerConnection() {
        return peer;
    }

    public EventType getEventType() {
        return eventType;
    }

    public enum EventType {
        FREE_SPACE, HAVE_BITFIELD, UNCHOKE, REQUEST, NOT_INTERESTING
    }
}
