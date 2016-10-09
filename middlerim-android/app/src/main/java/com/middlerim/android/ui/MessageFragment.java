package com.middlerim.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.middlerim.android.ui.dummy.DummyContent;
import com.middlerim.android.ui.dummy.DummyContent.DummyItem;
import com.middlerim.client.CentralEvents;

import java.nio.charset.Charset;
import java.util.List;

/**
 * https://developer.android.com/training/material/lists-cards.html
 */
public class MessageFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private MessageRecyclerViewAdapter viewAdaptor;
    private RecyclerView view;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MessageFragment newInstance(int columnCount) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = (RecyclerView) inflater.inflate(R.layout.fragment_message_list, container, false);

        Context context = view.getContext();
        if (mColumnCount <= 1) {
            view.setLayoutManager(new LinearLayoutManager(context));
        } else {
            view.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        view.setHasFixedSize(true);
        viewAdaptor = new MessageRecyclerViewAdapter(DummyContent.ITEMS);
        view.setAdapter(viewAdaptor);

        CentralEvents.onReceiveMessage(new CentralEvents.Listener<CentralEvents.ReceiveMessageEvent>() {
            private int i = 100;

            @Override
            public void handle(CentralEvents.ReceiveMessageEvent receiveMessageEvent) {
                receiveMessageEvent.message.position(0);
                byte[] bs = new byte[receiveMessageEvent.message.remaining()];
                receiveMessageEvent.message.get(bs, 0, bs.length);
                DummyContent.addItem(new DummyItem(
                        String.valueOf(++i),
                        new String(bs, Charset.forName("utf-8")),
                        "hoge"));
                viewAdaptor.notifyItemInserted(DummyContent.ITEMS.size() - 1);
                view.smoothScrollToPosition(viewAdaptor.getItemCount());
            }
        });
        return view;
    }


    public static class MessageRecyclerViewAdapter extends RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder> {

        private final List<DummyItem> mValues;

        public MessageRecyclerViewAdapter(List<DummyItem> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(mValues.get(position).id);
            holder.mContentView.setText(mValues.get(position).content);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(holder);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public DummyItem mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
