package com.middlerim.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class GpsDisabledAlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.error_dialog_gps_disabled_message)
                .setPositiveButton(R.string.button_label_fire, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        getActivity().startActivityForResult(settingsIntent, Codes.ACTIVITY_REQUEST_CODE_GPS);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.button_label_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO
                        final Toast toast = Toast.makeText(getActivity(), R.string.error_dialog_gps_disabled_message, Toast.LENGTH_SHORT);
                        toast.show();
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}