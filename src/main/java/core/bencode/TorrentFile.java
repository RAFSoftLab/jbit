package core.bencode;

import java.time.LocalDate;
import java.util.List;

public class TorrentFile extends BencodeDictionary {

    private static final String ANNOUNCE = "announce";
    private static final String ANNOUNCE_LIST = "announce-list";
    private static final String CREATION_DATE = "creation date";
    private static final String COMMENT = "comment";
    private static final String CREATED_BY = "created by";
    private static final String ENCODING = "encoding";
    private static final String INFO = "info";

    private final BencodeDictionary dictionary;
    private final String announce;
    private final List<String> announceList;
    private final LocalDate creationDate;
    private final String comment;
    private final String createdBy;
    private final String encoding;
    private final Info info;


    public TorrentFile(BencodeDictionary dictionary) {
        super(dictionary.getValue());
        this.dictionary = dictionary;

        this.announce = dictionary.getAsString(ANNOUNCE);
        this.announceList = ((BencodeList) dictionary.get(ANNOUNCE_LIST)).getValue()
                .stream()
                .map(BencodeList.class::cast)
                .flatMap(list -> list.getValue()
                        .stream())
                .map(BencodeString.class::cast)
                .map(BencodeString::getValue)
                .toList();
        this.creationDate = LocalDate.ofEpochDay(getAsLong(CREATION_DATE));
        this.comment = dictionary.getAsString(COMMENT);
        this.createdBy = dictionary.getAsString(CREATED_BY);
        this.encoding = dictionary.getAsString(ENCODING);
        this.info = new Info((BencodeDictionary) dictionary.get(INFO));

    }


    public Info getInfo() {
        return info;
    }

    public String getAnnounce() {
        return announce;
    }

    public List<String> getAnnounceList() {
        return announceList;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getEncoding() {
        return encoding;
    }

    public static final class Info {

        private static final String FILES = "files";
        private static final String NAME = "name";
        private static final String PIECE_LENGTH = "piece length";
        private static final String PIECES = "pieces";
        private static final String PRIVATE = "private";
        private static final String MD5SUM = "md5sum";
        private static final String LENGTH = "length";
        private static final String PATH = "path";

        private final Long pieceLength;
        private final String pieces;
        private final Integer privateTorrent;
        private final String md5sum;
        private final String name;
        private final String path;
        private final Long length;
        private final List<Info> files;

        public Info(BencodeDictionary info) {

            BencodeList files = info.get(FILES) != null ? (BencodeList) info.get(FILES) : null;

            this.files = files != null ? files.getValue()
                    .stream()
                    .map(BencodeDictionary.class::cast)
                    .map(Info::new)
                    .toList() : null;

            this.name = info.get(NAME, String.class);
            this.pieceLength = info.get(PIECE_LENGTH, Long.class);
            this.pieces = info.get(PIECES, String.class);
            this.privateTorrent = info.get(PRIVATE, Integer.class) == null ? 1 : 0;
            this.md5sum = info.get(MD5SUM, String.class);
            this.length = info.get(LENGTH, Long.class);
            this.path = info.get(PATH) != null ? ((BencodeList) info.get(PATH)).getValue()
                    .stream()
                    .map(BencodeString.class::cast)
                    .map(BencodeString::getValue)
                    .reduce("", (a, b) -> a + "/" + b) : null;
        }

        public Long getLength() {
            return length;
        }

        public long getPieceLength() {
            return pieceLength;
        }

        public String getPieces() {
            return pieces;
        }

        public int getPrivateTorrent() {
            return privateTorrent;
        }

        public String getMd5sum() {
            return md5sum;
        }

        public String getPath() {
            return path;
        }

        public List<Info> getFiles() {
            return files;
        }

        public String getName() {
            return name;
        }
    }

}