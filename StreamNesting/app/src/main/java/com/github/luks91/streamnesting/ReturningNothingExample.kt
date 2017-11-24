package com.github.luks91.streamnesting

import io.reactivex.Flowable
import io.reactivex.rxkotlin.withLatestFrom

fun combine(triggers: Flowable<Boolean>,
            gatekeepers: Flowable<Boolean>) =
    triggers.withLatestFrom(gatekeepers,
            { trigger, gateIsOpen ->
                if (gateIsOpen) {
                    Flowable.just(trigger)
                } else {
                    Flowable.empty()
                }
            })
            .concatMap { it }