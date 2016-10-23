package com.middlerim.android.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import com.middlerim.client.view.ViewEvents;
import com.middlerim.server.MessageCommands;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class NewMessageFragment extends Fragment {
    public static final String TAG = Middlerim.TAG + ".NEW_MSG";

    private static final int MAX_LENGTH = 1560;

    private String displayName = "仮名さん";
    private EditText editText;
    private ImageButton sendButton;
    private ByteBuffer buf;
    private CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
    private AndroidContext androidContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_new_message, container, false);
        editText = (EditText) view.findViewById(R.id.new_message);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onEdit(s, start, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sendButton = (ImageButton) view.findViewById(R.id.button_send_message);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendButton.isEnabled()) {
                    onSendMessageButtonClick(v);
                }
            }
        });
        view.findViewById(R.id.button_edit_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide IME
                editText.setEnabled(false);
                editText.setEnabled(true);

                androidContext.fragmentManager().openMap(NewMessageFragment.this);
            }
        });

        // Since the activity is using android:windowSoftInputMode="adjustPan",
        // The fragment needs to resize itself when a software keyboard is shown.
        final ScrollView scrollView = (ScrollView) editText.getParent();
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    int defaultBottomPadding = -1;

                    @Override
                    public void onGlobalLayout() {
                        if (!isVisible()) {
                            return;
                        }
                        Rect visibleSize = new Rect();
                        view.getRootView().getWindowVisibleDisplayFrame(visibleSize);
                        if (defaultBottomPadding == -1) {
                            defaultBottomPadding = visibleSize.bottom - view.getBottom();
                        }
                        int bottom = visibleSize.bottom - defaultBottomPadding;
                        scrollView.setBottom(bottom);
                    }
                });
        editText.setSelection(editText.length());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        androidContext = AndroidContext.get(getActivity());
        buf = ByteBuffer.allocateDirect(MAX_LENGTH * 2);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.button_new_message).setVisibility(View.INVISIBLE);
        if (!hasContent(editText.getText().subSequence(0, editText.length()))) {
            String editingMessage = androidContext.preferences().getString(Codes.PREF_EDITING_MESSAGE, null);
            if (editingMessage != null) {
                editText.getText().append(editingMessage);
            }
        }
        onEdit(editText.getText().subSequence(0, editText.length()), 0, -1);
    }

    private boolean hasContent(CharSequence s) {
        for (int i = 0, l = s.length(); i < l; i++) {
            char c = s.charAt(i);
            if (c != '\n' && c != '\r' && c != ' ' && c != '　' && c != '\t') {
                return true;
            }
        }
        return false;
    }

    private void color(CharSequence s, int offset, int length) {
        if (!(s instanceof SpannableStringBuilder)) {
            return;
        }
        SpannableStringBuilder sb = (SpannableStringBuilder) s;
        int startBlank = -1;
        int startChar = -1;
        int l = l = length > 0 ? offset + length : s.length();
        for (int i = offset; i < l; i++) {
            char c = s.charAt(i);
            if (c != '\n' && c != '\r' && c != ' ' && c != '　' && c != '\t') {
                if (startBlank != -1) {
                    sb.setSpan(new BackgroundColorSpan(Color.GRAY), startBlank, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                startBlank = -1;
                if (startChar == -1) {
                    startChar = i;
                }
            } else {
                if (startBlank == -1) {
                    startBlank = i;
                }
                if (startChar != -1) {
                    sb.setSpan(new BackgroundColorSpan(Color.TRANSPARENT), startChar, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                startChar = -1;
            }
        }
        if (startBlank != -1) {
            sb.setSpan(new BackgroundColorSpan(Color.GRAY), startBlank, l, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (startChar != -1) {
            sb.setSpan(new BackgroundColorSpan(Color.TRANSPARENT), startChar, l, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void onEdit(CharSequence s, int offset, int length) {
        boolean hasContent = hasContent(s);
        color(s, offset, length);
        sendButton.setEnabled(hasContent);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().findViewById(R.id.button_new_message).setVisibility(View.VISIBLE);

        if (editText.length() > 0) {
            SharedPreferences.Editor prefEditor = androidContext.preferences().edit();
            prefEditor.putString(Codes.PREF_EDITING_MESSAGE, editText.getText().subSequence(0, editText.length()).toString());
            prefEditor.apply();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (buf != null) {
            buf.clear();
            buf = null;
        }
    }

    public void onSendMessageButtonClick(View view) {
        char[] cs = new char[editText.length()];
        editText.getText().getChars(0, editText.length(), cs, 0);
        CharBuffer charBuffer = CharBuffer.wrap(cs);
        buf.clear();
        encoder.encode(charBuffer, buf, false);
        buf.limit(buf.position());
        buf.position(0);
        ViewEvents.fireSubmitMessage(displayName, MessageCommands.areaKM(16), buf);
    }
}
