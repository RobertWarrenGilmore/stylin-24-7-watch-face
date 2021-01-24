package com.robertwarrengilmore.stylin247watchface;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.SwitchPreferenceCompat;

public class CustomSwitchPreference extends SwitchPreferenceCompat {

  public CustomSwitchPreference(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes
  ) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public CustomSwitchPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomSwitchPreference(Context context) {
    super(context);
  }

  // This override skips the step of checking isEnabled.
  @Override
  public boolean shouldDisableDependents() {
    return getDisableDependentsState() == isChecked();
  }
}
