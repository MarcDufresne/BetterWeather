/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2013-2014 Marc-André Dufresne
 *
 * This file was modified by Marc-André Dufresne to include several
 * more features.
 */

package net.imatruck.betterweather.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.imatruck.betterweather.BetterWeatherExtension;
import net.imatruck.betterweather.R;
import net.imatruck.betterweather.utils.HelpUtils;

@SuppressWarnings("deprecation")
public class BetterWeatherSettingsActivity extends BaseSettingsActivity implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setIcon(R.drawable.climacons_sunny);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        updateShortcutPreferenceState(BetterWeatherExtension.PREF_WEATHER_REFRESH_ON_TOUCH);
        updateWeatherAPIKeyPrefState(getPreferenceScreen().getSharedPreferences(), BetterWeatherExtension.PREF_WEATHER_API);

    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void setupSimplePreferencesScreen() {
        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add preferences.
        addPreferencesFromResource(R.xml.pref_weather);

        // When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_UNITS));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_SPEED_UNITS));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_LOCATION));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_SHORTCUT));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_REFRESH_INTERVAL));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_ICON_THEME));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_API));
        bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_API_KEY));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        if (item.getItemId() == R.id.menu_settings_about) {
            HelpUtils.showAboutDialog(this);
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(BetterWeatherExtension.PREF_WEATHER_REFRESH_ON_TOUCH))
            updateShortcutPreferenceState(key);
        if (key.equals(BetterWeatherExtension.PREF_WEATHER_API) || key.equals(BetterWeatherExtension.PREF_WEATHER_API_KEY))
            updateWeatherAPIKeyPrefState(sharedPreferences, BetterWeatherExtension.PREF_WEATHER_API);
    }

    private void updateWeatherAPIKeyPrefState(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(BetterWeatherExtension.PREF_WEATHER_API_KEY);
        if (pref instanceof EditTextPreference) {
            EditTextPreference apiKeyPref = (EditTextPreference) pref;
            if (sharedPreferences.getString(key, "").equals(BetterWeatherExtension.FORECAST_WEATHER_API)) {
                String apiKey = sharedPreferences.getString(BetterWeatherExtension.PREF_WEATHER_API_KEY, "");

                apiKeyPref.setEnabled(true);
                if (apiKey.equals("")) {
                    apiKeyPref.setSummary(R.string.weather_api_key_help_text_empty);
                } else {
                    bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_API_KEY));
                }
            } else {
                apiKeyPref.setEnabled(false);
                apiKeyPref.setSummary(R.string.weather_api_key_help_text_not_needed);
            }
        }
    }

    private void updateShortcutPreferenceState(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof CheckBoxPreference) {
            CheckBoxPreference refreshOnTouchPref = (CheckBoxPreference) pref;

            Preference shortcutPref = findPreference(BetterWeatherExtension.PREF_WEATHER_SHORTCUT);

            if(shortcutPref == null) return;

            if (refreshOnTouchPref.isChecked()) {
                shortcutPref.setEnabled(false);
                shortcutPref.setSummary(R.string.shortcut_pref_help_text);
            } else {
                shortcutPref.setEnabled(true);
                bindPreferenceSummaryToValue(findPreference(BetterWeatherExtension.PREF_WEATHER_SHORTCUT));
            }
        }
    }
}
