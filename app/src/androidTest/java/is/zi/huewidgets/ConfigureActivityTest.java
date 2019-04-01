package is.zi.huewidgets;


import is.zi.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;


public class ConfigureActivityTest {

    @NonNull
    @Rule
    public final ActivityTestRule<ConfigureActivity> mActivityTestRule = new ActivityTestRule<>(ConfigureActivity.class, true);

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
    @Ignore("Unknown issue with Travis CI")
    public void runConfigActivityTest() {
        onView(
                allOf(
                        withId(android.R.id.list),
                        ConfigureActivityTest.childAtPosition(
                                ConfigureActivityTest.childAtPosition(
                                        withId(android.R.id.content),
                                        0
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );

    }
}
