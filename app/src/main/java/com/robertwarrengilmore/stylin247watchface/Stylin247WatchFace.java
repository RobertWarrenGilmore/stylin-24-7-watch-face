package com.robertwarrengilmore.stylin247watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;

public class Stylin247WatchFace extends CanvasWatchFaceService {

  private Settings settings;
  private LocationCache locationCache;

  /**
   * Handler message id for updating the time periodically in interactive mode.
   */
  private static final int MSG_UPDATE_TIME = 0;

  @Override
  public void onCreate() {
    super.onCreate();
    settings = new Settings(getApplicationContext());
    locationCache = new LocationCache(getApplicationContext());
  }

  @Override
  public Engine onCreateEngine() {
    return new Engine();
  }

  private static class EngineHandler extends Handler {

    private final WeakReference<Stylin247WatchFace.Engine> weakReference;

    public EngineHandler(Stylin247WatchFace.Engine reference) {
      weakReference = new WeakReference<>(reference);
    }

    @Override
    public void handleMessage(Message msg) {
      Stylin247WatchFace.Engine engine = weakReference.get();
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

    /* Handler to update the time in interactive mode. */
    private final Handler updateTimeHandler = new EngineHandler(this);
    private final Paint hourHandPaint = new Paint();
    private final Paint minuteHandPaint = new Paint();
    private final Paint secondHandPaint = new Paint();

    private Palette mutedPalette;
    private Palette vividPalette;
    private Palette ambientPalette;

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
    private boolean ambient;
    private boolean lowBitAmbient;
    private boolean burnInProtection;
    private float faceRadius;

    @Override
    public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);
      System.out.println("created watch face");

      setWatchFaceStyle(new WatchFaceStyle.Builder(Stylin247WatchFace.this)
          .setAcceptsTapEvents(false)
          .build());

      calendar = Calendar.getInstance();
    }

    @Override
    public void onDestroy() {
      System.out.println("destroyed watch face");
      updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      locationCache.stopUpdating();
      super.onDestroy();
    }

    @Override
    public void onPropertiesChanged(Bundle properties) {
      super.onPropertiesChanged(properties);
      final boolean lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
      final boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
      ambientPalette.setLowBitAmbient(lowBitAmbient);
      ambientPalette.setBurnInProtection(burnInProtection);
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

      /* Check and trigger whether or not timer should be running (only in interactive mode). */
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
       * Find the radius of the screen, and ignore the window insets, so that, on round watches with
       * a "chin", the watch face is centred on the entire screen, not just the usable portion.
       */
      faceRadius = width / 2f;

      createPalettes();
    }

    /**
     * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be used
     * for implementing specific logic to handle the gesture.
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
          break;
      }
      invalidate();
    }

    private void createPalettes() {
      mutedPalette = Palette.getMutedPalette(getApplicationContext(), faceRadius);
      vividPalette = Palette.getVividPalette(getApplicationContext(), faceRadius);
      ambientPalette = Palette.getAmbientPalette(getApplicationContext(), faceRadius);
    }

    Optional<Location> getLocation() {
      if (settings.getUseLocation()) {
        if (!locationCache.isUpdating()) {
          locationCache.startUpdating();
        }
        return locationCache.getLocation();
      }
      if (locationCache.isUpdating()) {
        locationCache.stopUpdating();
      }
      return Optional.empty();
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
      long now = System.currentTimeMillis();
      calendar.setTimeInMillis(now);

      String colourScheme = settings.getColourScheme();
      Palette palette;
      if (ambient) {
        palette = ambientPalette;
      } else if (colourScheme.equals(getString(R.string.settings_colour_scheme_value_vivid))) {
        palette = vividPalette;
      } else {
        palette = mutedPalette;
      }

      Painter.draw(
          canvas,
          bounds,
          palette,
          calendar,
          getLocation().orElse(null),
          settings.getDrawRealisticSun(),
          settings.getShowHourNumbers(),
          settings.getAngleHourNumbers(),
          settings.getShowSingleMinuteTicks(),
          settings.getShowSecondHand() && !ambient,
          settings.getAnimateSecondHandSmoothly() && !ambient
      );
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
     * Returns whether the {@link #updateTimeHandler} timer should be running. The timer should only
     * run in active mode.
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
        Duration updateRate;
        if (settings.getShowSecondHand()) {
          if (settings.getAnimateSecondHandSmoothly()) {
            updateRate = Duration.ofSeconds(1).dividedBy(20);
          } else {
            updateRate = Duration.ofSeconds(1);
          }
        } else {
          updateRate = Duration.ofMinutes(1);
        }
        long updateRateMs = (updateRate.getSeconds() * 1_000) + (updateRate.getNano() / 1_000_000);
        long timeMs = System.currentTimeMillis();
        long delayMs = updateRateMs - (timeMs % updateRateMs);
        updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
      }
    }
  }
}