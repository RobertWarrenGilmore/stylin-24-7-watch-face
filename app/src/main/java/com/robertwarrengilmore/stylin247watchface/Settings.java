package com.robertwarrengilmore.stylin247watchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

class Settings {

  private final Context context;
  private final SharedPreferences preferenceManager;

  Settings(Context context) {
    this.context = context;
    this.preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
    turnOffUseLocationIfNoPermission();
  }

  private void turnOffUseLocationIfNoPermission() {
    if (ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      preferenceManager
          .edit()
          .putBoolean(context.getString(R.string.settings_key_use_location), false)
          .apply();
    }
  }

  boolean getUseLocation() {
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_use_location),
        false
    );
  }

  boolean getShowSingleMinuteTicks() {
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_show_single_minute_ticks),
        false
    );
  }

  boolean getShowSecondHand() {
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_show_second_hand),
        false
    );
  }

  boolean getAnimateSecondHandSmoothly() {
    return getShowSecondHand() &&
        preferenceManager.getBoolean(context.getString(R.string.settings_key_animate_second_hand_smoothly),
            false
        );
  }

  boolean getDrawRealisticSun() {
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_draw_realistic_sun),
        false
    );
  }

  String getColourScheme() {
    return preferenceManager.getString(context.getString(R.string.settings_key_colour_scheme),
        context.getString(R.string.settings_colour_scheme_value_muted)
    );
  }
}

