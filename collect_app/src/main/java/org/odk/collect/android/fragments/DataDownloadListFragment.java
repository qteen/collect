package org.odk.collect.android.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.viewmodels.FormDownloadListViewModel;
import org.odk.collect.android.adapters.FormDownloadListAdapter;
import org.odk.collect.android.analytics.Analytics;
import org.odk.collect.android.analytics.AnalyticsEvents;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.formentry.RefreshFormListDialogFragment;
import org.odk.collect.android.formmanagement.FormDownloader;
import org.odk.collect.android.formmanagement.FormSourceExceptionMapper;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.formmanagement.ServerFormsDetailsFetcher;
import org.odk.collect.android.forms.FormSourceException;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.instancemanagement.DataDownloader;
import org.odk.collect.android.listeners.DownloadDataTaskListener;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.network.NetworkStateProvider;
import org.odk.collect.android.openrosa.HttpCredentialsInterface;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.tasks.DownloadDataTask;
import org.odk.collect.android.tasks.DownloadFormListTask;
import org.odk.collect.android.utilities.AuthDialogUtility;
import org.odk.collect.android.utilities.DialogUtils;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.utilities.TranslationHandler;
import org.odk.collect.android.utilities.WebCredentialsUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_DATE_DESC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;
import static org.odk.collect.android.utilities.ApplicationConstants.SortingOrder.BY_NAME_DESC;


/**
 * A simple [Fragment] subclass.
 * Use the [DataDownloadListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
public class DataDownloadListFragment extends AppListFragment implements FormListDownloaderListener, DownloadDataTaskListener, AuthDialogUtility.AuthDialogUtilityResultListener {
    private static final int LOADER_ID = 0x01;
    private static final String DATA_DOWNLOAD_LIST_SORTING_ORDER = "dataDownloadListSortingOrder";
    protected Button downloadButton;
    protected Button toggleButton;
    protected LinearLayout llParent;
    protected ProgressBar progressBar;

    private boolean progressBarVisible;
    private DownloadFormListTask downloadFormListTask;
    private DownloadDataTask downloadDataTask;
    private FormDownloadListViewModel viewModel;
    private AlertDialog alertDialog;

    protected LinkedHashSet<Integer> selectedForm = new LinkedHashSet<>();
    private final ArrayList<HashMap<String, String>> filteredFormList = new ArrayList<>();

    public static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    public static final String FORMID_DISPLAY = "formiddisplay";

    @Inject
    NetworkStateProvider connectivityProvider;
    @Inject
    ServerFormsDetailsFetcher serverFormsDetailsFetcher;
    @Inject
    Analytics analytics;
    @Inject
    WebCredentialsUtils webCredentialsUtils;
    @Inject
    DataDownloader dataDownloader;

    private ListView listView;
    private ProgressDialog progressDialog;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        DaggerUtils.getComponent(context).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_data_download_list, container, false);
        viewModel = new ViewModelProvider(this, new FormDownloadListViewModel.Factory(analytics))
                .get(FormDownloadListViewModel.class);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().setItemsCanFocus(false);
        downloadButton = view.findViewById(R.id.download_button);
        toggleButton = view.findViewById(R.id.toggle_button);
        llParent = view.findViewById(R.id.llParent);
        progressBar = getActivity().findViewById(R.id.progressBar);
        progressBarVisible = false;

        TextView emptyRefresh = view.findViewById(R.id.empty_refresh);
        emptyRefresh.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                downloadFormList();
                return true;
            }
        });
        listView = view.findViewById(android.R.id.list);
        listView.setEmptyView(emptyRefresh);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDownload();
            }
        });
        downloadButton.setEnabled(false);

        sortingOptions = new int[]{
                R.string.sort_by_name_asc, R.string.sort_by_name_desc,
                R.string.sort_by_date_asc, R.string.sort_by_date_desc
        };
        super.onViewCreated(view, savedInstanceState);
    }

    private void processDownload() {
        if (downloadDataTask != null &&
                downloadDataTask.getStatus() != AsyncTask.Status.FINISHED) {
            return; // we are already doing the download!!!
        } else if (downloadDataTask != null) {
            downloadDataTask.setDownloaderListener(null);
            downloadDataTask.cancel(true);
            downloadDataTask = null;
        }
        HashMap<String, ServerFormDetails> formDetailsByFormId = viewModel.getFormDetailsByFormId();
        ArrayList<ServerFormDetails> filesToDownload = new ArrayList();
        for(Integer itemid : selectedForm) {
            HashMap<String, String> filteredMap = filteredFormList.get(itemid);
            ServerFormDetails serverFormDetails = formDetailsByFormId.get(filteredMap.get(FORMDETAIL_KEY));
            if (serverFormDetails.isNotOnDevice()) {
                ToastUtils.showLongToast(TranslationHandler.getString(Collect.getInstance(), R.string.form_not_found, serverFormDetails.getFormId()));
                break;
            } else
                filesToDownload.add(serverFormDetails);
        }

        if(downloadDataTask == null && !filesToDownload.isEmpty()) {
            showProgressBar();
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getResources().getString(R.string.data_download_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

            downloadDataTask = new DownloadDataTask(dataDownloader, webCredentialsUtils);
            downloadDataTask.setDownloaderListener(this);
            downloadDataTask.execute(filesToDownload);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle bundle) {
        super.onViewStateRestored(bundle);
        downloadButton.setEnabled(areCheckedItems());
    }

    @Override
    public void onStart() {
        super.onStart();
        downloadFormList();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long rowId) {
        super.onListItemClick(l, v, position, rowId);

        if (getListView().isItemChecked(position)) {
            selectedForm.add(position);
        } else {
            selectedForm.remove(position);
        }

        toggleButtonLabel(toggleButton, getListView());
        downloadButton.setEnabled(areCheckedItems());
    }

    @Override
    protected void updateAdapter() {
        CharSequence charSequence = getFilterText();
        filteredFormList.clear();
        if (charSequence.length() > 0) {
            for (HashMap<String, String> form : viewModel.getFormList()) {
                if (form.get(FORMNAME).toLowerCase(Locale.US).contains(charSequence.toString().toLowerCase(Locale.US))) {
                    filteredFormList.add(form);
                }
            }
        } else {
            filteredFormList.addAll(viewModel.getFormList());
        }
        sortList();
        if (getListView().getAdapter() == null) {
            getListView().setAdapter(new FormDownloadListAdapter(getContext(), filteredFormList, viewModel.getFormDetailsByFormId()));
        } else {
            FormDownloadListAdapter formDownloadListAdapter = (FormDownloadListAdapter) getListView().getAdapter();
            formDownloadListAdapter.setFromIdsToDetails(viewModel.getFormDetailsByFormId());
            formDownloadListAdapter.notifyDataSetChanged();
        }
        toggleButton.setEnabled(!filteredFormList.isEmpty());
        toggleButtonLabel(toggleButton, getListView());
    }

    @Override
    protected String getSortingOrderKey() {
        return DATA_DOWNLOAD_LIST_SORTING_ORDER;
    }

    private void downloadFormList() {
        if (!connectivityProvider.isDeviceOnline()) {
            ToastUtils.showShortToast(R.string.no_connection);
        } else {
            viewModel.clearFormDetailsByFormId();
            showProgressBar();

            if (downloadFormListTask != null &&
                    downloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
                return; // we are already doing the download!!!
            } else if (downloadFormListTask != null) {
                downloadFormListTask.setDownloaderListener(null);
                downloadFormListTask.cancel(true);
                downloadFormListTask = null;
            }

            serverFormsDetailsFetcher.setCheckData(true);
            downloadFormListTask = new DownloadFormListTask(serverFormsDetailsFetcher);
            downloadFormListTask.setDownloaderListener(this);
            downloadFormListTask.execute();
        }
    }

    @Override
    public void formListDownloadingComplete(HashMap<String, ServerFormDetails> formList, FormSourceException exception) {
        hideProgressBar();
        downloadFormListTask.setDownloaderListener(null);
        downloadFormListTask = null;

        if (exception == null) {
            // Everything worked. Clear the list and add the results.
            viewModel.setFormDetailsByFormId(formList);
            viewModel.clearFormList();

            ArrayList<String> ids = new ArrayList<>(viewModel.getFormDetailsByFormId().keySet());
            for (int i = 0; i < formList.size(); i++) {
                String formDetailsKey = ids.get(i);
                ServerFormDetails details = viewModel.getFormDetailsByFormId().get(formDetailsKey);

                if (details.getDataToDownload() <= 0) {
                    continue;
                }

                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.getFormName());
                item.put(FORMID_DISPLAY,
                        ((details.getFormVersion() == null) ? "" : (getString(R.string.version) + " " + details.getFormVersion() + "\n")) +
                                "ID: " + details.getFormId() + "\nJml Data: " + details.getDataToDownload() +" dari "+details.getDataCount()+" Data");
                item.put(FORMDETAIL_KEY, formDetailsKey);

                // Insert the new form in alphabetical order.
                if (viewModel.getFormList().isEmpty()) {
                    viewModel.addForm(item);
                } else {
                    int j;
                    for (j = 0; j < viewModel.getFormList().size(); j++) {
                        HashMap<String, String> compareMe = viewModel.getFormList().get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(viewModel.getFormDetailsByFormId().get(ids.get(i)).getFormName()) > 0) {
                            break;
                        }
                    }
                    viewModel.addForm(j, item);
                }
            }

            filteredFormList.addAll(viewModel.getFormList());
            updateAdapter();
            downloadButton.setEnabled(areCheckedItems());
            toggleButton.setEnabled(getListView().getCount() > 0);
            toggleButtonLabel(toggleButton, getListView());
        } else {
            switch (exception.getType()) {
                case FETCH_ERROR:
                case UNREACHABLE:
                    String dialogMessage = new FormSourceExceptionMapper(getContext()).getMessage(exception);
                    String dialogTitle = getString(R.string.load_remote_form_error);

                    createAlertDialog(dialogTitle, dialogMessage);
                    break;

                case AUTH_REQUIRED:
                    createAuthDialog();
                    break;
            }
        }
    }

    /**
     * Creates an alert dialog with the given tite and message. If shouldExit is set to true, the
     * activity will exit when the user clicks "ok".
     */
    private void createAlertDialog(String title, String message) {
        alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // ok
                        // just close the dialog
                        viewModel.setAlertShowing(false);
                        break;
                }
            }
        };
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), quitListener);
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        viewModel.setAlertDialogMsg(message);
        viewModel.setAlertTitle(title);
        viewModel.setAlertShowing(true);
        viewModel.setShouldExit(false);
        DialogUtils.showDialog(alertDialog, getActivity());
    }

    private void createAuthDialog() {
        viewModel.setAlertShowing(false);

        AuthDialogUtility authDialogUtility = new AuthDialogUtility();
        if (viewModel.getUrl() != null && viewModel.getUsername() != null && viewModel.getPassword() != null) {
            authDialogUtility.setCustomUsername(viewModel.getUsername());
            authDialogUtility.setCustomPassword(viewModel.getPassword());
        }
        DialogUtils.showDialog(authDialogUtility.createDialog(getContext(), this, viewModel.getUrl()), getActivity());
    }

    protected void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        progressBarVisible = true;
    }

    protected void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        progressBarVisible = false;
    }

    @Override
    public void updatedCredentials() {
        // If the user updated the custom credentials using the dialog, let us update our
        // variables holding the custom credentials
        if (viewModel.getUrl() != null) {
            HttpCredentialsInterface httpCredentials = webCredentialsUtils.getCredentials(URI.create(viewModel.getUrl()));

            if (httpCredentials != null) {
                viewModel.setUsername(httpCredentials.getUsername());
                viewModel.setPassword(httpCredentials.getPassword());
            }
        }

        downloadFormList();
    }

    @Override
    public void cancelledUpdatingCredentials() {
    }

    @Override
    public void onResume() {
        // hook up to receive completion events
        if (downloadFormListTask != null) {
            downloadFormListTask.setDownloaderListener(this);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (downloadFormListTask != null) {
            downloadFormListTask.setDownloaderListener(null);
        }
        if(progressBarVisible) {
            hideProgressBar();
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        super.onPause();
    }

    private void sortList() {
        Collections.sort(filteredFormList, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {
                return lhs.get(FORMNAME).compareToIgnoreCase(rhs.get(FORMNAME));
            }
        });
    }

    @Override
    public void dataDownloadingComplete(Map<ServerFormDetails, String> result) {
        hideProgressBar();
        if(result==null || result.containsValue("error")) {
            analytics.logEvent(AnalyticsEvents.DATA_DOWNLOAD_FAILED, viewModel.getUsername());
            ToastUtils.showShortToast(getString(R.string.data_download_failed));
        } else {
            analytics.logEvent(AnalyticsEvents.DATA_DOWNLOAD_SUCCESSFUL, viewModel.getUsername());
            ToastUtils.showShortToast(getString(R.string.data_download_completed));
        }
        progressDialog.dismiss();
    }

    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        String message = String.format(getResources().getString(R.string.data_download_progress), currentFile, progress, total);
        progressDialog.setMessage(message);
    }

    @Override
    public void dataDownloadingCancelled() {
        hideProgressBar();
        progressDialog.dismiss();
    }
}