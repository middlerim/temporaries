package com.middlerim.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

public class GeneralPreferenceFragment extends PreferenceFragmentCompat {
    public static final String TAG = GeneralPreferenceFragment.class.getName();

    public static boolean open(Middlerim activity) {
        GeneralPreferenceFragment fragment = (GeneralPreferenceFragment) activity.getSupportFragmentManager().findFragmentByTag(GeneralPreferenceFragment.TAG);
        if (fragment == null) {
            fragment = new GeneralPreferenceFragment();
        } else if (fragment.isVisible()) {
            return false;
        }
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.middlerim, fragment, GeneralPreferenceFragment.TAG);
        transaction.addToBackStack(null);
        transaction.commit();
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        ((Middlerim) getActivity()).getSupportActionBar().hide();
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((Middlerim) getActivity()).getSupportActionBar().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), Middlerim.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
