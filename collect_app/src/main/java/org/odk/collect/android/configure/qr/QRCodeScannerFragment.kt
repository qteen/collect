package org.odk.collect.android.configure.qr

import android.content.Context
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.analytics.AnalyticsEvents
import org.odk.collect.android.fragments.BarCodeScannerFragment
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.storage.StoragePathProvider
import org.odk.collect.androidshared.ui.ToastUtils.showLongToast
import org.odk.collect.androidshared.utils.CompressionUtils
import org.odk.collect.projects.ProjectConfigurationResult
import org.odk.collect.settings.ODKAppSettingsImporter
import java.io.File
import java.io.IOException
import java.util.zip.DataFormatException
import javax.inject.Inject

class QRCodeScannerFragment : BarCodeScannerFragment() {

    @Inject
    lateinit var settingsImporter: ODKAppSettingsImporter

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    @Inject
    lateinit var storagePathProvider: StoragePathProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerUtils.getComponent(context).inject(this)
    }

    @Throws(IOException::class, DataFormatException::class)
    override fun handleScanningResult(result: String) {
        val oldProjectName = projectsDataService.requireCurrentProject().name

        val settingsImportingResult = settingsImporter.fromJSON(
            CompressionUtils.decompress(result),
            projectsDataService.requireCurrentProject()
        )

        when (settingsImportingResult) {
            ProjectConfigurationResult.SUCCESS -> {
                Analytics.log(AnalyticsEvents.RECONFIGURE_PROJECT)

                val newProjectName = projectsDataService.requireCurrentProject().name
                if (newProjectName != oldProjectName) {
                    File(storagePathProvider.getProjectRootDirPath() + File.separator + oldProjectName).delete()
                    File(storagePathProvider.getProjectRootDirPath() + File.separator + newProjectName).createNewFile()
                }

                showLongToast(
                    getString(org.odk.collect.strings.R.string.successfully_imported_settings)
                )
                ActivityUtils.startActivityAndCloseAllOthers(
                    requireActivity(),
                    MainMenuActivity::class.java
                )
            }

            ProjectConfigurationResult.INVALID_SETTINGS -> showLongToast(
                getString(
                    org.odk.collect.strings.R.string.invalid_qrcode
                )
            )

            ProjectConfigurationResult.GD_PROJECT -> showLongToast(
                getString(org.odk.collect.strings.R.string.settings_with_gd_protocol)
            )
        }
    }

    override fun isQrOnly(): Boolean {
        return true
    }
}
