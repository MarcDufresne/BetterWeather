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
 * Copyright 2013-2016 Marc-André Dufresne
 *
 * This file was modified by Marc-André Dufresne to include several
 * more features.
 */

package net.imatruck.betterweather.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.math.DoubleMath;

import net.imatruck.betterweather.BetterWeatherExtension;
import net.imatruck.betterweather.R;
import net.imatruck.betterweather.utils.HelpUtils;
import net.imatruck.betterweather.utils.LogUtils;

import static net.imatruck.betterweather.utils.LogUtils.LOGD;
import static net.imatruck.betterweather.utils.LogUtils.LOGW;

@SuppressWarnings("deprecation")
public class BetterWeatherSettingsActivity extends BaseSettingsActivity implements OnSharedPreferenceChangeListener {

    private static String TAG = LogUtils.makeLogTag(BetterWeatherSettingsActivity.class);
    private static int PLACE_AUTOCOMPLETE_REQ_CODE = 1;
    private static int LOCATION_REQUEST_CODE = 3;

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
        updateLocationPrefState(BetterWeatherExtension.PREF_WEATHER_AUTOMATIC_LOCATION);

        Preference locationPreference = findPreference(BetterWeatherExtension.PREF_WEATHER_LOCATION);
        locationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent autocompleteIntent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(BetterWeatherSettingsActivity.this);
                    startActivityForResult(autocompleteIntent, PLACE_AUTOCOMPLETE_REQ_CODE);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    LOGW(TAG, "Could not start place autocomplete activity");
                    return false;
                }
                return true;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                LOGD(TAG, "Got '" + place.getName() + "' from autocomplete");
                LatLng latlng = place.getLatLng();
                String prefValue = "0/" + place.getName() + "/" + latlng.latitude + "/" + latlng.longitude;

                WeatherLocationPreference locationPreference = (WeatherLocationPreference) findPreference(BetterWeatherExtension.PREF_WEATHER_LOCATION);
                locationPreference.setValue(prefValue);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                LOGD(TAG, "Autocomplete returned error status: " + status.getStatusMessage());
            } else {
                LOGD(TAG, "User cancelled operation");
            }
        }
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
        else if (key.equals(BetterWeatherExtension.PREF_WEATHER_AUTOMATIC_LOCATION))
            updateLocationPrefState(key);
    }

    private void updateLocationPrefState(String key) {
        SwitchPreference autoLocPref = (SwitchPreference) findPreference(key);
        WeatherLocationPreference locationPref = (WeatherLocationPreference) findPreference(BetterWeatherExtension.PREF_WEATHER_LOCATION);
        CheckBoxPreference hideNamePref = (CheckBoxPreference) findPreference(BetterWeatherExtension.PREF_WEATHER_HIDE_LOCATION_NAME);

        if (autoLocPref.isChecked()) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }

            locationPref.setEnabled(false);
            locationPref.setValue("");
            hideNamePref.setEnabled(false);
            hideNamePref.setChecked(true);
        } else {
            locationPref.setEnabled(true);
            locationPref.setValue(getString(R.string.pref_weather_location_default));
            hideNamePref.setEnabled(true);
            hideNamePref.setChecked(false);
        }

    }

    private void updateShortcutPreferenceState(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof CheckBoxPreference) {
            CheckBoxPreference refreshOnTouchPref = (CheckBoxPreference) pref;

            Preference shortcutPref = findPreference(BetterWeatherExtension.PREF_WEATHER_SHORTCUT);

            if (shortcutPref == null) return;

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
