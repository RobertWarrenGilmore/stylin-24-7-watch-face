package com.robertwarrengilmore.stylin247watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalTime;
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

  private static final Location location = new Location("");
  private static final boolean DRAW_LOCATION_STUFF = true;
  /*
   * Updates rate in milliseconds for interactive mode. We update once a second to advance the
   * second hand.
   */
  private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1) / 20;

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

    private static final float AMBIENT_HOUR_DISC_STROKE_WIDTH = 0.01f;
    private static final float SUN_AURA_WIDTH = 0.1f;
    private static final float HOUR_HAND_WIDTH = 0.04f;
    private static final float HOUR_HAND_LENGTH = 0.525f;
    private static final float MINUTE_HAND_WIDTH = 0.02f;
    private static final float MINUTE_HAND_LENGTH = 0.9f;
    private static final float SECOND_HAND_WIDTH = 0.01f;
    private static final float SECOND_HAND_LENGTH = 0.9f;
    private static final float HAND_CAP_RADIUS = 0.03f;
    private static final float HAND_SHADOW_WIDTH = 0.01f;
    private static final float LARGE_NOTCH_WIDTH = 0.02f;
    private static final float SMALL_NOTCH_WIDTH = 0.0125f;
    private static final float HOUR_DISC_RADIUS = 0.667f;
    private static final float SUN_AND_MOON_RADIUS = 0.15f;
    private static final float SUN_AND_MOON_CENTRE_OFFSET = 0.3f;
    private static final float LARGE_NOTCH_LENGTH = 0.425f;
    private static final float SMALL_NOTCH_LENGTH = 0.05f;
    private static final float MINUTE_NOTCH_OUTER_RADIUS = 1f;
    /* Handler to update the time once a second in interactive mode. */
    private final Handler updateTimeHandler = new EngineHandler(this);
    private final Paint hourHandPaint = new Paint();
    private final Paint minuteHandPaint = new Paint();
    private final Paint secondHandPaint = new Paint();
    private final Paint handCapPaint = new Paint();
    private final Paint smallNotchPaint = new Paint();
    private final Paint largeNotchPaint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private final Paint daySectorPaint = new Paint();
    private final Paint sunPaint = new Paint();
    private final Paint nightSectorPaint = new Paint();
    private final Paint moonLitPaint = new Paint();
    private final Paint moonDarkPaint = new Paint();
    private final Paint moonLinePaint = new Paint();
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
    private PointF centre;
    private float faceRadius;
    private boolean ambient;

    @Override
    public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);

      setWatchFaceStyle(new WatchFaceStyle.Builder(Stylin247WatchFace.this).setAcceptsTapEvents(true)
          .build());

      calendar = Calendar.getInstance();

      initialiseStyles();
      // Seattle
      location.setLatitude(47.608013);
      location.setLongitude(-122.335167);
      // Tierra del Fuego
      //      location.setLatitude(-54-(48/60f));
      //      location.setLongitude(-68-(18/60f));
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

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
      super.onInterruptionFilterChanged(interruptionFilter);
      boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

      /* Dim display in mute mode. */
      if (muteMode != inMuteMode) {
        muteMode = inMuteMode;
        hourHandPaint.setAlpha(inMuteMode ? 100 : 255);
        minuteHandPaint.setAlpha(inMuteMode ? 100 : 255);
        secondHandPaint.setAlpha(inMuteMode ? 80 : 255);
        invalidate();
      }
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      super.onSurfaceChanged(holder, format, width, height);

      /*
       * Find the coordinates of the centre point on the screen, and ignore the window
       * insets, so that, on round watches with a "chin", the watch face is centred on the
       * entire screen, not just the usable portion.
       */
      centre = new PointF(width / 2f, height / 2f);
      faceRadius = width / 2f;

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
//          Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT).show();
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

    private void initialiseStyles() {

      hourHandPaint.setAntiAlias(true);
      hourHandPaint.setStrokeCap(Paint.Cap.ROUND);

      minuteHandPaint.setAntiAlias(true);
      minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

      secondHandPaint.setAntiAlias(true);
      secondHandPaint.setStrokeCap(Paint.Cap.ROUND);

      handCapPaint.setAntiAlias(true);
      handCapPaint.setStyle(Paint.Style.FILL);

      largeNotchPaint.setAntiAlias(true);
      largeNotchPaint.setStrokeCap(Paint.Cap.BUTT);

      smallNotchPaint.setAntiAlias(true);
      smallNotchPaint.setStrokeCap(Paint.Cap.BUTT);

      daySectorPaint.setAntiAlias(true);

      sunPaint.setAntiAlias(true);
      sunPaint.setStyle(Paint.Style.FILL);

      nightSectorPaint.setAntiAlias(true);

      moonLinePaint.setAntiAlias(true);
      moonLinePaint.setStyle(Paint.Style.STROKE);

      moonLitPaint.setAntiAlias(true);
      moonLitPaint.setStyle(Paint.Style.FILL);
      moonLitPaint.setColor(Color.WHITE);

      moonDarkPaint.setAntiAlias(true);
      moonDarkPaint.setStyle(Paint.Style.FILL);
      moonDarkPaint.setColor(Color.BLACK);

      updateStyles();
    }

    private void updateStyles() {
      updateBackgroundStyles();
      updateHandStyles();
      updateNotchStyles();
    }

    private void updateBackgroundStyles() {
      if (ambient) {
        backgroundPaint.setColor(Color.BLACK);

        daySectorPaint.setColor(Color.WHITE);
        daySectorPaint.setStyle(Paint.Style.STROKE);
        daySectorPaint.setStrokeWidth(absoluteDimension(AMBIENT_HOUR_DISC_STROKE_WIDTH));

        sunPaint.setColor(Color.WHITE);
        sunPaint.setShadowLayer(absoluteDimension(SUN_AURA_WIDTH), 0, 0, Color.WHITE);

        nightSectorPaint.setColor(Color.WHITE);
        nightSectorPaint.setStyle(Paint.Style.STROKE);
        nightSectorPaint.setStrokeWidth(absoluteDimension(AMBIENT_HOUR_DISC_STROKE_WIDTH));

        moonLinePaint.setColor(Color.WHITE);
        moonLinePaint.setStrokeWidth(absoluteDimension(AMBIENT_HOUR_DISC_STROKE_WIDTH));
      } else {
        backgroundPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.4f}));

        daySectorPaint.setColor(Color.HSVToColor(new float[]{200f, 0.25f, 0.6f}));
        daySectorPaint.setStyle(Paint.Style.FILL);

        sunPaint.setColor(Color.HSVToColor(new float[]{45f, 0.3f, 1f}));
        // TODO Experiment with drawing triangles instead of aura.
        sunPaint.setShadowLayer(absoluteDimension(SUN_AURA_WIDTH),
            0,
            0,
            Color.HSVToColor(new float[]{45f, 0.3f, 1f}));

        nightSectorPaint.setColor(Color.HSVToColor(new float[]{230f, 0.25f, 0.25f}));
        nightSectorPaint.setStyle(Paint.Style.FILL);

        moonLinePaint.setColor(Color.TRANSPARENT);
      }
    }

    private void updateHandStyles() {
      hourHandPaint.setStrokeWidth(absoluteDimension(HOUR_HAND_WIDTH));
      minuteHandPaint.setStrokeWidth(absoluteDimension(MINUTE_HAND_WIDTH));
      secondHandPaint.setStrokeWidth(absoluteDimension(SECOND_HAND_WIDTH));
      largeNotchPaint.setStrokeWidth(absoluteDimension(LARGE_NOTCH_WIDTH));
      smallNotchPaint.setStrokeWidth(absoluteDimension(SMALL_NOTCH_WIDTH));

      if (ambient) {
        hourHandPaint.setColor(Color.WHITE);
        minuteHandPaint.setColor(Color.WHITE);
        secondHandPaint.setColor(Color.WHITE);
        handCapPaint.setColor(Color.WHITE);

        hourHandPaint.clearShadowLayer();
        minuteHandPaint.clearShadowLayer();
        secondHandPaint.clearShadowLayer();
        handCapPaint.clearShadowLayer();

      } else {
        hourHandPaint.setColor(Color.BLACK);
        minuteHandPaint.setColor(Color.BLACK);
        secondHandPaint.setColor(Color.HSVToColor(new float[]{0f, 0.75f, 0.75f}));
        handCapPaint.setColor(Color.BLACK);

        hourHandPaint.setShadowLayer(absoluteDimension(HAND_SHADOW_WIDTH), 0, 0, Color.BLACK);
        minuteHandPaint.setShadowLayer(absoluteDimension(HAND_SHADOW_WIDTH), 0, 0, Color.BLACK);
        secondHandPaint.setShadowLayer(absoluteDimension(HAND_SHADOW_WIDTH), 0, 0, Color.BLACK);
        handCapPaint.setShadowLayer(absoluteDimension(HAND_SHADOW_WIDTH), 0, 0, Color.BLACK);
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

    private void drawBackground(Canvas canvas) {
      canvas.drawPaint(backgroundPaint);

      float dayNightDiscRadius = absoluteDimension(HOUR_DISC_RADIUS);
      RectF
          boundingBox =
          new RectF(centre.x - dayNightDiscRadius,
              centre.y - dayNightDiscRadius,
              centre.x + dayNightDiscRadius,
              centre.y + dayNightDiscRadius);

      final Duration
          solarDayLength =
          DRAW_LOCATION_STUFF ?
              AstronomyCalculator.getSolarDayLength(location, calendar) :
              Duration.ofHours(12);
      final LocalTime
          solarNoon =
          DRAW_LOCATION_STUFF ?
              AstronomyCalculator.getSolarNoon(location, calendar) :
              LocalTime.NOON;


      final float noonOffsetDayFraction = solarNoon.toSecondOfDay() / (24f * 60 * 60 - 1);
      final float dayLengthFraction = solarDayLength.getSeconds() / (24f * 60 * 60 - 1);

      final float sunriseOffsetFraction = noonOffsetDayFraction - (dayLengthFraction / 2);
      canvas.drawArc(boundingBox,
          90 + sunriseOffsetFraction * 360,
          dayLengthFraction * 360,
          true,
          daySectorPaint);
      canvas.drawArc(boundingBox,
          90 + sunriseOffsetFraction * 360 + dayLengthFraction * 360,
          360 - dayLengthFraction * 360,
          true,
          nightSectorPaint);

      // TODO Experiment with using less power in ambient mode.
      // TODO Experiment with drawing stars.
      // TODO Experiment with painting the sun and moon over the hands.
      // TODO Experiment with drawing the weather. (Don't forget to turn the sun red if it's hazy.)

      final float noonAngle = noonOffsetDayFraction * 360 + 180;

      drawSun(canvas,
          cartesian(noonAngle, absoluteDimension(SUN_AND_MOON_CENTRE_OFFSET)),
          absoluteDimension(SUN_AND_MOON_RADIUS));
      float lunarPhase = AstronomyCalculator.getLunarPhase(calendar.toInstant());

      drawMoon(canvas,
          cartesian(noonAngle + 180f, absoluteDimension(SUN_AND_MOON_CENTRE_OFFSET)),
          absoluteDimension(SUN_AND_MOON_RADIUS),
          lunarPhase);
    }

    private void drawNotches(Canvas canvas) {

      float largeNotchLength = absoluteDimension(LARGE_NOTCH_LENGTH);
      float smallNotchLength = absoluteDimension(SMALL_NOTCH_LENGTH);

      // Draw the five-minute (and even-hour) notches.
      float outerNotchRadius = absoluteDimension(MINUTE_NOTCH_OUTER_RADIUS);
      for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
        float tickRot = (float) (tickIndex * 360 / 12);
        drawNotch(canvas, tickRot, outerNotchRadius, largeNotchLength, largeNotchPaint);
      }
      // Draw the single-minute notches.
      for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
        // Don't repeat the five-minute notches.
        if (tickIndex % 5 == 0) {
          continue;
        }
        float tickRot = (float) (tickIndex * 360 / 60);
        drawNotch(canvas, tickRot, outerNotchRadius, smallNotchLength, smallNotchPaint);
      }
      // Draw the odd-hour notches.
      outerNotchRadius = absoluteDimension(HOUR_DISC_RADIUS);
      for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
        float tickRot = (float) (tickIndex * 360 / 12) + (float) (360 / 24);
        drawNotch(canvas, tickRot, outerNotchRadius, smallNotchLength, smallNotchPaint);
      }
    }

    private void drawNotch(Canvas canvas,
                           float angle,
                           float outerRadius,
                           float length,
                           Paint paint) {
      PointF inside = cartesian(angle, outerRadius - length);
      PointF outside = cartesian(angle, outerRadius);
      canvas.drawLine(inside.x, inside.y, outside.x, outside.y, paint);

      // TODO Show calendar events as curved blocks just outside of the hour disc, and allow the user to tap on them to see info about the events. (Events from three hours ago to 21 hours in the future should be shown).
      // TODO Move all TODOs into Quire or Github issues.
      // TODO Remove Pallette API because we're not using it.
      // TODO Update preview image.
      // TODO Refactor all cartesian-to-polar conversion math into a helper. This helper should multiply the input radius (from 0 to 1) by the screen radius, so that users of the helper don't have to multiply everything by screen radius. (Maybe make another helper that just multiplies the argument by the screen radius, so that things like stroke widths can similarly be scaled.
      // TODO Turn all dimensions into constants.
      // TODO Make as many variables as possible final.
    }

    private void drawHands(Canvas canvas) {
      /*
       * These calculations reflect the rotation in degrees per unit of time, e.g.,
       * 360 / 60 = 6 and 360 / 12 = 30.
       */
      final float
          seconds =
          (calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f);
      final float secondsRotation = seconds * (360 / 60f);

      // In ambient mode, the minute hand should tick instead of moving gradually.
      final float partialMinute = ambient ? 0 : (calendar.get(Calendar.SECOND) / 60f);
      final float minutesRotation = (calendar.get(Calendar.MINUTE) + partialMinute) * (360 / 60f);

      final float partialHour = calendar.get(Calendar.MINUTE) / 60f;
      // The hour hand moves 15 degrees per hour on a 24-hour clock, not 30.
      float
          hoursRotation =
          ((calendar.get(Calendar.HOUR_OF_DAY) + partialHour) * (360 / 24f)) + 180;

      PointF secondHandEnd = cartesian(secondsRotation, absoluteDimension(SECOND_HAND_LENGTH));
      PointF minuteHandEnd = cartesian(minutesRotation, absoluteDimension(MINUTE_HAND_LENGTH));
      PointF hourHandEnd = cartesian(hoursRotation, absoluteDimension(HOUR_HAND_LENGTH));
      canvas.drawLine(centre.x, centre.y, hourHandEnd.x, hourHandEnd.y, hourHandPaint);
      canvas.drawLine(centre.x, centre.y, minuteHandEnd.x, minuteHandEnd.y, minuteHandPaint);
      /*
       * Ensure the second hand is drawn only when we are in interactive mode.
       * Otherwise, we only update the watch face once a minute.
       */
      if (!ambient) {
        canvas.drawLine(centre.x, centre.y, secondHandEnd.x, secondHandEnd.y, secondHandPaint);
      }
      canvas.drawCircle(centre.x, centre.y, absoluteDimension(HAND_CAP_RADIUS), handCapPaint);
    }

    private void drawSun(Canvas canvas, PointF centre, float radius) {
      canvas.drawCircle(centre.x, centre.y, radius, sunPaint);
    }

    private void drawMoon(Canvas canvas, PointF centre, float radius, float phase) {
      canvas.drawCircle(centre.x, centre.y, radius, moonDarkPaint);
      final boolean litOnRight = phase > 0.5f;
      final boolean mostlyLit = phase > 0.25f && phase < 0.75f;
      final boolean curveGoesRight = litOnRight ^ mostlyLit;
      final float curveOffset = (float) Math.cos(phase * 2 * Math.PI) * radius;
      canvas.drawArc(centre.x - radius,
          centre.y - radius,
          centre.x + radius,
          centre.y + radius,
          litOnRight ? 90f : 270f,
          180f,
          true,
          moonLitPaint);
      canvas.drawOval(centre.x - curveOffset,
          centre.y - radius,
          centre.x + curveOffset,
          centre.y + radius,
          mostlyLit ? moonLitPaint : moonDarkPaint);
      canvas.drawArc(centre.x - curveOffset,
          centre.y - radius,
          centre.x + curveOffset,
          centre.y + radius,
          curveGoesRight ? 90f : 270f,
          180f,
          false,
          moonLinePaint);
      canvas.drawCircle(centre.x, centre.y, radius, moonLinePaint);
    }

    /**
     * Convert radial coordinates to a cartesian point.
     */
    private PointF cartesian(PointF origin, float angle, float radius) {
      float x = origin.x + (float) Math.sin(Math.toRadians(angle)) * radius;
      float y = origin.y + (float) -Math.cos(Math.toRadians(angle)) * radius;
      return new PointF(x, y);
    }

    /**
     * Convert radial coordinates relative to the centre of the face to a cartesian point.
     */
    private PointF cartesian(float angle, float radius) {
      return cartesian(centre, angle, radius);
    }

    /**
     * Convert a relative dimension (where 1 is the radius of the face) to an absolute dimension (where 1 is a pixel).
     */
    private float absoluteDimension(float relativeDimension) {
      return faceRadius * relativeDimension;
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
        long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
        updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
      }
    }
  }
}