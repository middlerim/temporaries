package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MainFragment extends Fragment implements ButtonQueueManager.Adopter {
    public static final String TAG = Middlerim.TAG + ".Main";
    private AndroidContext androidContext;
    private ButtonQueueManager buttonQueueManager;
    private ViewGroup buttonQueue;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        androidContext = AndroidContext.get(getActivity());
        view.findViewById(R.id.button_new_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidContext.fragmentManager().openNewMessage();
            }
        });
        buttonQueue = (ViewGroup) view.findViewById(R.id.button_queue);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                //GeneralPreferenceFragment.open(getActivity());
                return true;
            case R.id.action_signIn:
                //SignInFragment.open(this);
                return true;
            case R.id.action_editArea:
                androidContext.fragmentManager().openAreaSelector();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View createNewButton(@DrawableRes int id) {
        ImageButton button = new ImageButton(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(lp);
        button.setImageResource(id);
        button.setClickable(true);
        return button;
    }

    @Override
    public void addButton(View button) {
        buttonQueue.addView(button);
    }

    @Override
    public void removeButton(View button) {
        buttonQueue.removeView(button);
    }
}
