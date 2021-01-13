package com.robertwarrengilmore.stylin247watchface;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class Stylin247SettingsActivity extends FragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stylin247_settings);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.settings_container, new SettingsFragment())
        .commit();

  }
}