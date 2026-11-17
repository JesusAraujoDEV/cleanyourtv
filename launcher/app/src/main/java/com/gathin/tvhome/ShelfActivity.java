package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Film suggestions over Home, from files a companion app wrote through {@link ThemeProvider}: the launcher
 * stays offline. A pick opens its Netflix page; without an id it opens Netflix's search for the title.
 */
public class ShelfActivity extends Activity {
    private static final String NETFLIX = "com.netflix.ninja";
    private static final long SLIDE_MS = 420;

    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private JSONArray films = new JSONArray();
    private TextView title;
    private TextView reason;

    static File file(Context context) {
        return new File(Config.dir(context), ThemeFiles.SHELF + "/" + ThemeFiles.SHELF_LIST);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        overridePendingTransition(0, 0);
        try {
            films = new JSONArray(new String(Files.readAllBytes(file(this).toPath()), StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            finish();
            return;
        }
        if (films.length() == 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_shelf);
        title = findViewById(R.id.shelfTitle);
        reason = findViewById(R.id.shelfReason);
        ImageView logo = findViewById(R.id.shelfLogo);
        try {
            logo.setImageDrawable(getPackageManager().getApplicationBanner(NETFLIX));
        } catch (PackageManager.NameNotFoundException e) {
            finish();
            return;
        }
        RecyclerView row = findViewById(R.id.shelfRow);
        row.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        row.setAdapter(new Posters());
        row.post(() -> {
            RecyclerView.ViewHolder first = row.findViewHolderForAdapterPosition(0);
            if (first != null) first.itemView.requestFocus();
        });
        slideIn(logo, 180);
        slideIn(row, 90);
    }

    private void slideIn(View view, int fromDp) {
        view.setTranslationY(fromDp * getResources().getDisplayMetrics().density);
        view.setAlpha(0f);
        view.animate().translationY(0f).alpha(1f).setDuration(SLIDE_MS).start();
    }

    @Override
    protected void onDestroy() {
        decoder.shutdownNow();
        super.onDestroy();
    }

    private void show(JSONObject film) {
        title.setText(film.optString("title"));
        reason.setText(film.optString("reason") + availability(film));
    }

    private static String availability(JSONObject film) {
        JSONArray countries = film.optJSONArray("netflixCountries");
        String all = countries == null ? "" : countries.toString();
        if (!all.contains("VE")) return "  ·  Solo en Netflix USA (VPN)";
        return all.contains("US") ? "" : "  ·  Solo en Netflix Venezuela";
    }

    private void open(JSONObject film) {
        String id = film.optString("netflixId");
        String url = id.matches("\\d+") ? "http://www.netflix.com/title/" + id
                : "http://www.netflix.com/search?q=" + Uri.encode(film.optString("title"));
        // "source" is what makes Netflix honour the link instead of just opening its home screen.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setClassName(NETFLIX, NETFLIX + ".MainActivity")
                .putExtra("source", "30")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            return;
        }
        finish();
    }

    private final class Posters extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return films.length();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shelf, parent, false);
            view.setClipToOutline(true);
            return new RecyclerView.ViewHolder(view) { };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            JSONObject film = films.optJSONObject(position);
            ImageView poster = (ImageView) holder.itemView;
            poster.setImageBitmap(null);
            poster.setContentDescription(film.optString("title"));
            poster.setOnClickListener(v -> open(film));
            poster.setOnFocusChangeListener((v, focused) -> {
                v.animate().scaleX(focused ? 1.1f : 1f).scaleY(focused ? 1.1f : 1f).setDuration(120).start();
                if (focused) show(film);
            });
            long id = film.optLong("tmdbId");
            poster.setTag(id);
            decoder.execute(() -> {
                BitmapFactory.Options small = new BitmapFactory.Options();
                small.inPreferredConfig = Bitmap.Config.RGB_565;
                File dir = file(ShelfActivity.this).getParentFile();
                Bitmap picture = BitmapFactory.decodeFile(new File(dir, id + ".jpg").getPath(), small);
                poster.post(() -> {
                    if (Long.valueOf(id).equals(poster.getTag())) poster.setImageBitmap(picture);
                });
            });
        }
    }
}
