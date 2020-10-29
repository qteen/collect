package org.odk.collect.android.preferences;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.odk.collect.android.preferences.PreferencesActivity.INTENT_KEY_ADMIN_MODE;

@RunWith(AndroidJUnit4.class)
public class GeneralPreferencesFragmentTest {
    private final AdminSharedPreferences adminSharedPreferences = AdminSharedPreferences.getInstance();

    @Test
    public void whenServerPreferenceEnabled_shouldBeVisibleWhenOpenedFromGeneralPreferences() {
        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(GeneralKeys.KEY_PROTOCOL).isVisible(), equalTo(true)));
    }

    @Test
    public void whenServerPreferenceEnabled_shouldBeVisibleWhenOpenedFromAdminPreferences() {
        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(GeneralKeys.KEY_PROTOCOL).isVisible(), equalTo(true)));
    }

    @Test
    public void whenServerPreferenceDisabled_shouldBeHiddenWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_SERVER, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(GeneralKeys.KEY_PROTOCOL), nullValue()));
    }

    @Test
    public void whenServerPreferenceDisabled_shouldBeVisibleWhenOpenedFromAdminPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_SERVER, false);

        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(GeneralKeys.KEY_PROTOCOL).isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllUserInterfacePreferencesDisabled_shouldPreferenceBeHiddenWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_APP_THEME, false);
        adminSharedPreferences.save(AdminKeys.KEY_APP_LANGUAGE, false);
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FONT_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_NAVIGATION, false);
        adminSharedPreferences.save(AdminKeys.KEY_SHOW_SPLASH_SCREEN, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_interface"),  nullValue()));
    }

    @Test
    public void whenAtLeastOneUserInterfacePreferenceIsEnabled_shouldPreferenceBeVisibleWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_APP_THEME, false);
        adminSharedPreferences.save(AdminKeys.KEY_APP_LANGUAGE, false);
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FONT_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_NAVIGATION, true);
        adminSharedPreferences.save(AdminKeys.KEY_SHOW_SPLASH_SCREEN, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_interface").isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllUserInterfacePreferencesDisabled_shouldPreferenceBeVisibleWhenOpenedFromAdminPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_APP_THEME, false);
        adminSharedPreferences.save(AdminKeys.KEY_APP_LANGUAGE, false);
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FONT_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_NAVIGATION, false);
        adminSharedPreferences.save(AdminKeys.KEY_SHOW_SPLASH_SCREEN, false);

        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_interface").isVisible(), equalTo(true)));
    }

    @Test
    public void whenMapsPreferenceEnabled_shouldBeVisibleWhenOpenedFromGeneralPreferences() {
        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(AdminKeys.KEY_MAPS).isVisible(), equalTo(true)));
    }

    @Test
    public void whenMapsPreferenceEnabled_shouldBeVisibleWhenOpenedFromAdminPreferences() {
        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(AdminKeys.KEY_MAPS).isVisible(), equalTo(true)));
    }

    @Test
    public void whenMapsPreferenceDisabled_shouldBeHiddenWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_MAPS, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(AdminKeys.KEY_MAPS), nullValue()));
    }

    @Test
    public void whenMapsPreferenceDisabled_shouldBeVisibleWhenOpenedFromAdminPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_MAPS, false);

        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference(AdminKeys.KEY_MAPS).isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllFormManagementPreferencesDisabled_shouldPreferenceBeHiddenWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_PERIODIC_FORM_UPDATES_CHECK, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOMATIC_UPDATE, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIDE_OLD_FORM_VERSIONS, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOSEND, false);
        adminSharedPreferences.save(AdminKeys.KEY_DELETE_AFTER_SEND, false);
        adminSharedPreferences.save(AdminKeys.KEY_DEFAULT_TO_FINALIZED, false);
        adminSharedPreferences.save(AdminKeys.KEY_CONSTRAINT_BEHAVIOR, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIGH_RESOLUTION, false);
        adminSharedPreferences.save(AdminKeys.KEY_IMAGE_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_GUIDANCE_HINT, false);
        adminSharedPreferences.save(AdminKeys.KEY_INSTANCE_FORM_SYNC, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("form_management"),  nullValue()));
    }

    @Test
    public void whenAtLeastOneFormManagementPreferenceIsEnabled_shouldPreferenceBeVisibleWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_PERIODIC_FORM_UPDATES_CHECK, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOMATIC_UPDATE, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIDE_OLD_FORM_VERSIONS, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOSEND, false);
        adminSharedPreferences.save(AdminKeys.KEY_DELETE_AFTER_SEND, true);
        adminSharedPreferences.save(AdminKeys.KEY_DEFAULT_TO_FINALIZED, false);
        adminSharedPreferences.save(AdminKeys.KEY_CONSTRAINT_BEHAVIOR, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIGH_RESOLUTION, false);
        adminSharedPreferences.save(AdminKeys.KEY_IMAGE_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_GUIDANCE_HINT, false);
        adminSharedPreferences.save(AdminKeys.KEY_INSTANCE_FORM_SYNC, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("form_management").isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllFormManagementPreferencesDisabled_shouldPreferenceBeVisibleWhenOpenedFromAdminPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_PERIODIC_FORM_UPDATES_CHECK, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOMATIC_UPDATE, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIDE_OLD_FORM_VERSIONS, false);
        adminSharedPreferences.save(AdminKeys.KEY_AUTOSEND, false);
        adminSharedPreferences.save(AdminKeys.KEY_DELETE_AFTER_SEND, false);
        adminSharedPreferences.save(AdminKeys.KEY_DEFAULT_TO_FINALIZED, false);
        adminSharedPreferences.save(AdminKeys.KEY_CONSTRAINT_BEHAVIOR, false);
        adminSharedPreferences.save(AdminKeys.KEY_HIGH_RESOLUTION, false);
        adminSharedPreferences.save(AdminKeys.KEY_IMAGE_SIZE, false);
        adminSharedPreferences.save(AdminKeys.KEY_GUIDANCE_HINT, false);
        adminSharedPreferences.save(AdminKeys.KEY_INSTANCE_FORM_SYNC, false);

        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("form_management").isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllIdentityPreferencesDisabled_shouldPreferenceBeHiddenWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FORM_METADATA, false);
        adminSharedPreferences.save(AdminKeys.KEY_ANALYTICS, false);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_and_device_identity"),  nullValue()));
    }

    @Test
    public void whenAtLeastOneIdentityPreferenceIsEnabled_shouldPreferenceBeVisibleWhenOpenedFromGeneralPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FORM_METADATA, false);
        adminSharedPreferences.save(AdminKeys.KEY_ANALYTICS, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_and_device_identity").isVisible(), equalTo(true)));
    }

    @Test
    public void whenAllIdentityPreferencesDisabled_shouldPreferenceBeVisibleWhenOpenedFromAdminPreferences() {
        adminSharedPreferences.save(AdminKeys.KEY_CHANGE_FORM_METADATA, false);
        adminSharedPreferences.save(AdminKeys.KEY_ANALYTICS, false);

        Bundle args = new Bundle();
        args.putBoolean(INTENT_KEY_ADMIN_MODE, true);

        FragmentScenario<GeneralPreferencesFragment> scenario = FragmentScenario.launch(GeneralPreferencesFragment.class, args);
        scenario.onFragment(fragment -> assertThat(fragment.findPreference("user_and_device_identity").isVisible(), equalTo(true)));
    }
}