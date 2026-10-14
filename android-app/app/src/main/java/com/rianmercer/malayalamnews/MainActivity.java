package com.rianmercer.malayalamnews;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

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

    private static final int IMMERSIVE_FLAGS =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    private GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;
    private String currentUrl = START_URL;
    private boolean contentFullscreen = false;

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

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(
                    GeckoSession changedSession,
                    String url,
                    List<GeckoSession.PermissionDelegate.ContentPermission> permissions,
                    Boolean hasUserGesture) {
                currentUrl = url == null ? START_URL : url;
                applyPlaybackOrientation(currentUrl);
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession changedSession, boolean fullScreen) {
                contentFullscreen = fullScreen;
                applyPlaybackOrientation(currentUrl);
            }
        });

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

    private void applyPlaybackOrientation(String url) {
        boolean landscape = contentFullscreen || isPlaybackRoute(url);
        int orientation = landscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (getRequestedOrientation() != orientation) {
            setRequestedOrientation(orientation);
        }

        View decor = getWindow().getDecorView();
        if (landscape) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decor.setSystemUiVisibility(IMMERSIVE_FLAGS);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setStatusBarColor(Color.rgb(11, 42, 74));
            getWindow().setNavigationBarColor(Color.BLACK);
        }
    }

    private boolean isPlaybackRoute(String url) {
        if (url == null) return false;

        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("#playing=")) return true;

        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);

        // These channels open the broadcaster's own page rather than an embed.
        // Keep that page in landscape until the user returns to the channel grid.
        if (host.equals("youtube.com") || host.endsWith(".youtube.com")) {
            return true;
        }
        if (host.equals("manoramanews.com") || host.endsWith(".manoramanews.com")) {
            return true;
        }
        return (host.equals("janamtv.com") || host.endsWith(".janamtv.com"))
                && uri.getPath() != null
                && uri.getPath().toLowerCase(Locale.ROOT).contains("live");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (contentFullscreen && session != null) {
            session.exitFullScreen();
            return;
        }
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
        if (runtime != null) {
            runtime = null;
        }
        super.onDestroy();
    }
}
