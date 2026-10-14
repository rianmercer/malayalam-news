package com.rianmercer.malayalamnews;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable splashTimeout = this::hideSplash;

    private GeckoRuntime runtime;
    private GeckoSession session;
    private View splashView;
    private String currentUrl = START_URL;
    private boolean contentFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(11, 42, 74));
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setBackgroundDrawableResource(R.drawable.splash_screen);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(4, 13, 42));

        GeckoView geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.BLACK);
        root.addView(geckoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        splashView = createSplashView();
        root.addView(splashView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        mainHandler.postDelayed(splashTimeout, 20000);

        runtime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStop(GeckoSession stoppedSession, boolean success) {
                runOnUiThread(() -> hideSplash());
            }
        });

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

        // A blocker failure must not interrupt startup; keep diagnostics in Logcat.
        runtime.getWebExtensionController().ensureBuiltIn(
                UBLOCK_ASSET_PATH, UBLOCK_EXTENSION_ID
        ).accept(
                (WebExtension extension) -> {
                    Log.i(TAG, "uBlock Origin extension registered.");
                    runOnUiThread(() -> session.loadUri(START_URL));
                },
                error -> {
                    Log.e(TAG,
                            "Could not register uBlock Origin; opening without content blocking.",
                            error);
                    runOnUiThread(() -> session.loadUri(START_URL));
                }
        );
    }

    private View createSplashView() {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.rgb(4, 13, 42));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.malayalam_news_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAdjustViewBounds(true);
        logo.setContentDescription("Malayalam News");
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        int logoSizeDp = Math.min(360, Math.max(260, screenWidthDp - 40));
        splash.setPadding(dp(16), dp(16), dp(16), dp(16));
        splash.addView(logo, new LinearLayout.LayoutParams(dp(logoSizeDp), dp(logoSizeDp)));

        ProgressBar progress = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(dp(34), dp(34));
        progressParams.topMargin = dp(16);
        progress.setIndeterminateTintList(
                android.content.res.ColorStateList.valueOf(Color.rgb(255, 212, 0)));
        splash.addView(progress, progressParams);

        TextView label = new TextView(this);
        label.setText("വാർത്താ ചാനലുകൾ തുറക്കുന്നു…");
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(12);
        splash.addView(label, labelParams);
        return splash;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideSplash() {
        if (splashView == null) return;
        mainHandler.removeCallbacks(splashTimeout);
        View overlay = splashView;
        splashView = null;
        overlay.animate().alpha(0f).setDuration(220).withEndAction(() -> {
            if (overlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
        }).start();
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
        mainHandler.removeCallbacks(splashTimeout);
        if (session != null) {
            session.close();
            session = null;
        }
        runtime = null;
        super.onDestroy();
    }
}
