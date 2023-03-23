package org.odk.collect.android.tasks;

import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.helpers.ContentResolverHelper;
import org.odk.collect.android.exception.GDriveConnectionException;
import org.odk.collect.android.fragments.dialogs.ProgressDialogFragment;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.network.NetworkStateProvider;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.ImageConverter;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.utilities.ToastUtils;
import org.odk.collect.android.utilities.TranslationHandler;
import org.odk.collect.android.widgets.BaseImageWidget;
import org.odk.collect.android.widgets.QuestionWidget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;

import timber.log.Timber;

public class MediaLoadingTask extends AsyncTask<Uri, Void, File> {

    private WeakReference<FormEntryActivity> formEntryActivity;
    private WeakReference<NetworkStateProvider> connectivityProvider;
    private boolean itemsetFile;

    public MediaLoadingTask(FormEntryActivity formEntryActivity, NetworkStateProvider connectivityProvider) {
        onAttach(formEntryActivity);
        this.itemsetFile = false;
        this.connectivityProvider = new WeakReference<>(connectivityProvider);
    }

    public void setItemsetFile(boolean itemsetFile) {
        this.itemsetFile = itemsetFile;
    }

    public void onAttach(FormEntryActivity formEntryActivity) {
        this.formEntryActivity = new WeakReference<>(formEntryActivity);
    }

    public void onDetach() {
        formEntryActivity = null;
        connectivityProvider = null;
    }

    @Override
    protected File doInBackground(Uri... uris) {

        File instanceFile;
        FormController formController = Collect.getInstance().getFormController();

        if (formController != null) {
            instanceFile = formController.getInstanceFile();
            if (instanceFile != null && !itemsetFile) {
                String instanceFolder = instanceFile.getParent();
                String extension = ContentResolverHelper.getFileExtensionFromUri(formEntryActivity.get(), uris[0]);
                String destMediaPath = instanceFolder
                        + File.separator
                        + System.currentTimeMillis()
                        + "."
                        + extension;

                try {
                    File chosenFile = MediaUtils.getFileFromUri(formEntryActivity.get(), uris[0], MediaStore.Images.Media.DATA, connectivityProvider.get());
                    if (chosenFile != null) {
                        final File newFile = new File(destMediaPath);
                        FileUtils.copyFile(chosenFile, newFile);
                        QuestionWidget questionWidget = formEntryActivity.get().getWidgetWaitingForBinaryData();

                        // apply image conversion if the widget is an image widget
                        if (questionWidget instanceof BaseImageWidget) {
                            ImageConverter.execute(newFile.getPath(), questionWidget, formEntryActivity.get());
                        }

                        return newFile;
                    } else {
                        Timber.e("Could not receive chosen file");
                        formEntryActivity.get().runOnUiThread(() -> ToastUtils.showShortToastInMiddle(R.string.error_occured));
                        return null;
                    }
                } catch (GDriveConnectionException e) {
                    Timber.e("Could not receive chosen file due to connection problem");
                    formEntryActivity.get().runOnUiThread(() -> ToastUtils.showLongToastInMiddle(R.string.gdrive_connection_exception));
                    return null;
                }
            } else if(itemsetFile) {
                File mediaFolder = formController.getMediaFolder();
                Timber.i("Media Folder: %s", mediaFolder.getAbsolutePath());
                String filePath = MediaUtils.getPathFromUri(formEntryActivity.get(), uris[0], null);

                boolean isError;
                try {
                    File chosenFile = new File(filePath);
                    if (chosenFile != null && chosenFile.getName().endsWith(".csv")
                            && chosenFile.getName().contains("itemsets")
                            && chosenFile.isFile() && chosenFile.canRead()) {
                        CharBuffer charBuffer = CharBuffer.allocate(100);
                        FileReader fileReader = new FileReader(chosenFile);
                        fileReader.read(charBuffer);
                        charBuffer.rewind();
                        String fileContent = charBuffer.toString();

                        if(fileContent.startsWith("\"list_name\",")
                                || fileContent.startsWith("list_name,")) {
                            String destMediaPath = mediaFolder.getAbsolutePath() + "/" + FormLoaderTask.ITEMSETS_CSV;
                            final File newFile = new File(destMediaPath);
                            FileUtils.copyFile(chosenFile, newFile);
                            return newFile;
                        } else throw new IOException("Not a CSV file");
                    } else throw new IOException("Cannot read CSV file");
                } catch (FileNotFoundException e) {
                    isError = true;
                    e.printStackTrace();
                } catch (IOException e) {
                    isError = true;
                    e.printStackTrace();
                }
                if(isError) {
                    Timber.e("Invalid itemsets file");
                    formEntryActivity.get().runOnUiThread(() ->
                            ToastUtils.showLongToast(TranslationHandler.getString(Collect.getInstance(), R.string.file_invalid, filePath)));
                    return null;
                }
            }
        }
        return null;

    }

    @Override
    protected void onPostExecute(File result) {
        Fragment prev = formEntryActivity.get().getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.COLLECT_PROGRESS_DIALOG_TAG);
        if (prev != null && !formEntryActivity.get().isInstanceStateSaved()) {
            ((DialogFragment) prev).dismiss();
        }
        formEntryActivity.get().setBinaryWidgetData(result);
    }
}
