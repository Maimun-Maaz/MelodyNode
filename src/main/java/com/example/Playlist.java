package com.example;

import java.util.function.Consumer;

public class Playlist {

    String Name = "Not Assigned";

    Song begin = null;
    Song end = null;
    Song current = null;

    int count = 0;

    // Create Playlist
    public String Create_Playlist(String a) {

        if (a == null || a.trim().isEmpty()) {
            return "Please enter a playlist name!";
        }

        Name = a.trim();

        return "Playlist successfully created: " + Name;
    }

    // Add Song
    public String Add_Song(String a, String b) {

        Song tempo = new Song(a, b);

        if (begin == null) {

            begin = tempo;
            end = tempo;
            current = tempo;

        } else {

            end.next = tempo;
            tempo.prev = end;
            end = tempo;
        }

        count++;

        return "Song successfully added to the playlist!";
    }

    // Delete Song by Name
    public String Delete_Song(String a) {

        if (begin == null) {
            return "Sorry, playlist is empty!";
        }

        Song temp = begin;

        while (temp != null) {

            if (temp.name.equals(a)) {
                return Delete_Node(temp);
            }

            temp = temp.next;
        }

        return "Song not found!";
    }

    // Delete Song by Index
    public String Delete_Song(int index) {

        if (index < 0 || index >= count) {
            return "Invalid song selection!";
        }

        Song temp = begin;

        for (int i = 0; i < index; i++) {
            temp = temp.next;
        }

        return Delete_Node(temp);
    }

    // Delete Node
    private String Delete_Node(Song target) {

        Stop_All_Songs();

        Song nextSong = target.next;
        Song previousSong = target.prev;

        if (target == current) {

            if (nextSong != null) {
                current = nextSong;
            } else {
                current = previousSong;
            }
        }

        if (target.prev != null) {
            target.prev.next = target.next;
        } else {
            begin = target.next;
        }

        if (target.next != null) {
            target.next.prev = target.prev;
        } else {
            end = target.prev;
        }

        target.next = null;
        target.prev = null;

        count--;

        if (begin == null) {
            current = null;
        } else if (current == null) {
            current = begin;
        }

        return "Song removed successfully!";
    }

    // Playlist Details
    public void Play_List_Details() {

        System.out.println("Playlist: " + Name);
        System.out.println("Length: " + count);
    }

    // Play Current Song
    public String Play_Current_Song() {
        return Play_Current_Song(null);
    }

    public String Play_Current_Song(Consumer<String> onError) {

        if (current == null) {
            return "Playlist is empty!";
        }

        Stop_All_Songs();

        boolean started = current.Play_Song(null, onError);

        if (!started) {
            return null;
        }

        return "Now playing: " + current.name;
    }

    // Stop Current Song
    public String Stop_Current_Song() {

        Stop_All_Songs();

        return "Playback stopped!";
    }

    // Play Next Song
    public String Play_Next_Song() {
        return Play_Next_Song(null);
    }

    public String Play_Next_Song(Consumer<String> onError) {

        if (begin == null) {
            return "Playlist is empty!";
        }

        if (current == null) {

            current = begin;

        } else if (current.next != null) {

            current = current.next;

        } else {

            return "Reached the end of the playlist!";
        }

        return Play_Current_Song(onError);
    }

    // Play Entire Playlist
    public String Play_the_Playlist() {
        return Play_the_Playlist(null);
    }

    public String Play_the_Playlist(Consumer<String> onError) {

        if (begin == null) {
            return "Playlist is empty!";
        }

        Stop_All_Songs();

        current = begin;

        if (!Play_Next_Automatically(onError)) {
            return null;
        }

        return "Playing playlist: " + Name;
    }

    // Automatically Play Next Song
    private boolean Play_Next_Automatically(
        Consumer<String> onError
    ) {

        if (current == null) {
            return false;
        }

        Song playing = current;

        return playing.Play_Song(() -> {

            if (current == playing) {

                current = playing.next;

                if (current != null) {
                    Play_Next_Automatically(onError);
                }
            }

        }, onError);
    }

    // Arrange Two Songs
    public String Arrange_Playlist(Song a, Song b) {

        if (a == null || b == null || a == b) {
            return "Invalid song selection!";
        }

        if (!Contains(a) || !Contains(b)) {
            return "Sorry, song not found!";
        }

        if (a.next == b) {

            Swap_Adjacent(a, b);

        } else if (b.next == a) {

            Swap_Adjacent(b, a);

        } else {

            Song aPrev = a.prev;
            Song aNext = a.next;

            Song bPrev = b.prev;
            Song bNext = b.next;

            if (aPrev != null) {
                aPrev.next = b;
            } else {
                begin = b;
            }

            if (aNext != null) {
                aNext.prev = b;
            } else {
                end = b;
            }

            if (bPrev != null) {
                bPrev.next = a;
            } else {
                begin = a;
            }

            if (bNext != null) {
                bNext.prev = a;
            } else {
                end = a;
            }

            a.prev = bPrev;
            a.next = bNext;

            b.prev = aPrev;
            b.next = aNext;
        }

        return "Songs changed successfully!";
    }

    // Swap Adjacent Nodes
    private void Swap_Adjacent(Song first, Song second) {

        Song before = first.prev;
        Song after = second.next;

        if (before != null) {
            before.next = second;
        } else {
            begin = second;
        }

        if (after != null) {
            after.prev = first;
        } else {
            end = first;
        }

        second.prev = before;
        second.next = first;

        first.prev = second;
        first.next = after;
    }

    // Check Whether Song Exists
    private boolean Contains(Song a) {

        Song temp = begin;

        while (temp != null) {

            if (temp == a) {
                return true;
            }

            temp = temp.next;
        }

        return false;
    }

    // Sort Playlist Alphabetically
    public String Sort_Playlist_Alphabetically() {

        if (begin == null) {
            return "Playlist is empty. Nothing to sort.";
        }

        boolean swapped;

        do {

            swapped = false;

            Song ptr = begin;

            while (ptr != null && ptr.next != null) {

                if (ptr.name.compareToIgnoreCase(ptr.next.name) > 0) {

                    Swap_Adjacent(ptr, ptr.next);

                    swapped = true;

                } else {

                    ptr = ptr.next;
                }
            }

        } while (swapped);

        return "Playlist sorted alphabetically successfully!";
    }

    // Rename Playlist
    public String Rename_Playlist(String newName) {

        if (newName == null || newName.trim().isEmpty()) {
            return "Please enter a playlist name!";
        }

        Name = newName.trim();

        return "Playlist renamed to: " + Name;
    }

    // Delete Entire Playlist
    public String Delete_Playlist() {

        Stop_All_Songs();

        begin = null;
        end = null;
        current = null;

        count = 0;

        Name = "Not Assigned";

        return "Playlist deleted!";
    }

    // Stop All Songs
    public void Stop_All_Songs() {

        Song temp = begin;

        while (temp != null) {

            temp.Stop_Song();

            temp = temp.next;
        }
    }

    // Set Current Song by Index
    public boolean Set_Current_Song(int index) {

        if (index < 0 || index >= count) {
            return false;
        }

        Song temp = begin;

        for (int i = 0; i < index; i++) {
            temp = temp.next;
        }

        current = temp;

        return true;
    }

    // Find Current Song Index
    public int Get_Current_Index() {

        int index = 0;

        Song temp = begin;

        while (temp != null) {

            if (temp == current) {
                return index;
            }

            temp = temp.next;
            index++;
        }

        return -1;
    }
}