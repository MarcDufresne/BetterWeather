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
import net.imatruck.betterweather.utils.LogUtils;
import net.imatruck.betterweather.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import static net.imatruck.betterweather.utils.LogUtils.LOGE;

/**
 * Contains calls required to use Yahoo! Weather API
 */
public class YahooWeatherAPIClient implements IWeatherAPI{

    private static final String TAG = LogUtils.makeLogTag(YahooWeatherAPIClient.class);

    private static XmlPullParserFactory sXmlPullParserFactory;
    static {
        try {
            sXmlPullParserFactory = XmlPullParserFactory.newInstance();
            sXmlPullParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            LOGE(TAG, "Could not instantiate XmlPullParserFactory", e);
        }
    }

    @Override
    public BetterWeatherData getWeatherDataForLocation(LocationInfo li) throws IOException {
        HttpURLConnection connection = Utils.openUrlConnection(buildWeatherQueryUrl(li.WOEID));

        try {
            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            BetterWeatherData data = new BetterWeatherData();
            boolean hasTodayForecast = false;
            boolean hasTomorrowForecast = false;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG
                        && "condition".equals(xpp.getName())) {
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("temp".equals(xpp.getAttributeName(i))) {
                            data.temperature = Integer.parseInt(xpp.getAttributeValue(i));
                        } else if ("code".equals(xpp.getAttributeName(i))) {
                            data.conditionCode = Integer.parseInt(xpp.getAttributeValue(i));
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG
                        && "forecast".equals(xpp.getName())
                        && !hasTodayForecast) {
                    hasTodayForecast = true;
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("code".equals(xpp.getAttributeName(i))) {
                            data.todayForecastConditionCode
                                    = Integer.parseInt(xpp.getAttributeValue(i));
                        } else if ("low".equals(xpp.getAttributeName(i))) {
                            data.todayLow = xpp.getAttributeValue(i);
                        } else if ("high".equals(xpp.getAttributeName(i))) {
                            data.todayHigh = xpp.getAttributeValue(i);
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG
                        && "forecast".equals(xpp.getName())
                        && hasTodayForecast && !hasTomorrowForecast) {
                    hasTomorrowForecast = true;
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("low".equals(xpp.getAttributeName(i))) {
                            data.tomorrowLow = xpp.getAttributeValue(i);
                        } else if ("high".equals(xpp.getAttributeName(i))) {
                            data.tomorrowHigh = xpp.getAttributeValue(i);
                        } else if ("code".equals(xpp.getAttributeName(i))) {
                            data.tomorrowForecastConditionCode = Integer.parseInt(xpp.getAttributeValue(i));
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG
                        && "location".equals(xpp.getName())) {
                    // already LocationInfo li has location name.
                    data.location = li.DISPLAYNAME;
                } else if (eventType == XmlPullParser.START_TAG
                        && "wind".equals(xpp.getName())) {
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("chill".equals(xpp.getAttributeName(i))) {
                            if (xpp.getAttributeValue(i).equals(""))
                                data.feelsLike = -1;
                            else
                                data.feelsLike = Integer.parseInt(xpp.getAttributeValue(i));
                        }
                        else if ("direction".equals(xpp.getAttributeName(i))) {
                            if (xpp.getAttributeValue(i).equals(""))
                                data.windDirection = 0;
                            else
                                data.windDirection = Integer.parseInt(xpp.getAttributeValue(i));
                        }
                        else if ("speed".equals(xpp.getAttributeName(i))) {
                            data.windSpeed = xpp.getAttributeValue(i);
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG
                        && "atmosphere".equals(xpp.getName())) {
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("humidity".equals(xpp.getAttributeName(i))) {
                            data.humidity = xpp.getAttributeValue(i);
                        }
                    }
                }
                eventType = xpp.next();
            }

            return data;

        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing weather feed XML.", e);
        } finally {
            connection.disconnect();
        }
    }

    private static String buildWeatherQueryUrl(String woeid) throws MalformedURLException {
        return "http://weather.yahooapis.com/forecastrss?w=" + woeid + "&u=" + BetterWeatherExtension.getWeatherUnits();
    }

}
