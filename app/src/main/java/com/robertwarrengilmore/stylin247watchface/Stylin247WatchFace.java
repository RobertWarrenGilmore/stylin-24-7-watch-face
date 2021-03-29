package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Stylin247WatchFace extends CanvasWatchFaceService
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  public static final Duration SMOOTH_UPDATE_RATE = Duration.ofSeconds(1).dividedBy(20);
  public static final Duration SECOND_UPDATE_RATE = Duration.ofSeconds(1);
  public static final Duration MINUTE_UPDATE_RATE = Duration.ofMinutes(1);
  private Location location;
  private SharedPreferences preferenceManager;
  private final Painter painter = new Painter();
  private Instant backgroundExpiration;

  /**
   * Handler message id for updating the time periodically in interactive mode.
   */
  private static final int MSG_UPDATE_TIME = 0;

  public static final LocationRequest LOCATION_REQUEST = new LocationRequest()
      .setSmallestDisplacement(200_000f)
      .setPriority(LocationRequest.PRIORITY_LOW_POWER)
      .setFastestInterval(Duration.ofSeconds(1).toMillis())
      .setInterval(Duration.ofHours(6).toMillis());
  private final LocationCallback locationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      super.onLocationResult(locationResult);
      location = locationResult.getLastLocation();
      invalidateCachedBackground();
    }
  };
  private FusedLocationProviderClient locationClient;

  private boolean showSecondHand;
  private boolean animateSecondHandSmoothly;
  private boolean useLocation;
  private String colourScheme;
  private boolean drawRealisticSun;
  private boolean showHourNumbers;
  private boolean angleHourNumbers;
  private boolean showSingleMinuteTicks;

  @Override
  public void onCreate() {
    super.onCreate();
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, false);
    preferenceManager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    turnOffUseLocationIfNoPermission();
    locationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
    updatePreferences();
    startLocationUpdates();
  }

  @Override
  public void onDestroy() {
    stopLocationUpdates();
    super.onDestroy();
  }

  @Override
  public boolean onUnbind(Intent intent) {
    stopLocationUpdates();
    super.onUnbind(intent);
    return true;
  }

  @Override
  public void onRebind(Intent intent) {
    turnOffUseLocationIfNoPermission();
    updatePreferences();
    startLocationUpdates();
    super.onRebind(intent);
  }

  private void startLocationUpdates() {
    if (!useLocation) {
      return;
    }
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      return;
    }
    locationClient.requestLocationUpdates(LOCATION_REQUEST, locationCallback, Looper.myLooper());
  }

  private void stopLocationUpdates() {
    locationClient.removeLocationUpdates(locationCallback);
  }

  private void turnOffUseLocationIfNoPermission() {
    if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      preferenceManager
          .edit()
          .putBoolean(getString(R.string.settings_key_use_location), false)
          .apply();
    }
  }

  private void invalidateCachedBackground() {
    painter.invalidateCachedBackground();
    backgroundExpiration = Instant.now().plus(Duration.ofHours(1));
  }

  @Override
  public Engine onCreateEngine() {
    return new Engine();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updatePreferences();
  }

  private void updatePreferences() {
    showSecondHand = preferenceManager.getBoolean(getString(R.string.settings_key_show_second_hand),
        false
    );
    animateSecondHandSmoothly = preferenceManager.getBoolean(getString(R.string.settings_key_animate_second_hand_smoothly),
        false
    );
    useLocation = preferenceManager.getBoolean(
        getString(R.string.settings_key_use_location),
        false
    );
    colourScheme = preferenceManager.getString(getString(R.string.settings_key_colour_scheme),
        getString(R.string.settings_colour_scheme_value_muted)
    );
    drawRealisticSun = preferenceManager.getBoolean(
        getString(R.string.settings_key_draw_realistic_sun),
        false
    );
    showHourNumbers = preferenceManager.getBoolean(
        getString(R.string.settings_key_show_hour_numbers),
        false
    );
    angleHourNumbers = preferenceManager.getBoolean(
        getString(R.string.settings_key_angle_hour_numbers),
        false
    );
    showSingleMinuteTicks = preferenceManager.getBoolean(getString(R.string.settings_key_show_single_minute_ticks),
        false
    );
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
    private float faceRadius;

    @Override
    public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);
      invalidateCachedBackground();

      setWatchFaceStyle(new WatchFaceStyle.Builder(Stylin247WatchFace.this)
          .setAcceptsTapEvents(false)
          .build());

      calendar = Calendar.getInstance();
    }

    @Override
    public void onDestroy() {
      updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      super.onDestroy();
    }

    @Override
    public void onPropertiesChanged(Bundle properties) {
      super.onPropertiesChanged(properties);
      final boolean lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
      final boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
      ambientPalette.setLowBitAmbient(lowBitAmbient);
      ambientPalette.setBurnInProtection(burnInProtection);
      invalidateCachedBackground();
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
      invalidateCachedBackground();

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
      invalidateCachedBackground();

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

    Location getLocationIfNeeded() {
      if (useLocation) {
        return location;
      }
      return null;
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
      Instant now = Instant.now();

      if (now.isAfter(backgroundExpiration)) {
        invalidateCachedBackground();
      }

      calendar.setTimeInMillis(now.toEpochMilli());

      Palette palette;
      if (ambient) {
        palette = ambientPalette;
      } else if (colourScheme.equals(getString(R.string.settings_colour_scheme_value_vivid))) {
        palette = vividPalette;
      } else {
        palette = mutedPalette;
      }

      painter.draw(canvas,
          bounds,
          palette,
          calendar,
          getLocationIfNeeded(),
          drawRealisticSun,
          showHourNumbers,
          angleHourNumbers,
          showSingleMinuteTicks,
          showSecondHand && !ambient,
          showSecondHand && animateSecondHandSmoothly && !ambient
      );
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
      super.onVisibilityChanged(visible);

      if (visible) {
        registerReceiver();
        /* Update time zone in case it changed while we weren"t visible. */
        calendar.setTimeZone(TimeZone.getDefault());
        updatePreferences();
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
        if (showSecondHand) {
          if (animateSecondHandSmoothly) {
            updateRate = SMOOTH_UPDATE_RATE;
          } else {
            updateRate = SECOND_UPDATE_RATE;
          }
        } else {
          updateRate = MINUTE_UPDATE_RATE;
        }
        long updateRateMs = (updateRate.getSeconds() * 1_000) + (updateRate.getNano() / 1_000_000);
        long timeMs = System.currentTimeMillis();
        long delayMs = updateRateMs - (timeMs % updateRateMs);
        updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
      }
    }
  }
}