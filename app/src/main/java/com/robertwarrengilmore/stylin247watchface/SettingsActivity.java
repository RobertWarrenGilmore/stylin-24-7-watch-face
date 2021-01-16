package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

/* In order to clear the location permission for manual testing, run `adb pm clear com.robertwarrengilmore.stylin247watchface`. More details: https://stackoverflow.com/a/49544908/662464 */

public class SettingsActivity extends FragmentActivity {


  private final SettingsFragment settingsFragment = new SettingsFragment();

  private ActivityResultLauncher<String> requestLocationPermissionLauncher;

  private String settingsKeyUseLocation;

  private Settings settings;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stylin247_settings);

    settings = new Settings(getApplicationContext());
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.settings_container, settingsFragment)
        .commitNow();

    settingsKeyUseLocation = getApplicationContext().getString(R.string.settings_key_use_location);
    requestLocationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            this::onLocationPermissionAnswer);
    settingsFragment.findPreference(settingsKeyUseLocation)
        .setOnPreferenceChangeListener((Preference preference, Object newValue) -> allowChangeUseLocation(
            (boolean) newValue));
  }

  public static class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.settings, rootKey);
    }
  }

  private void onLocationPermissionAnswer(boolean value) {
    if (!value) {
      Toast.makeText(getApplicationContext(),
          getApplicationContext().getString(R.string.location_permission_denied),
          Toast.LENGTH_LONG).show();
    }
    boolean changedInStorage = settings.setUseLocation(value);
    if (changedInStorage) {
      ((SwitchPreference) settingsFragment.findPreference(settingsKeyUseLocation)).setChecked(value);
    }
    setBusyDialogueVisible(false);
  }

  private boolean allowChangeUseLocation(boolean value) {
    // Setting to false is okay.
    if (!value) {
      return true;
    }
    // If we already have permission, no need to ask.
    if (hasLocationPermission()) {
      return true;
    }
    // No permission yet. Let the permission dialogue callback (onLocationPermissionAnswer) set the preference.
    setBusyDialogueVisible(true);
    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
    // TODO Detect when the permission has been denied permanently?
    return false;
  }

  private void setBusyDialogueVisible(boolean visible) {
    findViewById(R.id.busyOverlay).setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  private boolean hasLocationPermission() {
    return ActivityCompat.checkSelfPermission(getApplicationContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }
}