package core;

import core.bencode.TorrentFile;
import util.UserConfig;

import java.io.*;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ResumeData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String RESUME_FILE_NAME = ".resume.ser";
    private String filePath;

    private Map<String, ResumeTorrentData> resumeTorrentData = new ConcurrentHashMap<>();

    public ResumeData() {
        initResumeData();
    }

    public void printPercentages(){
        for (Map.Entry<String, ResumeTorrentData> entry : resumeTorrentData.entrySet()){
            BitSet bitSet = entry.getValue().getBitField();

            int totalPieces = bitSet.length();
            int downloadedPieces = 0;
            for (int i = 0; i < totalPieces; i++){
                if (bitSet.get(i)){
                    downloadedPieces++;
                }
            }
            double percentage = (double) downloadedPieces / totalPieces * 100;
            System.out.println("Torrent: " + entry.getKey() + " - " + percentage + "% downloaded");
        }
    }

    private void initResumeData() {
        File file = new File(UserConfig.DOWNLOAD_DIR + File.separator + RESUME_FILE_NAME);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ResumeData data = (ResumeData) ois.readObject();
            this.filePath = data.filePath;
            this.resumeTorrentData = data.resumeTorrentData;
            printPercentages();

        } catch (FileNotFoundException e) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                this.filePath = UserConfig.DOWNLOAD_DIR + File.separator + RESUME_FILE_NAME;
                oos.writeObject(this);
                oos.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void sync() {
        File file = new File(UserConfig.DOWNLOAD_DIR + File.separator + RESUME_FILE_NAME);
        if (!file.exists()) {
            throw new IllegalStateException("Resume file not found");
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
            oos.flush();
            printPercentages();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setPiece(TorrentFile torrentFile, int pieceIndex) {
        resumeTorrentData.get(torrentFile.getInfoHash())
                .getBitField()
                .set(pieceIndex);
    }

    public Set<Integer> getDownloadedPieces(TorrentFile torrentFile) {
        BitSet bitField = resumeTorrentData.get(torrentFile.getInfoHash())
                .getBitField();
        Set<Integer> downloadedPieces = new HashSet<>();
        for (int i = 0; i < bitField.length(); i++) {
            if (bitField.get(i)) {
                downloadedPieces.add(i);
            }
        }
        return downloadedPieces;
    }

    public String getFileName() {
        return RESUME_FILE_NAME;
    }

    public String getFilePath() {
        return filePath;
    }

    public ResumeTorrentData getMetaData(TorrentFile torrent) {
        ResumeTorrentData metaData = resumeTorrentData.getOrDefault(torrent.getInfoHash(), new ResumeTorrentData(torrent));
        resumeTorrentData.putIfAbsent(torrent.getInfoHash(), metaData);
        return metaData;
    }
}
