package com.robertwarrengilmore.stylin247watchface;

import android.graphics.Color;
import android.graphics.Paint;

import lombok.Getter;

@Getter
class Palette {

  private static final float HOUR_HAND_WIDTH = 0.04f;
  private static final float MINUTE_HAND_WIDTH = 0.03f;
  private static final float SECOND_HAND_WIDTH = 0.01f;
  private static final float HAND_SHADOW_WIDTH = 0.01f;
  private static final float LARGE_TICK_WIDTH = 0.02f;
  private static final float SMALL_TICK_WIDTH = 0.0125f;
  private static final float SOLAR_CORONA_WIDTH = 0.1f;

  private static final float AMBIENT_HOUR_DISC_STROKE_WIDTH = 0.015f;

  private final Paint hourHandPaint = new Paint();
  private final Paint minuteHandPaint = new Paint();
  private final Paint secondHandPaint = new Paint();
  private final Paint handCapPaint = new Paint();
  private final Paint smallTickPaint = new Paint();
  private final Paint largeTickPaint = new Paint();
  private final Paint backgroundPaint = new Paint();
  private final Paint daySectorPaint = new Paint();
  private final Paint cartoonSunPaint = new Paint();
  private final Paint realisticSunPaint = new Paint();
  private final Paint nightSectorPaint = new Paint();
  private final Paint moonLitPaint = new Paint();
  private final Paint moonDarkPaint = new Paint();
  private final Paint moonLinePaint = new Paint();

  private static Palette getCommonPalette(float scaleFactor) {
    Palette palette = new Palette();

    palette.hourHandPaint.setAntiAlias(true);
    palette.hourHandPaint.setStrokeCap(Paint.Cap.ROUND);
    palette.hourHandPaint.setStrokeWidth(HOUR_HAND_WIDTH * scaleFactor);

    palette.minuteHandPaint.setAntiAlias(true);
    palette.minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);
    palette.minuteHandPaint.setStrokeWidth(MINUTE_HAND_WIDTH * scaleFactor);

    palette.secondHandPaint.setAntiAlias(true);
    palette.secondHandPaint.setStrokeCap(Paint.Cap.ROUND);
    palette.secondHandPaint.setStrokeWidth(SECOND_HAND_WIDTH * scaleFactor);

    palette.handCapPaint.setAntiAlias(true);
    palette.handCapPaint.setStyle(Paint.Style.FILL);

    palette.largeTickPaint.setAntiAlias(true);
    palette.largeTickPaint.setStrokeCap(Paint.Cap.BUTT);
    palette.largeTickPaint.setStrokeWidth(LARGE_TICK_WIDTH * scaleFactor);

    palette.smallTickPaint.setAntiAlias(true);
    palette.smallTickPaint.setStrokeCap(Paint.Cap.BUTT);
    palette.smallTickPaint.setStrokeWidth(SMALL_TICK_WIDTH * scaleFactor);

    palette.daySectorPaint.setAntiAlias(true);

    palette.cartoonSunPaint.setAntiAlias(true);
    palette.cartoonSunPaint.setStyle(Paint.Style.FILL);

    palette.realisticSunPaint.setAntiAlias(true);
    palette.realisticSunPaint.setStyle(Paint.Style.FILL);

    palette.nightSectorPaint.setAntiAlias(true);

    palette.moonLinePaint.setAntiAlias(true);
    palette.moonLinePaint.setStyle(Paint.Style.STROKE);

    palette.moonLitPaint.setAntiAlias(true);
    palette.moonLitPaint.setStyle(Paint.Style.FILL);

    palette.moonDarkPaint.setAntiAlias(true);
    palette.moonDarkPaint.setStyle(Paint.Style.FILL);

    return palette;
  }

  private static Palette getInteractivePalette(float scaleFactor) {
    Palette palette = getCommonPalette(scaleFactor);

    palette.daySectorPaint.setStyle(Paint.Style.FILL);

    palette.nightSectorPaint.setStyle(Paint.Style.FILL);

    palette.moonLinePaint.setColor(Color.TRANSPARENT);

    palette.hourHandPaint.setColor(Color.BLACK);
    palette.minuteHandPaint.setColor(Color.BLACK);
    palette.secondHandPaint.setColor(Color.HSVToColor(new float[]{0f, 0.75f, 0.75f}));
    palette.handCapPaint.setColor(Color.BLACK);

    palette.hourHandPaint.setShadowLayer(HAND_SHADOW_WIDTH * scaleFactor, 0, 0, Color.BLACK);
    palette.minuteHandPaint.setShadowLayer(HAND_SHADOW_WIDTH * scaleFactor, 0, 0, Color.BLACK);
    palette.secondHandPaint.setShadowLayer(HAND_SHADOW_WIDTH * scaleFactor, 0, 0, Color.BLACK);
    palette.handCapPaint.setShadowLayer(HAND_SHADOW_WIDTH * scaleFactor, 0, 0, Color.BLACK);

    return palette;
  }

  static Palette getMutedPalette(float scaleFactor) {
    Palette palette = getInteractivePalette(scaleFactor);

    palette.daySectorPaint.setColor(Color.HSVToColor(new float[]{200f, 0.25f, 0.6f}));

    palette.cartoonSunPaint.setColor(Color.HSVToColor(new float[]{45f, 0.3f, 1f}));

    palette.realisticSunPaint.setColor(Color.HSVToColor(new float[]{45f, 0.3f, 1f}));
    palette.realisticSunPaint.setShadowLayer(SOLAR_CORONA_WIDTH * scaleFactor,
        0,
        0,
        palette.realisticSunPaint.getColor()
    );

    palette.nightSectorPaint.setColor(Color.HSVToColor(new float[]{230f, 0.25f, 0.25f}));

    palette.moonLitPaint.setColor(Color.WHITE);

    palette.moonDarkPaint.setColor(Color.BLACK);

    palette.backgroundPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.3f}));

    palette.largeTickPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));
    palette.smallTickPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));

    return palette;
  }

  static Palette getVividPalette(float scaleFactor) {
    Palette palette = getInteractivePalette(scaleFactor);

    palette.daySectorPaint.setColor(Color.HSVToColor(new float[]{185f, 1f, 1f}));

    palette.cartoonSunPaint.setColor(Color.HSVToColor(new float[]{45f, 0.5f, 1f}));

    palette.realisticSunPaint.setColor(Color.HSVToColor(new float[]{45f, 0.5f, 1f}));
    palette.realisticSunPaint.setShadowLayer(SOLAR_CORONA_WIDTH * scaleFactor,
        0,
        0,
        palette.realisticSunPaint.getColor()
    );

    palette.nightSectorPaint.setColor(Color.HSVToColor(new float[]{217f, 0.9f, 0.5f}));

    palette.moonLitPaint.setColor(Color.WHITE);

    palette.moonDarkPaint.setColor(Color.BLACK);

    palette.backgroundPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.3f}));

    palette.largeTickPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));
    palette.smallTickPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));

    return palette;
  }

  static Palette getAmbientPalette(float scaleFactor) {
    Palette palette = getCommonPalette(scaleFactor);

    palette.backgroundPaint.setColor(Color.BLACK);

    palette.daySectorPaint.setColor(Color.DKGRAY);
    palette.daySectorPaint.setStyle(Paint.Style.STROKE);
    palette.daySectorPaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);

    palette.cartoonSunPaint.setColor(Color.DKGRAY);

    palette.realisticSunPaint.setColor(Color.DKGRAY);
    palette.realisticSunPaint.setShadowLayer(SOLAR_CORONA_WIDTH * scaleFactor,
        0,
        0,
        palette.realisticSunPaint.getColor()
    );

    palette.nightSectorPaint.setColor(Color.DKGRAY);
    palette.nightSectorPaint.setStyle(Paint.Style.STROKE);
    palette.nightSectorPaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);

    palette.moonLinePaint.setColor(Color.DKGRAY);
    palette.moonLinePaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);

    palette.moonLitPaint.setColor(Color.DKGRAY);

    palette.moonDarkPaint.setColor(Color.BLACK);

    palette.hourHandPaint.setColor(Color.LTGRAY);
    palette.hourHandPaint.clearShadowLayer();

    palette.minuteHandPaint.setColor(Color.LTGRAY);
    palette.minuteHandPaint.clearShadowLayer();

    palette.secondHandPaint.setColor(Color.LTGRAY);
    palette.secondHandPaint.clearShadowLayer();

    palette.handCapPaint.setColor(Color.LTGRAY);
    palette.handCapPaint.clearShadowLayer();

    palette.largeTickPaint.setColor(Color.GRAY);
    palette.smallTickPaint.setColor(Color.GRAY);

    return palette;
  }
}
