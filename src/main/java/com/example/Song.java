package com.example;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

public class Song {

    public String name;
    public String filePath;
    public Song next;
    public Song prev;
    public float duration;

    private MediaPlayer mediaPlayer;

    public Song(String a, String b) {
        name = a;
        filePath = b;
    }

    public void Play_Song() {
        Play_Song(null, null);
    }

    public void Play_Song(Runnable onFinished) {
        Play_Song(onFinished, null);
    }

    public boolean Play_Song(
        Runnable onFinished,
        Consumer<String> onError
    ) {

        Stop_Song();

        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {

            sendError(
                "File not found: " + name,
                onError
            );

            return false;
        }

        if (!hasSupportedExtension(file)) {

            sendError(
                "Unsupported file type: " + name +
                ". Use MP3, WAV, AIFF, or M4A.",
                onError
            );

            return false;
        }

        try {

            Media media = new Media(file.toURI().toString());

            MediaPlayer player = new MediaPlayer(media);

            mediaPlayer = player;

            boolean[] errorReported = {false};

            media.setOnError(() -> {

                String details = media.getError() == null
                    ? null
                    : media.getError().getMessage();

                reportPlaybackError(
                    player,
                    details,
                    onError,
                    errorReported
                );
            });

            player.setOnError(() -> {

                String details = player.getError() == null
                    ? null
                    : player.getError().getMessage();

                reportPlaybackError(
                    player,
                    details,
                    onError,
                    errorReported
                );
            });

            player.setOnReady(() -> {

                duration = (float) player
                    .getTotalDuration()
                    .toSeconds();
            });

            player.setOnEndOfMedia(() -> {

                if (mediaPlayer == player) {
                    mediaPlayer = null;
                }

                player.dispose();

                if (onFinished != null) {
                    onFinished.run();
                }
            });

            player.play();

            System.out.println("Now playing: " + name);

            return true;

        } catch (Exception e) {

            Stop_Song();

            String message =
                "Unable to play: " + name +
                ". The file may be damaged or use an unsupported codec.";

            if (e.getMessage() != null) {
                message += " Details: " + e.getMessage();
            }

            sendError(message, onError);

            return false;
        }
    }

    private boolean hasSupportedExtension(File file) {

        String fileName = file.getName().toLowerCase(Locale.ROOT);

        return fileName.endsWith(".mp3")
            || fileName.endsWith(".wav")
            || fileName.endsWith(".aif")
            || fileName.endsWith(".aiff")
            || fileName.endsWith(".m4a");
    }

    private void reportPlaybackError(
        MediaPlayer player,
        String details,
        Consumer<String> onError,
        boolean[] errorReported
    ) {

        if (errorReported[0]) {
            return;
        }

        errorReported[0] = true;

        if (mediaPlayer == player) {
            mediaPlayer = null;
        }

        player.dispose();

        String message =
            "Unable to play \"" + name + "\". " +
            "The file may be damaged or use an audio codec " +
            "that JavaFX does not support.";

        if (details != null && !details.trim().isEmpty()) {
            message += " Details: " + details;
        }

        sendError(message, onError);
    }

    private void sendError(
        String message,
        Consumer<String> onError
    ) {

        if (onError != null) {
            onError.accept(message);
        } else {
            System.out.println(message);
        }
    }

    public void Stop_Song() {

        if (mediaPlayer != null) {

            MediaPlayer player = mediaPlayer;

            mediaPlayer = null;

            player.stop();
            player.dispose();
        }
    }
}