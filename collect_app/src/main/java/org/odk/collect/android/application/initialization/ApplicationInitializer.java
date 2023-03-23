package org.odk.collect.android.application.initialization;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import net.danlew.android.joda.JodaTimeAndroid;

import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.xform.parse.XFormParser;
import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.analytics.Analytics;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.FormUpdateMode;
import org.odk.collect.android.geo.MapboxUtils;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.logic.actions.setgeopoint.CollectSetGeopointActionHandler;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.utilities.UserAgentProvider;

import java.util.Locale;

import timber.log.Timber;

import static org.odk.collect.android.configure.SettingsUtils.getFormUpdateMode;

public class ApplicationInitializer {

    private final Application context;
    private final UserAgentProvider userAgentProvider;
    private final SettingsPreferenceMigrator preferenceMigrator;
    private final PropertyManager propertyManager;
    private final Analytics analytics;
    private final GeneralSharedPreferences generalSharedPreferences;
    private final AdminSharedPreferences adminSharedPreferences;

    public ApplicationInitializer(Application context, UserAgentProvider userAgentProvider, SettingsPreferenceMigrator preferenceMigrator, PropertyManager propertyManager, Analytics analytics) {
        this.context = context;
        this.userAgentProvider = userAgentProvider;
        this.preferenceMigrator = preferenceMigrator;
        this.propertyManager = propertyManager;
        this.analytics = analytics;

        generalSharedPreferences = GeneralSharedPreferences.getInstance();
        adminSharedPreferences = AdminSharedPreferences.getInstance();
    }

    public void initialize() {
        initializePreferences();
        initializeFrameworks();
        initializeLocale();
    }

    private void initializePreferences() {
        performMigrations();
        reloadSharedPreferences();
    }

    private void initializeFrameworks() {
        JodaTimeAndroid.init(context);
        initializeLogging();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        initializeMapFrameworks();
        initializeJavaRosa();
        initializeAnalytics();
    }

    private void initializeAnalytics() {
        FormUpdateMode formUpdateMode = getFormUpdateMode(context, generalSharedPreferences.getSharedPreferences());
        analytics.setUserProperty("FormUpdateMode", formUpdateMode.getValue(context));
    }

    private void initializeLocale() {
        Collect.defaultSysLanguage = Locale.getDefault().getLanguage();
    }

    private void initializeJavaRosa() {
        propertyManager.reload();
        org.javarosa.core.services.PropertyManager
                .setPropertyManager(propertyManager);

        // Register prototypes for classes that FormDef uses
        PrototypeManager.registerPrototypes(JavaRosaCoreModule.classNames);
        PrototypeManager.registerPrototypes(CoreModelModule.classNames);
        new XFormsModule().registerModule();

        // When registering prototypes from Collect, a proguard exception also needs to be added
        PrototypeManager.registerPrototype("org.odk.collect.android.logic.actions.setgeopoint.CollectSetGeopointAction");
        XFormParser.registerActionHandler(CollectSetGeopointActionHandler.ELEMENT_NAME, new CollectSetGeopointActionHandler());
    }

    private void initializeLogging() {
        Timber.plant(new Timber.DebugTree());
    }

    private void reloadSharedPreferences() {
        generalSharedPreferences.reloadPreferences();
        adminSharedPreferences.reloadPreferences();
    }

    private void performMigrations() {
        preferenceMigrator.migrate(generalSharedPreferences.getSharedPreferences(), adminSharedPreferences.getSharedPreferences());
    }

    private void initializeMapFrameworks() {
        try {
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgentProvider.getUserAgent());
            MapboxUtils.initMapbox();
        } catch (Exception | Error ignore) {
            // ignored
        }
    }
}

