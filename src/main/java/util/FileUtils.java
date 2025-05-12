package util;

import common.TorrentState;
import core.ResumeData;
import core.bencode.TorrentFile;
import storage.PieceStorage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileUtils {

    private final ResumeData resumeData;
    private final Set<TorrentFile> torrentFiles;

    private int iteration = 0;

    public FileUtils(Set<TorrentFile> torrentFiles, ResumeData resumeData) {
        this.torrentFiles = torrentFiles;
        this.resumeData = resumeData;
    }

    public void init() {

        Runnable runnable = () -> {
            try {
                System.out.println("RUNNING fileutils");
                for (TorrentFile torrentFile : torrentFiles) {
                    System.out.println("ovde");
                    List<PieceStorage> pieces = torrentFile.getInfo()
                            .getPiecesStorage();
                    Set<Integer> downloadedPieces = resumeData.getDownloadedPieces(torrentFile);
                    List<PieceStorage> notDownloadedPieces = pieces.stream()
                            .filter(piece -> !downloadedPieces.contains(piece.getIndex()))
                            .toList();

                    System.out.println("Iteration: " + ++iteration);
                    System.out.println("Pieces to download: " + notDownloadedPieces.size());

                    for (PieceStorage piece : notDownloadedPieces) {
                        if (piece.isVerified()) {
                            downloadPiece(piece, torrentFile);
                        }
                    }

                    if (resumeData.getDownloadedPieces(torrentFile)
                            .size() == pieces.size()) {
                        torrentFile.getMetaData().setState(TorrentState.FINISHED);
                    }
                }
                resumeData.sync();
            }catch (Exception e){
                System.out.println("Error in fileutils");
                e.printStackTrace();
            }
        };

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);

    }

    public void downloadPiece(PieceStorage piece, TorrentFile torrentFile) {
        String fileName = torrentFile.getInfo()
                .getName();

        List<TorrentFile.Info.Files> files = torrentFile.getInfo()
                .getFiles();
        long totalLength = 0;
        if (files != null) {
            totalLength = files.stream()
                    .mapToLong(TorrentFile.Info.Files::getLength)
                    .sum();
        }
        long pieceLength = torrentFile.getInfo()
                .getPieceLength();
        long totalLengthInPieces = pieceLength * torrentFile.getInfo()
                .getPiecesStorage()
                .size();
        assert totalLength <= totalLengthInPieces;

        fileName = (fileName != null) ? fileName : "TorrentFileName";
        long globalOffset = piece.getIndex() * pieceLength;
        byte[] pieceData = piece.getData();
        int bytesWritten = 0;

        if (files != null) {
            long previousFilesLength = 0;

            for (TorrentFile.Info.Files fileInfo : files) {
                long currentFileLength = fileInfo.getLength();
                long currentFileEnd = previousFilesLength + currentFileLength;
                if (currentFileEnd > globalOffset && bytesWritten < pieceData.length) {
                    long fileRelativeOffset = Math.max(globalOffset - previousFilesLength, 0);
                    int availableInFile = (int) (currentFileLength - fileRelativeOffset);
                    int remainingInPiece = pieceData.length - bytesWritten;
                    int bytesToWrite = Math.min(availableInFile, remainingInPiece);
                    String filePath = fileInfo.getPath()
                            .startsWith("/") ? fileInfo.getPath()
                            .substring(1) : fileInfo.getPath();
                    filePath = UserConfig.DOWNLOAD_DIR + File.separator + "jbitd" + File.separator + filePath;

                    try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                        raf.seek(fileRelativeOffset);
                        raf.write(pieceData, bytesWritten, bytesToWrite);
//                        torrentFile.getInfo()
//                                .getDownloadedPieces()
//                                .add(piece.getIndex());
                        torrentFile.getMetaData().setDownloadedPiece(piece.getIndex());
                        System.out.println("Downloaded piece with index " + piece.getIndex());
                        System.out.println("Written to: " + filePath);

                    } catch (IOException e) {
                        System.out.println("Error writing to file: " + filePath);
                        e.printStackTrace();
                    }

                    bytesWritten += bytesToWrite;
                }
                previousFilesLength += currentFileLength;

                if (bytesWritten >= pieceData.length) {
                    break;
                }
            }
        } else {
            try (RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
                file.seek(globalOffset);
                file.write(pieceData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (bytesWritten == pieceData.length) {
            resumeData.setPiece(torrentFile, piece.getIndex());
        }
    }


}
