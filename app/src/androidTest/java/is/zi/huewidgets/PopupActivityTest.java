package is.zi.huewidgets;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.ServiceTestRule;
import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.comm.Http;
import is.zi.hue.HueBridge;
import is.zi.hue.HueBridgeService;
import is.zi.hueaccounts.AccountAuthenticatorService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PopupActivityTest {

    @NonNull
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @NonNull
    @Rule
    final public ActivityTestRule<PopupActivity> activityRule = new ActivityTestRule<PopupActivity>(PopupActivity.class, true) {

        @Override
        protected void afterActivityFinished() {
            @SuppressWarnings("deprecation") AccountManager accountManager = AccountManager.get(ApplicationProvider.getApplicationContext());
            Account account = new Account(
                    "TestAccount",
                    AccountAuthenticatorService.ACCOUNT_TYPE
            );
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccount(account, null, null);
            } else {
                accountManager.removeAccountExplicitly(account);
            }
        }

        @Override
        protected void beforeActivityLaunched() {
            //noinspection deprecation
            PreferenceManager.getDefaultSharedPreferences(
                    ApplicationProvider.getApplicationContext()
            )
                    .edit()
                    .putString("light_0", "{\"path\": \"/lights/1\"}")
                    .apply();

            IBinder binder = null;
            try {
                binder = serviceRule.bindService(
                        new Intent(ApplicationProvider.getApplicationContext(),
                                HueBridgeService.class));
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            HueBridgeService service = ((HueBridgeService.LocalBinder) Objects.requireNonNull(binder)).getService();
            service.setThrowOnAlert();
            service.setBridgeFactory(new HueBridge.Factory() {
                @NonNull
                @Override
                public HueBridge create(@NonNull String url, @NonNull String username, @Nullable X509Certificate cert) throws MalformedURLException {
                    return new HueBridge(url, username, cert) {
                        @NonNull
                        @Override
                        public Http getHttp(@NonNull String path) {
                            Http http = mock(Http.class);
                            switch (path) {
                                case "/api/fake/lights/1":
                                    try {
                                        doReturn(new JSONObject("{\"state\": {\"on\": true, \"xy\": [.3, .3], \"bri\": 200}}")).when(http).json();
                                    } catch (@NonNull JSONException | Http.HttpException | IOException | Http.ApplicationLayerException e) {
                                        fail(e.getMessage());
                                    }
                                    break;
                                case "/api/fake/lights/1/state":
                                    try {
                                        doReturn(null).when(http).json(any());
                                    } catch (@NonNull JSONException | Http.HttpException | IOException | Http.ApplicationLayerException e) {
                                        fail(e.getMessage());
                                    }
                                    break;
                                default:
                                    fail("Unexpected HTTP request: " + path);
                                    break;
                            }
                            return http;

                        }
                    };
                }

                @NonNull
                @Override
                public HueBridge findNew(@NonNull List<HueBridge> ignore) throws MalformedURLException {
                    Log.d("PopupActivityTest", "Finding");
                    X509Certificate cert;
                    try {
                        cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                                new ByteArrayInputStream(
                                        ("-----BEGIN CERTIFICATE-----\n" +
                                                "MIICEjCCAXsCAg36MA0GCSqGSIb3DQEBBQUAMIGbMQswCQYDVQQGEwJKUDEOMAwG\n" +
                                                "A1UECBMFVG9reW8xEDAOBgNVBAcTB0NodW8ta3UxETAPBgNVBAoTCEZyYW5rNERE\n" +
                                                "MRgwFgYDVQQLEw9XZWJDZXJ0IFN1cHBvcnQxGDAWBgNVBAMTD0ZyYW5rNEREIFdl\n" +
                                                "YiBDQTEjMCEGCSqGSIb3DQEJARYUc3VwcG9ydEBmcmFuazRkZC5jb20wHhcNMTIw\n" +
                                                "ODIyMDUyNjU0WhcNMTcwODIxMDUyNjU0WjBKMQswCQYDVQQGEwJKUDEOMAwGA1UE\n" +
                                                "CAwFVG9reW8xETAPBgNVBAoMCEZyYW5rNEREMRgwFgYDVQQDDA93d3cuZXhhbXBs\n" +
                                                "ZS5jb20wXDANBgkqhkiG9w0BAQEFAANLADBIAkEAm/xmkHmEQrurE/0re/jeFRLl\n" +
                                                "8ZPjBop7uLHhnia7lQG/5zDtZIUC3RVpqDSwBuw/NTweGyuP+o8AG98HxqxTBwID\n" +
                                                "AQABMA0GCSqGSIb3DQEBBQUAA4GBABS2TLuBeTPmcaTaUW/LCB2NYOy8GMdzR1mx\n" +
                                                "8iBIu2H6/E2tiY3RIevV2OW61qY2/XRQg7YPxx3ffeUugX9F4J/iPnnu1zAxxyBy\n" +
                                                "2VguKv4SWjRFoRkIfIlHX0qVviMhSlNy2ioFLy7JcPZb+v3ftDGywUqcBiVDoea0\n" +
                                                "Hn+GmxZA\n" +
                                                "-----END CERTIFICATE-----").getBytes()
                                )
                        );
                        return new HueBridge("https://127.0.0.1", "fake", cert) {
                            @Override
                            public HueBridge pair() {
                                name = "TestAccount";
                                Log.d("PopupActivityTest", "Pairing");
                                return this;
                            }
                        };
                    } catch (CertificateException e) {
                        fail(e.getMessage());
                        return null;
                    }
                }
            });
        }
    };

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
    public void popupActivityTest() throws IOException, InterruptedException {
        onView(
                allOf(
                        withId(R.id.lightColor),
                        PopupActivityTest.childAtPosition(
                                PopupActivityTest.childAtPosition(
                                        withId(android.R.id.content),
                                        0
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Screenshot(activityRule.getActivity(), "popupActivity");
    }
}
