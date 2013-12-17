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

/**
 * A helper class representing weather data, for use with {@link BetterWeatherExtension}.
 */
public class BetterWeatherData {

    public static final int INVALID_TEMPERATURE = Integer.MIN_VALUE;
    public static final int INVALID_CONDITION = -1;

    public int temperature = INVALID_TEMPERATURE;
    public int conditionCode = INVALID_CONDITION;
    public int todayForecastConditionCode = INVALID_CONDITION;
    public String conditionText;
    public String forecastText;
    public String tomorrowForecastText;
    public String todayLow, todayHigh;
    public String tomorrowLow, tomorrowHigh;
    public String location;
    public String windSpeed;
    public int windDirection, windChill;
    public String humidity;
    public int tomorrowForecastConditionCode = INVALID_CONDITION;

    public BetterWeatherData() {
    }

    public boolean hasValidTemperature() {
        return temperature > Integer.MIN_VALUE;
    }

    public static int getWindDirectionText(int wDir){

        if(wDir >= 337 || wDir < 23)
            return R.string.wind_north;
        if (wDir >= 23 && wDir < 67)
            return R.string.wind_northeast;
        if(wDir >= 67 && wDir < 113)
            return R.string.wind_east;
        if(wDir >= 113 && wDir < 157)
            return R.string.wind_southeast;
        if(wDir >= 157 && wDir < 203)
            return R.string.wind_south;
        if(wDir >= 203 && wDir < 247)
            return R.string.wind_soutwest;
        if(wDir >= 247 && wDir < 293)
            return R.string.wind_west;
        if(wDir >= 293 && wDir < 337)
            return R.string.wind_northwest;

        return R.string.wind_north;
    }

    public static int getStatusTextId(int conditionCode) {
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

    public static int getClimaconsConditionIconId(int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 20: // foggy
                return R.drawable.climacons_foggy;
            case 21: // haze
            case 19: // dust
            case 22: // smoky
                return R.drawable.climacons_smoky;
            case 25: // cold
                return R.drawable.climacons_cold;
            case 26: // cloudy
                return R.drawable.climacons_cloudy;
            case 27: // mostly cloudy (night)
            case 29: // partly cloudy (night)
                return R.drawable.climacons_partly_cloudy_night;
            case 28: // mostly cloudy (day)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return R.drawable.climacons_partly_cloudy;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.climacons_clear_night;
            case 34: // fair (day)
            case 32: // sunny
            case 23: // blustery
            case 36: // hot
                return R.drawable.climacons_sunny;
            case 0: // tornado
            case 1: // tropical storm
            case 2: // hurricane
            case 24: // windy
                return R.drawable.climacons_windy;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 18: // sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 14: // light snow showers
                return R.drawable.climacons_mixed_rain_and_snow;
            case 17: // hail
            case 35: // mixed rain and hail
                return R.drawable.climacons_hail;
            case 9: // drizzle
                return R.drawable.climacons_drizzle;
            case 11: // showers
            case 12: // showers
                return R.drawable.climacons_showers;
            case 40: // scattered showers
                return R.drawable.climacons_scattered_showers;
            case 3: // severe thunderstorms
            case 4: // thunderstorms
            case 37: // isolated thunderstorms
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return R.drawable.climacons_thunderstorms;
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return R.drawable.climacons_scattered_thunderstorms;
            case 15: // blowing snow
                return R.drawable.climacons_blowing_snow;
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
            case 13: // snow flurries
            case 42: // scattered snow showers
                return R.drawable.climacons_heavy_snow;
        }

        return R.drawable.climacons_sunny;
    }

    public static int getWeatherconsConditionIconId(int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 20: // foggy
                return R.drawable.weathercons_foggy;
            case 21: // haze
            case 19: // dust
            case 22: // smoky
                return R.drawable.weathercons_smoky;
            case 25: // cold
                return R.drawable.weathercons_cold;
            case 26: // cloudy
                return R.drawable.weathercons_cloudy;
            case 27: // mostly cloudy (night)
                return R.drawable.weathercons_mostly_cloudy_night;
            case 29: // partly cloudy (night)
                return R.drawable.weathercons_partly_cloudy_night;
            case 28: // mostly cloudy (day)
                return R.drawable.weathercons_mostly_cloudy;
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return R.drawable.weathercons_partly_cloudy;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.weathercons_clear_night;
            case 34: // fair (day)
            case 32: // sunny
            case 23: // blustery
            case 36: // hot
                return R.drawable.weathercons_sunny;
            case 0: // tornado
            case 1: // tropical storm
            case 2: // hurricane
            case 24: // windy
                return R.drawable.weathercons_windy;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 18: // sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 14: // light snow showers
                return R.drawable.weathercons_mixed_rain_and_snow;
            case 17: // hail
            case 35: // mixed rain and hail
                return R.drawable.weathercons_hail;
            case 9: // drizzle
                return R.drawable.weathercons_drizzle;
            case 11: // showers
            case 12: // showers
                return R.drawable.weathercons_showers;
            case 40: // scattered showers
                return R.drawable.weathercons_scattered_showers;
            case 3: // severe thunderstorms
            case 4: // thunderstorms
            case 37: // isolated thunderstorms
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return R.drawable.weathercons_thundershowers;
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return R.drawable.weathercons_scattered_thunderstorms;
            case 15: // blowing snow
            case 16: // snow
            case 41: // heavy snow
            case 43: // heavy snow
            case 46: // snow showers
            case 13: // snow flurries
            case 42: // scattered snow showers
                return R.drawable.weathercons_heavy_snow;
        }

        return R.drawable.weathercons_sunny;
    }

    public static int getChameleonConditionIconId(int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 20: // foggy
            case 19: // dust
                return R.drawable.chameleon_foggy;
            case 21: // haze
                return R.drawable.chameleon_haze;
            case 26: // cloudy
                return R.drawable.chameleon_cloudy;
            case 27: // mostly cloudy (night)
                return R.drawable.chameleon_mostly_cloudy_night;
            case 29: // partly cloudy (night)
                return R.drawable.chameleon_partly_cloudy_night;
            case 28: // mostly cloudy (day)
                return R.drawable.chameleon_mostly_cloudy;
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return R.drawable.chameleon_partly_cloudy;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.chameleon_clear_night;
            case 34: // fair (day)
            case 32: // sunny
            case 23: // blustery
            case 36: // hot
                return R.drawable.chameleon_sunny;
            case 0: // tornado
            case 1: // tropical storm
            case 2: // hurricane
            case 24: // windy
            case 22: // smoky
                return R.drawable.chameleon_windy;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 18: // sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 14: // light snow showers
                return R.drawable.chameleon_mixed_rain_and_snow;
            case 17: // hail
            case 35: // mixed rain and hail
                return R.drawable.chameleon_hail;
            case 9: // drizzle
                return R.drawable.chameleon_drizzle;
            case 11: // showers
            case 12: // showers
            case 40: // scattered showers
                return R.drawable.chameleon_showers;
            case 4: // thunderstorms
            case 37: // isolated thunderstorms
                return R.drawable.chameleon_thunderstorms;
            case 45: // thundershowers
            case 47: // isolated thundershowers
                return R.drawable.chameleon_thundershowers;
            case 3: // severe thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return R.drawable.chameleon_scattered_thunderstorms;
            case 15: // blowing snow
            case 16: // snow
            case 25: // cold
            case 46: // snow showers
            case 13: // snow flurries
            case 42: // scattered snow showers
                return R.drawable.chameleon_cold;
            case 41: // heavy snow
            case 43: // heavy snow
                return R.drawable.chameleon_heavy_snow;
        }

        return R.drawable.chameleon_sunny;
    }

    public static int getGoogleNowConditionIconId(int conditionCode) {
        // http://developer.yahoo.com/weather/
        switch (conditionCode) {
            case 20: // foggy
            case 19: // dust
            case 21: // haze
                return R.drawable.googlenow_foggy;
            case 26: // cloudy
                return R.drawable.googlenow_cloudy;
            case 27: // mostly cloudy (night)
                return R.drawable.googlenow_mostly_cloudy_night;
            case 29: // partly cloudy (night)
                return R.drawable.googlenow_partly_cloudy_night;
            case 28: // mostly cloudy (day)
                return R.drawable.googlenow_mostly_cloudy;
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return R.drawable.googlenow_partly_cloudy;
            case 31: // clear (night)
            case 33: // fair (night)
                return R.drawable.googlenow_clear_night;
            case 34: // fair (day)
            case 32: // sunny
            case 23: // blustery
            case 36: // hot
                return R.drawable.googlenow_sunny;
            case 0: // tornado
            case 1: // tropical storm
            case 2: // hurricane
            case 24: // windy
            case 22: // smoky
                return R.drawable.googlenow_windy;
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 18: // sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 14: // light snow showers
            case 17: // hail
            case 35: // mixed rain and hail
                return R.drawable.googlenow_mixed_rain_and_sleet;
            case 9: // drizzle
            case 11: // showers
            case 12: // showers
            case 40: // scattered showers
                return R.drawable.googlenow_rain;
            case 4: // thunderstorms
            case 37: // isolated thunderstorms
            case 45: // thundershowers
            case 47: // isolated thundershowers
            case 3: // severe thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return R.drawable.googlenow_storm;
            case 15: // blowing snow
            case 16: // snow
            case 25: // cold
            case 46: // snow showers
            case 13: // snow flurries
            case 42: // scattered snow showers
            case 41: // heavy snow
            case 43: // heavy snow
                return R.drawable.googlenow_snow;
        }
        return R.drawable.googlenow_unknown;
    }

    public static int[] getErrorTitle(BetterWeatherExtension.ErrorCodes errorCode) {
        switch (errorCode) {
            case UNKNOWN:
                return new int[] {R.string.error_unknown, R.string.error_unknown_expandedBody};
            case LOCATION:
                return new int[] {R.string.error_location, R.string.error_location_expandedBody};
            case INTERNET:
                return new int[] {R.string.error_internet, R.string.error_internet_expandedBody};
        }

        return new int[]  {R.string.error_unknown, R.string.error_unknown_expandedBody};
    }
}
