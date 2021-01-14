package com.robertwarrengilmore.stylin247watchface;

import android.location.Location;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Calendar;

class AstronomyCalculator {

  private static final float MAXIMUM_SUN_DECLINATION = 23.5f;
  private static final Duration
      LUNAR_CYCLE_LENGTH =
      Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2);
  private static final Instant KNOWN_NEW_MOON_INSTANT = Instant.parse("2021-01-13T05:00:00Z");

  /**
   * Calculates the approximate phase of the moon on a given date. The result is expressed as a float 0 <= x < 1, where 0 is a new moon, 0.5 is a full moon, and 0.9 is a waning crescent.
   */
  static float getLunarPhase(Calendar when) {
    Duration timeSinceKnownNewMoon = Duration.between(KNOWN_NEW_MOON_INSTANT, when.toInstant());
    long secondsInLunarCycle = LUNAR_CYCLE_LENGTH.getSeconds();
    long
        secondsIntoLunarCycle =
        Math.floorMod(timeSinceKnownNewMoon.getSeconds(), LUNAR_CYCLE_LENGTH.getSeconds());
    return (float) ((double) secondsIntoLunarCycle / secondsInLunarCycle);
  }

  /**
   * Calculates the approximate day length at a given location on a given date.
   * <p>
   * Source: http://www.jgiesen.de/astro/solarday.htm
   */
  static Duration getSolarDayLength(Location location, Calendar when) {
    final float latitude = (float) location.getLatitude();
    final float sunDeclination = getSolarDeclination(when);
    final float
        localSunHourAngle =
        (float) (Math.acos(-1 *
            Math.tan(Math.toRadians(latitude)) *
            Math.tan(Math.toRadians(sunDeclination))) / Math.PI);
    if (Float.isNaN(localSunHourAngle)) {
      // We must be in the arctic or the antarctic during summer or winter. The day length is either 24 hours or 0.
      final boolean inNorthernHemisphere = latitude > 0;
      final boolean isNorthernSummer = sunDeclination < 0;
      if (inNorthernHemisphere ^ isNorthernSummer) {
        return Duration.ZERO;
      }
      return Duration.ofDays(1);
    }
    return Duration.ofSeconds((long) (Duration.ofDays(1).getSeconds() * localSunHourAngle));
  }

  /**
   * Calculates how many degrees north (positive) or south (negative) of the equator the sun is on a given date.
   */
  private static float getSolarDeclination(Calendar when) {
    final int vernalEquinoxDayOfYear = /* January */
        31 + /* February */ 28 +  /* 21st of March */ 21;
    final float
        daysSinceVernalEquinox =
        Math.floorMod(when.get(Calendar.DAY_OF_YEAR) - vernalEquinoxDayOfYear,
            when.getActualMaximum(Calendar.DAY_OF_YEAR));
    final float
        yearFraction =
        (float) daysSinceVernalEquinox / when.getActualMaximum(Calendar.DAY_OF_YEAR);
    return MAXIMUM_SUN_DECLINATION * (float) Math.sin(Math.toRadians(yearFraction * 360));
  }

  /**
   * Calculates the approximate civil time in the given time zone of astronomical noon in the given location on a given date.
   * <p>
   * The time zone is not derived from the location because the user may use a time zone different to that of his location. (For example, residents of Fort Pierre, South Dakota customarily use the time zone of Pierre, rather than the one in which Fort Pierre is technically located.)
   */
  static LocalTime getSolarNoon(Location location, Calendar when) {
    final long
        timeZoneOffsetSeconds =
        when.getTimeZone().toZoneId().getRules().getOffset(when.toInstant()).getTotalSeconds();
    final long
        astronomicalTimeOffsetSeconds =
        (long) (location.getLongitude() / 360 * (24 * 60 * 60 - 1));
    return LocalTime.NOON.plusSeconds(timeZoneOffsetSeconds - astronomicalTimeOffsetSeconds);
  }
}
