/*
 * Copyright 2013-2016 Marc-André Dufresne
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
 */
package net.imatruck.betterweather.weatherapi;

import net.imatruck.betterweather.BetterWeatherData;
import net.imatruck.betterweather.BetterWeatherExtension;
import net.imatruck.betterweather.BuildConfig;
import net.imatruck.betterweather.LocationInfo;
import net.imatruck.betterweather.YahooPlacesAPIClient;
import net.imatruck.betterweather.utils.JsonReader;
import net.imatruck.betterweather.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import static net.imatruck.betterweather.utils.LogUtils.LOGD;
import static net.imatruck.betterweather.utils.LogUtils.LOGW;

public class OpenWeatherMapWeatherAPIClient implements IWeatherAPI {

    private static final String TAG = LogUtils.makeLogTag(OpenWeatherMapWeatherAPIClient.class);

    private static final String REQUEST_URL_CURRENT = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=%s&APPID=%s";
    private static final String REQUEST_URL_FORECAST = "http://api.openweathermap.org/data/2.5/forecast/daily?lat=%s&lon=%s&cnt=2&units=%s&APPID=%s";

    @Override
    public BetterWeatherData getWeatherDataForLocation(LocationInfo locationInfo) throws IOException {

        BetterWeatherData data = new BetterWeatherData();

        JSONObject responseCurrent;
        JSONObject responseForecast;

        try {
            responseCurrent = getCurrentData(locationInfo, 0);
            responseForecast = getForecastData(locationInfo, 0);
        } catch (IOException ioe) {
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        if (parseCurrentConditionsData(data, responseCurrent)) {
            LOGW(TAG, "Could not parse current weather data");
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        parseForecastData(data, responseForecast);

        data.location = YahooPlacesAPIClient.getLocationNameFromCoords(locationInfo.LAT, locationInfo.LNG);

        return data;
    }

    private JSONObject getCurrentData(LocationInfo locationInfo, int retryCount) throws IOException {

        JSONObject responseCurrent;

        String API_KEY = BuildConfig.OWM_API_KEY[retryCount];

        try {
            String weatherUnit = (BetterWeatherExtension.getWeatherUnits().equals("c")) ? "metric" : "imperial";
            String formattedURL = String.format(Locale.getDefault(), REQUEST_URL_CURRENT, locationInfo.LAT, locationInfo.LNG, weatherUnit, API_KEY);
            LOGD(TAG, "Using URL: " + formattedURL);
            responseCurrent = JsonReader.readJsonFromUrl(formattedURL);
        } catch (JSONException je) {
            throw new IOException();
        } catch (FileNotFoundException fnfe) {
            LOGD(TAG, "Could not retrieve current weather info, retry #" + retryCount);
            if (retryCount < BuildConfig.OWM_API_KEY.length)
                return getCurrentData(locationInfo, ++retryCount);
            throw new IOException();
        }

        return responseCurrent;
    }

    private JSONObject getForecastData(LocationInfo locationInfo, int retryCount) throws IOException {

        JSONObject responseForecast;

        String API_KEY = BuildConfig.OWM_API_KEY[retryCount];

        try {
            String weatherUnit = (BetterWeatherExtension.getWeatherUnits().equals("c")) ? "metric" : "imperial";
            String formattedURL = String.format(Locale.getDefault(), REQUEST_URL_FORECAST, locationInfo.LAT, locationInfo.LNG, weatherUnit, API_KEY);
            LOGD(TAG, "Using URL: " + formattedURL);
            responseForecast = JsonReader.readJsonFromUrl(formattedURL);
        } catch (JSONException je) {
            throw new IOException();
        } catch (FileNotFoundException fnfe) {
            LOGD(TAG, "Could not retrieve forecast weather info, retry #" + retryCount);
            if (retryCount < BuildConfig.OWM_API_KEY.length)
                return getForecastData(locationInfo, ++retryCount);
            throw new IOException();
        }

        return responseForecast;
    }

    private boolean parseCurrentConditionsData(BetterWeatherData data, JSONObject response) {
        JSONObject currentWeather = null, currentMain = null, currentWind = null;

        if (response != null) {
            try {
                currentWeather = response.getJSONArray("weather").getJSONObject(0);
                currentMain = response.getJSONObject("main");
                currentWind = response.getJSONObject("wind");
            } catch (JSONException je) {
                return true;
            }
        }

        if (currentMain != null) {
            try {
                data.temperature = data.feelsLike = (int) Math.round(currentMain.getDouble("temp"));
                data.humidity = currentMain.getInt("humidity") + "";
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing current weather data");
            }
        }

        if (currentWeather != null) {
            try {
                data.conditionCode = convertToConditionCode(currentWeather.getInt("id"), currentWeather.getString("icon").contains("n"));
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing current weather condition");
            }
        }

        if (currentWind != null) {
            try {
                data.windSpeed = currentWind.getDouble("speed") + "";
                data.windDirection = currentWind.getInt("deg");
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing current wind data");
            }
        }
        return false;
    }

    private void parseForecastData(BetterWeatherData data, JSONObject response) {
        JSONObject todayWeather = null, todayMain = null, tomorrowWeather = null, tomorrowMain = null;

        if (response != null) {
            try {
                JSONArray list = response.getJSONArray("list");
                todayMain = list.getJSONObject(0);
                todayWeather = todayMain.getJSONArray("weather").getJSONObject(0);
                tomorrowMain = list.getJSONObject(1);
                tomorrowWeather = tomorrowMain.getJSONArray("weather").getJSONObject(0);
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing forecast data");
            }
        }

        if (todayMain != null && todayWeather != null) {
            try {
                data.todayForecastConditionCode = convertToConditionCode(todayWeather.getInt("id"), false);
                data.todayHigh = (int) Math.round(todayMain.getJSONObject("temp").getDouble("max")) + "";
                data.todayLow = (int) Math.round(todayMain.getJSONObject("temp").getDouble("min")) + "";
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing today's forecast data");
            }
        }

        if (tomorrowMain != null && tomorrowWeather != null) {
            try {
                data.tomorrowForecastConditionCode = convertToConditionCode(tomorrowWeather.getInt("id"), false);
                data.tomorrowHigh = (int) Math.round(tomorrowMain.getJSONObject("temp").getDouble("max")) + "";
                data.tomorrowLow = (int) Math.round(tomorrowMain.getJSONObject("temp").getDouble("min")) + "";
            } catch (JSONException je) {
                LOGW(TAG, "Error parsing tomorrow's forecast data");
            }
        }
    }

    private int convertToConditionCode(int conditionCode, boolean isNight) {
        switch (conditionCode) {
            case 200: //thunderstorm with light rain
            case 201: //thunderstorm with rain
            case 231: //thunderstorm with drizzle
            case 210: //light thunderstorm
            case 211: //thunderstorm
            case 230: //thunderstorm with light drizzle
                return 4;
            case 212: //heavy thunderstorm
            case 221: //ragged thunderstorm
            case 232: //thunderstorm with heavy drizzle
            case 202: //thunderstorm with heavy rain
                return 3;
            case 300: //light intensity drizzle
            case 301: //drizzle
            case 313: //shower rain and drizzle
            case 310: //light intensity drizzle rain
            case 311: //drizzle rain
            case 321: //shower drizzle
            case 312: //heavy intensity drizzle rain
            case 302: //heavy intensity drizzle
            case 314: //heavy shower rain and drizzle
                return 9;
            case 500: //light rain
            case 501: //moderate rain
            case 502: //heavy intensity rain
            case 503: //very heavy rain
            case 504: //extreme rain
            case 520: //light intensity shower rain
            case 521: //shower rain
            case 522: //heavy intensity shower rain
            case 531: //ragged shower rain
                return 12;
            case 511: //freezing rain
                return 10;
            case 600: //light snow
            case 601: //snow
                return 16;
            case 611: //sleet
            case 612: //shower sleet
                return 18;
            case 615: //light rain and snow
            case 616: //rain and snow
                return 5;
            case 620: //light shower snow
            case 621: //shower snow
            case 622: //heavy shower snow
                return 46;
            case 602: //heavy snow
                return 41;
            case 711: //smoke
                return 22;
            case 721: //haze
                return 21;
            case 741: //Fog
            case 701: //mist
                return 20;
            case 751: //sand
            case 761: //dust
            case 731: //Sand/Dust Whirls
            case 762: //VOLCANIC ASH
                return 19;
            case 771: //SQUALLS
                return 24;
            case 781: //TORNADO
            case 900: //tornado
                return 0;
            case 800: //sky is clear
            case 950: //setting
            case 951: //calm
            case 952: //light breeze
            case 953: //gentle breeze
            case 954: //moderate breeze
            case 955: //fresh breeze
                return isNight ? 31 : 32;
            case 801: //few clouds
            case 802: //scattered clouds
                return isNight ? 29 : 30;
            case 803: //broken clouds
                return isNight ? 27 : 28;
            case 804: //overcast clouds
                return 26;
            case 901: //tropical storm
            case 960: //storm
            case 961: //violent storm
                return 1;
            case 902: //hurricane
            case 962: //hurricane
                return 2;
            case 903: //cold
                return 25;
            case 904: //hot
                return 36;
            case 905: //windy
            case 956: //strong breeze
            case 957: //high wind, near gale
            case 958: //gale
            case 959: //severe gale
                return 24;
            case 906: //hail
                return 17;
        }
        return -1;
    }
}
