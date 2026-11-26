package com.example;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    private final List<Playlist> playlists = new ArrayList<>();

    private Playlist myPlaylist;

    private ObservableList<String> observablePlaylistNames;
    private ObservableList<String> observableSongNames;

    private ListView<String> playlistListView;
    private ListView<String> songListView;
    private Label statusLabel;

    private Timeline playlistMonitor;
    private int lastMonitoredIndex = -1;

    @Override
    public void start(Stage primaryStage) {

        String startupMessage = loadSavedPlaylists();

        observablePlaylistNames = FXCollections.observableArrayList();
        for (Playlist playlist : playlists) {
            observablePlaylistNames.add(playlist.Name);
        }

        observableSongNames = FXCollections.observableArrayList();

        // Title
        Label titleLabel = new Label("🎵 MelodyNode Playlist");
        titleLabel.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #f43f5e;"
        );

        // Status
        statusLabel = new Label(startupMessage);
        statusLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #a1a1aa;"
        );
        statusLabel.setWrapText(true);

        // Playlist names
        playlistListView = new ListView<>(observablePlaylistNames);
        playlistListView.setPrefWidth(190);
        playlistListView.setPlaceholder(
            new Label("No playlists yet.\nCreate one to begin.")
        );
        playlistListView.setStyle(
            "-fx-background-color: #27272a;" +
            "-fx-control-inner-background: #18181b;" +
            "-fx-border-color: #3f3f46;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );

        // Songs in the selected playlist
        songListView = new ListView<>(observableSongNames);
        songListView.setPrefHeight(330);
        songListView.setPlaceholder(
            new Label("Select a playlist, then add some songs.")
        );
        songListView.setStyle(
            "-fx-background-color: #27272a;" +
            "-fx-text-fill: white;" +
            "-fx-control-inner-background: #18181b;" +
            "-fx-border-color: #3f3f46;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );

        // Playlist buttons
        Button createPlaylistBtn = createStyledButton(
            "📂 Create Playlist", "#0ea5e9"
        );
        Button renamePlaylistBtn = createStyledButton(
            "✏ Rename", "#8b5cf6"
        );
        Button deletePlaylistBtn = createStyledButton(
            "❌ Delete", "#be123c"
        );

        // Song buttons
        Button addSongBtn = createStyledButton(
            "➕ Add Song", "#6366f1"
        );
        Button playBtn = createStyledButton(
            "▶ Play Current", "#22c55e"
        );
        Button playAllBtn = createStyledButton(
            "▶ Play All", "#16a34a"
        );
        Button stopBtn = createStyledButton(
            "⏹ Stop", "#ef4444"
        );
        Button nextBtn = createStyledButton(
            "⏭ Next Song", "#3b82f6"
        );
        Button sortBtn = createStyledButton(
            "🔤 Sort A-Z", "#8b5cf6"
        );
        Button removeSongBtn = createStyledButton(
            "🗑 Remove Song", "#f97316"
        );

        // Create a new, independent playlist
        createPlaylistBtn.setOnAction(e -> {

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create Playlist");
            dialog.setHeaderText("Create a New Playlist");
            dialog.setContentText("Enter playlist name:");

            dialog.showAndWait().ifPresent(name -> {

                String cleanName = name.trim();

                if (cleanName.isEmpty()) {
                    statusLabel.setText("Please enter a playlist name.");
                    return;
                }

                if (playlistNameExists(cleanName, null)) {
                    statusLabel.setText("A playlist with that name already exists.");
                    return;
                }

                Playlist newPlaylist = new Playlist();
                String result = newPlaylist.Create_Playlist(cleanName);

                if (newPlaylist.Name.equals("Not Assigned")) {
                    statusLabel.setText(result);
                    return;
                }

                playlists.add(newPlaylist);
                observablePlaylistNames.add(newPlaylist.Name);

                int newIndex = playlists.size() - 1;
                playlistListView.getSelectionModel().select(newIndex);

                statusLabel.setText(result + saveWarning());
            });
        });

        // Rename the selected playlist
        renamePlaylistBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            TextInputDialog dialog = new TextInputDialog(myPlaylist.Name);
            dialog.setTitle("Rename Playlist");
            dialog.setHeaderText("Rename " + myPlaylist.Name);
            dialog.setContentText("New playlist name:");

            dialog.showAndWait().ifPresent(name -> {

                String cleanName = name.trim();

                if (cleanName.isEmpty()) {
                    statusLabel.setText("Please enter a playlist name.");
                    return;
                }

                if (playlistNameExists(cleanName, myPlaylist)) {
                    statusLabel.setText("A playlist with that name already exists.");
                    return;
                }

                String result = myPlaylist.Rename_Playlist(cleanName);
                int index = playlistListView.getSelectionModel().getSelectedIndex();
                observablePlaylistNames.set(index, myPlaylist.Name);
                statusLabel.setText(result + saveWarning());
            });
        });

        // Delete only the selected playlist
        deletePlaylistBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Playlist");
            confirmation.setHeaderText("Delete " + myPlaylist.Name + "?");
            confirmation.setContentText(
                "This will delete this playlist and its saved song list.\n" +
                "The original music files on your computer will not be deleted."
            );

            confirmation.showAndWait().ifPresent(response -> {

                if (response == ButtonType.OK) {

                    int index = playlistListView.getSelectionModel().getSelectedIndex();
                    String deletedName = myPlaylist.Name;

                    stopPlaylistMonitor();
                    myPlaylist.Stop_All_Songs();
                    myPlaylist.Delete_Playlist();
                    myPlaylist = null;

                    playlists.remove(index);
                    observablePlaylistNames.remove(index);
                    refreshSongList();

                    int selectedIndex = playlistListView.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        selectPlaylist(selectedIndex);
                    } else if (!playlists.isEmpty()) {
                        playlistListView.getSelectionModel().select(
                            Math.min(index, playlists.size() - 1)
                        );
                    }

                    String message = "Deleted playlist: " + deletedName;
                    if (playlists.isEmpty()) {
                        message += ". Create another playlist to begin.";
                    }
                    statusLabel.setText(message + saveWarning());
                }
            });
        });

        // Add a song to the selected playlist
        addSongBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Audio File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                    "Supported Audio Files",
                    "*.mp3", "*.wav", "*.aiff", "*.aif", "*.m4a"
                )
            );

            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {

                myPlaylist.Add_Song(
                    selectedFile.getName(),
                    selectedFile.getAbsolutePath()
                );

                refreshSongList();
                statusLabel.setText(
                    "Added to " + myPlaylist.Name + ": " +
                    selectedFile.getName() +
                    " (Total: " + myPlaylist.count + ")" + saveWarning()
                );
            }
        });

        // Play selected song
        playBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            stopPlaylistMonitor();

            int selectedIndex = songListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                myPlaylist.Set_Current_Song(selectedIndex);
            }

            String result = myPlaylist.Play_Current_Song(
                message -> statusLabel.setText(message)
            );

            if (result != null) {
                statusLabel.setText(result);
            }

            refreshSongList();
        });

        // Play every song in the selected playlist
        playAllBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            stopPlaylistMonitor();

            String result = myPlaylist.Play_the_Playlist(
                message -> {
                    statusLabel.setText(message);
                    stopPlaylistMonitor();
                }
            );

            if (result != null) {
                statusLabel.setText(result);
                startPlaylistMonitor();
            }

            refreshSongList();
        });

        // Stop playback in the selected playlist
        stopBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            stopPlaylistMonitor();
            statusLabel.setText(myPlaylist.Stop_Current_Song());
            refreshSongList();
        });

        // Play the next song
        nextBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            stopPlaylistMonitor();

            int selectedIndex = songListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                myPlaylist.Set_Current_Song(selectedIndex);
            }

            String result = myPlaylist.Play_Next_Song(
                message -> statusLabel.setText(message)
            );

            if (result != null) {
                statusLabel.setText(result);
            }

            refreshSongList();
        });

        // Sort songs in the selected playlist
        sortBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            String result = myPlaylist.Sort_Playlist_Alphabetically();
            statusLabel.setText(result + saveWarning());
            refreshSongList();
        });

        // Remove selected song from the selected playlist
        removeSongBtn.setOnAction(e -> {

            if (!ensurePlaylistSelected()) {
                return;
            }

            int selectedIndex = songListView.getSelectionModel().getSelectedIndex();

            if (selectedIndex >= 0) {

                stopPlaylistMonitor();
                String result = myPlaylist.Delete_Song(selectedIndex);
                refreshSongList();
                statusLabel.setText(result + saveWarning());

            } else {
                statusLabel.setText("⚠ Please select a song to remove.");
            }
        });

        // Change the displayed songs when another playlist is selected
        playlistListView.getSelectionModel().selectedIndexProperty().addListener(
            (observable, oldValue, newValue) -> selectPlaylist(newValue.intValue())
        );

        // Playlist panel
        Label playlistHeading = new Label("MY PLAYLISTS");
        playlistHeading.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #d4d4d8;"
        );

        createPlaylistBtn.setMaxWidth(Double.MAX_VALUE);
        HBox playlistSmallButtons = new HBox(8, renamePlaylistBtn, deletePlaylistBtn);
        playlistSmallButtons.setAlignment(Pos.CENTER);
        renamePlaylistBtn.setMaxWidth(Double.MAX_VALUE);
        deletePlaylistBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(renamePlaylistBtn, Priority.ALWAYS);
        HBox.setHgrow(deletePlaylistBtn, Priority.ALWAYS);

        VBox playlistPanel = new VBox(
            10,
            playlistHeading,
            playlistListView,
            createPlaylistBtn,
            playlistSmallButtons
        );
        playlistPanel.setPrefWidth(210);
        playlistPanel.setMinWidth(190);
        VBox.setVgrow(playlistListView, Priority.ALWAYS);

        // Song panel
        Label songsHeading = new Label("SONGS");
        songsHeading.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #d4d4d8;"
        );

        HBox playbackControls = new HBox(
            8, playBtn, playAllBtn, stopBtn, nextBtn
        );
        playbackControls.setAlignment(Pos.CENTER_LEFT);

        HBox songManagementControls = new HBox(
            8, addSongBtn, sortBtn, removeSongBtn
        );
        songManagementControls.setAlignment(Pos.CENTER_LEFT);

        VBox songPanel = new VBox(
            10,
            songsHeading,
            songListView,
            playbackControls,
            songManagementControls
        );
        HBox.setHgrow(songPanel, Priority.ALWAYS);
        VBox.setVgrow(songListView, Priority.ALWAYS);

        HBox workspace = new HBox(15, playlistPanel, songPanel);
        VBox.setVgrow(workspace, Priority.ALWAYS);

        // Main layout
        VBox root = new VBox(15, titleLabel, statusLabel, workspace);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #18181b;");

        // Scene and stage
        Scene scene = new Scene(root, 920, 620);
        primaryStage.setTitle("MelodyNode - Music Player");
        primaryStage.setMinWidth(780);
        primaryStage.setMinHeight(540);
        primaryStage.setScene(scene);
        primaryStage.show();

        if (!playlists.isEmpty()) {
            playlistListView.getSelectionModel().select(0);
        }
    }

    // Load playlists saved during an earlier session
    private String loadSavedPlaylists() {

        try {
            playlists.addAll(PlaylistStorage.loadPlaylists());

            if (playlists.isEmpty()) {
                return "No saved playlists. Create a playlist to begin.";
            }

            return "Loaded " + playlists.size() + " saved playlist(s).";

        } catch (IOException e) {
            return "Could not load saved playlists. You can still create new ones. " +
                e.getMessage();
        }
    }

    // Check whether a playlist name is already in use
    private boolean playlistNameExists(String name, Playlist except) {

        for (Playlist playlist : playlists) {
            if (playlist != except && playlist.Name.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    // Make sure an operation has a selected playlist
    private boolean ensurePlaylistSelected() {

        if (myPlaylist == null) {
            statusLabel.setText("⚠ Please create or select a playlist first.");
            return false;
        }

        return true;
    }

    // Select a playlist and show only its songs
    private void selectPlaylist(int index) {

        if (index < 0 || index >= playlists.size()) {
            if (myPlaylist != null) {
                stopPlaylistMonitor();
                myPlaylist.Stop_All_Songs();
            }

            myPlaylist = null;
            refreshSongList();
            return;
        }

        Playlist selectedPlaylist = playlists.get(index);

        if (myPlaylist != selectedPlaylist) {

            stopPlaylistMonitor();

            if (myPlaylist != null) {
                myPlaylist.Stop_All_Songs();
            }

            myPlaylist = selectedPlaylist;
            refreshSongList();
            statusLabel.setText(
                "Selected playlist: " + myPlaylist.Name +
                " (" + myPlaylist.count + " song(s))"
            );
        }
    }

    // Save changes after playlist or song updates
    private String saveWarning() {

        try {
            PlaylistStorage.savePlaylists(playlists);
            return "";

        } catch (IOException e) {
            return " | WARNING: Could not save playlists: " + e.getMessage();
        }
    }

    // Button styling
    private Button createStyledButton(String text, String hexColor) {

        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + hexColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 8px 12px;" +
            "-fx-background-radius: 6px;" +
            "-fx-cursor: hand;"
        );

        return btn;
    }

    // Refresh the songs shown for the selected playlist
    private void refreshSongList() {

        observableSongNames.clear();

        if (myPlaylist == null) {
            return;
        }

        Song temp = myPlaylist.begin;

        while (temp != null) {
            observableSongNames.add(temp.name);
            temp = temp.next;
        }

        int index = myPlaylist.Get_Current_Index();

        if (index >= 0) {
            songListView.getSelectionModel().select(index);
            songListView.scrollTo(index);
        } else {
            songListView.getSelectionModel().clearSelection();
        }
    }

    // Monitor automatic playback of the selected playlist
    private void startPlaylistMonitor() {

        stopPlaylistMonitor();

        playlistMonitor = new Timeline(
            new KeyFrame(
                Duration.millis(300),
                e -> {

                    if (myPlaylist == null) {
                        stopPlaylistMonitor();
                        return;
                    }

                    int index = myPlaylist.Get_Current_Index();

                    if (index >= 0 && myPlaylist.current != null) {

                        if (index != lastMonitoredIndex) {
                            songListView.getSelectionModel().select(index);
                            songListView.scrollTo(index);
                            statusLabel.setText(
                                "▶ Now Playing: " + myPlaylist.current.name
                            );
                            lastMonitoredIndex = index;
                        }

                    } else {
                        stopPlaylistMonitor();
                        songListView.getSelectionModel().clearSelection();
                        statusLabel.setText("✓ Playlist finished!");
                    }
                }
            )
        );

        playlistMonitor.setCycleCount(Timeline.INDEFINITE);
        playlistMonitor.play();
    }

    // Stop the playback monitor
    private void stopPlaylistMonitor() {

        if (playlistMonitor != null) {
            playlistMonitor.stop();
            playlistMonitor = null;
        }

        lastMonitoredIndex = -1;
    }

    // Stop playback and save data when the application closes
    @Override
    public void stop() {

        stopPlaylistMonitor();

        for (Playlist playlist : playlists) {
            playlist.Stop_All_Songs();
        }

        try {
            PlaylistStorage.savePlaylists(playlists);
        } catch (IOException e) {
            System.err.println("Could not save MelodyNode playlists: " + e.getMessage());
        }
    }

    // Main method
    public static void main(String[] args) {
        launch(args);
    }
}
