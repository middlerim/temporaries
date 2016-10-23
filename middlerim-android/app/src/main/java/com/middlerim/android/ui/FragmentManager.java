package com.middlerim.android.ui;

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class FragmentManager {


    private android.support.v4.app.FragmentManager fm;

    public FragmentManager(FragmentActivity activity) {
        fm = activity.getSupportFragmentManager();
    }

    @SuppressWarnings("unchecked")
    public <F extends Fragment> F findById(@IdRes int id) {
        return (F) fm.findFragmentById(id);
    }

    public boolean openMap(Fragment current) {
        FragmentTransaction transaction = fm.beginTransaction();
        if (current != null) {
            transaction.hide(current);
            transaction.addToBackStack(null);
        }
        openMapFragment(transaction);
        openAreaSelectorFragment(transaction);
        hideStreamFragment(transaction);
        transaction.commit();
        return true;
    }

    public boolean openNewMessage(Fragment current) {
        FragmentTransaction transaction = fm.beginTransaction();
        // transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);

        if (current != null) {
            transaction.hide(current);
            transaction.addToBackStack(null);
        }
        openNewMessageFragment(transaction);

        transaction.commit();
        return true;
    }


    private void hideStreamFragment(FragmentTransaction transaction) {
        StreamFragment fragment = findById(R.id.fragment_stream);
        if (fragment != null && fragment.isVisible()) {
            transaction.hide(fragment);
        }
    }


    private void openNewMessageFragment(FragmentTransaction transaction) {
        NewMessageFragment fragment = findById(R.id.fragment_new_message);
        if (fragment == null) {
            fragment = new NewMessageFragment();
            transaction.add(R.id.middlerim, fragment);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
    }

    private void openMapFragment(FragmentTransaction transaction) {
        MapFragment fragment = findById(R.id.fragment_map);
        if (fragment == null) {
            fragment = new MapFragment();
            transaction.add(R.id.middlerim, fragment);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
    }

    private void openAreaSelectorFragment(FragmentTransaction transaction) {
        AreaSelectorFragment fragment = findById(R.id.fragment_area_selector);
        if (fragment == null) {
            fragment = new AreaSelectorFragment();
            transaction.add(R.id.middlerim, fragment);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
    }
}
