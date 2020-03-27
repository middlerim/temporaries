package com.middlerim.android.ui;

import android.content.res.Resources;
import android.os.Bundle;
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

import com.middlerim.client.central.CentralEvents;
import com.middlerim.client.view.MessagePool;

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
        messagePool.onAdded(new MessagePool.AddedListener<Message>() {
            @Override
            public void onAdded(final int index, Message message) {
                recyclerViewItemSize = messagePool.size();
                viewAdapter.notifyItemInserted(recyclerViewItemSize);
            }
        });
    }

    private boolean scrolling;

    private void scrollToFirstItemIfNeeded() {
        if (scrolling) {
            return;
        }
        scrolling = true;
        scrollToFirstItemIfNeeded0();
    }

    private void scrollToFirstItemIfNeeded0() {
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int firstVisibleItemPosition = ((LinearLayoutManager) view.getLayoutManager()).findFirstVisibleItemPosition();
                if (firstVisibleItemPosition == RecyclerView.NO_POSITION || view.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scrollToFirstItemIfNeeded0();
                        }
                    }, 10);
                    return;
                }
                if (firstVisibleItemPosition < messagePool.size() - messagePool.capacity()) {
                    view.smoothScrollToPosition(messagePool.size() - messagePool.capacity());
                }
                scrolling = false;
            }
        }, 10);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View wrapper = inflater.inflate(R.layout.fragment_stream, container, false);
        view = (RecyclerView) wrapper.findViewById(R.id.stream);
        footer = (TextView) wrapper.findViewById(R.id.stream_footer);
        view.setLayoutManager(new LinearLayoutManagerWithSmoothScroller(getContext(), LinearLayoutManager.VERTICAL, false));
        view.setHasFixedSize(true);
        view.setAdapter(viewAdapter);
        Toolbar toolbar = androidContext.getActivity().getToolbar();
        view.setOnTouchListener(new SynchronisedScrollTouchListener(toolbar) {
            private float threshold = Float.MAX_VALUE;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                super.onTouch(v, event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if (threshold == Float.MAX_VALUE) {
                            footer.setVisibility(View.INVISIBLE);
                            int firstVisibleItemPosition = ((LinearLayoutManager) view.getLayoutManager()).findFirstVisibleItemPosition();
                            if (firstVisibleItemPosition < messagePool.size() - messagePool.capacity()) {
                                view.onInterceptTouchEvent(event);
                                threshold = event.getY();
                                return true;
                            }
                        }
                        return threshold < event.getY();
                    case MotionEvent.ACTION_UP:
                        threshold = Float.MAX_VALUE;
                        scrollToFirstItemIfNeeded();
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
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recyclerViewItemSize = messagePool.size();
                    if (p != recyclerViewItemSize) {
                        viewAdapter.notifyItemRangeInserted(p, messagePool.size());
                        Resources res = getResources();
                        String text = res.getString(R.string.info_unread_messages, messagePool.size() - p);
                        footer.setText(text);
                        footer.setVisibility(View.VISIBLE);
                    } else {
                        footer.setVisibility(View.INVISIBLE);
                    }
                    scrollToFirstItemIfNeeded();
                }
            }, 200);
        } else {
            pauseAt = messagePool.size();
        }
    }
}
