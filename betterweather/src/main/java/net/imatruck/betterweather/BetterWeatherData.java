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

/**
 * A helper class representing weather data, for use with {@link BetterWeatherExtension}.
 */
public class BetterWeatherData {

    public static final int INVALID_TEMPERATURE = Integer.MIN_VALUE;
    public static final int INVALID_CONDITION = -1;

    //Currently
    /**
     * Should be between {@value net.imatruck.betterweather.BetterWeatherData#INVALID_TEMPERATURE} and {@value Integer#MAX_VALUE}
     */
    public int temperature = INVALID_TEMPERATURE;
    /**
     * Should be between -1 and 47 to follow Yahoo's values
     *
     * @see <a href="https://developer.yahoo.com/weather/#codes">Yahoo's API Doc</a>
     */
    public int conditionCode = INVALID_CONDITION;
    /**
     * Should be between 0 and {@value java.lang.Double#MAX_VALUE}, written as a double
     */
    public String windSpeed;
    /**
     * Should be between 0 and 359 (Absolute north is 0)
     */
    public int windDirection;
    /**
     * Will be displayed if different from {@link net.imatruck.betterweather.BetterWeatherData#temperature}
     *
     * @see net.imatruck.betterweather.BetterWeatherData#temperature
     */
    public int feelsLike;
    /**
     * Should be between 0 and 100
     */
    public String humidity;

    //Today
    /**
     * Should be between -1 and 47 to follow Yahoo's values
     *
     * @see <a href="https://developer.yahoo.com/weather/#codes">Yahoo's API Doc</a>
     */
    public int todayForecastConditionCode = INVALID_CONDITION;
    /**
     * Should be between {@value net.imatruck.betterweather.BetterWeatherData#INVALID_TEMPERATURE} and {@value Integer#MAX_VALUE}
     */
    public String todayLow, todayHigh;

    //Tomorrow
    /**
     * Should be between -1 and 47 to follow Yahoo's values
     *
     * @see <a href="https://developer.yahoo.com/weather/#codes">Yahoo's API Doc</a>
     */
    public int tomorrowForecastConditionCode = INVALID_CONDITION;
    /**
     * Should be between {@value net.imatruck.betterweather.BetterWeatherData#INVALID_TEMPERATURE} and {@value Integer#MAX_VALUE}
     */
    public String tomorrowLow, tomorrowHigh;

    //General
    /**
     * String value representing the data location
     */
    public String location;

    //Error Management
    public ErrorCodes errorCode = ErrorCodes.NONE;

    public static enum ErrorCodes {
        NONE, UNKNOWN, LOCATION, INTERNET, API
    }

    public BetterWeatherData() {
    }

    public BetterWeatherData(ErrorCodes errorCode) {
        this.errorCode = errorCode;
    }

    public boolean hasValidTemperature() {
        return temperature > Integer.MIN_VALUE;
    }

    public static int getWindDirectionText(int wDir) {

        if (wDir >= 337 || wDir < 23)
            return R.string.wind_north;
        if (wDir >= 23 && wDir < 67)
            return R.string.wind_northeast;
        if (wDir >= 67 && wDir < 113)
            return R.string.wind_east;
        if (wDir >= 113 && wDir < 157)
            return R.string.wind_southeast;
        if (wDir >= 157 && wDir < 203)
            return R.string.wind_south;
        if (wDir >= 203 && wDir < 247)
            return R.string.wind_soutwest;
        if (wDir >= 247 && wDir < 293)
            return R.string.wind_west;
        if (wDir >= 293 && wDir < 337)
            return R.string.wind_northwest;

        return R.string.wind_north;
    }

    public static int getStatusText(int conditionCode) {
        switch (conditionCode) {
            case 0:
                return R.string.cond_tornado;
            case 1:
                return R.string.cond_tropical_storm;
            case 2:
                return R.string.cond_hurricane;
            case 3:
                return R.string.cond_severe_thunderstorms;
            case 4:
                return R.string.cond_thunderstorms;
            case 5:
                return R.string.cond_mixed_rain_and_snow;
            case 6:
                return R.string.cond_mixed_rain_and_sleet;
            case 7:
                return R.string.cond_mixed_snow_and_sleet;
            case 8:
                return R.string.cond_freezing_drizzle;
            case 9:
                return R.string.cond_drizzle;
            case 10:
                return R.string.cond_freezing_rain;
            case 11:
            case 12:
                return R.string.cond_showers;
            case 13:
                return R.string.cond_snow_flurries;
            case 14:
                return R.string.cond_light_snow_showers;
            case 15:
                return R.string.cond_blowing_snow;
            case 16:
                return R.string.cond_snow;
            case 17:
                return R.string.cond_hail;
            case 18:
                return R.string.cond_sleet;
            case 19:
                return R.string.cond_dust;
            case 20:
                return R.string.cond_foggy;
            case 21:
                return R.string.cond_haze;
            case 22:
                return R.string.cond_smoky;
            case 23:
                return R.string.cond_blustery;
            case 24:
                return R.string.cond_windy;
            case 25:
                return R.string.cond_cold;
            case 26:
                return R.string.cond_cloudy;
            case 27:
            case 28:
                return R.string.cond_mostly_cloudy;
            case 44:
            case 29:
            case 30:
                return R.string.cond_partly_cloudy;
            case 31:
                return R.string.cond_clear;
            case 32:
                return R.string.cond_sunny;
            case 33:
            case 34:
                return R.string.cond_fair;
            case 35:
                return R.string.cond_mixed_rain_and_hail;
            case 36:
                return R.string.cond_hot;
            case 37:
                return R.string.cond_isolated_thunderstorms;
            case 38:
            case 39:
                return R.string.cond_scattered_thunderstorms;
            case 40:
                return R.string.cond_scattered_showers;
            case 43:
            case 41:
                return R.string.cond_heavy_snow;
            case 42:
                return R.string.cond_scattered_snow_showers;
            case 45:
                return R.string.cond_thundershowers;
            case 46:
                return R.string.cond_snow_showers;
            case 47:
                return R.string.cond_isolated_thundershowers;
        }

        return R.string.cond_na;
    }

    public static String convertSpeedUnits(String weatherUnit, String windSpeedString, int wantedUnit) {
        if (windSpeedString != null) {
            float windSpeed = Float.parseFloat(windSpeedString);
            if (weatherUnit.equals("c")) {
                switch (wantedUnit) {
                    case 0:
                        windSpeed = windSpeed / 1.609344f;
                        break; // Km/h -> Mph
                    case 2:
                        windSpeed = windSpeed * 0.2778f;
                        break; // Km/h -> M/s
                }
            } else {
                switch (wantedUnit) {
                    case 1:
                        windSpeed = windSpeed * 1.609344f;
                        break; // Mph -> Km/h
                    case 2:
                        windSpeed = windSpeed * 0.44704f;
                        break; // Mph -> M/s
                }
            }
            return Integer.toString(Math.round(windSpeed));
        }
        return "0";
    }

    /**
     * Convert wind speed to descrptive label.
     *
     * @see <a href="http://www.windfinder.com/wind/windspeed.htm">Wind speed labels</a>
     */
    public static int getWindSpeedLabel(String weatherUnit, String windSpeedString) {
        if (windSpeedString != null) {
            float windSpeed = Float.parseFloat(windSpeedString);
            if (weatherUnit.equals("c")) {
                windSpeed = windSpeed / 1.609344f; // Km/h -> Mph
            }
            // windSpeed in mph unit.
            //
            if (windSpeed < 1)
                return R.string.wind_0;
            else if (windSpeed < 4)
                return R.string.wind_1;
            else if (windSpeed < 8)
                return R.string.wind_2;
            else if (windSpeed < 13)
                return R.string.wind_3;
            else if (windSpeed < 18)
                return R.string.wind_4;
            else if (windSpeed < 25)
                return R.string.wind_5;
            else if (windSpeed < 31)
                return R.string.wind_6;
            else if (windSpeed < 39)
                return R.string.wind_7;
            else if (windSpeed < 47)
                return R.string.wind_8;
            else if (windSpeed < 55)
                return R.string.wind_9;
            else if (windSpeed < 64)
                return R.string.wind_10;
            else if (windSpeed < 74)
                return R.string.wind_11;
            else
                return R.string.wind_12;
        }
        return R.string.cond_na;
    }

    public static int[] getErrorMessage(ErrorCodes errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                return new int[]{R.string.error_unknown, R.string.error_unknown_expandedBody};
            case LOCATION:
                return new int[]{R.string.error_location, R.string.error_location_expandedBody};
            case INTERNET:
                return new int[]{R.string.error_internet, R.string.error_internet_expandedBody};
            case API:
                return new int[]{R.string.error_api, R.string.error_api_expandedBody};
        }

        return new int[]{R.string.error_unknown, R.string.error_unknown_expandedBody};
    }
}
