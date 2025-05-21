package core.bencode;

import common.TorrentState;
import core.TorrentMetadata;
import storage.PieceStorage;
import util.GlobalConfig;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private TorrentMetadata metaData;

    public TorrentFile(BencodeDictionary dictionary) {
        super(dictionary.getValue());
        this.dictionary = dictionary;

        this.announce = dictionary.getAsString(ANNOUNCE);
        BencodeElement<?> announceList = dictionary.get(ANNOUNCE_LIST);
        this.announceList = announceList != null ? ((BencodeList) announceList).getValue()
                .stream()
                .map(BencodeList.class::cast)
                .flatMap(list -> list.getValue()
                        .stream())
                .map(BencodeString.class::cast)
                .map(BencodeString::getValue)
                .toList() : new ArrayList<>();
        this.creationDate = LocalDate.ofEpochDay(getAsLong(CREATION_DATE));
        this.comment = dictionary.getAsString(COMMENT);
        this.createdBy = dictionary.getAsString(CREATED_BY);
        this.encoding = dictionary.getAsString(ENCODING);
        this.info = new Info((BencodeDictionary) dictionary.get(INFO));
        this.metaData = new TorrentMetadata(this);
        createInternalCopy();
    }

    public void setMetaData(TorrentMetadata metaData) {
        this.metaData = metaData;
        this.info.getPiecesStorage().forEach(pieceStorage -> {
            if (metaData.getBitField().get(pieceStorage.getIndex())) {
                pieceStorage.setVerified(true);
            }
        });
    }


    private void createInternalCopy() {
        String fileName = GlobalConfig.APP_DATA + File.separator + this.getInfoHash() + GlobalConfig.TORRENT_SUFFIX;
        File file = new File(fileName);

        if (file.exists()) {
            return;
        }

        try (OutputStream os = new FileOutputStream(file)) {
            os.write(this.encode());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public boolean isCompleted() {
        TorrentState state = this.metaData.getState();
        if (state  == null) {
            //throw new IllegalStateException("Torrent state is null");
            return false;
        }
        return state.equals(TorrentState.FINISHED);
    }

    public Set<Integer> getDownloadedPieces(){
        return metaData.getDownloadedPieces();
    }

    public TorrentMetadata getMetaData(){
        return metaData;
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
        private final String name;
        private final List<Files> files;
        private final List<PieceStorage> piecesStorage;
        private final long totalLength;


        public Info(BencodeDictionary info) {

            BencodeList files = info.get(FILES) != null ? (BencodeList) info.get(FILES) : null;

            this.files = files != null ? files.getValue()
                    .stream()
                    .map(BencodeDictionary.class::cast)
                    .map(Files::new)
                    .toList() : null;

            this.totalLength = this.files != null ? this.files.stream()
                    .map(Files::getLength)
                    .mapToLong(Long::longValue)
                    .sum() : info.get("length", long.class);

            this.pieceLength = info.get(PIECE_LENGTH, Long.class);
            this.pieces = info.get(PIECES, String.class);
            this.piecesStorage = processPieces((BencodeString) info.get(PIECES));

            this.privateTorrent = info.get(PRIVATE, Integer.class) == null ? 1 : 0;
            this.name = info.getAsString(NAME);

        }


        private List<PieceStorage> processPieces(BencodeString piecesString) {

            byte[] piecesBytes = piecesString.getBytes();
            List<PieceStorage> pieces = new ArrayList<>();

            int index = 0;
            int totalAmountOfPieces = (int) Math.ceil((double) totalLength / pieceLength);
            int lastPieceSize = (int) (totalLength % pieceLength == 0 ? pieceLength : totalLength % pieceLength);


            for (int i = 0; i < piecesBytes.length; i += 20) {
                if (i + 20 > piecesBytes.length) {
                    throw new IllegalArgumentException("Invalid pieces length: not a multiple of 20 bytes");
                }
                byte[] pieceHash = new byte[20];
                System.arraycopy(piecesBytes, i, pieceHash, 0, 20);
                boolean isLastPiece = index == totalAmountOfPieces - 1;
                int currentPieceLength = isLastPiece ? lastPieceSize : pieceLength.intValue();
                PieceStorage piece = new PieceStorage(index++, currentPieceLength, pieceHash);
                pieces.add(piece);
            }

            return pieces;
        }

        public List<PieceStorage> getPiecesStorage() {
            return piecesStorage;
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

        public List<Files> getFiles() {
            return files;
        }

        public String getName() {
            return name;
        }

        public static class Files {

            private final Long length;
            private final String md5sum;
            private final String path;

            public Files(BencodeDictionary dict) {
                this.length = dict.get(LENGTH, Long.class);
                this.md5sum = dict.get(MD5SUM, String.class);
                this.path = dict.get(PATH) != null ? ((BencodeList) dict.get(PATH)).getValue()
                        .stream()
                        .map(BencodeString.class::cast)
                        .map(BencodeString::getValue)
                        .reduce("", (a, b) -> a + "/" + b) : null;
            }

            public Long getLength() {
                return length;
            }

            public String getMd5sum() {
                return md5sum;
            }

            public String getPath() {
                return path;
            }
        }
    }

}