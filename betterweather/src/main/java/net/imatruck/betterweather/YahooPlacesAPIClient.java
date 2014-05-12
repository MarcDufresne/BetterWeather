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

import android.location.Location;
import android.text.TextUtils;

import net.imatruck.betterweather.utils.LogUtils;
import net.imatruck.betterweather.utils.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static net.imatruck.betterweather.utils.LogUtils.LOGD;
import static net.imatruck.betterweather.utils.LogUtils.LOGE;
import static net.imatruck.betterweather.utils.LogUtils.LOGW;


public class YahooPlacesAPIClient {

    private static final String TAG = LogUtils.makeLogTag(YahooPlacesAPIClient.class);

    public static final String API_KEY = "dj0yJmk9TWp2Y3IyMmdhbFp4JmQ9WVdrOVJFMXJVa2s1TlRBbWNHbzlNVFExTXpjd09ETTJNZy0tJnM9Y29uc3VtZXJzZWNyZXQmeD02MA--";

    private static XmlPullParserFactory sXmlPullParserFactory;
    static {
        try {
            sXmlPullParserFactory = XmlPullParserFactory.newInstance();
            sXmlPullParserFactory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            LOGE(YahooPlacesAPIClient.TAG, "Could not instantiate XmlPullParserFactory", e);
        }
    }

    private static final int MAX_SEARCH_RESULTS = 10;
    private static final int PARSE_STATE_NONE = 0;
    private static final int PARSE_STATE_PLACE = 1;
    private static final int PARSE_STATE_WOEID = 2;
    private static final int PARSE_STATE_NAME = 3;
    private static final int PARSE_STATE_COUNTRY = 4;
    private static final int PARSE_STATE_ADMIN1 = 5;
    private static final int PARSE_STATE_LAT = 7;
    private static final int PARSE_STATE_LNG = 8;

    public static String buildPlaceSearchUrl(Location l) throws MalformedURLException {
        // GeoPlanet API
        return "http://where.yahooapis.com/v1/places.q('"
                + l.getLatitude() + "," + l.getLongitude() + "')"
                + "?appid=" + API_KEY;
    }

    private static String buildPlaceSearchStartsWithUrl(String startsWith) {
        // GeoPlanet API
        startsWith = startsWith.replaceAll("[^\\w ]+", "").replaceAll(" ", "%20");
        return "http://where.yahooapis.com/v1/places.q('" + startsWith + "');"
                + "count=" + MAX_SEARCH_RESULTS
                + "?appid=" + API_KEY;
    }

    public static String getLocationNameFromCoords(double lat, double lng){
        LOGD(TAG, "Looking up name for location : " + lat + ", " + lng);

        String displayName = "N/A";

        HttpURLConnection connection = null;
        try {
            Location tempLoc = new Location("");
            tempLoc.setLatitude(lat);
            tempLoc.setLongitude(lng);
            connection = Utils.openUrlConnection(buildPlaceSearchUrl(tempLoc));

            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            String name = null, admin1 = null;
            StringBuilder sb = new StringBuilder();

            int state = PARSE_STATE_NONE;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    switch (state) {
                        case PARSE_STATE_NONE:
                            if ("place".equals(tagName)) {
                                state = PARSE_STATE_PLACE;
                                name = admin1 = null;
                            }
                            break;

                        case PARSE_STATE_PLACE:
                            if ("name".equals(tagName)) {
                                state = PARSE_STATE_NAME;
                            } else if ("admin1".equals(tagName)) {
                                state = PARSE_STATE_ADMIN1;
                            }
                            break;
                    }

                } else if (eventType == XmlPullParser.TEXT) {
                    switch (state) {
                        case PARSE_STATE_NAME:
                            name = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN1:
                            admin1 = xpp.getText();
                            break;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName)) {
                        sb.setLength(0);
                        if (!TextUtils.isEmpty(name)) {
                            sb.append(name);
                        }
                        if (!TextUtils.isEmpty(admin1)) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(admin1);
                        }
                        displayName = sb.toString();
                        state = PARSE_STATE_NONE;

                    } else if (state != PARSE_STATE_NONE) {
                        state = PARSE_STATE_PLACE;
                    }
                }

                eventType = xpp.next();
            }

        }
        catch (XmlPullParserException xppe){
            LOGW(TAG, "Error parsing place name XML");
        }
        catch (MalformedURLException mue) {
            LOGW(TAG, "Error parsing place name XML");
        }
        catch (IOException ioe) {
            LOGW(TAG, "Error parsing place name XML");
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return displayName;
    }

    public static List<LocationSearchResult> findLocationsAutocomplete(String startsWith) {
        LOGD(TAG, "Autocompleting locations starting with '" + startsWith + "'");

        List<LocationSearchResult> results = new ArrayList<LocationSearchResult>();

        HttpURLConnection connection = null;
        try {
            connection = Utils.openUrlConnection(buildPlaceSearchStartsWithUrl(startsWith));
            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            LocationSearchResult result = null;
            String name = null, country = null, admin1 = null;
            StringBuilder sb = new StringBuilder();

            int state = PARSE_STATE_NONE;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    switch (state) {
                        case PARSE_STATE_NONE:
                            if ("place".equals(tagName)) {
                                state = PARSE_STATE_PLACE;
                                result = new LocationSearchResult();
                                name = country = admin1 = null;
                            }
                            break;

                        case PARSE_STATE_PLACE:
                            if ("name".equals(tagName)) {
                                state = PARSE_STATE_NAME;
                            } else if ("woeid".equals(tagName)) {
                                state = PARSE_STATE_WOEID;
                            } else if ("country".equals(tagName)) {
                                state = PARSE_STATE_COUNTRY;
                            } else if ("admin1".equals(tagName)) {
                                state = PARSE_STATE_ADMIN1;
                            } else if ("latitude".equals(tagName)) {
                                state = PARSE_STATE_LAT;
                            } else if ("longitude".equals(tagName)) {
                                state = PARSE_STATE_LNG;
                            }
                            break;
                    }

                } else if (eventType == XmlPullParser.TEXT) {
                    switch (state) {
                        case PARSE_STATE_WOEID:
                            result.woeid = xpp.getText();
                            break;

                        case PARSE_STATE_NAME:
                            name = xpp.getText();
                            break;

                        case PARSE_STATE_COUNTRY:
                            country = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN1:
                            admin1 = xpp.getText();
                            break;

                        case PARSE_STATE_LAT:
                            result.lat = xpp.getText();
                            break;

                        case PARSE_STATE_LNG:
                            result.lng = xpp.getText();
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName)) {
                        sb.setLength(0);
                        if (!TextUtils.isEmpty(name)) {
                            sb.append(name);
                        }
                        if (!TextUtils.isEmpty(admin1)) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(admin1);
                        }
                        result.displayName = sb.toString();
                        result.country = country;
                        results.add(result);
                        state = PARSE_STATE_NONE;

                    } else if (state != PARSE_STATE_NONE) {
                        state = PARSE_STATE_PLACE;
                    }
                }

                eventType = xpp.next();
            }

        } catch (IOException e) {
            LOGW(TAG, "Error parsing place search XML");
        } catch (XmlPullParserException e) {
            LOGW(TAG, "Error parsing place search XML");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return results;
    }

    public static LocationInfo getLocationInfo(Location location)
            throws IOException, InvalidLocationException {
        LocationInfo li = new LocationInfo();

        HttpURLConnection connection;

        connection = Utils.openUrlConnection(buildPlaceSearchUrl(location));

        try {
            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            boolean inWoe = false;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "woeid".equals(xpp.getName())) {
                    inWoe = true;
                } else if (eventType == XmlPullParser.TEXT && inWoe) {
                    li.WOEID = xpp.getText();
                }

                if (eventType == XmlPullParser.END_TAG) {
                    inWoe = false;
                }

                eventType = xpp.next();
            }

            if (!TextUtils.isEmpty(li.WOEID)) {
                li.LNG = location.getLongitude();
                li.LAT = location.getLatitude();
                return li;
            }

            throw new InvalidLocationException();

        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing location XML response.", e);
        } finally {
            connection.disconnect();
        }
    }

    public static class LocationSearchResult {
        public String woeid;
        public String displayName;
        public String country;
        public String lat, lng;
    }
}
