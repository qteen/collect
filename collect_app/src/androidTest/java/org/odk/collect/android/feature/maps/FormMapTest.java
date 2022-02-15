package org.odk.collect.android.feature.maps;

import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.odk.collect.geo.GeoUtils.formatLocationResultString;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.odk.collect.android.R;
import org.odk.collect.android.support.rules.CollectTestRule;
import org.odk.collect.android.support.rules.TestRuleChain;
import org.odk.collect.androidtest.RecordedIntentsRule;
import org.odk.collect.externalapp.ExternalAppUtils;

public class FormMapTest {

    private static final String SINGLE_GEOPOINT_FORM = "single-geopoint.xml";
    private static final String NO_GEOPOINT_FORM = "basic.xml";

    public final CollectTestRule rule = new CollectTestRule();

    @Rule
    public RuleChain copyFormChain = TestRuleChain.chain()
            .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
            .around(new RecordedIntentsRule())
            .around(rule);

    @Test
    public void gettingBlankFormList_showsMapIcon_onlyForFormsWithGeometry() {
        rule.startAtMainMenu()
                .copyForm(SINGLE_GEOPOINT_FORM)
                .copyForm(NO_GEOPOINT_FORM)
                .clickFillBlankForm()
                .checkMapIconDisplayedForForm("Single geopoint")
                .checkMapIconNotDisplayedForForm("basic");
    }

    @Test
    public void clickingOnMapIcon_opensMapForForm() {
        rule.startAtMainMenu()
                .copyForm(SINGLE_GEOPOINT_FORM)
                .copyForm(NO_GEOPOINT_FORM)
                .clickFillBlankForm()
                .clickOnMapIconForForm("Single geopoint")
                .assertText("Single geopoint");
    }

    @Test
    public void fillingBlankForm_addsInstanceToMap() {
        String oneInstanceString = ApplicationProvider.getApplicationContext().getResources().getString(R.string.geometry_status, 1, 1);

        stubGeopointIntent();

        rule.startAtMainMenu()
                .copyForm(SINGLE_GEOPOINT_FORM)
                .copyForm(NO_GEOPOINT_FORM)
                .clickFillBlankForm()
                .clickOnMapIconForForm("Single geopoint")
                .clickFillBlankFormButton("Single geopoint")
                .inputText("Foo")
                .swipeToNextQuestion("Location")
                .clickWidgetButton()
                .swipeToEndScreen()
                .clickSaveAndExitBackToMap()
                .assertText(oneInstanceString);
    }

    private void stubGeopointIntent() {
        Location location = new Location("gps");
        location.setLatitude(125.1);
        location.setLongitude(10.1);
        location.setAltitude(5);

        Intent intent = ExternalAppUtils.getReturnIntent(formatLocationResultString(location));
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, intent);

        intending(hasComponent("org.odk.collect.geo.GeoPointActivity"))
                .respondWith(result);
    }
}
