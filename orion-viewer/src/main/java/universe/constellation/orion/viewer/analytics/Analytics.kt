package universe.constellation.orion.viewer.analytics

import android.content.ContentResolver
import android.content.Intent
import android.provider.Settings
import universe.constellation.orion.viewer.BuildConfig
import java.io.File

const val TAP_HELP_DIALOG = "TAPHELPDialog"
const val FALLBACK_DIALOG = "FALLBACK"
const val SHOW_ERROR_DIALOG = "SHOW_ERROR"

open class Analytics {


    open fun init(): Analytics {
        return this
    }

    open fun onNewIntent(contentResolver: ContentResolver, intent: Intent, isUserIntent: Boolean, isNewUI: Boolean) {

    }

    open fun fileOpenedSuccessfully(file: File) {

    }

    open fun errorDuringInitialFileOpen() {

    }

    open fun error(ex: Throwable) {

    }

    open fun dialog(name: String, opened: Boolean) {

    }

    open fun onApplicationInit(isNewUser: Boolean) {

    }

    open fun permissionEvent(screen: String, state: Boolean) {

    }

    companion object {

        fun initialize(contentResolver: ContentResolver, analytics: Analytics): Analytics {
            val isTestLabOrDebug = BuildConfig.DEBUG || Settings.System.getString(contentResolver, "firebase.test.lab").toBoolean()
            if (!isTestLabOrDebug) {
                return analytics.init()
            }
            return Analytics().init()
        }
    }
}