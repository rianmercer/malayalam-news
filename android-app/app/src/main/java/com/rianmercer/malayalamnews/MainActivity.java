package com.rianmercer.malayalamnews;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

public final class MainActivity extends Activity {
    private static final String TAG = "MalayalamNews";
    private static final String START_URL =
            "https://rianmercer.github.io/malayalam-news/?app=1";
    private static final String UBLOCK_ASSET_PATH =
            "resource://android/assets/ublock/";
    private static final String UBLOCK_EXTENSION_ID =
            "uBlock0@raymondhill.net";
    private static final long INTRO_MIN_MS = 1800L;
    private static final long INTRO_SAFETY_MS = 15000L;
    private static final int IMMERSIVE_FLAGS =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GeckoRuntime runtime;
    private GeckoSession session;
    private View splashView;
    private SplashBackdropView splashBackdrop;
    private TextView splashStatus;
    private Button splashRetry;
    private MediaPlayer startupPlayer;
    private ObjectAnimator logoPulseX;
    private ObjectAnimator logoPulseY;
    private ObjectAnimator livePulseAnimator;
    private boolean introTimeElapsed = false;
    private boolean introAudioFinished = false;
    private boolean pageReady = false;
    private boolean splashDismissed = false;
    private boolean contentFullscreen = false;
    private String currentUrl = START_URL;

    private final Runnable introMinimumTimer = () -> {
        introTimeElapsed = true;
        maybeDismissSplash();
    };

    private final Runnable splashSafetyTimer = () -> {
        if (splashDismissed) return;
        if (!pageReady) {
            if (splashStatus != null) {
                splashStatus.setText("ഇന്റർനെറ്റ് കണക്ഷൻ പരിശോധിച്ച് വീണ്ടും ശ്രമിക്കുക");
            }
            if (splashRetry != null) splashRetry.setVisibility(View.VISIBLE);
        }
    };

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

        splashView = createIntroSplash();
        root.addView(splashView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        startStartupJingle();
        mainHandler.postDelayed(introMinimumTimer, INTRO_MIN_MS);
        mainHandler.postDelayed(splashSafetyTimer, INTRO_SAFETY_MS);

        runtime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.open(runtime);
        geckoView.setSession(session);

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStop(GeckoSession stoppedSession, boolean success) {
                if (success) {
                    runOnUiThread(() -> {
                        pageReady = true;
                        if (splashRetry != null) splashRetry.setVisibility(View.GONE);
                        maybeDismissSplash();
                    });
                }
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

        // Open the dashboard underneath the intro straight away. The blocker
        // registers in parallel, before Pappa is likely to select a channel.
        session.loadUri(START_URL);
        runtime.getWebExtensionController().ensureBuiltIn(
                UBLOCK_ASSET_PATH, UBLOCK_EXTENSION_ID
        ).accept(
                (WebExtension extension) -> Log.i(TAG, "uBlock Origin registered."),
                error -> Log.e(TAG,
                        "Could not register uBlock Origin; continuing without content blocking.",
                        error)
        );
    }

    private View createIntroSplash() {
        FrameLayout splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.rgb(4, 13, 42));

        splashBackdrop = new SplashBackdropView();
        splash.addView(splashBackdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topLine = new LinearLayout(this);
        topLine.setOrientation(LinearLayout.HORIZONTAL);
        topLine.setGravity(Gravity.CENTER);
        TextView live = new TextView(this);
        live.setText("● LIVE");
        live.setTextColor(Color.WHITE);
        live.setTextSize(16);
        live.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        live.setPadding(dp(10), dp(6), dp(10), dp(6));
        android.graphics.drawable.GradientDrawable liveBg =
                new android.graphics.drawable.GradientDrawable();
        liveBg.setColor(Color.rgb(220, 22, 52));
        liveBg.setCornerRadius(dp(9));
        live.setBackground(liveBg);
        livePulseAnimator = ObjectAnimator.ofFloat(live, View.ALPHA, 0.72f, 1f);
        livePulseAnimator.setDuration(760L);
        livePulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        livePulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        livePulseAnimator.start();
        topLine.addView(live);

        TextView broadcast = new TextView(this);
        broadcast.setText("  പ്രക്ഷേപണം");
        broadcast.setTextColor(Color.rgb(232, 236, 246));
        broadcast.setTextSize(20);
        broadcast.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams broadcastParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        broadcastParams.leftMargin = dp(6);
        topLine.addView(broadcast, broadcastParams);

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        topParams.topMargin = dp(58);
        splash.addView(topLine, topParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.malayalam_news_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAdjustViewBounds(true);
        logo.setContentDescription("Malayalam News logo");
        logo.setAlpha(0f);
        logo.setScaleX(0.58f);
        logo.setScaleY(0.58f);
        logo.setElevation(dp(12));
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        int logoSizeDp = Math.min(330, Math.max(250, screenWidthDp - 48));
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
                dp(logoSizeDp), dp(logoSizeDp), Gravity.CENTER);
        splash.addView(logo, logoParams);
        logoPulseX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.975f, 1.025f);
        logoPulseY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.975f, 1.025f);
        for (ObjectAnimator animator : new ObjectAnimator[]{logoPulseX, logoPulseY}) {
            animator.setDuration(1050);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
        }
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(120L)
                .setDuration(1500L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (!splashDismissed) {
                        logoPulseX.start();
                        logoPulseY.start();
                    }
                })
                .start();

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER);

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(
                Color.rgb(255, 210, 112)));
        bottom.addView(progress, new LinearLayout.LayoutParams(dp(30), dp(30)));

        splashStatus = new TextView(this);
        splashStatus.setText("വാർത്തകൾ സജ്ജമാകുന്നു...");
        splashStatus.setTextColor(Color.rgb(246, 224, 177));
        splashStatus.setTextSize(17);
        splashStatus.setGravity(Gravity.CENTER);
        splashStatus.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(12);
        bottom.addView(splashStatus, statusParams);

        splashRetry = new Button(this);
        splashRetry.setText("വീണ്ടും ശ്രമിക്കുക");
        splashRetry.setTextSize(15);
        splashRetry.setVisibility(View.GONE);
        splashRetry.setOnClickListener(v -> retryDashboard());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryParams.topMargin = dp(8);
        bottom.addView(splashRetry, retryParams);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bottomParams.leftMargin = dp(18);
        bottomParams.rightMargin = dp(18);
        bottomParams.bottomMargin = dp(38);
        splash.addView(bottom, bottomParams);
        return splash;
    }

    private void startStartupJingle() {
        try {
            MediaPlayer player = new MediaPlayer();
            startupPlayer = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            AssetFileDescriptor asset = getResources().openRawResourceFd(
                    R.raw.startup_jingle);
            if (asset == null) {
                introAudioFinished = true;
                player.release();
                startupPlayer = null;
                maybeDismissSplash();
                return;
            }
            try {
                player.setDataSource(asset.getFileDescriptor(),
                        asset.getStartOffset(), asset.getLength());
            } finally {
                asset.close();
            }
            player.setOnPreparedListener(prepared -> {
                if (isFinishing() || isDestroyed() || startupPlayer != prepared) {
                    releaseStartupPlayer(prepared);
                    introAudioFinished = true;
                    maybeDismissSplash();
                    return;
                }
                prepared.setVolume(0f, 0f);
                prepared.start();
                fadeInAudio(prepared, SystemClock.uptimeMillis());
            });
            player.setOnCompletionListener(completed -> {
                releaseStartupPlayer(completed);
                introAudioFinished = true;
                maybeDismissSplash();
            });
            player.setOnErrorListener((failed, what, extra) -> {
                Log.w(TAG, "Startup jingle failed (" + what + ", " + extra + ").");
                releaseStartupPlayer(failed);
                introAudioFinished = true;
                maybeDismissSplash();
                return true;
            });
            player.prepareAsync();
        } catch (IOException | IllegalStateException ex) {
            Log.e(TAG, "Unable to prepare the startup jingle.", ex);
            if (startupPlayer != null) {
                try { startupPlayer.release(); } catch (Exception ignored) {}
                startupPlayer = null;
            }
            introAudioFinished = true;
            maybeDismissSplash();
        }
    }

    private void fadeInAudio(MediaPlayer player, long startMs) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if (startupPlayer != player || !isPlayerAlive(player)) return;
                float fraction = Math.min(1f,
                        (SystemClock.uptimeMillis() - startMs) / 320f);
                try {
                    player.setVolume(fraction, fraction);
                } catch (IllegalStateException ex) {
                    return;
                }
                if (fraction < 1f) mainHandler.postDelayed(this, 35L);
            }
        });
    }

    private boolean isPlayerAlive(MediaPlayer player) {
        try { return player.isPlaying(); }
        catch (IllegalStateException ex) { return false; }
    }

    private void releaseStartupPlayer(MediaPlayer player) {
        if (player == null) return;
        if (startupPlayer == player) startupPlayer = null;
        try { player.setOnCompletionListener(null); } catch (Exception ignored) {}
        try { player.setOnErrorListener(null); } catch (Exception ignored) {}
        try { player.stop(); } catch (Exception ignored) {}
        try { player.release(); } catch (Exception ignored) {}
    }

    private void maybeDismissSplash() {
        if (splashDismissed || !pageReady || !introTimeElapsed) return;
        splashDismissed = true;
        mainHandler.removeCallbacks(introMinimumTimer);
        mainHandler.removeCallbacks(splashSafetyTimer);
        View overlay = splashView;
        splashView = null;
        if (splashBackdrop != null) splashBackdrop.stop();
        if (logoPulseX != null) logoPulseX.cancel();
        if (logoPulseY != null) logoPulseY.cancel();
        if (livePulseAnimator != null) livePulseAnimator.cancel();
        if (overlay == null) return;
        overlay.animate().alpha(0f).setDuration(400).withEndAction(() -> {
            if (overlay.getParent() instanceof ViewGroup) {
                ((ViewGroup) overlay.getParent()).removeView(overlay);
            }
        }).start();
    }

    private void retryDashboard() {
        if (session == null) return;
        pageReady = false;
        if (splashRetry != null) splashRetry.setVisibility(View.GONE);
        if (splashStatus != null) splashStatus.setText("വാർത്തകൾ സജ്ജമാകുന്നു...");
        mainHandler.removeCallbacks(splashSafetyTimer);
        mainHandler.postDelayed(splashSafetyTimer, INTRO_SAFETY_MS);
        session.loadUri(START_URL + "&retry=" + System.currentTimeMillis());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void applyPlaybackOrientation(String url) {
        boolean landscape = contentFullscreen || isPlaybackRoute(url);
        int orientation = landscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if (getRequestedOrientation() != orientation) setRequestedOrientation(orientation);

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
        if (host.equals("youtube.com") || host.endsWith(".youtube.com")) return true;
        if (host.equals("manoramanews.com") || host.endsWith(".manoramanews.com")) return true;
        return (host.equals("janamtv.com") || host.endsWith(".janamtv.com"))
                && uri.getPath() != null
                && uri.getPath().toLowerCase(Locale.ROOT).contains("live");
    }

    private final class SplashBackdropView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long startAt = SystemClock.uptimeMillis();
        private boolean running = true;

        SplashBackdropView() {
            super(MainActivity.this);
            // Use the default hardware-accelerated canvas; a software canvas with multiple
            // shadowed rings caused visible startup jank on mid-range phones.
        }

        void stop() { running = false; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            float t = (SystemClock.uptimeMillis() - startAt) / 1000f;
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{Color.rgb(3, 10, 31), Color.rgb(7, 28, 65),
                            Color.rgb(19, 5, 35)},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(197, 157, 84));
            paint.setAlpha(190);
            canvas.drawRoundRect(new RectF(dp(15), dp(20), w-dp(15), h-dp(20)),
                    dp(20), dp(20), paint);

            float cx = w * 0.5f, cy = h * 0.49f;
            float maxRadius = Math.min(w * 0.49f, h * 0.35f);
            for (int i=0; i<5; i++) {
                float pulse = 1f + 0.025f*(float)Math.sin(t*2.2f-i*0.5f);
                float radius = maxRadius * (0.30f + i*0.105f) * pulse;
                boolean gold = i % 3 == 0;
                ringPaint.setStyle(Paint.Style.STROKE);
                ringPaint.setStrokeWidth(dp(i==0?2.4f:1.4f));
                int alpha = (int)(48 + 42*(0.5+0.5*Math.sin(t*2.0f+i)));
                ringPaint.setColor(gold
                        ? Color.argb(alpha, 235, 195, 112)
                        : Color.argb(alpha+35, 53, 163, 242));
                ringPaint.setShadowLayer(dp(5), 0, 0, gold
                        ? Color.argb(70, 255, 200, 80)
                        : Color.argb(70, 40, 170, 255));
                canvas.drawCircle(cx, cy, radius, ringPaint);
                ringPaint.clearShadowLayer();
            }

            particlePaint.setStyle(Paint.Style.FILL);
            for (int i=0; i<14; i++) {
                float x = w * (0.08f + ((i*37)%83)/100f);
                float y = h * (0.20f + ((i*23)%63)/100f)
                        + dp(5)*(float)Math.sin(t*1.5f+i);
                int alpha = (int)(65 + 150*(0.5+0.5*Math.sin(t*2.7f+i*1.7f)));
                particlePaint.setColor(i%4==0
                        ? Color.argb(alpha, 255, 214, 140)
                        : Color.argb(alpha, 106, 200, 255));
                canvas.drawCircle(x, y, dp(1.0f+(i%3)*0.5f), particlePaint);
            }

            notePaint.setColor(Color.rgb(255, 211, 122));
            notePaint.setTextSize(dp(32));
            notePaint.setTypeface(Typeface.create("serif", Typeface.BOLD));
            notePaint.setAlpha(210);
            canvas.drawText("♫", w*0.12f, h*0.70f + dp(5)*(float)Math.sin(t*1.7f), notePaint);
            canvas.drawText("♪", w*0.79f, h*0.34f + dp(7)*(float)Math.sin(t*1.3f+1f), notePaint);
            canvas.drawText("♫", w*0.78f, h*0.72f + dp(8)*(float)Math.sin(t*1.8f+2f), notePaint);

            paint.setStyle(Paint.Style.FILL);
            for (int i=0; i<9; i++) {
                float x = w*(0.27f+i*0.033f);
                float r = dp(1.6f + 1.2f*(0.5f+0.5f*(float)Math.sin(t*5f+i)));
                paint.setColor(Color.rgb(231, 185, 98));
                canvas.drawCircle(x, h*0.88f, r, paint);
            }
            if (running) postInvalidateDelayed(66L);
        }
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
        mainHandler.removeCallbacks(introMinimumTimer);
        mainHandler.removeCallbacks(splashSafetyTimer);
        if (startupPlayer != null) {
            releaseStartupPlayer(startupPlayer);
        }
        if (splashBackdrop != null) splashBackdrop.stop();
        if (logoPulseX != null) logoPulseX.cancel();
        if (logoPulseY != null) logoPulseY.cancel();
        if (livePulseAnimator != null) livePulseAnimator.cancel();
        if (session != null) {
            session.close();
            session = null;
        }
        runtime = null;
        super.onDestroy();
    }
}
