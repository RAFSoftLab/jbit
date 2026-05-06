package ui;

import core.TorrentViewModel;
import core.bencode.TorrentFile;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainWindow extends Application {

    private final TorrentViewModel torrentViewModel;
    private TableView<TorrentEntry> tableView;
    private String lastBrowsedDir;

    public MainWindow() {
        this.torrentViewModel = new TorrentViewModel();
        executeDownSpeed();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void executeDownSpeed() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Set<TorrentFile> managedTorrents = torrentViewModel.getTorrents().stream().map(TorrentEntry::getTorrent).collect(Collectors.toSet());
            for (TorrentFile torrent : managedTorrents) {
                int averageBytesPerSecond = (int) torrent.getMetaData().getDownloadSpeed().getAverageBytesPerSecond() / 1000 / 1000;

                int downloadedAmount = torrent.getDownloadedPieces().size();
                int total = torrent.getInfo().getPiecesStorage().size();
                int downloadProgress = (downloadedAmount / total);

                long totalLengthInBytes = torrent.getInfo().getTotalLength();
                long pieceLength = torrent.getInfo().getPieceLength();
                long totalDownloadedPiecesInBytes = downloadedAmount * pieceLength;
                TorrentEntry entry = torrent.getTorrentEntry();
                long left = totalLengthInBytes - totalDownloadedPiecesInBytes;

                long timeLeftInSeconds = 0;
                if (left <= 0) {
                    System.out.println("Treba ovde " + left);
                } else {
                    timeLeftInSeconds = averageBytesPerSecond / (left);
                    System.out.println("Time left in seconds: " + timeLeftInSeconds);
                }

                String eta = timeLeftInSeconds + "s";
                Platform.runLater(() -> {

                    entry.progressProperty().set(downloadProgress);
                    entry.downSpeedProperty().set(String.format("%d MB/S", averageBytesPerSecond));
                    entry.etaProperty().set(eta);
                    entry.peersProperty().set(torrentViewModel.getAmountOfPeersByTorrent(torrent) + " (x)");

                });
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Torrent Client");

        // Sidebar
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.getChildren().addAll(
                new Label("BitTorrent Speed"),
                new Label("DLive"),
                new Label("TronTV"),
                new Label("Upgrade to Pro"),
                new Separator(),
                new Label("Torrents (15)"),
                new Label("Labels"),
                new Label("Feeds (0)"),
                new Label("Devices (0)")
        );
        ToolBar toolBar = getToolBar(primaryStage);

        // Table of torrents
        tableView = new TableView<>();
        TableColumn<TorrentEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<TorrentEntry, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> data.getValue().sizeProperty());

        TableColumn<TorrentEntry, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());

        TableColumn<TorrentEntry, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(data -> data.getValue().stateProperty());

        TableColumn<TorrentEntry, String> downSpeedCol = new TableColumn<>("Down Speed");
        downSpeedCol.setCellValueFactory(data -> data.getValue().downSpeedProperty());

        TableColumn<TorrentEntry, String> upSpeedCol = new TableColumn<>("Up Speed");
        upSpeedCol.setCellValueFactory(data -> data.getValue().upSpeedProperty());

        TableColumn<TorrentEntry, String> etaCol = new TableColumn<>("ETA");
        etaCol.setCellValueFactory(data -> data.getValue().etaProperty());

        TableColumn<TorrentEntry, String> peersCol = new TableColumn<>("Peers");
        peersCol.setCellValueFactory(data -> data.getValue().peersProperty());

        tableView.getColumns().addAll(nameCol, sizeCol, progressCol, stateCol, downSpeedCol, upSpeedCol, etaCol, peersCol);
        tableView.setItems(torrentViewModel.getTorrents());

        // Bottom progress and stats (mocked as text for simplicity)
        VBox bottomBox = new VBox(5);
        bottomBox.setPadding(new Insets(10));
        Label transferLabel = new Label("Transfer: Downloaded: 328 MB, Uploaded: 0.0 B");
        ProgressBar bottomProgress = new ProgressBar(0.57);
        bottomBox.getChildren().addAll(transferLabel, bottomProgress);

        // Layout setup
        BorderPane layout = new BorderPane();
        layout.setLeft(sidebar);
        layout.setTop(toolBar);
        layout.setCenter(tableView);
        layout.setBottom(bottomBox);

        Scene scene = new Scene(layout, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private ToolBar getToolBar(Stage primaryStage) {
        Button plus = new Button("+");
        plus.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Resource File");

            FileChooser.ExtensionFilter exitFilter = new FileChooser.ExtensionFilter("Torrent Files (*.torrent)", "*.torrent");
            fileChooser.getExtensionFilters().add(exitFilter);

            fileChooser.setInitialDirectory(new File(lastBrowsedDir != null ? lastBrowsedDir : System.getProperty("user.home")));

            File file = fileChooser.showOpenDialog(primaryStage);

            if (file != null) {
                torrentViewModel.addTorrent(file);
                lastBrowsedDir = file.getParentFile().getAbsolutePath();
            }
        });

        Button minus = new Button("-");
        minus.setOnAction(event -> {
            // gracefully remove selected torrent and it's allocations
            TorrentEntry torrent = tableView.getSelectionModel().getSelectedItem();
            AtomicBoolean deleteFilesFromDisk = new AtomicBoolean(false);

            if (torrent != null) {
                Alert alert = createAlertWithOptOut(Alert.AlertType.CONFIRMATION, "Remove torrent", "Do you want to stop and remove torrent?", "Delete files from disk?",
                        deleteFilesFromDisk::set, ButtonType.YES, ButtonType.NO);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    torrentViewModel.removeTorrent(torrent);
                    if (deleteFilesFromDisk.get()) {
                        torrentViewModel.deleteFromDisk(torrent);
                    }


                } else {
                    System.out.println("said no");
                }
            }
        });

        // Top control buttons
        ToolBar toolBar = new ToolBar(
                plus,
                minus,
                new Button("↑"),
                new Button("↓"),
                new Button("▶"),
                new Button("■"),
                new Button("⚙")
        );
        return toolBar;
    }

    private Alert createAlertWithOptOut(Alert.AlertType type, String title, String headerText, String optOutMessage, Consumer<Boolean> optOutAction, ButtonType... buttons) {

        Alert alert = new Alert(type);
        alert.getDialogPane().applyCss();

        Node graphic = alert.getDialogPane().getGraphic();

        alert.setDialogPane(new DialogPane() {

            @Override
            protected Node createDetailsButton() {
                CheckBox optOut = new CheckBox();
                optOut.setText(optOutMessage);
                optOut.setOnAction(e -> optOutAction.accept(optOut.isSelected()));
                return optOut;
            }
        });

        alert.getDialogPane().getButtonTypes().addAll(buttons);

        alert.getDialogPane().setExpandableContent(new Group());
        alert.getDialogPane().setExpanded(true);

        alert.getDialogPane().setGraphic(graphic);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        return alert;
    }
}

