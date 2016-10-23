package com.middlerim.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.middlerim.client.Config;
import com.middlerim.client.view.MessagePool;
import com.middlerim.location.Coordinate;

import java.nio.ByteBuffer;

public class StreamFragment extends Fragment {
    private static final String TAG = Middlerim.TAG + "MSG";

    private RecyclerView view;
    private MessagePool<Message> messagePool;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final TextView contentView;
        public Message item;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.contentView = (TextView) view.findViewById(R.id.message_details);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + contentView.getText() + "'";
        }
    }

    private RecyclerView.Adapter<ViewHolder> viewAdapter = new RecyclerView.Adapter<ViewHolder>() {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Message msg = messagePool.get(position);
            if (msg == null) {
                Log.w(TAG, "Invalid index access. " + position);
            }
            holder.item = msg;
            holder.contentView.setText(holder.item.message);

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(holder);
                }
            });
        }

        @Override
        public int getItemCount() {
            return messagePool.size();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messagePool = new MessagePool<>(3, new MessagePool.Adapter<Message>() {
            private int lastIndex;

            @Override
            public Message onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
                byte[] bs = new byte[message.remaining()];
                message.get(bs);
                SpannableStringBuilder sb = new SpannableStringBuilder(new String(bs, Config.MESSAGE_ENCODING));
                Message msg = new Message(userId, location, displayName, sb, numberOfDelivery);
                viewAdapter.notifyItemInserted(messagePool.size());
                view.smoothScrollToPosition(messagePool.size());
                return msg;
            }
        });
        messagePool.onRemoved(new MessagePool.RemovedListener<Message>() {
            @Override
            public void onRemoved(int index, Message message) {
                viewAdapter.notifyItemRemoved(index);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        messagePool.startListen();
    }

    @Override
    public void onPause() {
        messagePool.stopListen();
        super.onPause();
    }

    public static class Message {
        public final long userId;
        public final Coordinate location;
        public final String displayName;
        public final CharSequence message;
        public final int numberOfDelivery;

        public Message(long userId, Coordinate location, String displayName, CharSequence message, int numberOfDelivery) {
            this.userId = userId;
            this.location = location;
            this.displayName = displayName;
            this.message = message;
            this.numberOfDelivery = numberOfDelivery;
        }

        @Override
        public String toString() {
            return "user ID: " + userId + ", location: " + location + ", displayName: " + displayName + ", message: " + message + ", number of delivery: " + numberOfDelivery;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = (RecyclerView) inflater.inflate(R.layout.fragment_stream, container, false);

        Context context = view.getContext();
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setHasFixedSize(true);
        view.setAdapter(viewAdapter);
        return view;
    }
}
