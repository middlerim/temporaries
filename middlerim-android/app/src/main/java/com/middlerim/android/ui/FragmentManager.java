package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import java.util.ArrayDeque;

public class FragmentManager {

    private static ArrayDeque<Fragment[]> fragmentstack;
    private static android.support.v4.app.FragmentManager fm;

    public enum Page {
        MinuteMessage
    }

    private FragmentActivity activity;

    public FragmentManager(FragmentActivity activity) {
        this.activity = activity;
    }

    private android.support.v4.app.FragmentManager fm() {
        if (fm != null && !fm.isDestroyed()) {
            return fm;
        }
        synchronized (FragmentManager.class) {
            if (fm != null && !fm.isDestroyed()) {
                return fm;
            }
            fragmentstack = new ArrayDeque<>(5);
            fm = activity.getSupportFragmentManager();
            fm.addOnBackStackChangedListener(new android.support.v4.app.FragmentManager.OnBackStackChangedListener() {
                private int lastStackSize = 0;

                @Override
                public void onBackStackChanged() {
                    int currentStackSize = fm.getBackStackEntryCount();
                    if (lastStackSize >= currentStackSize) {
                        // Back button was pressed.
                        fragmentstack.pop();
                    }
                    lastStackSize = currentStackSize;
                }
            });
            return fm;
        }
    }

    public MapFragment getMapFragment() {
        return (MapFragment) fm().findFragmentByTag(MapFragment.TAG);
    }

    public MainFragment getMainFragment() {
        return (MainFragment) fm().findFragmentByTag(MainFragment.TAG);
    }

    public AreaSelectorFragment getAreaSelectorFragment() {
        return (AreaSelectorFragment) fm().findFragmentByTag(AreaSelectorFragment.TAG);
    }

    public StreamFragment getStreamFragment() {
        return (StreamFragment) fm().findFragmentByTag(StreamFragment.TAG);
    }

    public NewMessageFragment getNewMessageFragment() {
        return (NewMessageFragment) fm().findFragmentByTag(NewMessageFragment.TAG);
    }

    public MinuteMessageFragment getMinuteMessageFragment() {
        return (MinuteMessageFragment) fm().findFragmentByTag(MinuteMessageFragment.TAG);
    }

    public GeneralPreferenceFragment getGeneralPreferenceFragment() {
        return (GeneralPreferenceFragment) fm().findFragmentByTag(GeneralPreferenceFragment.TAG);
    }

    private void manageBackStack(FragmentTransaction transaction, Fragment... newFragments) {
        Fragment[] currentFragments = fragmentstack.peek();
        fragmentstack.push(newFragments);
        if (currentFragments != null) {
            F:
            for (int i = currentFragments.length - 1; i >= 0; i--) {
                for (Fragment c : newFragments) {
                    if (c == currentFragments[i]) {
                        // Don't need to hide the fragment opening again.
                        continue F;
                    }
                }
                transaction.hide(currentFragments[i]);
            }
            transaction.addToBackStack(null);
        }
    }

    public boolean openWelcomeSet() {
        FragmentTransaction transaction = fm().beginTransaction();
        Fragment map = openMapFragment(transaction);
        Fragment stream = openStreamFragment(transaction);
        Fragment main = openMainFragment(transaction);
        manageBackStack(transaction, map, stream, main);
        transaction.commit();
        return true;
    }

    public boolean openAreaSelector() {
        FragmentTransaction transaction = fm().beginTransaction();
        Fragment map = openMapFragment(transaction);
        Fragment areaSelector = openAreaSelectorFragment(transaction);
        manageBackStack(transaction, map, areaSelector);
        transaction.commit();
        return true;
    }

    public boolean openNewMessage() {
        FragmentTransaction transaction = fm().beginTransaction();
        // transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        Fragment map = openMapFragment(transaction);
        Fragment main = openMainFragment(transaction);
        Fragment newMessage = openNewMessageFragment(transaction);
        manageBackStack(transaction, map, main, newMessage);
        transaction.commit();
        return true;
    }

    public boolean openMinuteMessage(Bundle args) {
        FragmentTransaction transaction = fm().beginTransaction();
        Fragment map = openMapFragment(transaction);
        Fragment main = openMainFragment(transaction);
        Fragment minuteMessage = openMinuteMessageFragment(transaction);
        minuteMessage.setArguments(args);
        manageBackStack(transaction, map, main, minuteMessage);
        transaction.commit();
        return true;
    }

    public boolean openGeneralPreference() {
        FragmentTransaction transaction = fm().beginTransaction();
        Fragment generalPreference = openGeneralPreferenceFragment(transaction);
        manageBackStack(transaction, generalPreference);
        transaction.commit();
        return true;
    }

    public void open(Page page, Bundle args) {
        switch (page) {
            case MinuteMessage:
                openMinuteMessage(args);
                break;
        }
    }

    private MainFragment openMainFragment(FragmentTransaction transaction) {
        MainFragment fragment = getMainFragment();
        if (fragment == null) {
            fragment = new MainFragment();
            transaction.add(R.id.middlerim, fragment, MainFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }

    private StreamFragment openStreamFragment(FragmentTransaction transaction) {
        StreamFragment fragment = getStreamFragment();
        if (fragment == null) {
            fragment = new StreamFragment();
            transaction.add(R.id.middlerim, fragment, StreamFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }

    private NewMessageFragment openNewMessageFragment(FragmentTransaction transaction) {
        NewMessageFragment fragment = getNewMessageFragment();
        if (fragment == null) {
            fragment = new NewMessageFragment();
            transaction.add(R.id.middlerim, fragment, NewMessageFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }


    private MinuteMessageFragment openMinuteMessageFragment(FragmentTransaction transaction) {
        MinuteMessageFragment fragment = getMinuteMessageFragment();
        if (fragment == null) {
            fragment = new MinuteMessageFragment();
            transaction.add(R.id.middlerim, fragment, MinuteMessageFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }

    private MapFragment openMapFragment(FragmentTransaction transaction) {
        MapFragment fragment = getMapFragment();
        if (fragment == null) {
            fragment = new MapFragment();
            transaction.add(R.id.middlerim, fragment, MapFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }

    private AreaSelectorFragment openAreaSelectorFragment(FragmentTransaction transaction) {
        AreaSelectorFragment fragment = getAreaSelectorFragment();
        if (fragment == null) {
            fragment = new AreaSelectorFragment();
            transaction.add(R.id.middlerim, fragment, AreaSelectorFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }

    private GeneralPreferenceFragment openGeneralPreferenceFragment(FragmentTransaction transaction) {
        GeneralPreferenceFragment fragment = getGeneralPreferenceFragment();
        if (fragment == null) {
            fragment = new GeneralPreferenceFragment();
            transaction.add(R.id.middlerim, fragment, GeneralPreferenceFragment.TAG);
        } else if (!fragment.isVisible()) {
            transaction.show(fragment);
        }
        return fragment;
    }
}
