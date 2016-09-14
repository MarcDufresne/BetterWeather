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

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import net.imatruck.betterweather.R;
import net.imatruck.betterweather.utils.LogUtils;

/**
 * A preference that allows the user to choose a location, using the Yahoo! GeoPlanet API.
 */
public class WeatherLocationPreference extends Preference {

    private static final String TAG = LogUtils.makeLogTag(WeatherLocationPreference.class);

    public WeatherLocationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeatherLocationPreference(Context context) {
        super(context);
    }

    public WeatherLocationPreference(Context context, AttributeSet attrs,
                                     int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setValue(String value) {
        if (value == null) {
            value = "";
        }

        if (callChangeListener(value)) {
            persistString(value);
            notifyChanged();
        }
    }

    public static CharSequence getDisplayValue(Context context, String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            return context.getString(R.string.pref_weather_location_automatic);
        }
        String[] locationDetails = value.split("/");
        return locationDetails[1];
    }

    public static String getWoeidFromValue(String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            if (value.matches("\\d+,[0-9a-zA-Z,. ]*"))
                return value.substring(0, value.indexOf(","));
            return null;
        }

        String[] locationDetails = value.split("/");
        return locationDetails[0];
    }

    /**
     * This method is for getting location name.
     *
     * @param value pref location string.
     * @return location name saved in setting.
     */
    public static String getDisplayNameFromValue(String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            if (value.matches("\\d+,[0-9a-zA-Z,. ]*"))
                return value.substring(value.indexOf(",") + 1).trim();
            return null;
        }

        String[] locationDetails = value.split("/");
        return locationDetails[1];
    }

    public static String getLatFromValue(String value) {
        // if value is a form like "11111/SEOUL, KOREA/12.3333/45.6666", in which
        // SEOUL, KOREA are not ASCIIs but Korean characters, then
        // if(!value.matches("\\d+/[0-9a-zA-Z,. ]*/\\d+[,.]?\\d*/\\d+[,.]?\\d*"))
        // fails to match lat/lon, so changed loosely.
        if (!value.matches("\\d+/[^/]*/-?\\d+[,.]?\\d*/-?\\d+[,.]?\\d*"))
            return "0";

        String[] locationDetails = value.split("/");
        return locationDetails[2];
    }

    public static String getLngFromValue(String value) {
        if (!value.matches("\\d+/[^/]*/-?\\d+[,.]?\\d*/-?\\d+[,.]?\\d*"))
            return "0";

        String[] locationDetails = value.split("/");
        return locationDetails[3];
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString("") : (String) defaultValue);
    }
}
