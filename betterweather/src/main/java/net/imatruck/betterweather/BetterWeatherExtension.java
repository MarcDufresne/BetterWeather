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
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import net.imatruck.betterweather.iconthemes.IconThemeFactory;
import net.imatruck.betterweather.settings.AppChooserPreference;
import net.imatruck.betterweather.settings.WeatherLocationPreference;
import net.imatruck.betterweather.utils.LogUtils;
import net.imatruck.betterweather.weatherapi.IWeatherAPI;
import net.imatruck.betterweather.weatherapi.WeatherAPIFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.imatruck.betterweather.utils.LogUtils.LOGD;
import static net.imatruck.betterweather.utils.LogUtils.LOGE;
import static net.imatruck.betterweather.utils.LogUtils.LOGW;

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
    public static final String PREF_WEATHER_SHOW_WIND_SPEED_AS_LABEL = "pref_weather_show_wind_speed_as_label";
    public static final String PREF_WEATHER_SHOW_FEELS_LIKE = "pref_weather_show_feels_like";
    public static final String PREF_WEATHER_SHOW_HUMIDITY = "pref_weather_show_humidity";
    public static final String PREF_WEATHER_INVERT_HIGHLOW = "pref_weather_invert_highlow";
    public static final String PREF_PEBBLE_ENABLE = "pref_pebble_enable";
    public static final String PREF_PEBBLE_SHOW_FEELS_LIKE = "pref_pebble_show_feels_like";
    public static final String PREF_WEATHER_API = "pref_weather_api";
    public static final String PREF_WEATHER_API_KEY = "pref_weather_api_key";

    public static final Uri DEFAULT_WEATHER_INTENT_URI = Uri.parse("http://www.google.com/search?q=weather");
    public static final Intent DEFAULT_WEATHER_INTENT = new Intent(Intent.ACTION_VIEW, DEFAULT_WEATHER_INTENT_URI);

    public static final String CLIMACONS_ICON_THEME = "climacons";
    public static final String WEATHERCONS_ICON_THEME = "weathercons";
    public static final String CHAMELEON_ICON_THEME = "chameleon";
    public static final String GOOGLENOW_ICON_THEME = "googlenow";

    public static final String YAHOO_WEATHER_API = "yahoo_weather_api";
    public static final String OPENWEATHERMAP_WEATHER_API = "openweathermap_weather_api";

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
    private static boolean sShowWindLabel = false;
    private static boolean sShowFeelsLike = false;
    private static boolean sShowHumidity = false;
    private static boolean sUseOnlyNetworkLocation = false;
    private static boolean sInvertHighLowTemps = false;
    private static boolean sPebbleEnable = false;
    private static boolean sPebbleShowFeelsLike = true;
    private static String sWeatherAPI = YAHOO_WEATHER_API;
    private static String sWeatherAPIKey = "";

    public static long lastUpdateTime;

    public static final int UPDATE_REASON_INTERVAL_TOO_BIG = 35483874;
    public static final int UPDATE_REASON_USER_REQUESTED = 826452;

    private OnClickReceiver onClickReceiver;

    private boolean mOneTimeLocationListenerActive = false;

    static Handler gpsFixHandler = new Handler();

    static {
        sLocationCriteria = new Criteria();
        sLocationCriteria.setPowerRequirement(Criteria.POWER_LOW);
        sLocationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        sLocationCriteria.setCostAllowed(false);
    }


    /**
     * Registers the {@link net.imatruck.betterweather.BetterWeatherExtension.OnClickReceiver} handler
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

    /**
     * Unregisters the {@link net.imatruck.betterweather.BetterWeatherExtension.OnClickReceiver} handler
     */
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

    /**
     * Starts the update process, will verify the reason before continuing
     * @param reason Update reason, provided by DashClock or this app
     */
    @Override
    protected void onUpdateData(int reason) {

        LOGD(TAG, "Update reason: " + getReasonText(reason));

        // Whenever updating, set sLang to Yahoo's format(en-US, not en_US)
        // If sLang is set in elsewhere, and user changes phone's locale
        // without entering BW setting menu, then Yahoo's place name in widget
        // may be in wrong locale.
        Locale current = getResources().getConfiguration().locale;
        YahooPlacesAPIClient.sLang = current.getLanguage() + "-" + current.getCountry();

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

        if (sPebbleEnable) {
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
            publishUpdate(new BetterWeatherData(BetterWeatherData.ErrorCodes.LOCATION));
            scheduleRefresh(5);
            return;
        }

        requestLocationUpdate(lm, provider);
    }

    /**
     * Requests a location update if setting is Automatic, else it will give a dummy location
     * @param lm Location Manager from {@link net.imatruck.betterweather.BetterWeatherExtension#onUpdateData(int)}
     * @param provider Provider determined in {@link net.imatruck.betterweather.BetterWeatherExtension#onUpdateData(int)}
     */
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
                    gpsFixHandler.postDelayed(new Runnable() {
                        public void run() {
                            disableOneTimeLocationListener();
                            LOGD(TAG, "We didn't get a GPS fix quick enough, we'll try again later");
                            scheduleRefresh(0);
                        }
                    }, 30 * 1000);
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

    /**
     * Generates a {@link net.imatruck.betterweather.LocationInfo} object from the app's settings
     * @return LocationInfo
     */
    public static LocationInfo getLocationInfoFromSettings() {

        // Location displayName("New York, USA" form) also assigned.
        return new LocationInfo(WeatherLocationPreference.getWoeidFromValue(sSetLocation),
                WeatherLocationPreference.getDisplayNameFromValue(sSetLocation),
                Double.parseDouble(WeatherLocationPreference.getLatFromValue(sSetLocation)),
                Double.parseDouble(WeatherLocationPreference.getLngFromValue(sSetLocation)));
    }

    /**
     * Requests weather update from the selected API
     * @param location Location object to get devices coords
     * @return {@link net.imatruck.betterweather.BetterWeatherData} object with data from the selected API
     * @throws InvalidLocationException If location is invalid
     * @throws IOException If there's a problem parsing the data
     */
    private static BetterWeatherData getWeatherForLocation(Location location)
            throws InvalidLocationException, IOException {

        LocationInfo locationInfo;
        if (!sUseCurrentLocation) {
            locationInfo = getLocationInfoFromSettings();
        } else {
            if (BuildConfig.DEBUG) {
                LOGD(TAG, "Using location: " + location.getLatitude()
                        + "," + location.getLongitude() + " to get weather");
            }

            locationInfo = YahooPlacesAPIClient.getLocationInfo(location);
        }

        LOGD(TAG, "Using WOEID: " + locationInfo.WOEID + "(" + locationInfo.LAT + "," + locationInfo.LNG + ")");

        IWeatherAPI mWeatherAPI = WeatherAPIFactory.getWeatherAPIFromSetting(sWeatherAPI);

        LOGD(TAG, "Using " + mWeatherAPI.getClass());

        return mWeatherAPI.getWeatherDataForLocation(locationInfo);
    }

    /**
     * Calls {@link net.imatruck.betterweather.BetterWeatherExtension#renderExtensionData(BetterWeatherData)} and sends it to DashClock's publishUpdate
     * @param weatherData Data from the API
     */
    private void publishUpdate(BetterWeatherData weatherData) {

        publishUpdate(renderExtensionData(weatherData));

        if (sPebbleEnable) {
            Pebble.sendWeather(getApplicationContext(), weatherData, sPebbleShowFeelsLike);
        }

        LOGD(TAG, "Published new data to extension");
        lastUpdateTime = System.currentTimeMillis();
        scheduleRefresh(0);

        disableOneTimeLocationListener();
    }

    /*
     * Functions used to refresh the data, restarts the main flow
     */

    /**
     * Calls {@link net.imatruck.betterweather.BetterWeatherExtension#onUpdateData(int)}
     */
    class OnClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            showRefreshToast();
            onUpdateData(UPDATE_REASON_USER_REQUESTED);
        }
    }

    /**
     * Schedule an update with a {@link android.app.PendingIntent}
     * @param intervalOverride Override in minutes for the next refresh, if 0 use settings value
     */
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


    /**
     * Calls the API and publishes the update
     */
    private class RefreshWeatherTask extends AsyncTask<Void, Void, BetterWeatherData> {

        Location mLocation = null;

        public RefreshWeatherTask(Location location) {
            mLocation = location;
        }

        @Override
        protected BetterWeatherData doInBackground(Void... params) {
            LOGD(TAG, "Refreshing weather from RefreshWeatherTask");
            gpsFixHandler.removeCallbacksAndMessages(null);
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
                publishUpdate(betterWeatherData);
        }
    }

    /**
     * Displays a toast if setting is enabled
     */
    private void showRefreshToast() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showRefreshToast = sp.getBoolean(PREF_WEATHER_SHOW_REFRESH_TOAST, false);
        if (showRefreshToast)
            Toast.makeText(this, getString(R.string.toast_refreshing), Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper function used to determine the reason of an update
     * @param reason Update reason
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

    /**
     * Methods used to get current app settings
     */
    private void getCurrentPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sWeatherUnits = sp.getString(PREF_WEATHER_UNITS, sWeatherUnits);
        sSpeedUnits = Integer.parseInt(sp.getString(PREF_WEATHER_SPEED_UNITS, Integer.toString(sSpeedUnits)));
        sSetLocation = sp.getString(PREF_WEATHER_LOCATION, sSetLocation).trim();
        sUseCurrentLocation = TextUtils.isEmpty(sSetLocation);
        sUseOnlyNetworkLocation = sp.getBoolean(PREF_WEATHER_USE_ONLY_NETWORK, sUseOnlyNetworkLocation);
        sShowTodayForecast = sp.getBoolean(PREF_WEATHER_SHOW_TODAY_FORECAST, sShowTodayForecast);
        sShowTomorrowForecast = sp.getBoolean(PREF_WEATHER_SHOW_TOMORROW_FORECAST, sShowTomorrowForecast);
        sRefreshOnTouch = sp.getBoolean(PREF_WEATHER_REFRESH_ON_TOUCH, sRefreshOnTouch);
        sWeatherIntent = AppChooserPreference.getIntentValue(sp.getString(PREF_WEATHER_SHORTCUT, null), DEFAULT_WEATHER_INTENT);
        sRefreshInterval = Integer.parseInt(sp.getString(PREF_WEATHER_REFRESH_INTERVAL, "60"));
        sHideLocationName = sp.getBoolean(PREF_WEATHER_HIDE_LOCATION_NAME, sHideLocationName);
        sShowHumidity = sp.getBoolean(PREF_WEATHER_SHOW_HUMIDITY, sShowHumidity);
        sShowFeelsLike = sp.getBoolean(PREF_WEATHER_SHOW_FEELS_LIKE, sShowFeelsLike);
        sShowWindDetails = sp.getBoolean(PREF_WEATHER_SHOW_WIND_DETAILS, sShowWindDetails);
        sShowWindLabel = sp.getBoolean(PREF_WEATHER_SHOW_WIND_SPEED_AS_LABEL, sShowWindLabel);
        sInvertHighLowTemps = sp.getBoolean(PREF_WEATHER_INVERT_HIGHLOW, sInvertHighLowTemps);
        sPebbleEnable = sp.getBoolean(PREF_PEBBLE_ENABLE, sPebbleEnable);
        sPebbleShowFeelsLike = sp.getBoolean(PREF_PEBBLE_SHOW_FEELS_LIKE, sPebbleShowFeelsLike);
        sWeatherAPI = sp.getString(PREF_WEATHER_API, sWeatherAPI);
        sWeatherAPIKey = sp.getString(PREF_WEATHER_API_KEY, sWeatherAPIKey);

        //Fail safe for change in format from 2.3.3 to 3.0
        convertLocationToNewFormat(sSetLocation, sp);

        LOGD(TAG, "Location from settings is: " + ((sUseCurrentLocation) ? "Automatic" : sSetLocation));
    }

    private static void convertLocationToNewFormat(String oldLocationData, SharedPreferences sp) {

        if(oldLocationData.contains("/") || sUseCurrentLocation)
            return;

        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PREF_WEATHER_LOCATION, oldLocationData.replaceFirst(",", "/") + "/0.0/0.0");
        editor.commit();
    }

    /**
     * @return App's API key
     */
    public static String getWeatherAPIKey() {
        return sWeatherAPIKey;
    }

    /**
     * @return App's weather units setting
     */
    public static String getWeatherUnits() {
        return sWeatherUnits;
    }

    /*
     * Functions used to render the extension on the main widget
     */

    /**
     * Displays weather data, or an error if there's one
     * @param weatherData Weather data from the API
     * @return ExtensionData for DashClock
     */
    private ExtensionData renderExtensionData(BetterWeatherData weatherData) {

        if (weatherData.errorCode != BetterWeatherData.ErrorCodes.NONE) {

            int[] errorStrings = BetterWeatherData.getErrorMessage(weatherData.errorCode);

            @SuppressWarnings("ResourceType") ExtensionData extensionData = new ExtensionData()
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

            @SuppressWarnings("ResourceType") String conditionText = getString(BetterWeatherData.getStatusText(weatherData.conditionCode));

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

    /**
     * Creates the intent from the settings
     * @return Intent from the settings
     */
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

    /**
     * Formats the expended body's text from the weather data
     * @param weatherData Weather data from the API
     * @return Formatted data
     */
    private StringBuilder formatExpendedBody(BetterWeatherData weatherData) {
        StringBuilder expandedBody = new StringBuilder();

        if (sShowFeelsLike && weatherData.feelsLike != weatherData.temperature && weatherData.feelsLike != BetterWeatherData.INVALID_TEMPERATURE) {
            expandedBody.append(getString(R.string.wind_chill_template, weatherData.feelsLike));
            expandedBody.append("\n");
        }

        if (sShowHumidity || sShowWindDetails) {
            StringBuilder detailsLine = new StringBuilder();
            if (sShowWindDetails && !"".equals(weatherData.windSpeed)) {
                // Western users.
                //      "Wind: SW 1 mph"
                // Asaian users with their wind_details_template in values-*
                //      "WIND: DIRECTION PREFIX SPEED UNIT"
                String speed = BetterWeatherData.convertSpeedUnits(sWeatherUnits, weatherData.windSpeed, sSpeedUnits);
                String unit = getSpeedUnitDisplayValue(sSpeedUnits);
                String prefix = getSpeedUnitDisplayPrefixValue(sSpeedUnits);
                @SuppressWarnings("ResourceType") String windDirection = getString(BetterWeatherData.getWindDirectionText(weatherData.windDirection));
                detailsLine.append(getString(R.string.wind_details_template, windDirection, speed, unit, prefix));

                if (sShowWindLabel) {
                    // For example, "Wind: SW 1 mph (Light breeze)"
                    @SuppressWarnings("ResourceType") String speedLabel = getString(BetterWeatherData.getWindSpeedLabel(sWeatherUnits, weatherData.windSpeed));
                    detailsLine.append(" (").append(speedLabel).append(")");
                }
                if (sShowHumidity) {
                    // if sShowWindLabel, then detailsLine can be a little long.
                    detailsLine.append(sShowWindLabel ? "\n" : ", ");
                }

            }
            if (sShowHumidity)
                detailsLine.append(getString(R.string.humidity_template, weatherData.humidity)).append("%");

            expandedBody.append(detailsLine.toString());
        }

        if (sShowTodayForecast) {
            if (sShowHumidity || sShowWindDetails) expandedBody.append("\n");
            int todayForecastTextId = BetterWeatherData.getStatusText(weatherData.todayForecastConditionCode);
            @SuppressWarnings("ResourceType") String todayForecastText = getString(todayForecastTextId);
            expandedBody.append((sInvertHighLowTemps) ?
                    getString(R.string.today_forecast_template, todayForecastText, weatherData.todayHigh, weatherData.todayLow) :
                    getString(R.string.today_forecast_template, todayForecastText, weatherData.todayLow, weatherData.todayHigh));
        }

        if (sShowTomorrowForecast) {
            if (sShowTodayForecast || sShowHumidity || sShowWindDetails) expandedBody.append("\n");
            int tomorrowForecastTextId = BetterWeatherData.getStatusText(weatherData.conditionCode);
            @SuppressWarnings("ResourceType") String tomorrowForecastText = getString(tomorrowForecastTextId);
            expandedBody.append((sInvertHighLowTemps) ?
                    getString(R.string.tomorrow_forecast_template, tomorrowForecastText, weatherData.tomorrowHigh, weatherData.tomorrowLow) :
                    getString(R.string.tomorrow_forecast_template, tomorrowForecastText, weatherData.tomorrowLow, weatherData.tomorrowHigh));
        }

        if (!sHideLocationName) {
            if (sShowHumidity || sShowTodayForecast || sShowTomorrowForecast || sShowWindDetails)
                expandedBody.append("\n");

            // Irrespective of sUseCurrentLocation, weatherData.location has its value,
            // because getLocationInfoFromSettings called before calling this formatExpendedBody() method
            // we prepared location field from settings already.
            String displayLocationName = weatherData.location;

            // displayLocationName shown in pref setting has always "small, large" format.
            // However asian users can customize location name in WIDGET with location_template
            // having a form like "%2$s %1$s", in which %1$s is small, %2$s is large location name.
            String[] locs = displayLocationName.split(",");
            String smallLocation, largeLocation;
            if (locs.length == 2) {
                smallLocation = locs[0].trim();
                largeLocation = locs[1].trim();
            }
            else {
                smallLocation = displayLocationName;
                largeLocation = "";
            }
            expandedBody.append(getString(R.string.location_template, smallLocation, largeLocation));

        }

        return expandedBody;
    }

    /**
     * Formats the status text depending on the settings
     * @param weatherData Weather data from the API
     * @param temperature Temperature formatted for validity
     * @return Formatted status text
     */
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

    /**
     * Gets the appropriate icon from the right icon theme
     * @param conditionCode The status code for the current condition
     * @return Resource ID for the icon
     */
    private int getConditionIconId(int conditionCode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String sIconTheme = sp.getString(PREF_WEATHER_ICON_THEME, CLIMACONS_ICON_THEME);

        return IconThemeFactory.getIconThemeFromSetting(sIconTheme).getConditionIcon(conditionCode);
    }

    /**
     * Gets the wind speed unit from the settings
     * @param speedUnitIndex Index to fetch
     * @return Wind speed unit
     */
    private String getSpeedUnitDisplayValue(int speedUnitIndex) {
        // array R.array.pref_weather_speed_units_display_names is for use in pref menu.
        // array R.array.weather_speed_units_display_names is for use in widget.
        // For asian users, two arrays may be not same, so it is modified.
        String[] units = getResources().getStringArray(R.array.weather_speed_units_display_names);
        if (speedUnitIndex >= 0 && speedUnitIndex < units.length)
            return units[speedUnitIndex];
        return units[0];
    }

    /**
     * Gets the wind speed unit prefix for asian users
     * @param speedUnitIndex Index to fetch
     * @return Wind speed unit prefix
     */
    private String getSpeedUnitDisplayPrefixValue(int speedUnitIndex) {
        String[] prefixes = getResources().getStringArray(R.array.weather_speed_units_display_prefix_names);
        if (speedUnitIndex >= 0 && speedUnitIndex < prefixes.length)
            return prefixes[speedUnitIndex];
        return prefixes[0];
    }

    /*
     * Methods used for Location updates and management
     */

    /**
     * Disables the location listener
     */
    private void disableOneTimeLocationListener() {
        if (mOneTimeLocationListenerActive) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            lm.removeUpdates(mOneTimeLocationListener);
            mOneTimeLocationListenerActive = false;
        }
    }

    /**
     * Detects when location changes or if the service is disabled
     */
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
            publishUpdate(new BetterWeatherData(BetterWeatherData.ErrorCodes.LOCATION));
        }
    };
}
