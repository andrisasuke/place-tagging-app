package com.andrisasuke.placetagging

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import com.andrisasuke.placetagging.util.LocalPreferences

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var snackbar: Snackbar
    protected lateinit var alert: AlertDialog
    protected val handler by lazy { Handler() }
    protected val localPreferences by lazy { LocalPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alert = AlertDialog.Builder(this).create()
    }

    fun showSnackbar(view: View, message: String) {
        this.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setAction("OK") {
                    snackbar.dismiss()
                }
        this.snackbar.show()
    }

    fun simpleDialog(title: String?, message: String?, cancelable: Boolean, cancelButton: Boolean, listener: DialogInterface.OnClickListener) {
        dismissSimpleDialog()
        if (!this.isFinishing) {
            val builder = AlertDialog.Builder(this)
                    .setMessage(message ?: "")
                    .setCancelable(cancelable)
                    .setPositiveButton("OK", listener)
                    .setTitle(title)
            if (cancelButton)
                builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
            alert = builder.show()
        }
    }

    fun dismissSimpleDialog() {
        if (alert.isShowing)
            alert.dismiss()
    }

    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        return handler.postDelayed(r, delayMillis)
    }
}