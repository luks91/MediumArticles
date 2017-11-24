package com.github.luks91.streamnesting

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //note than none of the streams in this example are unsubsribed, this has to be done in case of production code
        triggerOverlyProducingStreamExample()
        triggerContentObserverExample()
        triggerHelperClassesExample()
        triggerReturningNothingExample()
    }

    private fun triggerOverlyProducingStreamExample() {
        val source = Flowable.interval(200, TimeUnit.MILLISECONDS)
                .map { Contact("FirstName $it", "LastName $it", "$it") }

        Repository().save(source)
    }

    private fun triggerContentObserverExample() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    0)
        } else {
            contacts(contentResolver).subscribe { Log.i("MainActivity", "New contact: $it") }
            //stream is not being unsubscribed so we can see the logcat information when adding contacts from the Android UI
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    contacts(contentResolver).subscribe { Log.i("MainActivity", "New contact: $it") }
                }
            }
        }
    }

    private fun triggerHelperClassesExample() {
        val credentials = Flowable.just(Credentials("username", "password"))
        val urls = Flowable.just("https://sampleurl.com")

        obtainContacts(credentials, urls)
                .subscribe { Log.i("HelperClassesExample", "Received contact $it") }
    }

    private fun triggerReturningNothingExample() {
        val triggers = Flowable.interval(1, TimeUnit.SECONDS).map { it % 2L == 0L }
        val gatekeepers = Flowable.interval(5, TimeUnit.SECONDS).map { it % 2L != 0L }.startWith(true)

        combine(triggers, gatekeepers)
                .subscribe { Log.i("TriggersWithGatekeeper", "Received: $it") }
    }
}
