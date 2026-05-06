package core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RingBuffer<T> {

    private final AtomicReferenceArray<T> buffer;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final int capacity;

    public RingBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be greater than 0");
        this.capacity = capacity;
        this.buffer = new AtomicReferenceArray<>(capacity);
    }

    public void add(T element) {
        int index = writeIndex.getAndIncrement() % capacity;
        buffer.set(index, element);
    }

    public T get(int index) {
        int currentIndex = writeIndex.get();
        if (index < 0 || index >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        int targetIndex = (currentIndex - 1 - index + capacity) % capacity;
        return buffer.get(targetIndex);
    }

    public int size() {
        return Math.min(writeIndex.get(), capacity);
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) {
            buffer.set(i, null);
        }
        writeIndex.set(0);
    }

}
