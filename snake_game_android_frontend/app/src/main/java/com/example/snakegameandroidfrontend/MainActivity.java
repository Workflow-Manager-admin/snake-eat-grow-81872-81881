package com.example.snakegameandroidfrontend;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * MainActivity: Entry point for the Snake game.
 * Features: Dark theme, responsive modern UI, classic snake gameplay, touch controls, score display, sound toggle.
 */
public class MainActivity extends AppCompatActivity {
    private SnakeGameView gameView;
    private TextView scoreText;
    private LinearLayout controlsLayout;
    private ImageButton btnUp, btnDown, btnLeft, btnRight;
    private ImageButton btnSettings;
    private SoundPool soundPool;
    private int soundEat, soundGameOver;
    private boolean isSoundOn;
    private SharedPreferences prefs;

    // PUBLIC_INTERFACE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set dark theme background
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#121212"));
        prefs = getSharedPreferences("snake_prefs", MODE_PRIVATE);
        isSoundOn = prefs.getBoolean("sound_on", true);

        // UI: Score bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(30, 56, 30, 20);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#212121"));
        topBar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        scoreText = new TextView(this);
        scoreText.setText("Score: 0");
        scoreText.setTextColor(Color.parseColor("#fbc02d"));
        scoreText.setTextSize(20f);
        scoreText.setGravity(Gravity.CENTER_VERTICAL);

        btnSettings = new ImageButton(this);
        btnSettings.setImageResource(android.R.drawable.ic_menu_preferences);
        btnSettings.setBackgroundColor(Color.TRANSPARENT);
        btnSettings.setColorFilter(Color.parseColor("#fbc02d"));

        LinearLayout.LayoutParams spacer = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);

        topBar.addView(scoreText);
        topBar.addView(new View(this), spacer);
        topBar.addView(btnSettings);

        // UI: Game area
        gameView = new SnakeGameView(this, new SnakeGameView.GameListener() {
            @Override
            public void onScoreChanged(final int score) {
                runOnUiThread(() -> scoreText.setText("Score: " + score));
                if (isSoundOn) playEat();
            }

            @Override
            public void onGameOver(final int score) {
                runOnUiThread(() -> {
                    if (isSoundOn) playGameOver();
                    showGameOverDialog(score);
                });
            }
        });
        FrameLayout.LayoutParams gameParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        gameParams.topMargin = (int) dp(68);
        gameParams.bottomMargin = (int) dp(128);

        // UI: Controls area (bottom)
        controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);
        controlsLayout.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams controlsParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        controlsParams.gravity = Gravity.BOTTOM;
        controlsParams.bottomMargin = (int) dp(24);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        btnUp = makeArrowButton(android.R.drawable.arrow_up_float);

        row1.addView(btnUp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        btnLeft = makeArrowButton(R.drawable.ic_arrow_left);
        btnDown = makeArrowButton(android.R.drawable.arrow_down_float);
        btnRight = makeArrowButton(R.drawable.ic_arrow_right);

        row2.addView(btnLeft);
        row2.addView(btnDown);
        row2.addView(btnRight);

        controlsLayout.addView(row1);
        controlsLayout.addView(row2);

        // Listeners: Touch controls
        btnUp.setOnClickListener(v -> gameView.setDirection(SnakeGameView.Direction.UP));
        btnDown.setOnClickListener(v -> gameView.setDirection(SnakeGameView.Direction.DOWN));
        btnLeft.setOnClickListener(v -> gameView.setDirection(SnakeGameView.Direction.LEFT));
        btnRight.setOnClickListener(v -> gameView.setDirection(SnakeGameView.Direction.RIGHT));

        // Sound
        initSound();

        // Settings dialog
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        // Layout composition
        root.addView(gameView, gameParams);
        root.addView(topBar);
        root.addView(controlsLayout, controlsParams);

        setContentView(root);
    }

    private void showGameOverDialog(final int score) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogDark);
        builder.setTitle("Game Over")
                .setMessage("Your Score: " + score)
                .setCancelable(false)
                .setPositiveButton("Restart", (dialog, which) -> {
                    gameView.restartGame();
                    dialog.dismiss();
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    private void showSettingsDialog() {
        SwitchCompat soundSwitch = new SwitchCompat(this);
        soundSwitch.setChecked(isSoundOn);
        soundSwitch.setText("Sound");
        soundSwitch.setTextColor(Color.WHITE);
        soundSwitch.setPadding(16, 24, 16, 24);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogDark);
        builder.setTitle("Settings")
                .setView(soundSwitch)
                .setPositiveButton("OK", (d, w) -> {
                    isSoundOn = soundSwitch.isChecked();
                    prefs.edit().putBoolean("sound_on", isSoundOn).apply();
                })
                .show();
    }

    // Modern look for arrow buttons
    private ImageButton makeArrowButton(int drawableRes) {
        ImageButton btn = new ImageButton(this);
        btn.setImageResource(drawableRes);
        btn.setBackgroundResource(R.drawable.round_button_snake);
        btn.setColorFilter(Color.parseColor("#fbc02d"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) dp(56), (int) dp(56));
        p.setMargins(16, 16, 16, 16);
        btn.setLayoutParams(p);
        btn.setScaleType(ImageView.ScaleType.CENTER);
        return btn;
    }

    private float dp(int dp) { return getResources().getDisplayMetrics().density * dp; }

    /**
     * Initialize game sounds. If sound resources are missing (expected .wav files), disables sound gracefully.
     * Note: To enable game sound, replace 'res/raw/snake_eat.txt' and 'res/raw/snake_game_over.txt' with
     * valid short .wav files named respectively.
     */
    private void initSound() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setAudioAttributes(attrs).setMaxStreams(2).build();
            soundEat = soundPool.load(this, R.raw.snake_eat, 1);
            soundGameOver = soundPool.load(this, R.raw.snake_game_over, 1);
        } catch (Exception e) {
            // Resource missing - disable sound, but don't crash app
            soundPool = null;
            isSoundOn = false;
        }
    }

    private void playEat() { if (soundPool!=null) soundPool.play(soundEat,1,1,0,0,1); }
    private void playGameOver() { if (soundPool!=null) soundPool.play(soundGameOver,1,1,0,0,1); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) soundPool.release();
    }
}
