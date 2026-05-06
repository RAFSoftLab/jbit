package core;

public class UpDownSpeedTracker {

    private static final int MAX_CAPACITY = 100;
    private static final long WINDOW_MS = 5000;

    private final RingBuffer<BytesSnapshot> buffer = new RingBuffer<>(MAX_CAPACITY);

    public void record(long bytes) {
        buffer.add(new BytesSnapshot(System.currentTimeMillis(), bytes));
    }

    public double getAverageBytesPerSecond() {

        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS;

        long totalBytes = 0;
        long earliestTime = now;

        for (int i = 0; i < buffer.size(); i++) {
            BytesSnapshot snapshot = buffer.get(i);
            if (snapshot == null) continue;

            if (snapshot.timestamp() >= cutoff) {
                totalBytes += snapshot.bytes();
                if (snapshot.timestamp() < earliestTime) {
                    earliestTime = snapshot.timestamp();
                }
            }
        }

        long windowDuration = Math.max(1, now - earliestTime);
        return (double) (totalBytes * 1000) / windowDuration;
    }


}
