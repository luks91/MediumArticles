package com.github.luks91.streamnesting

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DefaultSubscriber

data class Contact(val firstName: String, val lastName: String, val phoneNumber: String)

class Repository {
    fun save(contacts: Flowable<Contact>) =
            contacts
                    .observeOn(Schedulers.io())
                    .window(30)
                    .concatMap { window -> window.toList().toFlowable() }
                    .subscribe(object : DefaultSubscriber<List<Contact>>() {
                        override fun onStart() = request(1)

                        override fun onNext(contacts: List<Contact>) {
                            openConnection<Contact>().use {
                                for (contact in contacts)
                                it.write(contact)
                            }
                            request(1)
                        }

                        override fun onComplete() {}
                        override fun onError(t: Throwable) {
                            Log.e("TAG", "Oops!", t)
                        }
                    })

    fun <T> openConnection(): Connection<T> {
        Log.i("TAG", "Opening database...")
        Thread.sleep(2000)
        return Connection()
    }

    class Connection<in T>: AutoCloseable {
        fun write(data: T) {
            Thread.sleep(10)
            Log.i("TAG", "Saving: $data")
        }

        override fun close() {
            Log.i("TAG", "Closing database...")
            Thread.sleep(2000)
        }
    }
}
