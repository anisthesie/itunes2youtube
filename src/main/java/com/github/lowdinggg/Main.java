package com.github.lowdinggg;

import com.github.lowdinggg.media.Song;
import com.github.lowdinggg.youtube.Auth;
import com.github.lowdinggg.youtube.PlaylistUtil;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String DELIMITER = "\t";

    private static YouTube youtube;

    public static String PLAYLIST_ID;

    private static final long NUMBER_OF_VIDEOS_RETURNED = 1;


    public static void main(String[] args) {

        final String path = args[0];
        final ArrayList<Song> songs = getSongs(path);

        youtube = Auth.initYoutube();

        for (final Song song : songs) {
            final String videoId = getVideoId(song.getArtist() + " " + song.getTitle());
            try {
                PlaylistUtil.insertPlaylistItem(PLAYLIST_ID, videoId, youtube);
                System.out.println("Added video id #" + videoId + " for " + song.getTitle());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private static String getVideoId(final String queryTerm) {

        try {

            YouTube.Search.List search = youtube.search().list("id,snippet");


            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = Auth.properties.getProperty("youtube.apikey");
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null && searchResultList.size() > 0) {
                SearchResult result = searchResultList.get(0);
                ResourceId resourceId = result.getId();
                return resourceId.getVideoId();
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
             + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;

    }

    private static ArrayList<Song> getSongs(final String filePath) {

        final File file = new File(filePath);
        Scanner scanner = null;

        try {
            scanner = new Scanner(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.out.println("File " + filePath + " not found");
            System.exit(-1);
        }

        final ArrayList<Song> songs = new ArrayList<Song>();

        boolean firstIteration = true;

        while (scanner.hasNextLine()) {

            if (firstIteration){
                firstIteration = false;
                scanner.nextLine();
            }

            final String[] arr = scanner.nextLine().split(DELIMITER);
            final String title = arr[0];
            final String artist = arr[1];

            songs.add(new Song(title, artist));
        }
        return songs;
    }

}
