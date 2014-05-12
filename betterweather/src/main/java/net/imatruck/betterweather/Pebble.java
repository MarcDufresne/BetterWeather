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

package net.imatruck.betterweather;

import android.content.Context;
import android.content.Intent;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import net.imatruck.betterweather.utils.LogUtils;

import java.util.UUID;

public class Pebble {

    private static final String TAG = "BetterWeather_Pebble";

    private static final UUID APP_UUID = UUID.fromString("4bef12ff-8e26-4899-a269-588d55f6b171");

    private static PebbleKit.PebbleDataReceiver dataReceiver;

    private static final int ICON_ATM = 0;
    private static final int ICON_CLEAR = 1;
    private static final int ICON_CLOUDS = 2;
    private static final int ICON_DRIZZLE = 3;
    private static final int ICON_EXTREME = 4;
    private static final int ICON_RAIN = 5;
    private static final int ICON_SNOW = 6;
    private static final int ICON_STORM = 7;
    private static final int ICON_CLOUDS_NIGHT = 8;
    private static final int ICON_CLEAR_NIGHT = 9;
    private static final int ICON_UNKNOWN = 10;

    public static void registerPebbleDataReceived(Context appContext){
        PebbleKit.registerReceivedDataHandler(appContext, getDataReceiver());
    }

    private static PebbleKit.PebbleDataReceiver getDataReceiver() {
        if(dataReceiver == null) {
            dataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                    PebbleKit.sendAckToPebble(context, transactionId);

                    if(data.getInteger(0) == 1) {
                        requestWeatherUpdate(context);
                    }
                }
            };
        }
        return dataReceiver;
    }

    private static void requestWeatherUpdate(Context appContext) {
        appContext.sendBroadcast(new Intent(BetterWeatherExtension.REFRESH_INTENT_FILTER));
    }

    public static void sendWeather(Context appContext, BetterWeatherData weatherData, boolean showFeelsLike) {

        if(PebbleKit.isWatchConnected(appContext)) {
            if(PebbleKit.areAppMessagesSupported(appContext)) {
                try{
                    LogUtils.LOGD(TAG, "Pebble is connected!");

                    PebbleDictionary pebbleData = new PebbleDictionary();
                    pebbleData.addInt8(0, (byte) getWeatherIconId(weatherData.conditionCode));
                    pebbleData.addString(1, getDisplayTemperature(weatherData, showFeelsLike));

                    PebbleKit.sendDataToPebble(appContext, APP_UUID, pebbleData);
                    LogUtils.LOGD(TAG, "Data sent to Pebble.");
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
            }
            else {
                LogUtils.LOGD(TAG, "Pebble doesn't support AppMessage.");
            }
        }
        else {
            LogUtils.LOGD(TAG, "Pebble not connected.");
        }

    }

    private static String getDisplayTemperature(BetterWeatherData weatherData, boolean showFeelsLike) {
        StringBuilder displayTemp = new StringBuilder();
        if(weatherData.feelsLike < weatherData.temperature && showFeelsLike) {
            displayTemp.append(Integer.toString(weatherData.feelsLike)).append("\u002A");
        }
        else {
            displayTemp.append(Integer.toString(weatherData.temperature)).append("\u00B0");
        }
        return displayTemp.append(BetterWeatherExtension.getWeatherUnits().toUpperCase()).toString();
    }

    private static int getWeatherIconId(int conditionCode) {

        switch(conditionCode){
            case 20: // foggy
            case 19: // dust
            case 21: // haze
            case 24: // windy
            case 22: // smoky
                return ICON_ATM;
            case 27: // mostly cloudy (night)
            case 29: // partly cloudy (night)
                return ICON_CLOUDS_NIGHT;
            case 26: // cloudy
            case 28: // mostly cloudy (day)
            case 30: // partly cloudy (day)
            case 44: // partly cloudy
                return ICON_CLOUDS;
            case 31: // clear (night)
            case 33: // fair (night)
                return ICON_CLEAR_NIGHT;
            case 34: // fair (day)
            case 32: // sunny
            case 23: // blustery
            case 36: // hot
                return ICON_CLEAR;
            case 9: // drizzle
                return ICON_DRIZZLE;
            case 35: // mixed rain and hail
            case 11: // showers
            case 12: // showers
            case 40: // scattered showers
                return ICON_RAIN;
            case 0: // tornado
            case 1: // tropical storm
            case 2: // hurricane
                return ICON_EXTREME;
            case 4: // thunderstorms
            case 37: // isolated thunderstorms
            case 45: // thundershowers
            case 47: // isolated thundershowers
            case 3: // severe thunderstorms
            case 38: // scattered thunderstorms
            case 39: // scattered thunderstorms
                return ICON_STORM;
            case 15: // blowing snow
            case 16: // snow
            case 25: // cold
            case 46: // snow showers
            case 13: // snow flurries
            case 42: // scattered snow showers
            case 41: // heavy snow
            case 43: // heavy snow
            case 5: // mixed rain and snow
            case 6: // mixed rain and sleet
            case 7: // mixed snow and sleet
            case 18: // sleet
            case 8: // freezing drizzle
            case 10: // freezing rain
            case 14: // light snow showers
            case 17: // hail
                return ICON_SNOW;
        }

        return ICON_UNKNOWN;
    }

}
