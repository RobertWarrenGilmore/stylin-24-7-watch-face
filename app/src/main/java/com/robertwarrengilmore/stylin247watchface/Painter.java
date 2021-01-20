package com.robertwarrengilmore.stylin247watchface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;

import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;

class Painter {


  public static final float SUN_RAY_WIDTH_DEGREES = 20;
  public static final float SUN_RAY_LENGTH = 0.35f;
  public static final float SUN_RAY_OFFSET = 0.2f;
  private static final float HOUR_DISC_RADIUS = 0.667f;
  private static final float SUN_AND_MOON_RADIUS = 0.15f;
  private static final float SUN_AND_MOON_CENTRE_OFFSET = 0.3f;

  static void draw(
      Canvas canvas,
      Rect bounds,
      Palette palette,
      Calendar calendar,
      @Nullable Location location,
      boolean drawRealisticSun,
      boolean showSingleMinuteTicks,
      boolean showSecondHand,
      boolean animateSecondHandSmoothly
  ) {
    final PointF centre = new PointF(bounds.width() / 2f, bounds.height() / 2f);
    final float faceRadius = bounds.width() / 2f;

    drawBackground(canvas, palette, centre, faceRadius, calendar, location, drawRealisticSun);
    drawTicks(canvas, palette, centre, faceRadius, showSingleMinuteTicks);
    drawHands(canvas,
        palette,
        centre,
        faceRadius,
        calendar,
        showSecondHand,
        animateSecondHandSmoothly
    );
  }

  private static void drawBackground(
      Canvas canvas,
      Palette palette,
      PointF centre,
      float faceRadius,
      Calendar calendar,
      @Nullable Location location,
      boolean drawRealisticSun
  ) {
    canvas.drawPaint(palette.getBackgroundPaint());

    float hourDiscRadius = HOUR_DISC_RADIUS * faceRadius;

    RectF boundingBox = new RectF(centre.x - hourDiscRadius,
        centre.y - hourDiscRadius,
        centre.x + hourDiscRadius,
        centre.y + hourDiscRadius
    );

    final Duration solarDayLength = (location != null) ?
                                    AstronomyCalculator.getSolarDayLength(location, calendar) :
                                    Duration.ofHours(12);
    final LocalTime solarNoon = (location != null) ?
                                AstronomyCalculator.getSolarNoon(location, calendar) :
                                LocalTime.NOON;


    final float noonOffsetDayFraction = solarNoon.toSecondOfDay() / (24f * 60 * 60 - 1);
    final float dayLengthFraction = solarDayLength.getSeconds() / (24f * 60 * 60 - 1);

    final float sunriseOffsetFraction = noonOffsetDayFraction - (dayLengthFraction / 2);
    canvas.drawArc(boundingBox,
        90 + sunriseOffsetFraction * 360,
        dayLengthFraction * 360,
        true,
        palette.getDaySectorPaint()
    );
    canvas.drawArc(boundingBox,
        90 + sunriseOffsetFraction * 360 + dayLengthFraction * 360,
        360 - dayLengthFraction * 360,
        true,
        palette.getNightSectorPaint()
    );

    final float noonAngle = noonOffsetDayFraction * 360 + 180;

    drawSun(canvas,
        palette,
        cartesian(centre, noonAngle, SUN_AND_MOON_CENTRE_OFFSET * faceRadius),
        SUN_AND_MOON_RADIUS * faceRadius,
        drawRealisticSun
    );
    float lunarPhase = AstronomyCalculator.getLunarPhase(calendar);

    drawMoon(canvas,
        palette,
        cartesian(centre, noonAngle + 180f, SUN_AND_MOON_CENTRE_OFFSET * faceRadius),
        SUN_AND_MOON_RADIUS * faceRadius,
        lunarPhase
    );
  }

  private static void drawSun(
      Canvas canvas, Palette palette, PointF centre, float radius, boolean drawRealisticSun
  ) {
    if (drawRealisticSun) {
      canvas.drawCircle(centre.x, centre.y, radius, palette.getRealisticSunPaint());
      return;
    }

    canvas.drawCircle(centre.x, centre.y, radius, palette.getCartoonSunPaint());
    final float rayOffset = radius * SUN_RAY_OFFSET;
    final float rayLength = radius * SUN_RAY_LENGTH;
    for (int rayIndex = 0; rayIndex < 12; rayIndex++) {
      final float rayTipAngle = (float) (rayIndex * 360 / 12);
      final PointF rayTip = cartesian(centre, rayTipAngle, radius + rayOffset + rayLength);
      final PointF rayBottomLeft = cartesian(centre,
          rayTipAngle - SUN_RAY_WIDTH_DEGREES / 2,
          rayOffset
      );
      final PointF rayBottomRight = cartesian(centre,
          rayTipAngle + SUN_RAY_WIDTH_DEGREES / 2,
          rayOffset
      );
      final float rayBottomRadius = (radius + rayOffset);
      final RectF rayOffsetBoundingBox = new RectF(centre.x - rayBottomRadius,
          centre.y - rayBottomRadius,
          centre.x + rayBottomRadius,
          centre.y + rayBottomRadius
      );
      final Path ray = new Path();
      ray.moveTo(rayTip.x, rayTip.y);
      ray.arcTo(rayOffsetBoundingBox,
          rayTipAngle - SUN_RAY_WIDTH_DEGREES / 2 - 90,
          SUN_RAY_WIDTH_DEGREES
      );
      ray.close();
      canvas.drawPath(ray, palette.getCartoonSunPaint());
    }
  }

  private static void drawMoon(
      Canvas canvas, Palette palette, PointF centre, float radius, float phase
  ) {
    final float curveOffset = (float) Math.cos(phase * 2 * Math.PI) * radius;
    final float crescentWidth = radius - Math.abs(curveOffset);
    final boolean drawCurve = crescentWidth >= 0.25;
    final boolean mostlyLit = phase > 0.25f && phase < 0.75f;
    final boolean fullMoon = !drawCurve && mostlyLit;

    if (fullMoon) {
      canvas.drawCircle(centre.x, centre.y, radius, palette.getMoonLitPaint());
    } else {
      canvas.drawCircle(centre.x, centre.y, radius, palette.getMoonDarkPaint());
    }
    if (drawCurve) {
      final boolean litOnRight = phase > 0.5f;
      final boolean curveGoesRight = litOnRight ^ mostlyLit;
      canvas.drawArc(centre.x - radius,
          centre.y - radius,
          centre.x + radius,
          centre.y + radius,
          litOnRight ? 90f : 270f,
          180f,
          true,
          palette.getMoonLitPaint()
      );
      canvas.drawOval(centre.x - curveOffset,
          centre.y - radius,
          centre.x + curveOffset,
          centre.y + radius,
          mostlyLit ? palette.getMoonLitPaint() : palette.getMoonDarkPaint()
      );
      canvas.drawArc(centre.x - curveOffset,
          centre.y - radius,
          centre.x + curveOffset,
          centre.y + radius,
          curveGoesRight ? 90f : 270f,
          180f,
          false,
          palette.getMoonLinePaint()
      );
    }
    canvas.drawCircle(centre.x, centre.y, radius, palette.getMoonLinePaint());
  }

  private static final float HOUR_HAND_LENGTH = 0.525f;
  private static final float MINUTE_HAND_LENGTH = 0.9f;
  private static final float SECOND_HAND_LENGTH = 0.9f;
  private static final float HAND_CAP_RADIUS = 0.03f;
  private static final float LARGE_TICK_LENGTH = 0.11f;
  private static final float SMALL_TICK_LENGTH = 0.05f;
  private static final float MINUTE_TICK_OUTER_RADIUS = 1f;

  private static void drawTicks(
      Canvas canvas, Palette palette, PointF centre, float faceRadius, boolean showSingleMinuteTicks
  ) {
    // Draw the minute ticks.
    for (int minuteIndex = 0; minuteIndex < 60; minuteIndex++) {
      final float angle = (float) (minuteIndex * 360 / 60);
      if (minuteIndex % 5 == 0) {
        drawTick(canvas,
            centre,
            angle,
            MINUTE_TICK_OUTER_RADIUS * faceRadius,
            LARGE_TICK_LENGTH * faceRadius,
            palette.getLargeTickPaint()
        );
      } else if (showSingleMinuteTicks) {
        drawTick(canvas,
            centre,
            angle,
            MINUTE_TICK_OUTER_RADIUS * faceRadius,
            SMALL_TICK_LENGTH * faceRadius,
            palette.getSmallTickPaint()
        );
      }
    }
    // Draw the hour ticks.
    for (int tickIndex = 0; tickIndex < 24; tickIndex++) {
      float angle = (float) (tickIndex * 360 / 24) + 180;
      if (tickIndex % 3 == 0) {
        drawTick(canvas,
            centre,
            angle,
            HOUR_DISC_RADIUS * faceRadius,
            LARGE_TICK_LENGTH * faceRadius,
            palette.getLargeTickPaint()
        );
      } else {
        drawTick(canvas,
            centre,
            angle,
            HOUR_DISC_RADIUS * faceRadius,
            SMALL_TICK_LENGTH * faceRadius,
            palette.getSmallTickPaint()
        );
      }
    }
  }

  private static void drawTick(
      Canvas canvas, PointF centre, float angle, float outerRadius, float length, Paint paint
  ) {
    PointF inside = cartesian(centre, angle, outerRadius - length);
    PointF outside = cartesian(centre, angle, outerRadius);
    canvas.drawLine(inside.x, inside.y, outside.x, outside.y, paint);
  }

  private static void drawHands(
      Canvas canvas,
      Palette palette,
      PointF centre,
      float faceRadius,
      Calendar calendar,
      boolean showSecondHand,
      boolean animateSecondHandSmoothly
  ) {
    /*
     * These calculations reflect the rotation in degrees per unit of time, e.g.,
     * 360 / 60 = 6 and 360 / 12 = 30.
     */
    final float partialSecond = animateSecondHandSmoothly ?
                                calendar.get(Calendar.MILLISECOND) / 1000f :
                                0;
    final float seconds = calendar.get(Calendar.SECOND) + partialSecond;
    final float secondsRotation = seconds * (360 / 60f);

    // In ambient mode, the minute hand should tick instead of moving gradually.
    final float partialMinute = showSecondHand ? (calendar.get(Calendar.SECOND) / 60f) : 0;
    final float minutesRotation = (calendar.get(Calendar.MINUTE) + partialMinute) * (360 / 60f);

    final float partialHour = calendar.get(Calendar.MINUTE) / 60f;
    // The hour hand moves 15 degrees per hour on a 24-hour clock, not 30.
    final float hoursRotation =
        ((calendar.get(Calendar.HOUR_OF_DAY) + partialHour) * (360 / 24f)) + 180;

    PointF secondHandEnd = cartesian(centre, secondsRotation, SECOND_HAND_LENGTH * faceRadius);
    PointF minuteHandEnd = cartesian(centre, minutesRotation, MINUTE_HAND_LENGTH * faceRadius);
    PointF hourHandEnd = cartesian(centre, hoursRotation, HOUR_HAND_LENGTH * faceRadius);
    canvas.drawLine(centre.x, centre.y, hourHandEnd.x, hourHandEnd.y, palette.getHourHandPaint());
    canvas.drawLine(centre.x,
        centre.y,
        minuteHandEnd.x,
        minuteHandEnd.y,
        palette.getMinuteHandPaint()
    );
    if (showSecondHand) {
      canvas.drawLine(centre.x,
          centre.y,
          secondHandEnd.x,
          secondHandEnd.y,
          palette.getSecondHandPaint()
      );
    }
    canvas.drawCircle(centre.x, centre.y, HAND_CAP_RADIUS * faceRadius, palette.getHandCapPaint());
  }

  /**
   * Convert radial coordinates to a cartesian point.
   */
  private static PointF cartesian(PointF origin, float angle, float radius) {
    final float x = origin.x + (float) Math.sin(Math.toRadians(angle)) * radius;
    final float y = origin.y + (float) -Math.cos(Math.toRadians(angle)) * radius;
    return new PointF(x, y);
  }
}
