package com.middlerim.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.middlerim.client.Config;
import com.middlerim.client.view.ViewEvents;
import com.middlerim.message.SequentialMessage;

import java.nio.ByteBuffer;

public class MinuteMessageFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".MinuteMsg";

    public static final String ARG_MESSAGE_TAG = "message_tag";
    public static final String ARG_MESSAGE_INDEX = "message_index";

    private AndroidContext androidContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        androidContext = AndroidContext.get(getActivity());
        View view = inflater.inflate(R.layout.fragment_minute_message, container, false);

        int tag = getArguments().getInt(ARG_MESSAGE_TAG, SequentialMessage.UNASSIGNED);
        if (tag != SequentialMessage.UNASSIGNED) {
            inflateMyMessageByTag(tag, view);
        } else {
            int index = getArguments().getInt(ARG_MESSAGE_INDEX);
            inflateMyMessageByIndex(index, view);
        }
        return view;
    }

    private void inflateMyMessageByTag(int tag, View view) {
        ViewEvents.SubmitMessageEvent event = androidContext.getMessagePool().getUnreachedMessage(tag);
        ByteBuffer buf = event.message.duplicate();
        buf.position(0);
        byte[] messageBytes = new byte[buf.remaining()];
        buf.get(messageBytes);
        TextView messageView = (TextView) view.findViewById(R.id.minute_message);
        messageView.setText(new String(messageBytes, Config.MESSAGE_ENCODING));
    }

    private void inflateMyMessageByIndex(int index, View view) {
        Message message = androidContext.getMessagePool().get(index);
        if (message == null) {
            // TODO
            return;
        }
        TextView messageView = (TextView) view.findViewById(R.id.minute_message);
        messageView.setText(message.message);
    }
}
