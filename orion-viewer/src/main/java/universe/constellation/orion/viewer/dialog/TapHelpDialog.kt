package universe.constellation.orion.viewer.dialog

import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionTapActivity.Companion.getDefaultAction
import universe.constellation.orion.viewer.prefs.OrionTapActivity.Companion.getKey

class TapHelpDialog(activity: OrionViewerActivity) :
    DialogOverView(activity, R.layout.tap, android.R.style.Theme_Translucent) {
    init {
        dialog.setTitle(R.string.tap_zones_header)
        val table = dialog.findViewById<TableLayout>(R.id.tap_table)
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        for (i in 0 until table.childCount) {
            val row = table.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val layout = row.getChildAt(j)
                val shortText = layout.findViewById<TextView>(R.id.shortClick)
                val longText = layout.findViewById<TextView>(R.id.longClick)
                longText.visibility = View.GONE
                var shortCode = prefs.getInt(getKey(i, j, false), -1)
                if (shortCode == -1) {
                    shortCode = getDefaultAction(i, j, false)
                }
                val saction = Action.getAction(shortCode)
                //ffcc66
                layout.setBackgroundColor(if (saction === Action.NEXT) -0x2255bc else if (saction === Action.PREV) -0x1144ab else -0x339a)
                shortText.text = activity.getResources().getString(saction.getName())
                shortText.textSize = 20f


                //if (!(i == 1 && j == 1)) {
                val layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.addRule(if (i == 1 && j == 1) RelativeLayout.ALIGN_TOP else RelativeLayout.CENTER_IN_PARENT)
                shortText.setLayoutParams(layoutParams)
                //}
            }
        }
        val view = dialog.findViewById<ImageView>(R.id.tap_help_close)
        view.setVisibility(View.VISIBLE)
        view.isClickable = true
        view.setOnClickListener { v: View? ->
            dialog.dismiss()
            activity.globalOptions.saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false)
        }
    }

    fun showDialog() {
        initDialogSize()
        dialog.show()
    }
}
