package org.odk.collect.android.formentry;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.fragments.dialogs.ProgressDialogFragment;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;

public class RefreshFormListDialogFragment extends ProgressDialogFragment {

    protected RefreshFormListDialogFragmentListener listener;

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        GeneralSharedPreferences generalSharedPreferences = Collect.getInstance().getComponent().generalSharedPreferences();
        String serverUrl = (String) generalSharedPreferences.get(GeneralKeys.KEY_SERVER_URL);

        if (context instanceof RefreshFormListDialogFragmentListener) {
            listener = (RefreshFormListDialogFragmentListener) context;
        }
        setTitle(getString(R.string.downloading_data, serverUrl));
        setMessage(getString(R.string.please_wait));
        setCancelable(false);
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        ((AlertDialog) dialog).setIcon(android.R.drawable.ic_dialog_info);
    }

    @Override
    protected String getCancelButtonText() {
        return getString(R.string.cancel_loading_form);
    }

    @Override
    protected Cancellable getCancellable() {
        return () -> {
            listener.onCancelFormLoading();
            dismiss();
            return true;
        };
    }

    public interface RefreshFormListDialogFragmentListener {
            void onCancelFormLoading();
    }
}
