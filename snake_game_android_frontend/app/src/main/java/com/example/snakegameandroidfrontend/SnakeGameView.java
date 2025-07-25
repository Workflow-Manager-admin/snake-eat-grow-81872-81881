package com.example.snakegameandroidfrontend;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;
import java.util.Random;

/**
 * SnakeGameView - Custom View to render and run the classic snake game.
 */
public class SnakeGameView extends View {
    public interface GameListener {
        void onScoreChanged(int score);
        void onGameOver(int score);
    }

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final int gridSize = 20; // 20x20 blocks
    private int cellSize, offsetX, offsetY;
    private final Handler handler = new Handler();
    private final int frameDelay = 130; // milliseconds per tick
    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private final LinkedList<Point> snake = new LinkedList<>();
    private Point food;
    private int score = 0;
    private boolean running = true, showGameOver = false;
    private boolean hasMovedThisFrame = false;
    private final Paint snakePaint, headPaint, foodPaint, boardPaint, borderPaint, shadowPaint, textPaint;
    private final Random random = new Random();
    private final GameListener listener;

    public SnakeGameView(Context ctx, GameListener l) {
        super(ctx);
        this.listener = l;

        // Color setup (dark + accent/primary/secondary)
        snakePaint = new Paint(); snakePaint.setColor(Color.parseColor("#388e3c"));
        headPaint = new Paint(); headPaint.setColor(Color.parseColor("#fbc02d"));
        foodPaint = new Paint(); foodPaint.setColor(Color.parseColor("#d32f2f"));
        boardPaint = new Paint(); boardPaint.setColor(Color.parseColor("#161616"));
        borderPaint = new Paint(); borderPaint.setColor(Color.parseColor("#333333"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);

        shadowPaint = new Paint(); 
        shadowPaint.setColor(Color.parseColor("#222222"));
        shadowPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#fbc02d"));
        textPaint.setTextSize(64f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        restartGame();
    }

    // PUBLIC_INTERFACE
    public void restartGame() {
        snake.clear();
        snake.add(new Point(gridSize/2, gridSize/2));
        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        score = 0;
        running = true;
        showGameOver = false;
        placeFood();
        handler.removeCallbacks(gameRunnable);
        handler.postDelayed(gameRunnable, frameDelay);
        if (listener != null) listener.onScoreChanged(score);
        invalidate();
    }

    /** PUBLIC_INTERFACE - Set intended direction from input (used by activity for arrow buttons) */
    public void setDirection(Direction dir) {
        // Prevent direct reversal & avoid multiple moves per frame
        if (hasMovedThisFrame) return;
        if ((direction == Direction.UP && dir == Direction.DOWN) ||
            (direction == Direction.DOWN && dir == Direction.UP) ||
            (direction == Direction.LEFT && dir == Direction.RIGHT) ||
            (direction == Direction.RIGHT && dir == Direction.LEFT)) {
            // illegal turn
            return;
        }
        if (direction != dir) {
            nextDirection = dir;
            hasMovedThisFrame = true;
        }
    }

    private void placeFood() {
        do {
            food = new Point(random.nextInt(gridSize), random.nextInt(gridSize));
        } while (snake.contains(food));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int minSize = Math.min(w, h);
        cellSize = minSize / gridSize;
        offsetX = (w - cellSize * gridSize) / 2;
        offsetY = (h - cellSize * gridSize) / 2;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    // Game logic loop
    private final Runnable gameRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                moveSnake();
                invalidate();
                handler.postDelayed(this, frameDelay);
            }
        }
    };

    private void moveSnake() {
        direction = nextDirection;
        hasMovedThisFrame = false;

        Point head = snake.getFirst();
        Point next = head.move(direction);

        // Check for collisions (walls or self)
        if (next.x < 0 || next.y < 0 || next.x >= gridSize || next.y >= gridSize || snake.contains(next)) {
            running = false;
            showGameOver = true;
            handler.removeCallbacks(gameRunnable);
            if (listener != null) listener.onGameOver(score);
            invalidate();
            return;
        }
        snake.addFirst(next);

        if (next.equals(food)) {
            score++;
            if (listener != null) listener.onScoreChanged(score);
            placeFood();
        } else {
            snake.removeLast();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Board background + border
        canvas.drawRect(offsetX, offsetY, offsetX + cellSize * gridSize, offsetY + cellSize * gridSize, boardPaint);
        canvas.drawRect(offsetX, offsetY, offsetX + cellSize * gridSize, offsetY + cellSize * gridSize, borderPaint);

        // Draw snake shadow
        for (int i = snake.size()-1; i>=0; --i) {
            Point p = snake.get(i);
            RectF r = new RectF(offsetX + p.x*cellSize+cellSize*0.15f,
                                offsetY + p.y*cellSize+cellSize*0.15f,
                                offsetX + (p.x+1)*cellSize-cellSize*0.1f,
                                offsetY + (p.y+1)*cellSize-cellSize*0.1f);
            canvas.drawRoundRect(r, cellSize*0.25f, cellSize*0.25f, shadowPaint);
        }
        // Draw snake
        boolean isHead = true;
        for (Point p : snake) {
            RectF r = new RectF(offsetX + p.x*cellSize+cellSize*0.1f,
                                offsetY + p.y*cellSize+cellSize*0.1f,
                                offsetX + (p.x+1)*cellSize-cellSize*0.1f,
                                offsetY + (p.y+1)*cellSize-cellSize*0.1f);
            canvas.drawRoundRect(r, cellSize*0.25f, cellSize*0.25f, isHead ? headPaint : snakePaint);
            isHead = false;
        }
        // Draw food
        RectF rf = new RectF(offsetX + food.x*cellSize+cellSize*0.18f,
                             offsetY + food.y*cellSize+cellSize*0.18f,
                             offsetX + (food.x+1)*cellSize-cellSize*0.18f,
                             offsetY + (food.y+1)*cellSize-cellSize*0.18f);
        canvas.drawOval(rf, foodPaint);

        // Draw Game Over text
        if (showGameOver) {
            canvas.drawText("GAME OVER", getWidth()/2f, getHeight()/2f, textPaint);
        }
    }

    // Touch: Swipe gesture alternative for arrow controls, for modern touch devices
    private float startX, startY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!running) return super.onTouchEvent(event);
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX(); startY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - startX, dy = event.getY() - startY;
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 60) setDirection(Direction.RIGHT);
                    else if (dx < -60) setDirection(Direction.LEFT);
                } else {
                    if (dy > 60) setDirection(Direction.DOWN);
                    else if (dy < -60) setDirection(Direction.UP);
                }
                break;
        }
        return true;
    }

    // Simple point struct for board
    private static class Point {
        public final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        Point move(Direction dir) {
            switch(dir) {
                case UP: return new Point(x, y-1);
                case DOWN: return new Point(x, y+1);
                case LEFT: return new Point(x-1, y);
                case RIGHT: return new Point(x+1, y);
            }
            return this;
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false;
            Point p = (Point)o;
            return p.x == x && p.y == y;
        }
        @Override
        public int hashCode() { return x*31+y; }
    }
}
