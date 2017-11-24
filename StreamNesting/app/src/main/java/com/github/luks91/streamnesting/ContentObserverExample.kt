package com.github.luks91.streamnesting

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract.Contacts
import io.reactivex.BackpressureStrategy
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable

data class SimpleContact(val displayName: String, val isStarred: Boolean) {
    companion object {
        fun from(cursor: Cursor) =
                SimpleContact(
                    cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)),
                    cursor.getInt(cursor.getColumnIndex(Contacts.STARRED)) == 1)
    }
}

fun contacts(contentResolver: ContentResolver): Flowable<SimpleContact> =
        contentResolver.uriChangesOf(
                {
                    itemsFor(
                            { query(Contacts.CONTENT_URI,
                                    arrayOf(Contacts.DISPLAY_NAME, Contacts.STARRED),
                                    null, null,
                                    "${Contacts.DISPLAY_NAME} ASC") },
                            { SimpleContact.from(it) }
                    )
                }, Contacts.CONTENT_URI)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .switchMap { obs -> obs }

inline fun <T> ContentResolver.uriChangesOf(
        crossinline onUriChanged: ContentResolver.() -> T,
        vararg uris: Uri, notifyForDescendants: Boolean = false): Flowable<T> =
        Flowable.create<T>({ emitter ->
            val contentObserver = object : ContentObserver(Handler()) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    emitter.onNext(onUriChanged())
                }
            }
            uris.forEach {
                registerContentObserver(it, notifyForDescendants, contentObserver)
            }
            emitter.onNext(onUriChanged())
            emitter.setCancellable { unregisterContentObserver(contentObserver) }
        }, BackpressureStrategy.LATEST)

inline fun <T : Any> ContentResolver.itemsFor(
        crossinline queryRawData: ContentResolver.() -> Cursor,
        crossinline mapRawData: (Cursor) -> T): Flowable<T> =
        Flowable.generate<T, Cursor>(
                Callable<Cursor> { queryRawData(this) },
                BiFunction<Cursor, Emitter<T>, Cursor> { cursor, emitter ->
                    if (cursor.moveToNext()) {
                        emitter.onNext(mapRawData(cursor))
                    } else {
                        emitter.onComplete()
                    }
                    return@BiFunction cursor
                },
                Consumer<Cursor> { cursor -> cursor.close() }
        )