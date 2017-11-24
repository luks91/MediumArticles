package com.github.luks91.streamnesting

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables


data class Credentials(val username: String, val password: String)

interface ContactsApi {
    fun getContacts(credentials: Credentials): Flowable<SimpleContact>

    companion object {
        fun createFor(serverUrl: String) = object: ContactsApi {
            override fun getContacts(credentials: Credentials): Flowable<SimpleContact> {
                Log.i("ContactsApi",
                        "Querying for contacts from $serverUrl with $credentials")
                return Flowable.just(
                        SimpleContact("Contact1", isStarred = false),
                        SimpleContact("Contact2", isStarred = false),
                        SimpleContact("Contact3", isStarred = true)
                )
            }
        }
    }
}

fun obtainContacts(credentials: Flowable<Credentials>,
                   serverUrls: Flowable<String>) =
    Flowables.combineLatest(
            credentials, serverUrls,
            { secrets, url ->
                ContactsApi.createFor(url).getContacts(secrets)
            }
    ).switchMap { it }
