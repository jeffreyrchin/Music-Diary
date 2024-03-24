package com.example.musicdiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchMusicActivity extends AppCompatActivity {
    private EditText editTextPlaylistId;
    private String accessToken;
    private OkHttpClient client;
    private FragmentManager fragmentManager;
    private ArrayList<TrackItem> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_music);

        accessToken = MainActivity.accessToken;
        client = new OkHttpClient();

        editTextPlaylistId = findViewById(R.id.editTextPlaylistId);
        editTextPlaylistId.setOnKeyListener(this::onClickEditTextPlaylistId);
        Button buttonSearchForPlaylist = findViewById(R.id.buttonPlaylistSearch);
        buttonSearchForPlaylist.setOnClickListener(view -> onClickSearchForPlaylist());

        fragmentManager = getSupportFragmentManager();
        searchResults = new ArrayList<>();
    }

    private boolean onClickEditTextPlaylistId(View view, int keyCode, KeyEvent keyEvent) {
        if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
            // 3cEYpjA9oz9GiPac4AsH4n
            InputMethodManager inputMethodManager = (InputMethodManager)getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(editTextPlaylistId.getWindowToken(), 0);

            editTextPlaylistId.clearFocus();

            return true;
        }

        return false;
    }

    private void onClickSearchForPlaylist() {
        String playlistId = editTextPlaylistId.getText().toString();
        if (playlistId.equals("")) {
            Toast toast = Toast.makeText(this, "Please enter a playlist id to search for!", Toast.LENGTH_SHORT);
            toast.show();

            return;
        }

        getPlaylistData(playlistId);
    }

    private void getPlaylistData(String playlistID) {
        Request request = new Request.Builder().url("https://api.spotify.com/v1/playlists/" + playlistID)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject playlistData = new JSONObject(responseString);
                        JSONObject tracks = playlistData.getJSONObject("tracks");
                        JSONArray items = tracks.getJSONArray("items");

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            JSONObject track = item.getJSONObject("track");
                            String trackName = track.getString("name");
                            JSONArray trackArtists = track.getJSONArray("artists");

                            StringBuilder artists = new StringBuilder();

                            for (int j = 0; j < trackArtists.length(); j++) {
                                JSONObject artist = trackArtists.getJSONObject(j);
                                String artistName = artist.getString("name");
                                artists.append(artistName).append(", ");
                            }

                            searchResults.add(new TrackItem(trackName, artists.toString()));
                        }

                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        TracklistFragment tracklistFragment = TracklistFragment.newInstance(searchResults);
                        transaction.replace(R.id.fragmentContainerView, tracklistFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }
        });
    }
}