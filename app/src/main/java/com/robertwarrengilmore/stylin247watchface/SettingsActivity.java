package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import java.util.Map;

/* In order to clear the location permission for manual testing, run `adb pm clear com.robertwarrengilmore.stylin247watchface`. More details: https://stackoverflow.com/a/49544908/662464 */

public class SettingsActivity extends FragmentActivity {

  private final SettingsFragment settingsFragment = new SettingsFragment();
  private ActivityResultLauncher<String[]> requestLocationPermissionLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stylin247_settings);

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.settings_container, settingsFragment)
        .commitNow();

    requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
        this::onLocationPermissionAnswer
    );
    settingsFragment
        .findPreference(getString(R.string.settings_key_use_location))
        .setOnPreferenceChangeListener((Preference preference, Object newValue) -> allowChangeUseLocation(
            (boolean) newValue));
  }

  public static class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.settings, rootKey);
    }

    public RecyclerView onCreateRecyclerView(
        LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState
    ) {
      // This is mostly plagiarised from the method it overrides. The main difference is we use a *wearable* recycler view.
      WearableRecyclerView wearableRecyclerView = (WearableRecyclerView) inflater.inflate(R.layout.settings_wearable_recycler_view,
          parent,
          false
      );
      wearableRecyclerView.setEdgeItemsCenteringEnabled(true);

      wearableRecyclerView.setLayoutManager(onCreateLayoutManager());
      wearableRecyclerView.setAccessibilityDelegateCompat(new PreferenceRecyclerViewAccessibilityDelegate(
          wearableRecyclerView));

      return wearableRecyclerView;
    }
  }

  private void onLocationPermissionAnswer(Map<String, Boolean> values) {
    boolean isGranted = values.containsValue(true);
    if (!isGranted) {
      Toast.makeText(getApplicationContext(),
          getString(R.string.location_permission_denied),
          Toast.LENGTH_LONG
      ).show();
    } else {
      boolean changedInStorage = settingsFragment
          .getPreferenceManager()
          .getSharedPreferences()
          .edit()
          .putBoolean(getString(R.string.settings_key_use_location), true)
          .commit();
      if (changedInStorage) {
        ((SwitchPreference) settingsFragment.findPreference(getString(R.string.settings_key_use_location)))
            .setChecked(true);
      }
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
    requestLocationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION});
    return false;
  }

  private void setBusyDialogueVisible(boolean visible) {
    findViewById(R.id.busyOverlay).setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  private boolean hasLocationPermission() {
    return ActivityCompat.checkSelfPermission(getApplicationContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        getApplicationContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED;
  }
}