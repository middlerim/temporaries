package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat {
    public static final String TAG = Middlerim.TAG + ".PREF";

    private AndroidContext context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        context = AndroidContext.get(getContext());
        context.getActivity().getSupportActionBar().hide();
        addPreferencesFromResource(R.xml.pref_general);

        findPreference("background_mode").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean backgroundMode = Boolean.valueOf(newValue.toString());

                return true;
            }
        });
        findPreference("display_name").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String displayName = newValue.toString();
                context.preferences().edit().putString(Codes.PREF_DISPLAY_NAME, displayName).apply();
                return true;
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context.getActivity().getSupportActionBar().show();
    }
}
