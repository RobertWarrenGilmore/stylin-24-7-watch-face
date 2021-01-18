package com.robertwarrengilmore.stylin247watchface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.time.Duration;
import java.util.Optional;

import lombok.Getter;

public class LocationCache {

  private final FusedLocationProviderClient fusedLocationClient;
  private Location cachedLocation;

  private static final Duration FASTEST_INTERVAL = Duration.ofSeconds(1);
  private static final Duration INTERVAL = Duration.ofHours(6);
  private static final float SMALLEST_DISPLACEMENT = 200_000f;
  @Getter
  private boolean updating = false;

  private final LocationCallback locationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      super.onLocationResult(locationResult);
      cachedLocation = locationResult.getLastLocation();
    }
  };

  LocationCache(Context context) {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
  }

  @SuppressLint("MissingPermission")
  void startUpdating() {
    fusedLocationClient.requestLocationUpdates(new LocationRequest().setSmallestDisplacement(
        SMALLEST_DISPLACEMENT)
        .setPriority(LocationRequest.PRIORITY_LOW_POWER)
        .setFastestInterval(FASTEST_INTERVAL.toMillis())
        .setInterval(INTERVAL.toMillis()), locationCallback, Looper.myLooper());
    updating = true;
  }

  void stopUpdating() {
    fusedLocationClient.removeLocationUpdates(locationCallback);
    updating = false;
  }

  Optional<Location> getLocation() {
    return Optional.ofNullable(cachedLocation);
  }
}
