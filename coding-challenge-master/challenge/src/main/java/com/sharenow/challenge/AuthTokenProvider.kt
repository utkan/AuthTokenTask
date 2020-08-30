package com.sharenow.challenge

import io.reactivex.Observable
import io.reactivex.Observable.never
import io.reactivex.Scheduler
import io.reactivex.Single
import org.threeten.bp.ZonedDateTime

class AuthTokenProvider(

    /**
     * Scheduler used for background operations. Execute time relevant operations on this one,
     * so we can use [TestScheduler] within unit tests.
     */
    private val computationScheduler: Scheduler,

    /**
     * Single to be observed in order to get a new token.
     */
    private val refreshAuthToken: Single<AuthToken>,

    /**
     * Observable for the login state of the user. Will emit true, if he is logged in.
     */
    private val isLoggedInObservable: Observable<Boolean>,

    /**
     * Function that returns you the current time, whenever you need it. Please use this whenever you check the
     * current time, so we can manipulate time in unit tests.
     */
    private val currentTime: () -> ZonedDateTime
) {

    /**
     * @return the observable auth token as a string
     */
    fun observeToken(): Observable<String> {
        return never() // TODO Fill this method with life
    }
}