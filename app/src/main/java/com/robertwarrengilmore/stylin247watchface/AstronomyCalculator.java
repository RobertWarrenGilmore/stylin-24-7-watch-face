package com.robertwarrengilmore.stylin247watchface;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;

class AstronomyCalculator {

  private static Duration
      LUNAR_CYCLE_LENGTH =
      Duration.ofDays(29).plusHours(12).plusMinutes(44).plusSeconds(2);
  private static Instant KNOWN_NEW_MOON_INSTANT = Instant.parse("2021-01-13T05:00:00Z");

  /**
   * Calculates the approximate phase of the moon on a given date. The result is expressed as a float 0 <= x < 1, where 0 is a new moon, 0.5 is a full moon, and 0.9 is a waning crescent.
   */
  static float getMoonPhase(Instant when) {
    Duration timeSinceKnownNewMoon = Duration.between(KNOWN_NEW_MOON_INSTANT, when);
    long secondsInLunarCycle = LUNAR_CYCLE_LENGTH.getSeconds();
    long
        secondsIntoLunarCycle =
        Math.floorMod(timeSinceKnownNewMoon.getSeconds(), LUNAR_CYCLE_LENGTH.getSeconds());
    return (float) ((double) secondsIntoLunarCycle / secondsInLunarCycle);
  }
}
