package com.github.luks91.creatingflowables

import android.content.SharedPreferences
import android.text.TextUtils
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

fun sharedPreferenceValues(preferences: SharedPreferences, key: String): Flowable<String> =
        Flowable.create({ emitter ->
            val emitCurrentKey = { emitter.onNext(preferences.getString(key, "")) }

            val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, updatedKey ->
                if (TextUtils.equals(key, updatedKey)) {
                    emitCurrentKey()
                }
            }

            preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
            emitCurrentKey()
            emitter.setCancellable { preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener) }
        }, BackpressureStrategy.LATEST)