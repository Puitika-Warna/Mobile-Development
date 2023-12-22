package com.puitika.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.puitika.R
import org.mozilla.gecko.util.ThreadUtils
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.MessageDelegate
import org.mozilla.geckoview.WebExtension.MessageSender


class GoogleSignIn : AppCompatActivity() {
    private var sRuntime: GeckoRuntime? = null
    private val EXTENSION_LOCATION = "resource://android/assets/googlesignin/"
    private val EXTENSION_ID = "googlesignin@ihsan.com"

    override fun onDestroy() {
        super.onDestroy()
//        sRuntime?.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)

        val view = findViewById<GeckoView>(R.id.googlesignin_webview)
        val session = GeckoSession()
        session.contentDelegate = object : ContentDelegate {}

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this)
        }


        val messageDelegate: MessageDelegate = object : MessageDelegate {
            @Nullable
            override fun onMessage(
                nativeApp: String,
                message: Any,
                sender: MessageSender
            ): GeckoResult<Any>? {
                Log.d("MessageDelegate", message.toString())

                val returnIntent = Intent()
                returnIntent.putExtra("data", message.toString())
                setResult(RESULT_OK, returnIntent)
                finish()

                session.stop()
                session.close()

                return null
            }
        }

        sRuntime!!
            .webExtensionController
            .ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID)
            .accept( // Set delegate that will receive messages coming from this extension.
                { extension: WebExtension? ->
                    ThreadUtils.runOnUiThread(
                        Runnable {
                            session
                                .webExtensionController
                                .setMessageDelegate(extension!!, messageDelegate, "browser")
                        })
                }
            )  // Something bad happened, let's log an error
            { e: Throwable? ->
                Log.e(
                    "MessageDelegate",
                    "Error registering extension",
                    e
                )
            }

        session.open(sRuntime!!)
        view.setSession(session)
        session.loadUri("http://puitika.my.id/loginwithgoogle");
    }
}