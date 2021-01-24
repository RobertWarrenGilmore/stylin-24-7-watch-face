package com.robertwarrengilmore.stylin247watchface;

import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.widget.TextView;

public class LicenceReaderActivity extends WearablePreferenceActivity {

  private TextView mTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_licence_reader);

    mTextView = (TextView) findViewById(R.id.text);

    // Enables Always-on
    setAmbientEnabled();
  }
}