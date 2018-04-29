package com.github.luks91.creatingflowables

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import java.util.concurrent.Callable

fun contacts(contentResolver: ContentResolver): Flowable<SimpleContact> =
        Flowable.generate<SimpleContact, Cursor>(
                Callable<Cursor> {
                    contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                            arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.STARRED),
                            null, null,
                            "${ContactsContract.Contacts.DISPLAY_NAME} ASC")
                },

                BiFunction<Cursor, Emitter<SimpleContact>, Cursor> { cursor, emitter ->
                    if (cursor.moveToNext()) {
                        emitter.onNext(SimpleContact.from(cursor))
                    } else {
                        emitter.onComplete()
                    }
                    return@BiFunction cursor
                },

                Consumer<Cursor> {
                    cursor -> cursor.close()
                }
        )

data class SimpleContact(val displayName: String, val isStarred: Boolean) {
    companion object {
        fun from(cursor: Cursor) =
                SimpleContact(
                        cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
                        cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1)
    }
}