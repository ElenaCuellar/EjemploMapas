package com.example.caxidy.ejemplomapas;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ActivityPref extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
