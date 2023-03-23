package org.odk.collect.android.listeners;

import org.odk.collect.android.formmanagement.ServerFormDetails;

import java.util.Map;

public interface DownloadDataTaskListener {
    void dataDownloadingComplete(Map<ServerFormDetails, String> result);

    void progressUpdate(String currentFile, int progress, int total);

    void dataDownloadingCancelled();
}
