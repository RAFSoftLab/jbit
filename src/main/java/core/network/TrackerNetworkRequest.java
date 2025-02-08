package core.network;

import core.bencode.TorrentFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackerNetworkRequest {

    public final String info_hash;
    private final byte[] infoHashBytes;
    public final String peer_id;
    public final int port;
    public final long uploaded;
    public final long downloaded;
    public final long left;
    public final int compact;
    public final String event;
    private final String announceUrl;
    private final List<String> announceList;
    private final AtomicInteger urlIndex = new AtomicInteger(-1);
    private final List<String> urls;

    private TrackerNetworkRequest(Builder builder) {
        this.info_hash = builder.infoHash;
        this.announceUrl = builder.announceUrl;
        this.peer_id = builder.peerId;
        this.port = builder.port;
        this.uploaded = builder.uploaded;
        this.downloaded = builder.downloaded;
        this.left = builder.left;
        this.compact = builder.compact;
        this.event = builder.event;
        this.announceList = builder.announceList;
        this.urls = createUrls();
        this.infoHashBytes = builder.infoHashBytes;
    }

    public static TrackerNetworkRequest of(TorrentFile torrent) {
        return new Builder().announceUrl(torrent.getAnnounceList()
                                                 .getFirst())
                .peerId("MFLJDCEACGABLBFDIGPF")
                .port(6881)
                .uploaded(0)
                .downloaded(0)
                .left(39763640)
                .compact(1)
                .infoHash(torrent.getInfoHash())
                .infoHashBytes(torrent.getInfoHashBytes())
                .announceList(torrent.getAnnounceList())
                .build();
    }

    public String getURL() {

        StringBuilder sb = new StringBuilder();
        sb.append(getUrl());
        sb.append("?");
        Field[] fields = this.getClass()
                .getFields();

        for (Field field : fields) {
            try {
                if (field.get(this) == null) continue;
                sb.append(field.getName());
                sb.append("=");
                sb.append(field.get(this));
                sb.append("&");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void resolveNextUrl() {
        urlIndex.incrementAndGet();
        if(urlIndex.get() >= urls.size()) urlIndex.set(0);
    }

    public String getUrl(){
        int index = urlIndex.get();
        return index <= 0 ? announceUrl : announceList.get(urlIndex.get());
    }

    public byte[] getInfoHashBytes() {
        return infoHashBytes;
    }

    private List<String> createUrls(){
        List<String> urls = new ArrayList<>(announceList);
        urls.addFirst(announceUrl);
        return urls;
    }

    public String getTrackerPort(){
        return urls.get(urlIndex.get())
                .split(":")[2];
    }

    public List<String> getUrls(){
        return urls;
    }

    public String getPeerId() {
        return peer_id;
    }

    public int getPort() {
        return port;
    }

    public long getUploaded() {
        return uploaded;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getLeft() {
        return left;
    }

    public int getCompact() {
        return compact;
    }

    public String getEvent() {
        return event;
    }

    public List<String> getAnnounceList() {
        return announceList;
    }

    public static class Builder {

        private String infoHash;
        private byte[] infoHashBytes;
        private String announceUrl;
        private String peerId;
        private int port;
        private long uploaded;
        private long downloaded;
        private long left;
        private int compact;
        private String event;
        private List<String> announceList;

        public Builder infoHash(String infoHash) {
            this.infoHash = infoHash;
            return this;
        }

        public Builder infoHashBytes(byte[] infoHashBytes) {
            this.infoHashBytes = infoHashBytes;
            return this;
        }

        public Builder announceList(List<String> announceList) {
            this.announceList = announceList;
            return this;
        }

        public Builder announceUrl(String announceUrl) {
            this.announceUrl = announceUrl;
            return this;
        }

        public Builder peerId(String peerId) {
            this.peerId = peerId;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder uploaded(long uploaded) {
            this.uploaded = uploaded;
            return this;
        }

        public Builder downloaded(long downloaded) {
            this.downloaded = downloaded;
            return this;
        }

        public Builder left(long left) {
            this.left = left;
            return this;
        }

        public Builder compact(int compact) {
            this.compact = compact;
            return this;
        }

        public Builder event(String event) {
            this.event = event;
            return this;
        }

        public TrackerNetworkRequest build() {
            return new TrackerNetworkRequest(this);
        }


    }
}

