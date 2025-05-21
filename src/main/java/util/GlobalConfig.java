package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class GlobalConfig {

    public static final String APP_NAME = "jbit";
    public static final String RESUME_SUFFIX = ".resume";
    public static final String TORRENT_SUFFIX = ".torrent";
    public static final String OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    public static final String APP_DATA = appDataPath();

    private static String appDataPath() {
        try {
            Path dir;
            if (OS.contains("win")) {
                String roaming = System.getProperty("APPDATA");
                dir = Paths.get(roaming != null ? roaming : System.getProperty("user.home"), APP_NAME);
            } else if (OS.contains("mac")) {
                dir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", APP_NAME);
            } else {
                String xdg = System.getProperty("XDG_DATA_HOME");
                dir = Paths.get(xdg != null ? xdg : System.getProperty("user.home") + "/.local/share", APP_NAME);
            }
            Files.createDirectories(dir);

            return dir.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
