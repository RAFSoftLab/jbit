package core;

import common.TorrentState;
import core.bencode.BencodeDictionary;
import core.bencode.BencodeElement;
import core.bencode.BencodeString;
import core.bencode.TorrentFile;
import util.GlobalConfig;
import util.UserConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public final class TorrentMetadata {

    private static final BencodeString INFO_HASH_KEY = new BencodeString("info_hash", "info_hash".getBytes(StandardCharsets.UTF_8));
    private static final BencodeString BIT_FIELD_KEY = new BencodeString("bit_field", "bit_field".getBytes(StandardCharsets.UTF_8));
    private static final BencodeString STATE_KEY = new BencodeString("state", "state".getBytes(StandardCharsets.UTF_8));
    private static final BencodeString DOWNLOAD_PATH_KEY = new BencodeString("download_path", "download_path".getBytes(StandardCharsets.UTF_8));

    private final BitSet bitField;
    private final String infoHash;
    private final int threshold;
    private final Lock lock = new ReentrantLock();
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    private final TorrentFile torrentFile;

    private TorrentState state;
    private String downloadPath;

    public TorrentMetadata(TorrentFile torrentFile) {
        this.infoHash = torrentFile.getInfoHash();
        this.torrentFile = torrentFile;
        int numOfPieces = torrentFile.getInfo()
                .getPiecesStorage()
                .size();
        this.bitField = new BitSet(numOfPieces);
        this.state = TorrentState.DOWNLOADING;
        this.threshold = calculateThreshold();
        this.downloadPath = UserConfig.DOWNLOAD_DIR;
    }

    private TorrentMetadata(String infoHash, BitSet bitfield, TorrentState state, String downloadPath, TorrentFile torrentFile) {
        this.infoHash = infoHash;
        this.bitField = bitfield;
        this.state = state;
        this.downloadPath = downloadPath;
        this.torrentFile = torrentFile;
        this.threshold = calculateThreshold();
    }

    public static TorrentMetadata of(BencodeDictionary dictionary, TorrentFile torrentFile) {
        try {
            byte[] bytes = ((BencodeString) dictionary.getValue().get(BIT_FIELD_KEY)).getBytes();
            String infoHash = ((BencodeString) dictionary.getValue().get(INFO_HASH_KEY)).getValue();
            String state = ((BencodeString) dictionary.getValue().get(STATE_KEY)).getValue();
            String downloadPath = ((BencodeString) dictionary.getValue().get(DOWNLOAD_PATH_KEY)).getValue();
            BitSet bitfield = BitSet.valueOf(bytes);

            return new TorrentMetadata(infoHash, bitfield, TorrentState.valueOf(state), downloadPath, torrentFile);
        } catch (Exception e) {
            e.printStackTrace();
            return new TorrentMetadata(torrentFile);
        }

    }

    private int calculateThreshold() {
        return (int) Math.max(1, torrentFile.getInfo()
                .getPiecesStorage()
                .size() * 0.10);

    }

    public Set<Integer> getDownloadedPieces() {
        return bitField.stream()
                .filter(bitField::get)
                .boxed()
                .collect(Collectors.toSet());
    }

    public TorrentState getState() {
        return this.state;
    }

    public void setState(TorrentState state) {
        this.state = state;
        sync(0, true);
    }

    public String getDownloadPath() {
        return this.downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
        sync(0, true);
    }

    public BitSet getBitField() {
        return this.bitField;
    }

    public void updateBitField(int pieceIndex) {
        int updateCount = updateCounter.addAndGet(1);
        bitField.set(pieceIndex);

        if (updateCount > threshold) {
            sync(updateCount, false);
        }
    }

    public void sync(int updateCount, boolean force) {
        try {
            lock.lock();

            int totalUpdates = updateCounter.get();
            File file = new File(GlobalConfig.APP_DATA, infoHash + GlobalConfig.RESUME_SUFFIX);
            if (totalUpdates > threshold || force) {
                try (FileOutputStream os = new FileOutputStream(file)) {

                    BencodeDictionary bencodeDictionary = new BencodeDictionary(snapshot());
                    byte[] bytes = bencodeDictionary.encode();
                    os.write(bytes);

                    if (updateCounter.get() - updateCount > 0) {
                        updateCounter.set(updateCounter.get() - updateCount);
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private Map<BencodeString, BencodeElement<?>> snapshot() {
        Map<BencodeString, BencodeElement<?>> encodeMap = new LinkedHashMap<>();
        encodeMap.put(INFO_HASH_KEY, new BencodeString(infoHash, infoHash.getBytes(StandardCharsets.UTF_8)));
        encodeMap.put(STATE_KEY, new BencodeString(state.toString(), state.toString().getBytes(StandardCharsets.UTF_8)));
        encodeMap.put(DOWNLOAD_PATH_KEY, new BencodeString(downloadPath, downloadPath.getBytes(StandardCharsets.UTF_8)));
        encodeMap.put(BIT_FIELD_KEY, new BencodeString(BIT_FIELD_KEY.getValue(), bitField.toByteArray()));
        return encodeMap;
    }


    @Override
    public int hashCode() {
        return infoHash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TorrentMetadata other && this.infoHash.equals(other.infoHash);
    }
}
