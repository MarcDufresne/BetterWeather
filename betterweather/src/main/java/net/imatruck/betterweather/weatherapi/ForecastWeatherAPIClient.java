/*
 * Copyright 2013-2014 Marc-André Dufresne
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
import net.imatruck.betterweather.LocationInfo;
import net.imatruck.betterweather.YahooPlacesAPIClient;
import net.imatruck.betterweather.utils.JsonReader;
import net.imatruck.betterweather.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import static net.imatruck.betterweather.utils.LogUtils.LOGW;

public class ForecastWeatherAPIClient implements IWeatherAPI{

    private static final String TAG = LogUtils.makeLogTag(BetterWeatherExtension.class);

    private static final String REQUEST_URL = "https://api.forecast.io/forecast/%s/%s,%s?units=%s";

    @Override
    public BetterWeatherData getWeatherDataForLocation(LocationInfo locationInfo) throws IOException {

        JSONObject response;

        try {
            String weatherUnit = (BetterWeatherExtension.getWeatherUnits().equals("c")) ? "si" : "us";
            response = JsonReader.readJsonFromUrl(String.format(Locale.getDefault(), REQUEST_URL, BetterWeatherExtension.getWeatherAPIKey(), locationInfo.LAT, locationInfo.LNG, weatherUnit));
        }
        catch (JSONException je){
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }
        catch (FileNotFoundException fnfe){
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        JSONObject currently;
        JSONObject today, tomorrow;

        try {
            currently = response.getJSONObject("currently");
            today = response.getJSONObject("daily").getJSONArray("data").getJSONObject(0);
            tomorrow = response.getJSONObject("daily").getJSONArray("data").getJSONObject(1);
        }
        catch (JSONException je) {
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        BetterWeatherData data = new BetterWeatherData();

        parseCurrentConditionsData(currently, data);

        parseForecastData(today, tomorrow, data);

        data.location = YahooPlacesAPIClient.getLocationNameFromCoords(locationInfo.LAT, locationInfo.LNG);

        return data;
    }

    private void parseCurrentConditionsData(JSONObject currently, BetterWeatherData data) {
        if(currently != null){
            try{
                data.conditionCode = convertToConditionCode(currently.getString("icon"));
                data.temperature = (int) Math.round(currently.getDouble("temperature"));
                data.humidity = ((int) (currently.getDouble("humidity") * 100)) + "";
                data.windSpeed = currently.getDouble("windSpeed") + "";
                data.windDirection = !"0".equals(data.windSpeed) ? currently.getInt("windBearing") : 0;
                data.feelsLike = (int) Math.round(currently.getDouble("apparentTemperature"));
            }
            catch (JSONException je) {
                LOGW(TAG, "Error parsing current conditions");
            }
        }
    }

    private void parseForecastData(JSONObject today, JSONObject tomorrow, BetterWeatherData data) {
        if(today != null){
            try{
                data.todayForecastConditionCode = convertToConditionCode(today.getString("icon"));
                data.todayHigh = (int) Math.round(today.getDouble("temperatureMax")) + "";
                data.todayLow = (int) Math.round(today.getDouble("temperatureMin")) + "";
            }
            catch (JSONException je) {
                LogUtils.LOGD(TAG, "Error parsing today's forecast");
            }
        }

        if(tomorrow != null){
            try{
                data.tomorrowForecastConditionCode = convertToConditionCode(tomorrow.getString("icon"));
                data.tomorrowHigh = (int) Math.round(tomorrow.getDouble("temperatureMax")) + "";
                data.tomorrowLow = (int) Math.round(tomorrow.getDouble("temperatureMin")) + "";
            }
            catch (JSONException je) {
                LogUtils.LOGD(TAG, "Error parsing tomorrow's forecast");
            }
        }
    }

    private int convertToConditionCode(String conditionCode) {
        if("clear-day".equals(conditionCode)) return 32;
        if("clear-night".equals(conditionCode)) return 31;
        if("rain".equals(conditionCode)) return 11;
        if("snow".equals(conditionCode)) return 16;
        if("sleet".equals(conditionCode)) return 18;
        if("wind".equals(conditionCode)) return 24;
        if("fog".equals(conditionCode)) return 20;
        if("cloudy".equals(conditionCode)) return 26;
        if("partly-cloudy-day".equals(conditionCode)) return 44;
        if("partly-cloudy-night".equals(conditionCode)) return 29;
        if("hail".equals(conditionCode)) return 17;
        if("thunderstorm".equals(conditionCode)) return 4;
        if("tornado".equals(conditionCode)) return 0;

        return -1;
    }
}
