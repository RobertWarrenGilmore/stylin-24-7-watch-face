package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class LocationCache {

  private final FusedLocationProviderClient fusedLocationClient;
  private final Context context;
  private Location cachedLocation;
  private boolean waitingOnLocationTask = false;
  private Instant lastChange;
  private Instant lastCheck;
  private static final Duration COOL_DOWN_TIME = Duration.ofSeconds(5);
  private static final Duration SOFT_REFRESH_DELAY = Duration.ofSeconds(30);
  private static final Duration HARD_REFRESH_DELAY = Duration.ofMinutes(30);
  public static final float SATISFACTORY_ACCURACY = 200_000f;

  LocationCache(Context context) {
    this.context = context;
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
  }

  private boolean shouldChangeLocation(Location newLocation) {
    if (newLocation == null) {
      return false;
    }
    // We have a new location to consider,
    if (cachedLocation == null) {
      return true;
    }
    // and there is already a location in the cache,
    if (lastChange.isBefore(Instant.now().minus(SOFT_REFRESH_DELAY))) {
      return true;
    }
    // which isn't stale yet,
    if (newLocation.getAccuracy() > SATISFACTORY_ACCURACY) {
      return false;
    }
    // and the new location is reasonably precise,
    if (cachedLocation.getAccuracy() != 0f &&
        newLocation.getAccuracy() >= cachedLocation.getAccuracy()) {
      return true;
    }
    // but not as precise as the cached location.
    return false;
  }

  private void asyncUpdate() {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      return;
    }
    Task<Location> getLocationTask;
    if (lastChange == null || lastChange.isBefore(Instant.now().minus(HARD_REFRESH_DELAY))) {
      getLocationTask =
          fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_LOW_POWER, null);
    } else {
      getLocationTask = fusedLocationClient.getLastLocation();
    }
    getLocationTask.addOnCompleteListener(lastLocationTask -> {
      if (lastLocationTask.isSuccessful()) {
        lastCheck = Instant.now();
        if (lastLocationTask.getResult() != null) {
          changeLocation(lastLocationTask.getResult());
        }
      }
      waitingOnLocationTask = false;
    });
  }

  private void changeLocation(Location newLocation) {
    if (!shouldChangeLocation(newLocation)) {
      return;
    }
    cachedLocation = newLocation;
    lastChange = Instant.now();
  }

  private boolean shouldUpdate() {
    if (waitingOnLocationTask) {
      return false;
    }
    if (lastCheck == null) {
      return true;
    }
    if (lastCheck.isAfter(Instant.now().minus(COOL_DOWN_TIME))) {
      return false;
    }
    return true;
  }

  Optional<Location> getLocation() {
    if (shouldUpdate()) {
      asyncUpdate();
    }
    return Optional.ofNullable(cachedLocation);
  }
}
