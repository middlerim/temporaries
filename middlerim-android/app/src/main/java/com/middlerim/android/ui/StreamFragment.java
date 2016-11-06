package com.middlerim.android.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.middlerim.client.view.MessagePool;

import java.util.concurrent.atomic.AtomicInteger;

public class StreamFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".Stream";
    private AndroidContext androidContext;
    private MessagePool<Message> messagePool;
    private RecyclerView view;
    private TextView footer;

    private int pauseAt;
    private int recyclerViewItemSize;

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final TextView displayName;
        final TextView message;
        final TextView numberOfDelivery;

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

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle args = new Bundle();
                    int index = (int) v.getTag();
                    args.putInt(MinuteMessageFragment.ARG_MESSAGE_INDEX, index);
                    androidContext.fragmentManager().openMinuteMessage(args);
                }
            });
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Message msg = messagePool.get(position);
            if (msg == null) {
                holder.view.setVisibility(View.GONE);
                return;
            }
            holder.displayName.setText(msg.displayName);
            holder.message.setText(msg.message);
            if (msg.numberOfDelivery != null) {
                holder.numberOfDelivery.setText(msg.numberOfDelivery);
            } else {
                holder.numberOfDelivery.setVisibility(View.INVISIBLE);
            }
            holder.view.setTag(position);
            holder.view.setVisibility(View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return recyclerViewItemSize;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidContext = AndroidContext.get(getActivity());
        messagePool = androidContext.getMessagePool();
    }

    private boolean isFirstItemDisplaying(RecyclerView view) {
        if (messagePool.size() > messagePool.capacity()) {
            int firstVisibleItemPosition = ((LinearLayoutManager) view.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            if (firstVisibleItemPosition != RecyclerView.NO_POSITION && firstVisibleItemPosition < messagePool.size() - messagePool.capacity())
                return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View wrapper = inflater.inflate(R.layout.fragment_stream, container, false);
        view = (RecyclerView) wrapper.findViewById(R.id.stream);
        footer = (TextView) wrapper.findViewById(R.id.stream_footer);
        view.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        view.setHasFixedSize(true);
        view.setAdapter(viewAdapter);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                footer.setVisibility(View.INVISIBLE);
            }
        });
        Toolbar toolbar = androidContext.getActivity().getToolbar();
        view.setOnTouchListener(new SynchronisedScrollTouchListener(toolbar) {
            private float scrollStartY;
            private boolean enabled = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                super.onTouch(v, event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        scrollStartY = event.getY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        if (scrollStartY < event.getY()) {
                            if (enabled && !isFirstItemDisplaying(view)) {
                                enabled = false;
                            }
                            if (!enabled) {
                                return true;
                            }
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        if (!enabled) {
                            enabled = true;
                            View item = view.getLayoutManager().findViewByPosition(messagePool.size() - messagePool.capacity() - 1);
                            if (item == null) {
                                return false;
                            }
                            final int firstItemY = (int) item.getTop();
                            view.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    view.smoothScrollBy(0, firstItemY);
                                }
                            }, 100);
                            return true;
                        }
                }
                return false;
            }
        });
        return wrapper;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        onViewToggled(!hidden);
    }

    @Override
    public void onResume() {
        super.onResume();
        onViewToggled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        onViewToggled(false);
    }

    private void onViewToggled(boolean show) {
        if (show) {
            final int p = pauseAt;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    recyclerViewItemSize = messagePool.size();
                    if (p != recyclerViewItemSize) {
                        viewAdapter.notifyItemRangeInserted(p, messagePool.size() - p);
                        Resources res = getResources();
                        String text = res.getString(R.string.info_unread_messages, messagePool.size() - p);
                        footer.setText(text);
                        footer.setVisibility(View.VISIBLE);
                    } else {
                        footer.setVisibility(View.INVISIBLE);
                    }
                }
            }, 100);
        } else {
            pauseAt = messagePool.size();
        }
    }
}
