package com.middlerim.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

import com.middlerim.client.Config;
import com.middlerim.client.view.MessagePool;
import com.middlerim.location.Coordinate;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.NumberFormat;

public class StreamFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".Stream";
    private static final int ACTIVE = -1;
    private AndroidContext androidContext;
    private RecyclerView view;
    private MessagePool<Message> messagePool;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int pauseAt = ACTIVE;

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final TextView displayName;
        final TextView message;
        final TextView numberOfDelivery;
        Message item;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            this.displayName = (TextView) view.findViewById(R.id.display_name);
            this.message = (TextView) view.findViewById(R.id.message);
            this.numberOfDelivery = (TextView) view.findViewById(R.id.number_delivery);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + message.getText() + "'";
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
            Message msg = messagePool.getLatest(position % Math.min(messagePool.size(), messagePool.capacity()));
            holder.item = msg;
            holder.displayName.setText(holder.item.displayName);
            holder.message.setText(holder.item.message);
            if (holder.item.numberOfDelivery != null) {
                holder.numberOfDelivery.setText(holder.item.numberOfDelivery);
            } else {
                holder.numberOfDelivery.setVisibility(View.INVISIBLE);
            }
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(holder);
                }
            });
        }

        @Override
        public int getItemCount() {
            return messagePool.size() >= messagePool.capacity() ? Integer.MAX_VALUE : messagePool.size();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidContext = AndroidContext.get(getActivity());
        messagePool = new MessagePool<>(10, new MessagePool.Adapter<Message>() {
            @Override
            public Message onReceive(long userId, Coordinate location, String displayName, ByteBuffer message, int numberOfDelivery) {
                byte[] bs = new byte[message.remaining()];
                message.get(bs);
                SpannableStringBuilder sb = new SpannableStringBuilder(new String(bs, Config.MESSAGE_ENCODING));
                Message msg = new Message(userId, location, displayName, sb, numberOfDelivery);

                if (pauseAt == ACTIVE) {
                    scrollTo(messagePool.size());
                }
                return msg;
            }

            @Override
            public File storage() {
                return new File(getContext().getFilesDir(), "messages");
            }
        });
        int latestMessageSize = androidContext.preferences().getInt(Codes.PREF_LATEST_MESSAGE_SIZE, -1);
        if (latestMessageSize >= 0) {
            messagePool.loadLatestMessages(latestMessageSize);
        }
        messagePool.startListen();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messagePool.stopListen();
        androidContext.preferences().edit().putInt(Codes.PREF_LATEST_MESSAGE_SIZE, messagePool.size()).apply();
        messagePool.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = (RecyclerView) inflater.inflate(R.layout.fragment_stream, container, false);

        Context context = view.getContext();
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setHasFixedSize(true);
        view.setAdapter(viewAdapter);
        androidContext.getActivity().setToolbarHandler(view);
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            pauseAt = messagePool.size();
        } else {
            int p = pauseAt;
            pauseAt = ACTIVE;
            if (p != ACTIVE && messagePool.size() > 0) {
                Resources res = getResources();
                String text = res.getString(R.string.info_unread_messages, messagePool.size() - p);
                final Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private void scrollTo(final int position) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                viewAdapter.notifyItemInserted(position);
                view.smoothScrollToPosition(position);
            }
        });
    }

    public static class Message {
        private static NumberFormat numberFormat = NumberFormat.getIntegerInstance();

        public final long userId;
        public final Coordinate location;
        public final String displayName;
        public final CharSequence message;
        public final String numberOfDelivery;

        public Message(long userId, Coordinate location, String displayName, CharSequence message, int numberOfDelivery) {
            this.userId = userId;
            this.location = location;
            this.displayName = displayName;
            this.message = message;
            if (numberOfDelivery >= 0) {
                this.numberOfDelivery = numberFormat.format(numberOfDelivery);
            } else {
                this.numberOfDelivery = null;
            }
        }

        @Override
        public String toString() {
            return "user ID: " + userId + ", location: " + location + ", displayName: " + displayName + ", message: " + message + ", number of delivery: " + numberOfDelivery;
        }
    }
}
