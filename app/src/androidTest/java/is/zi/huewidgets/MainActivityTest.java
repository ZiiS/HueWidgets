package is.zi.huewidgets;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import androidx.test.rule.ActivityTestRule;
import is.zi.NonNull;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

public class MainActivityTest {

    @NonNull
    @Rule
    public final ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, true);

    private static Matcher<View> childAtPosition(
            @NonNull Matcher<View> parentMatcher, @SuppressWarnings("SameParameterValue") int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(@NonNull Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(@NonNull View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    @Test
    public void mainActivityTest() throws IOException, InterruptedException {
        onView(
                allOf(
                        withId(R.id.add),
                        withContentDescription("Shortcut"),
                        MainActivityTest.childAtPosition(
                                MainActivityTest.childAtPosition(
                                        withId(android.R.id.content),
                                        0
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );

        new Screenshot(mActivityTestRule.getActivity(), "mainActivity");
    }


}
