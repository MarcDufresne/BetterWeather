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
 * Copyright 2013 Marc-André Dufresne
 *
 * This file was modified by Marc-André Dufresne to include several
 * more features.
 */

package net.imatruck.betterweather;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import net.imatruck.betterweather.YahooWeatherAPIClient.LocationInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.imatruck.betterweather.LogUtils.LOGD;
import static net.imatruck.betterweather.LogUtils.LOGE;
import static net.imatruck.betterweather.LogUtils.LOGW;

/**
 * A local weather and forecast extension.
 */
public class BetterWeatherExtension extends DashClockExtension {
    private static final String TAG = LogUtils.makeLogTag(BetterWeatherExtension.class);
    public static final String REFRESH_INTENT_FILTER = "net.imatruck.betterweather.action.RefreshWeather";
    public static Intent REFRESH_INTENT = new Intent(REFRESH_INTENT_FILTER);

    public static final String PREF_WEATHER_UNITS = "pref_weather_units";
    public static final String PREF_WEATHER_SPEED_UNITS = "pref_weather_speed_units";
    public static final String PREF_WEATHER_LOCATION = "pref_weather_location";
    public static final String PREF_WEATHER_USE_ONLY_NETWORK = "pref_weather_use_only_network";
    public static final String PREF_WEATHER_SHOW_TODAY_FORECAST = "pref_weather_show_today_forecast";
    public static final String PREF_WEATHER_SHOW_TOMORROW_FORECAST = "pref_weather_show_tomorrow_forecast";
    public static final String PREF_WEATHER_REFRESH_ON_TOUCH = "pref_weather_refresh_on_touch";
    public static final String PREF_WEATHER_SHORTCUT = "pref_weather_shortcut";
    public static final String PREF_WEATHER_REFRESH_INTERVAL = "pref_weather_refresh_interval";
    public static final String PREF_WEATHER_SHOW_REFRESH_TOAST = "pref_weather_show_refresh_toast";
    public static final String PREF_WEATHER_SHOW_HIGHLOW = "pref_weather_show_highlow";
    public static final String PREF_WEATHER_ICON_THEME = "pref_weather_icon_theme";
    public static final String PREF_WEATHER_HIDE_LOCATION_NAME = "pref_weather_hide_location_name";
    public static final String PREF_WEATHER_SHOW_WIND_DETAILS = "pref_weather_show_wind_details";
    public static final String PREF_WEATHER_SHOW_WIND_CHILL = "pref_weather_show_wind_chill";
    public static final String PREF_WEATHER_SHOW_HUMIDITY = "pref_weather_show_humidity";
    public static final String PREF_WEATHER_INVERT_HIGHLOW = "pref_weather_invert_highlow";
    public static final String PREF_PEBBLE_ENABLE = "pref_pebble_enable";

    public static final Uri DEFAULT_WEATHER_INTENT_URI = Uri.parse("http://www.google.com/search?q=weather");
    public static final Intent DEFAULT_WEATHER_INTENT = new Intent(Intent.ACTION_VIEW, DEFAULT_WEATHER_INTENT_URI);

    private static final String CLIMACONS_ICON_THEME = "climacons";
    private static final String WEATHERCONS_ICON_THEME = "weathercons";
    private static final String CHAMELEON_ICON_THEME = "chameleon";
    private static final String GOOGLENOW_ICON_THEME = "googlenow";

    private static final long STALE_LOCATION_NANOS = 10l * 60000000000l; // 10 minutes

    private static final Criteria sLocationCriteria;

    private static String sWeatherUnits = "f";
    private static int sSpeedUnits = 0;
    private static String sSetLocation = "";
    private static boolean sUseCurrentLocation = false;
    private static boolean sShowTodayForecast = false;
    private static boolean sShowTomorrowForecast = false;
    private static boolean sRefreshOnTouch = false;
    private static Intent sWeatherIntent = DEFAULT_WEATHER_INTENT;
    private static int sRefreshInterval = 60;
    private static boolean sShowHighlow = false;
    private static boolean sHideLocationName = false;
    private static boolean sShowWindDetails = false;
    private static boolean sShowWindChill = false;
    private static boolean sShowHumidity = false;
    private static boolean sUseOnlyNetworkLocation = false;
    private static boolean sInvertHighLowTemps = false;
    private static boolean sEnablePebble = false;

    public static long lastUpdateTime;

    public static final int UPDATE_REASON_INTERVAL_TOO_BIG = 35483874;
    public static final int UPDATE_REASON_USER_REQUESTED = 826452;

    private OnClickReceiver onClickReceiver;

    private boolean mOneTimeLocationListenerActive = false;

    public static enum ErrorCodes {
        NONE, UNKNOWN, LOCATION, INTERNET
    }

    static {
        sLocationCriteria = new Criteria();
        sLocationCriteria.setPowerRequirement(Criteria.POWER_LOW);
        sLocationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        sLocationCriteria.setCostAllowed(false);
    }


    /*
     * Overridden class functions used to register handlers
     */
    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        if (onClickReceiver != null) {
            try {
                unregisterReceiver(onClickReceiver);
            } catch (Exception e) {
                LOGE(TAG, "Receiver already unregistred");
            }
        }

        IntentFilter intentFilter = new IntentFilter(REFRESH_INTENT_FILTER);
        onClickReceiver = new OnClickReceiver();
        registerReceiver(onClickReceiver, intentFilter);

        scheduleRefresh(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (onClickReceiver != null) {
            try {
                unregisterReceiver(onClickReceiver);
            } catch (Exception e) {
                LOGE(TAG, "Receiver already unregistred");
            }
        }
        disableOneTimeLocationListener();
    }


    /*
     * Main data flow starts here
     * Functions used to update data
     */
    @Override
    protected void onUpdateData(int reason) {

        LOGD(TAG, "Update reason: " + getReasonText(reason));

        if (reason != UPDATE_REASON_USER_REQUESTED &&
                reason != UPDATE_REASON_SETTINGS_CHANGED &&
                reason != UPDATE_REASON_INITIAL &&
                reason != UPDATE_REASON_INTERVAL_TOO_BIG) {
            LOGD(TAG, "Skipping update");
            if ((System.currentTimeMillis() - lastUpdateTime > (sRefreshInterval * 1000 * 60)) && sRefreshInterval > 0)
                onUpdateData(UPDATE_REASON_INTERVAL_TOO_BIG);

            return;
        }

        LOGD(TAG, "Updating data");

        if(sEnablePebble) {
            LOGD(TAG, "Registered Pebble Data Receiver");
            Pebble.registerPebbleDataReceived(getApplicationContext());
        }

        getCurrentPreferences();

        NetworkInfo ni = ((ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            LOGD(TAG, "No internet connection detected, scheduling refresh in 5 minutes");
            scheduleRefresh(5);
            return;
        }

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        String provider;
        if (sUseOnlyNetworkLocation)
            provider = LocationManager.NETWORK_PROVIDER;
        else
            provider = lm.getBestProvider(sLocationCriteria, true);

        if (TextUtils.isEmpty(provider)) {
            LOGE(TAG, "No available location providers matching criteria, scheduling refresh in 5 minutes.");
            publishUpdate(null, ErrorCodes.LOCATION);
            scheduleRefresh(5);
            return;
        }

        requestLocationUpdate(lm, provider);
    }

    private void requestLocationUpdate(final LocationManager lm, final String provider) {
        if (sUseCurrentLocation) {
            final Location lastLocation = lm.getLastKnownLocation(provider);
            if (lastLocation == null ||
                    (SystemClock.elapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos())
                            >= STALE_LOCATION_NANOS) {
                LOGW(TAG, "Stale or missing last-known location; requesting single coarse location "
                        + "update. " + ((lastLocation != null) ? lastLocation.getLatitude() + ", " + lastLocation.getLongitude() : "Last location is null"));
                try {
                    disableOneTimeLocationListener();
                    mOneTimeLocationListenerActive = true;
                    lm.requestSingleUpdate(provider, mOneTimeLocationListener, null);
                    LOGD(TAG, "Requested single location update");
                    if (lastLocation != null) {
                        new RefreshWeatherTask(lastLocation).execute();
                    }
                } catch (Exception e) {
                    LOGW(TAG, "RuntimeException on requestSingleUpdate. " + e.toString());
                    scheduleRefresh(2);
                }
            } else {
                new RefreshWeatherTask(lastLocation).execute();
            }
        } else {
            LOGD(TAG, "Using set location");
            disableOneTimeLocationListener();
            Location dummyLocation = new Location(provider);
            new RefreshWeatherTask(dummyLocation).execute();
        }
    }

    private static BetterWeatherData getWeatherForLocation(Location location)
            throws InvalidLocationException, IOException {

        if (BuildConfig.DEBUG) {
            LOGD(TAG, "Using location: " + location.getLatitude()
                    + "," + location.getLongitude() + " to get weather");
        }

        LocationInfo locationInfo;
        if (!sUseCurrentLocation) {
            locationInfo = new LocationInfo();
            locationInfo.woeid = getSetLocationWoeid();
        } else {
            locationInfo = YahooWeatherAPIClient.getLocationInfo(location);
        }

        LOGD(TAG, "Using WOEID: " + locationInfo.woeid);

        return YahooWeatherAPIClient.getWeatherDataForLocation(locationInfo);
    }

    private void publishUpdate(BetterWeatherData weatherData, ErrorCodes errorCode) {
        publishUpdate(renderExtensionData(weatherData, errorCode));

        if(sEnablePebble) {
            Pebble.sendWeather(getApplicationContext(), weatherData);
        }

        LOGD(TAG, "Published new data to extension");
        lastUpdateTime = System.currentTimeMillis();
        scheduleRefresh(0);
    }

    /*
     * Functions used to refresh the data, restarts the main flow
     */
    class OnClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            showRefreshToast();
            onUpdateData(UPDATE_REASON_USER_REQUESTED);
        }
    }

    private void scheduleRefresh(int intervalOverride) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sRefreshInterval = Integer.parseInt(sp.getString(PREF_WEATHER_REFRESH_INTERVAL, "60"));
        if (sRefreshInterval < 0) {
            WeatherRefreshReceiver.cancelPendingIntent(this);
            return;
        }

        int realRefreshInterval = sRefreshInterval;
        if (intervalOverride > 0)
            realRefreshInterval = intervalOverride;

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + (realRefreshInterval * 60 * 1000), WeatherRefreshReceiver.getPendingIntent(this));
        LOGD(TAG, "Scheduled refresh in " + realRefreshInterval + " minutes.");
    }

    private class RefreshWeatherTask extends AsyncTask<Void, Void, BetterWeatherData> {

        Location mLocation = null;

        public RefreshWeatherTask(Location location) {
            mLocation = location;
        }

        @Override
        protected BetterWeatherData doInBackground(Void... params) {
            LOGD(TAG, "Refreshing weather from RefreshWeatherTask");
            BetterWeatherData weatherData = null;
            try {
                weatherData = getWeatherForLocation(mLocation);
                LOGD(TAG, "Using new weather data for location: " + weatherData.location + " at " + SimpleDateFormat.getTimeInstance().format(new Date()));
            } catch (InvalidLocationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return weatherData;
        }

        @Override
        protected void onPostExecute(BetterWeatherData betterWeatherData) {
            if (betterWeatherData != null)
                publishUpdate(betterWeatherData, ErrorCodes.NONE);
        }
    }

    private void showRefreshToast() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showRefreshToast = sp.getBoolean(PREF_WEATHER_SHOW_REFRESH_TOAST, false);
        if (showRefreshToast)
            Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
    }

    /*
     * Helper function used to determine the reason of an update
     */
    private static String getReasonText(int reason) {

        switch (reason) {
            case UPDATE_REASON_INTERVAL_TOO_BIG:
                return "It's been a long time since last successful update";
            case UPDATE_REASON_SETTINGS_CHANGED:
                return "Settings changed";
            case UPDATE_REASON_USER_REQUESTED:
                return "User requested";
            case UPDATE_REASON_CONTENT_CHANGED:
                return "Content changed";
            case UPDATE_REASON_INITIAL:
                return "Initial";
            case UPDATE_REASON_PERIODIC:
                return "Periodic";
            case UPDATE_REASON_SCREEN_ON:
                return "Screen on";
            case UPDATE_REASON_UNKNOWN:
                return "Unknown";
        }
        return "Unknown reason :/";
    }

    /*
     * Methods used to get current app settings
     */
    private void getCurrentPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sWeatherUnits = sp.getString(PREF_WEATHER_UNITS, sWeatherUnits);
        sSpeedUnits = Integer.parseInt(sp.getString(PREF_WEATHER_SPEED_UNITS, Integer.toString(sSpeedUnits)));
        sSetLocation = sp.getString(PREF_WEATHER_LOCATION, sSetLocation).trim().toLowerCase(Locale.getDefault());
        sUseCurrentLocation = TextUtils.isEmpty(sSetLocation);
        sUseOnlyNetworkLocation = sp.getBoolean(PREF_WEATHER_USE_ONLY_NETWORK, sUseOnlyNetworkLocation);
        sShowTodayForecast = sp.getBoolean(PREF_WEATHER_SHOW_TODAY_FORECAST, sShowTodayForecast);
        sShowTomorrowForecast = sp.getBoolean(PREF_WEATHER_SHOW_TOMORROW_FORECAST, sShowTomorrowForecast);
        sRefreshOnTouch = sp.getBoolean(PREF_WEATHER_REFRESH_ON_TOUCH, sRefreshOnTouch);
        sWeatherIntent = AppChooserPreference.getIntentValue(sp.getString(PREF_WEATHER_SHORTCUT, null), DEFAULT_WEATHER_INTENT);
        sRefreshInterval = Integer.parseInt(sp.getString(PREF_WEATHER_REFRESH_INTERVAL, "60"));
        sHideLocationName = sp.getBoolean(PREF_WEATHER_HIDE_LOCATION_NAME, sHideLocationName);
        sShowHumidity = sp.getBoolean(PREF_WEATHER_SHOW_HUMIDITY, sShowHumidity);
        sShowWindChill = sp.getBoolean(PREF_WEATHER_SHOW_WIND_CHILL, sShowWindChill);
        sShowWindDetails = sp.getBoolean(PREF_WEATHER_SHOW_WIND_DETAILS, sShowWindDetails);
        sInvertHighLowTemps = sp.getBoolean(PREF_WEATHER_INVERT_HIGHLOW, sInvertHighLowTemps);
        sEnablePebble = sp.getBoolean(PREF_PEBBLE_ENABLE, sEnablePebble);

        LOGD(TAG, "Location from settings is: " + ((sUseCurrentLocation) ? "Automatic" : sSetLocation));
    }

    public static String getSetLocationWoeid() {
        try {
            return sSetLocation.substring(0, sSetLocation.indexOf(","));
        } catch (Exception e) {
            return sSetLocation;
        }
    }

    public static String getWeatherUnits() {
        return sWeatherUnits;
    }

    /*
     * Functions used to render the extension on the main widget
     */
    private ExtensionData renderExtensionData(BetterWeatherData weatherData, ErrorCodes errorCode) {

        if (errorCode != ErrorCodes.NONE) {

            int[] errorStrings = BetterWeatherData.getErrorTitle(errorCode);

            ExtensionData extensionData = new ExtensionData()
                    .visible(true)
                    .status(getString(R.string.error_status))
                    .expandedTitle(getString(errorStrings[0]))
                    .expandedBody(getString(errorStrings[1]))
                    .icon(getConditionIconId(-1))
                    .clickIntent(prepareClickIntent());

            LOGD(TAG, "Created error data, " + extensionData.expandedTitle());

            return extensionData;
        } else {

            String temperature = weatherData.hasValidTemperature()
                    ? getString(R.string.temperature_template, weatherData.temperature)
                    : getString(R.string.status_none);


            String status = formatStatusText(weatherData, temperature);

            int conditionIconId = getConditionIconId(weatherData.conditionCode);

            String conditionText = getString(BetterWeatherData.getStatusTextId(weatherData.conditionCode));

            StringBuilder expandedBody = formatExpendedBody(weatherData);

            Intent clickIntent = prepareClickIntent();

            ExtensionData extData = new ExtensionData()
                    .visible(true)
                    .status(status)
                    .expandedTitle(getString(R.string.weather_expanded_title_template,
                            temperature + sWeatherUnits.toUpperCase(Locale.getDefault()),
                            conditionText))
                    .icon(conditionIconId)
                    .expandedBody(expandedBody.toString());

            LOGD(TAG, "Created ExtensionData, " + extData.expandedTitle());

            return extData.clickIntent(clickIntent);
        }
    }

    private Intent prepareClickIntent() {
        Intent clickIntent = sWeatherIntent;

        if (sRefreshOnTouch) {
            clickIntent = REFRESH_INTENT;
        }
        //Shortcut is set to Default
        else if (clickIntent.getData() != null && clickIntent.getData().toString().contains(DEFAULT_WEATHER_INTENT_URI.toString())) {
            URI intentURI = null;

            try {
                intentURI = new URI("http", "www.google.com", "/search", "q=weather", null);
            } catch (URISyntaxException e) {
                LOGE(TAG, "Malformed URI exception");
            }

            if (!sUseCurrentLocation) {
                try {
                    intentURI = new URI("http", "www.google.com", "/search", "q=weather " + WeatherLocationPreference.getDisplayValue(this, sSetLocation), null);

                } catch (URISyntaxException e) {
                    LOGE(TAG, "Malformed URI exception");
                }
            }

            if (BuildConfig.DEBUG) {
                assert intentURI != null;
                LOGD(TAG, "Action URI: " + intentURI.toASCIIString());
            }

            assert intentURI != null;
            clickIntent.setData(Uri.parse(intentURI.toASCIIString()));
            LOGD(TAG, "Intent URI: " + clickIntent.getData());
        }
        return clickIntent;
    }

    private StringBuilder formatExpendedBody(BetterWeatherData weatherData) {
        StringBuilder expandedBody = new StringBuilder();

        if (sShowWindChill && weatherData.windChill != weatherData.temperature && weatherData.windChill != -1) {
            expandedBody.append(getString(R.string.wind_chill_template, weatherData.windChill));
            expandedBody.append("\n");
        }

        if (sShowHumidity || sShowWindDetails) {
            StringBuilder detailsLine = new StringBuilder();
            if (sShowWindDetails && !"".equals(weatherData.windSpeed)) {
                detailsLine.append(getString(R.string.wind_details_template,
                        getString(BetterWeatherData.getWindDirectionText(weatherData.windDirection)),
                        BetterWeatherData.convertSpeedUnits(sWeatherUnits, weatherData.windSpeed, sSpeedUnits),
                        getSpeedUnitDisplayValue(sSpeedUnits)));
                if (sShowHumidity)
                    detailsLine.append(", ");
            }
            if (sShowHumidity)
                detailsLine.append(getString(R.string.humidity_template, weatherData.humidity)).append("%");

            expandedBody.append(detailsLine.toString());
        }

        if (sShowTodayForecast) {
            if (sShowHumidity || sShowWindDetails) expandedBody.append("\n");
            int todayForecastTextId = BetterWeatherData.getStatusTextId(weatherData.todayForecastConditionCode);
            String todayForecastText = getString(todayForecastTextId);
            expandedBody.append((sInvertHighLowTemps) ?
                    getString(R.string.today_forecast_template, todayForecastText, weatherData.todayHigh, weatherData.todayLow) :
                    getString(R.string.today_forecast_template, todayForecastText, weatherData.todayLow, weatherData.todayHigh));
        }

        if (sShowTomorrowForecast) {
            if (sShowTodayForecast || sShowHumidity || sShowWindDetails) expandedBody.append("\n");
            int tomorrowForecastTextId = BetterWeatherData.getStatusTextId(weatherData.conditionCode);
            String tomorrowForecastText = getString(tomorrowForecastTextId);
            expandedBody.append((sInvertHighLowTemps) ?
                    getString(R.string.tomorrow_forecast_template, tomorrowForecastText, weatherData.tomorrowHigh, weatherData.tomorrowLow) :
                    getString(R.string.tomorrow_forecast_template, tomorrowForecastText, weatherData.tomorrowLow, weatherData.tomorrowHigh));
        }

        if (!sHideLocationName) {
            if (sShowHumidity || sShowTodayForecast || sShowTomorrowForecast || sShowWindDetails)
                expandedBody.append("\n");
            expandedBody.append(weatherData.location);
        }

        return expandedBody;
    }

    private String formatStatusText(BetterWeatherData weatherData, String temperature) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sShowHighlow = sp.getBoolean(PREF_WEATHER_SHOW_HIGHLOW, sShowHighlow);

        String status = temperature;

        if (sShowHighlow) {
            status += "\n" + ((!sInvertHighLowTemps) ?
                    getString(R.string.highlow_template, weatherData.todayLow, weatherData.todayHigh) :
                    getString(R.string.highlow_template, weatherData.todayHigh, weatherData.todayLow));
        }
        return status;
    }

    private int getConditionIconId(int conditionCode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String sIconTheme = sp.getString(PREF_WEATHER_ICON_THEME, CLIMACONS_ICON_THEME);

        if (sIconTheme.equals(CLIMACONS_ICON_THEME)) {
            return BetterWeatherData.getClimaconsConditionIconId(conditionCode);
        } else if (sIconTheme.equals(WEATHERCONS_ICON_THEME)) {
            return BetterWeatherData.getWeatherconsConditionIconId(conditionCode);
        } else if (sIconTheme.equals(CHAMELEON_ICON_THEME)) {
            return BetterWeatherData.getChameleonConditionIconId(conditionCode);
        } else if (sIconTheme.equals(GOOGLENOW_ICON_THEME)) {
            return BetterWeatherData.getGoogleNowConditionIconId(conditionCode);
        } else {
            LOGD(TAG, "Using default theme");
            return BetterWeatherData.getClimaconsConditionIconId(conditionCode);
        }
    }

    private String getSpeedUnitDisplayValue(int speedUnitIndex) {
        String[] units = getResources().getStringArray(R.array.pref_weather_speed_units_display_names);
        if (speedUnitIndex >= 0 && speedUnitIndex < units.length)
            return units[speedUnitIndex];
        return units[0];
    }

    /*
     * Methods used for Location updates and management
     */
    private void disableOneTimeLocationListener() {
        if (mOneTimeLocationListenerActive) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            lm.removeUpdates(mOneTimeLocationListener);
            mOneTimeLocationListenerActive = false;
        }
    }

    private LocationListener mOneTimeLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LOGD(TAG, "Location changed, new location : " + location.getLatitude() + ", " + location.getLongitude());
            if (sUseCurrentLocation)
                new RefreshWeatherTask(location).execute();
            disableOneTimeLocationListener();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
            LOGD(TAG, "Provider disabled");
            publishUpdate(null, ErrorCodes.LOCATION);
        }
    };


}
