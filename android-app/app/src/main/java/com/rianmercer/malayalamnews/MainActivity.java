package com.rianmercer.malayalamnews;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

public final class MainActivity extends Activity {
    private static final String TAG = "MalayalamNews";
    private static final String START_URL =
            "https://rianmercer.github.io/malayalam-news/";
    private static final String UBLOCK_ASSET_PATH =
            "resource://android/assets/ublock/";
    private static final String UBLOCK_EXTENSION_ID =
            "uBlock0@raymondhill.net";

    private GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(11, 42, 74));
        getWindow().setNavigationBarColor(Color.BLACK);

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.BLACK);
        setContentView(geckoView);

        runtime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        // Load the channel-selection page after registering the bundled extension.
        // If the blocker fails to load, the channel app still opens normally.
        runtime.getWebExtensionController().ensureBuiltIn(
                UBLOCK_ASSET_PATH, UBLOCK_EXTENSION_ID
        ).accept(
                (WebExtension extension) -> {
                    Log.i(TAG, "uBlock Origin extension registered.");
                    runOnUiThread(() -> session.loadUri(START_URL));
                },
                error -> {
                    Log.e(TAG, "Could not register uBlock Origin extension.", error);
                    runOnUiThread(() -> {
                        Toast.makeText(
                                this,
                                "Content blocker could not start; opening Malayalam News.",
                                Toast.LENGTH_LONG
                        ).show();
                        session.loadUri(START_URL);
                    });
                }
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Browser history returns from a broadcaster page to the channel grid.
        if (session != null) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
