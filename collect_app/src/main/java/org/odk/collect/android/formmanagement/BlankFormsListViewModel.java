package org.odk.collect.android.formmanagement;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.odk.collect.android.analytics.Analytics;
import org.odk.collect.android.analytics.AnalyticsEvents;
import org.odk.collect.android.backgroundwork.ChangeLock;
import org.odk.collect.android.formmanagement.matchexactly.ServerFormsSynchronizer;
import org.odk.collect.android.formmanagement.matchexactly.SyncStatusRepository;
import org.odk.collect.android.forms.FormSourceException;
import org.odk.collect.android.notifications.Notifier;
import org.odk.collect.android.preferences.FormUpdateMode;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.PreferencesProvider;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.async.Scheduler;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import static org.odk.collect.android.configure.SettingsUtils.getFormUpdateMode;

public class BlankFormsListViewModel extends ViewModel {

    private final Application application;
    private final Scheduler scheduler;
    private final SyncStatusRepository syncRepository;
    private final ServerFormsSynchronizer serverFormsSynchronizer;
    private final PreferencesProvider preferencesProvider;
    private final Notifier notifier;
    private final ChangeLock changeLock;
    private final Analytics analytics;

    public BlankFormsListViewModel(Application application, Scheduler scheduler, SyncStatusRepository syncRepository, ServerFormsSynchronizer serverFormsSynchronizer, PreferencesProvider preferencesProvider, Notifier notifier, ChangeLock changeLock, Analytics analytics) {
        this.application = application;
        this.scheduler = scheduler;
        this.syncRepository = syncRepository;
        this.serverFormsSynchronizer = serverFormsSynchronizer;
        this.preferencesProvider = preferencesProvider;
        this.notifier = notifier;
        this.changeLock = changeLock;
        this.analytics = analytics;
    }

    public boolean isMatchExactlyEnabled() {
        return getFormUpdateMode(application, preferencesProvider.getGeneralSharedPreferences()) == FormUpdateMode.MATCH_EXACTLY;
    }

    public LiveData<Boolean> isSyncing() {
        return syncRepository.isSyncing();
    }

    public LiveData<Boolean> isOutOfSync() {
        return Transformations.map(syncRepository.getSyncError(), Objects::nonNull);
    }

    public LiveData<Boolean> isAuthenticationRequired() {
        return Transformations.map(syncRepository.getSyncError(), error -> {
            if (error != null) {
                return error.getType() == FormSourceException.Type.AUTH_REQUIRED;
            } else {
                return false;
            }
        });
    }

    public LiveData<Boolean> syncWithServer() {
        logManualSync();

        MutableLiveData<Boolean> result = new MutableLiveData<>();

        changeLock.withLock(acquiredLock -> {
            if (acquiredLock) {
                syncRepository.startSync();

                scheduler.immediate(() -> {
                    FormSourceException exception = null;

                    try {
                        serverFormsSynchronizer.synchronize();
                    } catch (FormSourceException e) {
                        exception = e;
                    }

                    return exception;
                }, exception -> {
                        if (exception == null) {
                        syncRepository.finishSync(null);
                        notifier.onSync(null);
                        analytics.logEvent(AnalyticsEvents.MATCH_EXACTLY_SYNC_COMPLETED, "Success");

                        result.setValue(true);
                    } else {
                        syncRepository.finishSync(exception);
                        notifier.onSync(exception);
                        analytics.logEvent(AnalyticsEvents.MATCH_EXACTLY_SYNC_COMPLETED, exception.getType().toString());

                        result.setValue(false);
                    }
                });
            }

            return null;
        });

        return result;
    }

    private void logManualSync() {
        Uri uri = Uri.parse(preferencesProvider.getGeneralSharedPreferences().getString(GeneralKeys.KEY_SERVER_URL, ""));
        String host = uri.getHost() != null ? uri.getHost() : "";
        String urlHash = FileUtils.getMd5Hash(new ByteArrayInputStream(host.getBytes()));
        analytics.logEvent(AnalyticsEvents.MATCH_EXACTLY_SYNC, "Manual", urlHash);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final Scheduler scheduler;
        private final SyncStatusRepository syncRepository;
        private final ServerFormsSynchronizer serverFormsSynchronizer;
        private final PreferencesProvider preferencesProvider;
        private final Notifier notifier;
        private final ChangeLock changeLock;
        private final Analytics analytics;

        @Inject
        public Factory(Application application, Scheduler scheduler, SyncStatusRepository syncRepository, ServerFormsSynchronizer serverFormsSynchronizer, PreferencesProvider preferencesProvider, Notifier notifier, @Named("FORMS") ChangeLock changeLock, Analytics analytics) {
            this.application = application;
            this.scheduler = scheduler;
            this.syncRepository = syncRepository;
            this.serverFormsSynchronizer = serverFormsSynchronizer;
            this.preferencesProvider = preferencesProvider;
            this.notifier = notifier;
            this.changeLock = changeLock;
            this.analytics = analytics;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new BlankFormsListViewModel(application, scheduler, syncRepository, serverFormsSynchronizer, preferencesProvider, notifier, changeLock, analytics);
        }
    }
}
