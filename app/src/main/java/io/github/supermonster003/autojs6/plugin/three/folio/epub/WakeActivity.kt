package io.github.supermonster003.autojs6.plugin.three.folio.epub

import android.app.Activity
import android.os.Bundle

/** Activation entry point (`org.autojs.plugin.action.WAKE`): finishes immediately, no side effects. */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
