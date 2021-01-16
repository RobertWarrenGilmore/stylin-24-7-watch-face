package com.robertwarrengilmore.stylin247watchface;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Optional;

public class LocationCache {

  private final FusedLocationProviderClient fusedLocationClient;
  private final Context context;
  private Location cachedLocation;
  private boolean waitingOnLocationTask = false;


  LocationCache(Context context) {
    this.context = context;
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
  }

  private boolean needsUpdate() {
    // TODO Surely we want to update in cases when the location is stale as well.
    return cachedLocation == null;
  }

  private void asyncUpdate() {
    if (waitingOnLocationTask) {
      return;
    }
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      System.out.println("No permission!");
      return;
    }
    System.out.println("Getting location!");
    fusedLocationClient.getLastLocation()
        .addOnSuccessListener(this::onLocationTaskSuccess)
        .addOnCompleteListener(lastLocationTask -> {
          if (lastLocationTask.isSuccessful() && lastLocationTask.getResult() != null) {
            waitingOnLocationTask = false;
            return;
          }
          fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_LOW_POWER, null)
              .addOnSuccessListener(this::onLocationTaskSuccess)
              .addOnCompleteListener(task -> waitingOnLocationTask = false);
        });
  }

  private void onLocationTaskSuccess(Location newLocation) {
    // TODO Decide whether to replace the location. https://developer.android.com/training/location/retrieve-current#BestEstimate
    System.out.println("Got location!");
    if (newLocation == null) {
      return;
    }
    cachedLocation = newLocation;
  }

  Optional<Location> getLocation() {
    if (needsUpdate()) {
      asyncUpdate();
    }
    System.out.println("Location is " + cachedLocation + ".");
    return Optional.ofNullable(cachedLocation);
  }

  // TODO Notify the watch face when the cache is updated, so it can redraw.
}
