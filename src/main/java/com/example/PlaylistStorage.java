package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PlaylistStorage {

    private static final Path STORAGE_DIRECTORY = Paths.get(
        System.getProperty("user.home"), ".melodynode"
    );

    private static final Path STORAGE_FILE =
        STORAGE_DIRECTORY.resolve("playlists.dat");

    // Load playlist names and their song file paths from local storage
    public static List<Playlist> loadPlaylists() throws IOException {

        List<Playlist> savedPlaylists = new ArrayList<>();

        if (!Files.exists(STORAGE_FILE)) {
            return savedPlaylists;
        }

        Playlist currentPlaylist = null;

        try (BufferedReader reader = Files.newBufferedReader(
            STORAGE_FILE, StandardCharsets.UTF_8
        )) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts = line.split("\\t", -1);

                try {
                    if (parts.length == 2 && parts[0].equals("P")) {

                        currentPlaylist = null;
                        String playlistName = decode(parts[1]);
                        Playlist playlist = new Playlist();
                        playlist.Create_Playlist(playlistName);

                        if (!playlist.Name.equals("Not Assigned")) {
                            savedPlaylists.add(playlist);
                            currentPlaylist = playlist;
                        } else {
                            currentPlaylist = null;
                        }

                    } else if (parts.length == 3 && parts[0].equals("S")
                        && currentPlaylist != null) {

                        String songName = decode(parts[1]);
                        String songPath = decode(parts[2]);

                        if (!songName.isEmpty() && !songPath.isEmpty()) {
                            currentPlaylist.Add_Song(songName, songPath);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore a damaged storage record and continue loading others.
                }
            }
        }

        return savedPlaylists;
    }

    // Save all playlists and song paths to local storage
    public static void savePlaylists(List<Playlist> playlists) throws IOException {

        Files.createDirectories(STORAGE_DIRECTORY);

        Path temporaryFile = STORAGE_DIRECTORY.resolve("playlists.tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(
            temporaryFile, StandardCharsets.UTF_8
        )) {

            writer.write("# MelodyNode playlist storage v1");
            writer.newLine();

            for (Playlist playlist : playlists) {

                if (playlist == null || playlist.Name == null
                    || playlist.Name.trim().isEmpty()
                    || playlist.Name.equals("Not Assigned")) {
                    continue;
                }

                writer.write("P\t" + encode(playlist.Name));
                writer.newLine();

                Song song = playlist.begin;

                while (song != null) {
                    writer.write(
                        "S\t" + encode(song.name) + "\t" + encode(song.filePath)
                    );
                    writer.newLine();
                    song = song.next;
                }
            }
        }

        try {
            Files.move(
                temporaryFile,
                STORAGE_FILE,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                temporaryFile,
                STORAGE_FILE,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    // The file is stored in the current Windows user's home folder
    public static Path getStorageFile() {
        return STORAGE_FILE;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
            value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String decode(String value) {
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }
}
