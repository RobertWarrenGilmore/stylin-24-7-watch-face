package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

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


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stylin247_settings);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.settings_container, settingsFragment)
        .commitNow();

    settingsKeyUseLocation = getApplicationContext().getString(R.string.settings_key_use_location);

    requestLocationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            this::setUseLocation);
    listenToUseLocationChange();
  }

  public static class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.settings, rootKey);
    }
  }

  private void setUseLocation(boolean value) {
    System.out.println("Setting use_location to " + value);
    boolean
        changedInStorage =
        settingsFragment.getPreferenceManager()
            .getSharedPreferences()
            .edit()
            .putBoolean(settingsKeyUseLocation, value)
            .commit();
    System.out.println("Did we change the preference in storage?: " + changedInStorage);
    if (changedInStorage) {
      ((SwitchPreference) settingsFragment.findPreference(settingsKeyUseLocation)).setChecked(value);
    }
  }

  public void listenToUseLocationChange() {
    settingsFragment.findPreference(settingsKeyUseLocation)
        .setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
          System.out.println("Changing use_location.");
          // Setting to false is okay.
          if (!(boolean) newValue) {
            System.out.println("Set to false.");
            return true;
          }
          System.out.println("Set to true.");
          // If we already have permission, no need to ask.
          if (hasLocationPermission()) {
            System.out.println("Already have permission.");
            return true;
          }
          // No permission yet. We will only set the setting if we get permission.
          System.out.println("Asking for permission.");
          requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
          System.out.println("Done synchronously calling the launcher.");
          // TODO Detect when the permission has been denied permanently?
          // TODO Update the setting if the location permission has been revoked externally.
          return true;
        });
  }

  private boolean hasLocationPermission() {
    return ActivityCompat.checkSelfPermission(getApplicationContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }
}