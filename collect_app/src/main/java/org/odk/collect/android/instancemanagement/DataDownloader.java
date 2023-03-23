package org.odk.collect.android.instancemanagement;

import org.odk.collect.android.formmanagement.FormDownloadException;
import org.odk.collect.android.formmanagement.FormDownloader;
import org.odk.collect.android.formmanagement.ServerFormDetails;

import java.io.IOException;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public interface DataDownloader {
    void downloadData(ServerFormDetails form, @Nullable FormDownloader.ProgressReporter progressReporter, @Nullable Supplier<Boolean> isCancelled) throws IOException;
}
