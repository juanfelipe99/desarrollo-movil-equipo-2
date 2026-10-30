
package com.equipo2.vinilos.hu02

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.equipo2.vinilos.R
import com.equipo2.vinilos.support.scrollToWithBarsHidden

fun expandAlbumDescription() {
    onView(withId(R.id.albumDescriptionToggle))
        .perform(scrollToWithBarsHidden())
        .check(matches(isDisplayed()))
        .perform(click())
}

fun verifyTracksAndCommentsSections() {
    onView(withId(R.id.tracksCount))
        .perform(scrollToWithBarsHidden())
        .check(matches(isDisplayed()))

    onView(withId(R.id.commentsCount))
        .perform(scrollToWithBarsHidden())
        .check(matches(isDisplayed()))
}
