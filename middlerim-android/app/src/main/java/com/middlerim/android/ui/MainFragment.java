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
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.middlerim.client.channel.OutboundSynchronizer;
import com.middlerim.client.message.Text;
import com.middlerim.message.SequentialMessage;

import java.util.ArrayDeque;

public class MainFragment extends Fragment implements ButtonQueueManager.Adopter {
    public static final String TAG = Middlerim.TAG + ".Main";
    private AndroidContext androidContext;
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
        inflateButtonQueue();

        return view;
    }

    private void inflateButtonQueue() {
        ArrayDeque<OutboundSynchronizer.MessageAndContext<SequentialMessage>> leftOverMessages = OutboundSynchronizer.getMessageQueue();
        OutboundSynchronizer.MessageAndContext<SequentialMessage> m;
        while ((m = leftOverMessages.poll()) != null) {
            if (m.message instanceof Text.Out) {
                Bundle args = new Bundle();
                args.putInt(MinuteMessageFragment.ARG_MESSAGE_TAG, m.message.tag());
                androidContext.buttonQueueManager().addButton(m.message.tag(), R.drawable.ic_sync_white_24px, FragmentManager.Page.MinuteMessage, args);
            }
        }
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
                androidContext.fragmentManager().openGeneralPreference();
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
        button.getBackground().setAlpha(0);

        RotateAnimation rotate = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(3000);
        rotate.setFillAfter(true);
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        button.startAnimation(rotate);

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
