package common;


public enum TorrentState {
    START(0),
    PAUSED(1),
    STOPPED(2),
    DOWNLOADING(4),
    SEEDING(8),
    FINISHED(16);

    private final int value;

    TorrentState(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }






}
