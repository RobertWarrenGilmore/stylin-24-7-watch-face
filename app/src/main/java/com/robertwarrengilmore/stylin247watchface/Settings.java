package com.robertwarrengilmore.stylin247watchface;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

class Settings {

  private final Context context;
  private final SharedPreferences preferenceManager;

  Settings(Context context) {
    this.context = context;
    this.preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
  }

  boolean getShowSingleMinuteTicks() {
    // TODO Consider implementing the settings listener if this ends up being a slow operation.
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_show_single_minute_ticks),
        false);
  }

  boolean getShowSecondHand() {
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_show_second_hand),
        false);
  }

  boolean getAnimateSecondHandSmoothly() {
    return getShowSecondHand() &&
        preferenceManager.getBoolean(context.getString(R.string.settings_key_animate_second_hand_smoothly),
            false);
  }

  boolean getDrawRealisticSun() {
    // TODO Consider implementing the settings listener if this ends up being a slow operation.
    return preferenceManager.getBoolean(context.getString(R.string.settings_key_draw_realistic_sun),
        false);
  }
}

