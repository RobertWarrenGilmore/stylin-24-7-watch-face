package com.robertwarrengilmore.stylin247watchface;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;

import androidx.wear.widget.BoxInsetLayout;

public class Stylin247SettingsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_stylin247_settings);

    BoxInsetLayout content = findViewById(R.id.content);

    // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
    content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
      @Override
      public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        if (!insets.isRound()) {
          v.setPaddingRelative((int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
              v.getPaddingTop(),
              v.getPaddingEnd(),
              v.getPaddingBottom());
        } else {
        }
        return v.onApplyWindowInsets(insets);
      }
    });


  }
}