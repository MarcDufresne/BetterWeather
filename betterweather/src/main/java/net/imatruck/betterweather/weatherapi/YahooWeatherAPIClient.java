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
import net.imatruck.betterweather.LocationInfo;
import net.imatruck.betterweather.utils.JsonReader;
import net.imatruck.betterweather.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import static net.imatruck.betterweather.utils.LogUtils.LOGW;

public class YahooWeatherAPIClient implements IWeatherAPI {

    private static final String TAG = LogUtils.makeLogTag(YahooWeatherAPIClient.class);

    private static final String REQUEST_URL = "https://query.yahooapis.com/v1/public/yql?q=select * from weather.forecast where woeid=%s and u='%s'&format=json";

    @Override
    public BetterWeatherData getWeatherDataForLocation(LocationInfo locationInfo) throws IOException {

        JSONObject response;

        try {
            response = JsonReader.readJsonFromUrl(String.format(Locale.getDefault(), REQUEST_URL, locationInfo.WOEID, BetterWeatherExtension.getWeatherUnits()));
        }
        catch (JSONException je) {
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }
        catch (FileNotFoundException fe) {
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        return parseData(response);
    }

    private BetterWeatherData parseData(JSONObject response) {
        BetterWeatherData data = new BetterWeatherData();
        JSONObject weatherInfo = null;
        JSONArray forecast = null;

        if (response != null) {
            try {
                weatherInfo = response.getJSONObject("query").getJSONObject("results").getJSONObject("channel");
                forecast = weatherInfo.getJSONArray("forecast");
            } catch (JSONException e) {
                return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
            }
        }

        if (weatherInfo != null) {
            try {
                JSONObject condition = weatherInfo.getJSONObject("item").getJSONObject("condition");
                data.temperature = data.feelsLike = condition.getInt("temp");
                data.conditionCode = condition.getInt("code");
            } catch (JSONException e) {
                LOGW(TAG, "Error parsing current weather condition");
            }

            try {
                JSONObject wind = weatherInfo.getJSONObject("wind");
                data.windSpeed = wind.getDouble("speed") + "";
                data.windDirection = wind.getInt("direction");
            } catch (JSONException e) {
                LOGW(TAG, "Error parsing wind details");
            }

            try {
                JSONObject atmosphere = weatherInfo.getJSONObject("atmosphere");
                data.humidity = atmosphere.getDouble("humidity") + "";
            } catch (JSONException e) {
                LOGW(TAG, "Error parsing humidity");
            }

            if (forecast != null) {
                try {
                    JSONObject today = forecast.getJSONObject(0);
                    data.todayForecastConditionCode = today.getInt("code");
                    data.todayHigh = today.getInt("high") + "";
                    data.todayLow = today.getInt("low") + "";
                } catch (JSONException e) {
                    LOGW(TAG, "Error parsing today's forecast");
                }

                try {
                    JSONObject tomorrow = forecast.getJSONObject(1);
                    data.tomorrowForecastConditionCode = tomorrow.getInt("code");
                    data.tomorrowHigh = tomorrow.getInt("high") + "";
                    data.tomorrowLow = tomorrow.getInt("low") + "";
                } catch (JSONException e) {
                    LOGW(TAG, "Error parsing tomorrow's forecast");
                }
            }

            try {
                JSONObject location = weatherInfo.getJSONObject("location");
                data.location = String.format(Locale.getDefault(), "%s, %s, %s",
                        location.getString("city"), location.getString("region"), location.getString("country"));
            } catch (JSONException e) {
                LOGW(TAG, "Error parsing location name");
            }

        }
        else {
            return new BetterWeatherData(BetterWeatherData.ErrorCodes.API);
        }

        return data;

    }

}
