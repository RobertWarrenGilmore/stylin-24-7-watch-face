package com.robertwarrengilmore.stylin247watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

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
        private float centreX;
        private float centreY;
        private Paint hourPaint = new Paint();
        private Paint minutePaint = new Paint();
        private Paint secondPaint = new Paint();
        private Paint centerPaint = new Paint();
        private Paint smallNotchPaint = new Paint();
        private Paint largeNotchPaint = new Paint();
        private Paint backgroundPaint = new Paint();
        private boolean ambient;

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

            hourPaint.setAntiAlias(true);
            hourPaint.setStrokeCap(Paint.Cap.SQUARE);

            minutePaint.setAntiAlias(true);
            minutePaint.setStrokeCap(Paint.Cap.SQUARE);

            secondPaint.setAntiAlias(true);
            secondPaint.setStrokeCap(Paint.Cap.SQUARE);

            centerPaint.setAntiAlias(true);
            centerPaint.setStyle(Paint.Style.FILL);

            largeNotchPaint.setAntiAlias(true);
            largeNotchPaint.setStrokeCap(Paint.Cap.SQUARE);

            smallNotchPaint.setAntiAlias(true);
            smallNotchPaint.setStrokeCap(Paint.Cap.SQUARE);

            updateStyles();
        }

        private void updateStyles() {
            updateBackgroundStyles();
            updateHandStyles();
            updateNotchStyles();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
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

            updateStyles();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateBackgroundStyles() {
            if (ambient) {
                backgroundPaint.setColor(Color.BLACK);
            } else {
                backgroundPaint.setColor(Color.DKGRAY);
            }
        }

        private void updateHandStyles() {
            /*
             * Calculate widths of different hands based on watch screen size.
             */
            hourPaint.setStrokeWidth(centreX * 0.04f);
            minutePaint.setStrokeWidth(centreX * 0.02f);
            secondPaint.setStrokeWidth(centreX * 0.01f);
            largeNotchPaint.setStrokeWidth(centreX * 0.02f);
            smallNotchPaint.setStrokeWidth(centreX * 0.0125f);

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

                hourPaint.setShadowLayer(centreX * 0.01f, 0, 0, Color.BLACK);
                minutePaint.setShadowLayer(centreX * 0.01f, 0, 0, Color.BLACK);
                secondPaint.setShadowLayer(centreX * 0.01f, 0, 0, Color.BLACK);
                centerPaint.setShadowLayer(centreX * 0.01f, 0, 0, Color.BLACK);
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
            centreX = width / 2f;
            centreY = height / 2f;

            updateStyles();
        }

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
            canvas.drawPaint(backgroundPaint);
        }

        private void drawNotches(Canvas canvas) {

            float largeNotchLength = centreX * 0.425f;
            float smallNotchLength = centreX * 0.05f;

            // Draw the five-minute (and even-hour) notches.
            float outerNotchRadius = centreX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                drawNotch(canvas, tickRot, outerNotchRadius, largeNotchLength, largeNotchPaint);
            }
            // Draw the single-minute notches.
            outerNotchRadius = centreX;
            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                // Don't repeat the five-minute notches.
                if (tickIndex % 5 == 0) {
                    continue;
                }
                float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                drawNotch(canvas, tickRot, outerNotchRadius, smallNotchLength, smallNotchPaint);
            }
            // Draw the odd-hour notches.
            outerNotchRadius = centreX * 0.667f;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12) + (float) (Math.PI * 2 / 24);
                drawNotch(canvas, tickRot, outerNotchRadius, smallNotchLength, smallNotchPaint);
            }
        }

        private void drawNotch(Canvas canvas, float angle, float outerRadius, float length, Paint paint) {
            float innerX = (float) Math.sin(angle) * (outerRadius - length);
            float innerY = (float) -Math.cos(angle) * (outerRadius - length);
            float outerX = (float) Math.sin(angle) * outerRadius;
            float outerY = (float) -Math.cos(angle) * outerRadius;
            canvas.drawLine(centreX + innerX, centreY + innerY,
                    centreX + outerX, centreY + outerY, paint);
        }

        private void drawHands(Canvas canvas) {

            float secondHandLength = centreX * 0.9f;
            float minuteHandLength = centreX * 0.9f;
            float hourHandLength = centreX * 0.667f;
            float centerCircleRadius = centreX * 0.03f;

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (calendar.get(Calendar.SECOND) /*+ calendar.get(Calendar.MILLISECOND) / 1000f*/);
            final float secondsRotation = seconds * (360/60f);

            // In ambient mode, the minute hand should tick instead of moving gradually.
            final float minuteHandOffset = ambient ? 0 : (secondsRotation / 60);
            final float minutesRotation = calendar.get(Calendar.MINUTE) * 6f + minuteHandOffset;

            final float hourHandOffset = minutesRotation / 24;
            // The hour hand moves 15 degrees per hour on a 24-hour clock, not 30.
            final float hoursRotation = (calendar.get(Calendar.HOUR) * 15) + hourHandOffset + 180;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, centreX, centreY);
            canvas.drawLine(
                    centreX,
                    centreY,
                    centreX,
                    centreY - hourHandLength,
                    hourPaint);

            canvas.rotate(minutesRotation - hoursRotation, centreX, centreY);
            canvas.drawLine(
                    centreX,
                    centreY,
                    centreX,
                    centreY - minuteHandLength,
                    minutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!ambient) {
                canvas.rotate(secondsRotation - minutesRotation, centreX, centreY);
                canvas.drawLine(
                        centreX,
                        centreY,
                        centreX,
                        centreY - secondHandLength,
                        secondPaint);

            }
            canvas.drawCircle(
                    centreX,
                    centreY,
                    centerCircleRadius,
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