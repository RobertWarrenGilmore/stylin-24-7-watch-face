package com.robertwarrengilmore.stylin247watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.palette.graphics.Palette;

import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class Stylin247WatchFace extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Stylin247WatchFace.Engine> mWeakReference;

        public EngineHandler(Stylin247WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Stylin247WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;
        /* Handler to update the time once a second in interactive mode. */
        private final Handler updateTimeHandler = new EngineHandler(this);
        private Calendar calendar;
        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean registeredTimeZoneReceiver = false;
        private boolean muteMode;
        private float centerX;
        private float centerY;
        private float secondHandLength;
        private float minuteHandLength;
        private float hourHandLength;
        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private int watchHandColor;
        private int watchHandHighlightColor;
        private int watchHandShadowColor;
        private Paint hourPaint = new Paint();
        private Paint minutePaint = new Paint();
        private Paint secondPaint = new Paint();
        private Paint centerPaint = new Paint();
        private Paint smallNotchPaint = new Paint();
        private Paint largeNotchPaint = new Paint();
        private Paint backgroundPaint = new Paint();
        //        private Bitmap mBackgroundBitmap;
//        private Bitmap mGrayBackgroundBitmap;
        private boolean ambient;
        private boolean lowBitAmbient;
        private boolean burnInProtection;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(Stylin247WatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            calendar = Calendar.getInstance();

            initialiseStyles();
        }

        private void initialiseStyles() {
            backgroundPaint.setColor(Color.BLACK);

            hourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            hourPaint.setAntiAlias(true);
            hourPaint.setStrokeCap(Paint.Cap.SQUARE);

            minutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            minutePaint.setAntiAlias(true);
            minutePaint.setStrokeCap(Paint.Cap.SQUARE);

            secondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            secondPaint.setAntiAlias(true);
            secondPaint.setStrokeCap(Paint.Cap.SQUARE);

            centerPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            centerPaint.setAntiAlias(true);
            centerPaint.setStyle(Paint.Style.FILL);

            largeNotchPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            largeNotchPaint.setAntiAlias(true);
            largeNotchPaint.setStrokeCap(Paint.Cap.SQUARE);

            smallNotchPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            smallNotchPaint.setAntiAlias(true);
            smallNotchPaint.setStrokeCap(Paint.Cap.SQUARE);

            updateStyles();
        }

        private void updateStyles() {
            updateHandStyles();
            updateNotchStyles();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            ambient = inAmbientMode;

            // TODO Refactor so that the styles of both the hands and the background are changed on ambient mode changed instead of determining the background style at painting time.

            updateStyles();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateHandStyles() {
            if (ambient) {
                hourPaint.setColor(Color.WHITE);
                minutePaint.setColor(Color.WHITE);
                secondPaint.setColor(Color.WHITE);
                centerPaint.setColor(Color.WHITE);

                hourPaint.clearShadowLayer();
                minutePaint.clearShadowLayer();
                secondPaint.clearShadowLayer();
                centerPaint.clearShadowLayer();

            } else {
                hourPaint.setColor(Color.BLACK);
                minutePaint.setColor(Color.BLACK);
                secondPaint.setColor(Color.BLACK);
                centerPaint.setColor(Color.BLACK);

                hourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, watchHandShadowColor);
                minutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, watchHandShadowColor);
                secondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, watchHandShadowColor);
                centerPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, watchHandShadowColor);
            }
        }

        private void updateNotchStyles() {
            if (ambient) {
                largeNotchPaint.setColor(Color.WHITE);
                smallNotchPaint.setColor(Color.WHITE);
            } else {
                largeNotchPaint.setColor(Color.BLACK);
                smallNotchPaint.setColor(Color.BLACK);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode;
                hourPaint.setAlpha(inMuteMode ? 100 : 255);
                minutePaint.setAlpha(inMuteMode ? 100 : 255);
                secondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            centerX = width / 2f;
            centerY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            secondHandLength = (float) (centerX * 0.9);
            minuteHandLength = (float) (centerX * 0.9);
            hourHandLength = (float) (centerX * 0.667);

//            /* Scale loaded background image (more efficient) if surface dimensions change. */
//            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();
//
//            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                    (int) (mBackgroundBitmap.getWidth() * scale),
//                    (int) (mBackgroundBitmap.getHeight() * scale), true);
//
//            /*
//             * Create a gray version of the image only if it will look nice on the device in
//             * ambient mode. That means we don"t want devices that support burn-in
//             * protection (slight movements in pixels, not great for images going all the way to
//             * edges) and low ambient mode (degrades image quality).
//             *
//             * Also, if your watch face will know about all images ahead of time (users aren"t
//             * selecting their own photos for the watch face), it will be more
//             * efficient to create a black/white version (png, etc.) and load that when you need it.
//             */
//            if (!mBurnInProtection && !mLowBitAmbient) {
//                initGrayBackgroundBitmap();
//            }
        }

//        private void initGrayBackgroundBitmap() {
//            mGrayBackgroundBitmap = Bitmap.createBitmap(
//                    mBackgroundBitmap.getWidth(),
//                    mBackgroundBitmap.getHeight(),
//                    Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
//            Paint grayPaint = new Paint();
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setSaturation(0);
//            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
//            grayPaint.setColorFilter(filter);
//            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
//        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawNotches(canvas);
            drawHands(canvas);
        }

        private void drawBackground(Canvas canvas) {

            if (ambient && (lowBitAmbient || burnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (ambient) {
                canvas.drawColor(Color.BLACK);
//                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawColor(Color.DKGRAY);
//                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
        }

        private void drawNotches(Canvas canvas) {
            // Draw the five-minute (and even-hour) notches.
            float innerNotchRadius = hourHandLength * 0.9f;
            float outerNotchRadius = centerX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerNotchRadius;
                float innerY = (float) -Math.cos(tickRot) * innerNotchRadius;
                float outerX = (float) Math.sin(tickRot) * outerNotchRadius;
                float outerY = (float) -Math.cos(tickRot) * outerNotchRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, largeNotchPaint);
            }
            // Now draw the single-minute notches.
            innerNotchRadius = minuteHandLength;
            outerNotchRadius = centerX;
            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                if (tickIndex % 5 == 0) {
                    continue;
                }
                float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                float innerX = (float) Math.sin(tickRot) * innerNotchRadius;
                float innerY = (float) -Math.cos(tickRot) * innerNotchRadius;
                float outerX = (float) Math.sin(tickRot) * outerNotchRadius;
                float outerY = (float) -Math.cos(tickRot) * outerNotchRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, smallNotchPaint);
            }
            // Draw the odd-hour notches.
            innerNotchRadius = hourHandLength * 0.95f;
            outerNotchRadius = hourHandLength;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12) + (float) (Math.PI * 2 / 24);
                float innerX = (float) Math.sin(tickRot) * innerNotchRadius;
                float innerY = (float) -Math.cos(tickRot) * innerNotchRadius;
                float outerX = (float) Math.sin(tickRot) * outerNotchRadius;
                float outerY = (float) -Math.cos(tickRot) * outerNotchRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, smallNotchPaint);
            }
        }

        private void drawHands(Canvas canvas) {

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = calendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = calendar.get(Calendar.MINUTE) / 4f;
            // The hour hand moves 15 degrees per hour on a 24-hour clock, not 30.
            final float hoursRotation = (calendar.get(Calendar.HOUR) * 15) + hourHandOffset + 180;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, centerX, centerY);
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - hourHandLength,
                    hourPaint);

            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY);
            canvas.drawLine(
                    centerX,
                    centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerX,
                    centerY - minuteHandLength,
                    minutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!ambient) {
                canvas.rotate(secondsRotation - minutesRotation, centerX, centerY);
                canvas.drawLine(
                        centerX,
                        centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        centerX,
                        centerY - secondHandLength,
                        secondPaint);

            }
            canvas.drawCircle(
                    centerX,
                    centerY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    centerPaint);

            /* Restore the canvas" original orientation. */
            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren"t visible. */
                calendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            Stylin247WatchFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            Stylin247WatchFace.this.unregisterReceiver(timeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #updateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !ambient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}