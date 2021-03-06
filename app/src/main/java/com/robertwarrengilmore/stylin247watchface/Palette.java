package com.robertwarrengilmore.stylin247watchface;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.core.content.res.ResourcesCompat;

import lombok.Getter;

@Getter
class Palette {

  private static final float HOUR_HAND_WIDTH = 0.04f;
  private static final float MINUTE_HAND_WIDTH = 0.03f;
  private static final float SECOND_HAND_WIDTH = 0.01f;
  private static final float HAND_SHADOW_WIDTH = 0.01f;
  private static final float LARGE_TICK_WIDTH = 0.025f;
  private static final float SMALL_TICK_WIDTH = 0.02f;
  private static final float NUMBER_TEXT_SIZE = 0.175f;
  private static final float SOLAR_CORONA_WIDTH = 0.1f;

  private static final float AMBIENT_HOUR_DISC_STROKE_WIDTH = 0.015f;

  private final Paint hourHandPaint = new Paint();
  private final Paint minuteHandPaint = new Paint();
  private final Paint secondHandPaint = new Paint();
  private final Paint handCapPaint = new Paint();
  private final Paint smallTickPaint = new Paint();
  private final Paint largeTickPaint = new Paint();
  private final Paint numberPaint = new Paint();
  private final Paint backgroundPaint = new Paint();
  private final Paint daySectorPaint = new Paint();
  private final Paint cartoonSunPaint = new Paint();
  private final Paint realisticSunPaint = new Paint();
  private final Paint nightSectorPaint = new Paint();
  private final Paint moonLitPaint = new Paint();
  private final Paint moonDarkPaint = new Paint();
  private final Paint moonLinePaint = new Paint();

  private final float scaleFactor;

  private Palette(
      Context context, float scaleFactor
  ) {
    this.scaleFactor = scaleFactor;

    setLowBitAmbient(false);
    setBurnInProtection(false);

    hourHandPaint.setStrokeCap(Paint.Cap.ROUND);
    hourHandPaint.setStrokeWidth(HOUR_HAND_WIDTH * scaleFactor);

    minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);
    minuteHandPaint.setStrokeWidth(MINUTE_HAND_WIDTH * scaleFactor);

    secondHandPaint.setStrokeCap(Paint.Cap.ROUND);
    secondHandPaint.setStrokeWidth(SECOND_HAND_WIDTH * scaleFactor);

    handCapPaint.setStyle(Paint.Style.FILL);

    largeTickPaint.setStrokeCap(Paint.Cap.BUTT);
    largeTickPaint.setStrokeWidth(LARGE_TICK_WIDTH * scaleFactor);

    smallTickPaint.setStrokeCap(Paint.Cap.BUTT);
    smallTickPaint.setStrokeWidth(SMALL_TICK_WIDTH * scaleFactor);

    numberPaint.setTextSize(NUMBER_TEXT_SIZE * scaleFactor);
    numberPaint.setTextAlign(Paint.Align.CENTER);
    numberPaint.setTypeface(ResourcesCompat.getFont(context, R.font.ubuntu_regular));

    realisticSunPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    cartoonSunPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

    moonLinePaint.setStyle(Paint.Style.STROKE);
    moonLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

    moonDarkPaint.setStyle(Paint.Style.FILL);
    moonDarkPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

    moonLitPaint.setStyle(Paint.Style.FILL);
    moonLitPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
  }

  private void setAntiAlias(boolean value) {
    hourHandPaint.setAntiAlias(value);
    minuteHandPaint.setAntiAlias(value);
    secondHandPaint.setAntiAlias(value);
    handCapPaint.setAntiAlias(value);
    largeTickPaint.setAntiAlias(value);
    smallTickPaint.setAntiAlias(value);
    numberPaint.setAntiAlias(value);
    daySectorPaint.setAntiAlias(value);
    cartoonSunPaint.setAntiAlias(value);
    realisticSunPaint.setAntiAlias(value);
    nightSectorPaint.setAntiAlias(value);
    moonLinePaint.setAntiAlias(value);
    moonLitPaint.setAntiAlias(value);
    moonDarkPaint.setAntiAlias(value);
  }

  void setLowBitAmbient(boolean lowBitAmbient) {
    setAntiAlias(!lowBitAmbient);
    if (lowBitAmbient) {
      realisticSunPaint.clearShadowLayer();
    } else {
      realisticSunPaint.setShadowLayer(SOLAR_CORONA_WIDTH * scaleFactor,
          0,
          0,
          realisticSunPaint.getColor()
      );
    }
  }

  private static Palette getInteractivePalette(Context context, float scaleFactor) {
    Palette palette = new Palette(context, scaleFactor);

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

  static Palette getMutedPalette(Context context, float scaleFactor) {
    Palette palette = getInteractivePalette(context, scaleFactor);

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

    palette.largeTickPaint.setColor(Color.BLACK);
    palette.smallTickPaint.setColor(Color.BLACK);

    palette.numberPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));

    return palette;
  }

  static Palette getVividPalette(Context context, float scaleFactor) {
    Palette palette = getInteractivePalette(context, scaleFactor);

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

    palette.largeTickPaint.setColor(Color.BLACK);
    palette.smallTickPaint.setColor(Color.BLACK);

    palette.numberPaint.setColor(Color.HSVToColor(new float[]{0f, 0f, 0.1f}));

    return palette;
  }

  void setBurnInProtection(boolean burnInProtection) {
    cartoonSunPaint.setStyle(burnInProtection ? Paint.Style.STROKE : Paint.Style.FILL);
    realisticSunPaint.setStyle(burnInProtection ? Paint.Style.STROKE : Paint.Style.FILL);
    moonLitPaint.setAlpha(burnInProtection ? 0 : 255);
  }

  static Palette getAmbientPalette(
      Context context, float scaleFactor
  ) {
    Palette palette = new Palette(context, scaleFactor);

    palette.backgroundPaint.setColor(Color.BLACK);

    palette.daySectorPaint.setColor(Color.DKGRAY);
    palette.daySectorPaint.setStyle(Paint.Style.STROKE);
    palette.daySectorPaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);

    palette.cartoonSunPaint.setColor(Color.DKGRAY);
    palette.cartoonSunPaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);

    palette.realisticSunPaint.setColor(Color.DKGRAY);
    palette.realisticSunPaint.setStrokeWidth(AMBIENT_HOUR_DISC_STROKE_WIDTH * scaleFactor);
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

    palette.numberPaint.setColor(Color.GRAY);

    return palette;
  }
}
