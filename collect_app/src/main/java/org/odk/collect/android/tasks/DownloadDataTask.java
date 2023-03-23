package org.odk.collect.android.tasks;

import android.os.AsyncTask;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.instancemanagement.DataDownloader;
import org.odk.collect.android.listeners.DownloadDataTaskListener;
import org.odk.collect.android.utilities.WebCredentialsUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DownloadDataTask extends
        AsyncTask<ArrayList<ServerFormDetails>, String, Map<ServerFormDetails, String>> {
    private WebCredentialsUtils webCredentialsUtils;
    private DataDownloader dataDownloader;
    private DownloadDataTaskListener stateListener;

    public DownloadDataTask(DataDownloader dataDownloader, WebCredentialsUtils webCredentialsUtils) {
        this.dataDownloader = dataDownloader;
        this.webCredentialsUtils = webCredentialsUtils;
    }

    @Override
    protected Map<ServerFormDetails, String> doInBackground(ArrayList<ServerFormDetails>... values) {
        ArrayList<ServerFormDetails> toDownload = values[0];
        HashMap<ServerFormDetails, String> results = new HashMap();

        int index = 1;
        for (ServerFormDetails fd : toDownload) {
            try {
                String currentFormNumber = String.valueOf(index);
                String totalForms = String.valueOf(values[0].size());
                publishProgress(fd.getFormName(), currentFormNumber, totalForms);

                dataDownloader.downloadData(fd, count -> {
                    String message = Collect.getInstance().getString(R.string.data_download_progress,
                            fd.getFormName(),
                            String.valueOf(count),
                            String.valueOf(fd.getDataCount())
                    );

                    publishProgress(message, currentFormNumber, totalForms);
                }, this::isCancelled);

                results.put(fd, Collect.getInstance().getString(R.string.success));
            } catch (IOException e) {
                e.printStackTrace();
                results.put(fd, "error");
            }
        }

        return results;
    }

    @Override
    protected void onPostExecute(Map<ServerFormDetails, String> result) {
        synchronized (this) {
            if (stateListener != null) {
                stateListener.dataDownloadingComplete(result);
            }
        }
    }

    @Override
    protected void onCancelled() {
        synchronized (this) {
            if (stateListener != null) {
                stateListener.dataDownloadingCancelled();
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        synchronized (this) {
            if (stateListener != null) {
                stateListener.progressUpdate(values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2]));
            }
        }
    }

    public void setDownloaderListener(DownloadDataTaskListener sl) {
        synchronized (this) {
            stateListener = sl;
        }
    }
}
