package com.github.luks91.creatingflowables

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.disposables.CancellableDisposable

fun currentNetwork(context: Context,
                   scheduler: Scheduler = AndroidSchedulers.mainThread())
        : Flowable<Network> {

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return Flowable.create<Network>({ emitter ->

        val worker = scheduler.createWorker()
        val emit = { network: Network ->
            worker.schedule { emitter.onNext(network) }
        }

        emit(Network.NONE)
        val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        val networkCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                emit(Network.CELLULAR)
            }

            override fun onLost(network: android.net.Network) {
                emit(Network.NONE)
            }
        }

        connectivityManager.requestNetwork(request, networkCallback)
        emitter.setDisposable(CompositeDisposable(
                worker,
                CancellableDisposable {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        ))
    }, BackpressureStrategy.LATEST)
}

enum class Network {
    NONE,
    CELLULAR
}
