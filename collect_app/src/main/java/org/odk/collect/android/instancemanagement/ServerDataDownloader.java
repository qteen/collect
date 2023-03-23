package org.odk.collect.android.instancemanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.javarosa.core.model.instance.FormInstance;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.database.DatabaseInstancesRepository;
import org.odk.collect.android.exception.ParsingException;
import org.odk.collect.android.formentry.loading.FormInstanceFileCreator;
import org.odk.collect.android.formmanagement.FormDownloadException;
import org.odk.collect.android.formmanagement.FormDownloader;
import org.odk.collect.android.formmanagement.FormMetadataParser;
import org.odk.collect.android.formmanagement.ServerFormDetails;
import org.odk.collect.android.forms.Form;
import org.odk.collect.android.forms.FormSource;
import org.odk.collect.android.forms.FormSourceException;
import org.odk.collect.android.forms.FormsRepository;
import org.odk.collect.android.forms.SubmissionManifest;
import org.odk.collect.android.instances.Instance;
import org.odk.collect.android.instances.InstancesRepository;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.storage.StoragePathProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import timber.log.Timber;

public class ServerDataDownloader implements DataDownloader {
    private final FormSource formSource;
    private FormsRepository formsRepository;
    private StoragePathProvider storagePathProvider;

    public ServerDataDownloader(FormSource formSource, FormsRepository formsRepository, StoragePathProvider storagePathProvider) {
        this.formSource = formSource;
        this.formsRepository = formsRepository;
        this.storagePathProvider = storagePathProvider;
    }

    @Override
    public void downloadData(ServerFormDetails form, @Nullable FormDownloader.ProgressReporter progressReporter, @Nullable Supplier<Boolean> isCancelled) throws IOException {
        Form formOnDevice = formsRepository.get(form.getFormId(), form.getFormVersion());
        if (formOnDevice != null && formOnDevice.isDeleted()) {
            formsRepository.restore(formOnDevice.getId());
        }

        FormInstanceFileCreator formInstanceFileCreator = new FormInstanceFileCreator(
                storagePathProvider,
                System::currentTimeMillis
        );

        try {
            List<String> formSubmissionIds = formSource.fetchFormSubmissionIds(form.getFormId());
            int count = 0;
            InstancesDao instancesDao = new InstancesDao();
            for (String submissionId : formSubmissionIds) {
                SubmissionManifest submissionManifest = formSource.fetchData(form.getFormId(), form.getFormVersion(), submissionId);
                File instanceFile = formInstanceFileCreator.createInstanceFile(formOnDevice.getFormFilePath(), submissionManifest);
                Cursor sentById = instancesDao.getSentInstancesCursorByName(submissionManifest.getInstanceName());

                if(!instanceFile.exists() || sentById.getCount()>0) {
                    exportXmlFile(submissionManifest, instanceFile);
                    updateInstanceDatabase(true, true, submissionManifest.getInstanceName(), instanceFile, form);
                }

                count++;
                progressReporter.onDownloadingMediaFile(count);
            }
        } catch (FormSourceException | ParsingException e) {
            e.printStackTrace();
        }
    }

    private void updateInstanceDatabase(boolean incomplete, boolean canEditAfterCompleted, String instanceName, File instanceFile, ServerFormDetails fd) {
        String instancePath = instanceFile.getAbsolutePath();
        ContentValues values = new ContentValues();
        if (instanceName != null) {
            values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, instanceName);
        }
        if (incomplete) {
            values.put(InstanceProviderAPI.InstanceColumns.STATUS, Instance.STATUS_INCOMPLETE);
        } else {
            values.put(InstanceProviderAPI.InstanceColumns.STATUS, Instance.STATUS_COMPLETE);
        }
        values.put(InstanceProviderAPI.InstanceColumns.CAN_EDIT_WHEN_COMPLETE, Boolean.toString(canEditAfterCompleted));

        // Set uri to handle encrypted case (see exportData)
        InstancesRepository instances = new DatabaseInstancesRepository();
        Instance instance = instances.getByPath(instancePath);
        Uri uri = FormsProviderAPI.FormsColumns.CONTENT_URI;
        if (instance != null) {
            uri = Uri.withAppendedPath(InstanceProviderAPI.InstanceColumns.CONTENT_URI, instance.getId().toString());

//            String geometryXpath = getGeometryXpathForInstance(uri);
//            ContentValues geometryContentValues = extractGeometryContentValues(formInstance, geometryXpath);
//            if (geometryContentValues != null) {
//                values.putAll(geometryContentValues);
//            }
        }

        String where = InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + "=?";
        int updated = new InstancesDao().updateInstance(values, where, new String[] {new StoragePathProvider().getInstanceDbPath(instancePath)});
        if (updated > 1) {
            Timber.w("Updated more than one entry, that's not good: %s", instancePath);
        } else if (updated == 1) {
            Timber.i("Instance found and successfully updated: %s", instancePath);
            // already existed and updated just fine
        } else {
            Timber.i("No instance found, creating");
            String[] selectionArgs = new String[]{fd.getFormId(), fd.getFormVersion()};
            String selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? AND "
                    + FormsProviderAPI.FormsColumns.JR_VERSION + "=?";
            if(fd.getFormVersion()==null) {
                selectionArgs = new String[]{fd.getFormId()};
                selection = FormsProviderAPI.FormsColumns.JR_FORM_ID + "=?";
            }
            try (Cursor c = Collect.getInstance().getContentResolver().query(uri, null, selection, selectionArgs, null)) {
                // retrieve the form definition...
                c.moveToFirst();
                String formname = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.DISPLAY_NAME));
                String submissionUri = null;
                if (!c.isNull(c.getColumnIndex(FormsProviderAPI.FormsColumns.SUBMISSION_URI))) {
                    submissionUri = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.SUBMISSION_URI));
                }

                // add missing fields into values
                values.put(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH, new StoragePathProvider().getInstanceDbPath(instancePath));
                values.put(InstanceProviderAPI.InstanceColumns.SUBMISSION_URI, submissionUri);
                if (instanceName != null) {
                    values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, instanceName);
                } else {
                    values.put(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME, formname);
                }
                String jrformid = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID));
                String jrversion = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_VERSION));
                values.put(InstanceProviderAPI.InstanceColumns.JR_FORM_ID, jrformid);
                values.put(InstanceProviderAPI.InstanceColumns.JR_VERSION, jrversion);

//                String geometryXpath = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.GEOMETRY_XPATH));
//                ContentValues geometryContentValues = extractGeometryContentValues(formInstance, geometryXpath);
//                if (geometryContentValues != null) {
//                    values.putAll(geometryContentValues);
//                }
            }
            uri = new InstancesDao().saveInstance(values);
        }
    }

    private void exportXmlFile(SubmissionManifest submissionManifest, File instanceFile) throws IOException {
        if (instanceFile.exists() && !instanceFile.delete()) {
            throw new IOException("Cannot overwrite " + instanceFile.getAbsolutePath() + ". Perhaps the file is locked?");
        }

        FileWriter writer = new FileWriter(instanceFile);
        writer.write(submissionManifest.getSubmissionXml());
        writer.flush();
        writer.close();
    }
}
