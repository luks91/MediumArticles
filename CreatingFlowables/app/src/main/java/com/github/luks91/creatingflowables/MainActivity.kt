package com.github.luks91.creatingflowables

import android.Manifest
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.util.*

class MainActivity : AppCompatActivity() {

    private var disposable = Disposables.disposed()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.READ_CONTACTS)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            disposable = CompositeDisposable(
                                    //Controlled Source
                                    contacts(contentResolver)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe { Log.i("TMPTAG", "Contact: $it") },
                                    //Uncontrolled source
                                    sharedPreferenceValues(preferences, "key")
                                            .subscribe { Log.i("TMPTAG", "Obtained a new preferences value: $it") },
                                    //Uncontrolled multithreaded source
                                    currentNetwork(this@MainActivity)
                                            .subscribe {
                                                Log.i("TMPTAG", "Current network: $it on thread: ${Thread.currentThread().name}")
                                            }
                            )
                        } else {
                            finish()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()

                val random = Random()
                findViewById<View>(R.id.button).setOnClickListener {
                    preferences.edit().putString("key", "value ${random.nextFloat()}").apply()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }


}



