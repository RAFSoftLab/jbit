package core;

import core.bencode.BencodeDictionary;
import core.bencode.Bencoder;
import core.bencode.TorrentFile;
import util.GlobalConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class SessionLoader {

    public List<TorrentFile> onLoad() {

        File file = new File(GlobalConfig.APP_DATA);
        File[] files = file.listFiles();

        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<TorrentFile> torrents = new ArrayList<>();

        Map<String, File> torrentFiles = Arrays.stream(files).filter(f -> f.getName().endsWith(GlobalConfig.TORRENT_SUFFIX)).collect(Collectors.toMap(f -> f.getName().split("\\.")[0], f -> f));
        Map<String, File> resumeFiles = Arrays.stream(files).filter(f -> (f.getName().endsWith(GlobalConfig.RESUME_SUFFIX))).collect(Collectors.toMap(f -> f.getName().split("\\.")[0], f -> f));

        torrentFiles.forEach((key, value) -> {
            try (Bencoder bencoder = new Bencoder(new BufferedInputStream(new FileInputStream(value)))) {
                BencodeDictionary decode = bencoder.decode();
                TorrentFile torrentFile = new TorrentFile(decode);
                File resumeFile = resumeFiles.get(key);
                if (resumeFile != null) {
                    try (Bencoder decoder = new Bencoder(new BufferedInputStream(new FileInputStream(resumeFile)))) {
                        BencodeDictionary decode1 = decoder.decode();
                        TorrentMetadata metadata = TorrentMetadata.of(decode1, torrentFile);
                        torrentFile.setMetaData(metadata);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                torrents.add(torrentFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return torrents;
    }


}

