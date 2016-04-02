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
import java.net.URLEncoder;
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
    private static final int PARSE_STATE_ADMIN2 = 9;
    private static final int PARSE_STATE_ADMIN3 = 10;

    // sLang has Yahoo lang form like "en-US, not en_US"
    // this parameter enables for users to have location name with their language.
    public static String sLang;
    // Method findLocationsAutocomplete get WOEID from methodgetLocationNameFromCoords
    // using this static variable.
    private static String sWoeid;

    public static String buildPlaceSearchUrl(Location l) throws MalformedURLException {
        // GeoPlanet API
        return "http://where.yahooapis.com/v1/places.q('"
                + l.getLatitude() + "," + l.getLongitude() + "')"
                + "?lang=" + sLang + "&appid=" + API_KEY;
    }

    private static String buildPlaceSearchStartsWithUrl(String startsWith) {
        // GeoPlanet API
        startsWith = startsWith.replaceAll("[^\\w ]+", "").replaceAll(" ", "%20");

        // In searching a place name with non-ASCII characters.
        try {
            startsWith = URLEncoder.encode(startsWith, "UTF-8");
        } catch (Exception e) {
        }

        return "http://where.yahooapis.com/v1/places.q('" + startsWith + "');"
                + "count=" + MAX_SEARCH_RESULTS
                + "?lang=" + sLang + "&appid=" + API_KEY;
    }

    public static String getLocationNameFromCoords(double lat, double lng) {
        LOGD(TAG, "Looking up name for location : " + lat + ", " + lng);

        String displayName = "N/A";
        // reset WOEID.
        sWoeid = "";
        HttpURLConnection connection = null;
        try {
            Location tempLoc = new Location("");
            tempLoc.setLatitude(lat);
            tempLoc.setLongitude(lng);
            connection = Utils.openUrlConnection(buildPlaceSearchUrl(tempLoc));

            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            // addrs have {'name', 'admin3', 'admin2', 'admin1', 'country'}, small -> large.
            // Not so few places have empty *local*, and/or admin* values in Yahoo place API.
            // So in some cases, only one name or two names with same values was shown.
            // For example, "SEOUL" or "SEOUL, SEOUL" can be removed.
            // So modified algorithm to get names. Not used local fields.
            // From name to country, skip empty or same value and make "small, large" form.
            String[] addrs = {"", "", "", "", ""};
            String smallLocation = "";
            String largeLocation = "";

            StringBuffer sb = new StringBuffer();

            int state = PARSE_STATE_NONE;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    switch (state) {
                        case PARSE_STATE_NONE:
                            if ("place".equals(tagName)) {
                                state = PARSE_STATE_PLACE;
                            }
                            break;

                        case PARSE_STATE_PLACE:
                            if ("name".equals(tagName)) {
                                state = PARSE_STATE_NAME;
                            } else if ("admin1".equals(tagName)) {
                                state = PARSE_STATE_ADMIN1;
                            } else if ("admin2".equals(tagName)) {
                                state = PARSE_STATE_ADMIN2;
                            } else if ("admin3".equals(tagName)) {
                                state = PARSE_STATE_ADMIN3;
                            } else if ("country".equals(tagName)) {
                                state = PARSE_STATE_COUNTRY;
                            } else if ("woeid".equals(tagName)) {
                                state = PARSE_STATE_WOEID;
                            }

                            break;
                    }

                } else if (eventType == XmlPullParser.TEXT) {
                    switch (state) {
                        case PARSE_STATE_NAME:
                            addrs[0] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN3:
                            addrs[1] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN2:
                            addrs[2] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN1:
                            addrs[3] = xpp.getText();
                            break;

                        case PARSE_STATE_COUNTRY:
                            addrs[4] = xpp.getText();
                            break;

                        case PARSE_STATE_WOEID:
                            // This method 'getLocationNameFromCoords' itself doesnt need WOEID.
                            // But other method 'getLocationInfo' calls it and needs WOEID.
                            // So in this methdd, save WOEID to private static variable.
                            sWoeid = xpp.getText();
                            break;
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName)) {
                        for (int i = 0; i < addrs.length; i++) {
                            if (TextUtils.isEmpty(addrs[i])) {
                                // if field is empty, skip.
                                continue;
                            } else {
                                smallLocation = addrs[i];
                                largeLocation = "";
                                for (int j = i + 1; j < addrs.length; j++) {
                                    if (TextUtils.isEmpty(addrs[j])) {
                                        continue;
                                    }
                                    if (!smallLocation.equals(addrs[j])) {
                                        // if second name is not empty and not same with previus one
                                        // (smallLocation) then it is to be largeLocation.
                                        largeLocation = addrs[j];
                                        break;
                                    }
                                }
                                break;
                            }
                        }

                        sb.setLength(0);
                        if (!TextUtils.isEmpty(smallLocation)) {
                            sb.append(smallLocation);
                        }
                        if (!TextUtils.isEmpty(largeLocation)) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(largeLocation);
                        }
                        displayName = sb.toString();
                        state = PARSE_STATE_NONE;

                    } else if (state != PARSE_STATE_NONE) {
                        state = PARSE_STATE_PLACE;
                    }
                }

                eventType = xpp.next();
            }

        } catch (XmlPullParserException xppe) {
            LOGW(TAG, "Error parsing place name XML");
        } catch (MalformedURLException mue) {
            LOGW(TAG, "Error parsing place name XML");
        } catch (IOException ioe) {
            LOGW(TAG, "Error parsing place name XML");
        } finally {
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
            // See above method 'getLocationNameFromCoords'.
            String[] addrs = {"", "", "", "", "", ""};

            int state = PARSE_STATE_NONE;

            // latitude and longitude values are shown in many entries.
            // But values in <centroid>, not in <boundingBox> are relatively exact one.
            // So using this flag, get only values in centroid entry.
            boolean centroid = false;

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    switch (state) {
                        case PARSE_STATE_NONE:
                            if ("place".equals(tagName)) {
                                state = PARSE_STATE_PLACE;
                                result = new LocationSearchResult();
                                // reset
                                for (int i = 0; i < addrs.length; i++)
                                    addrs[i] = "";
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
                            } else if (centroid && "latitude".equals(tagName)) {
                                state = PARSE_STATE_LAT;
                            } else if (centroid && "longitude".equals(tagName)) {
                                state = PARSE_STATE_LNG;
                            } else if ("admin2".equals(tagName)) {
                                state = PARSE_STATE_ADMIN2;
                            } else if ("admin3".equals(tagName)) {
                                state = PARSE_STATE_ADMIN3;
                            } else if ("centroid".equals(tagName)) {
                                // We use lat/lon values in <centroid> entry.
                                centroid = true;
                            }
                            break;
                    }

                } else if (eventType == XmlPullParser.TEXT) {
                    switch (state) {
                        case PARSE_STATE_WOEID:
                            result.woeid = xpp.getText();
                            break;

                        case PARSE_STATE_NAME:
                            addrs[0] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN3:
                            addrs[1] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN2:
                            addrs[2] = xpp.getText();
                            break;

                        case PARSE_STATE_ADMIN1:
                            addrs[3] = xpp.getText();
                            break;

                        case PARSE_STATE_COUNTRY:
                            addrs[4] = xpp.getText();
                            break;

                        case PARSE_STATE_LAT:
                            result.lat = xpp.getText();
                            break;

                        case PARSE_STATE_LNG:
                            result.lng = xpp.getText();
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName)) {

                        String smallLocation = "";
                        String largeLocation = "";

                        for (int i = 0; i < addrs.length; i++) {
                            if (TextUtils.isEmpty(addrs[i])) {
                                continue;
                            } else {
                                smallLocation = addrs[i];
                                for (int j = i + 1; j < addrs.length; j++) {
                                    if (TextUtils.isEmpty(addrs[j])) {
                                        continue;
                                    }
                                    if (!smallLocation.equals(addrs[j])) {
                                        largeLocation = addrs[j];
                                        break;
                                    }
                                }
                                if (!TextUtils.isEmpty(largeLocation)) {
                                    largeLocation = ", " + largeLocation;
                                }
                                break;
                            }
                        }

                        result.displayName = smallLocation + largeLocation;
                        result.country = addrs[4];

                        results.add(result);
                        state = PARSE_STATE_NONE;
                    } else if ("centroid".equals(tagName)) {
                        centroid = false;
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

        // Original routine used only WOEID/LNG/LAT values from Yahoo place API feed.
        // However we can also use DISPLAY_NAME of location embedded in this feed,
        // because the *LOCALIIZED* location name can be found only in place feed,
        // not in weather feed which only has english or mixed location names.
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String displayName = getLocationNameFromCoords(lat, lon);

        // After calling getLocationNameFromCoords, sWoeid may have WOEID.
        if (!TextUtils.isEmpty(sWoeid)) {
            return new LocationInfo(sWoeid, displayName, lat, lon);
        }
        throw new InvalidLocationException();
    }

    public static class LocationSearchResult {
        public String woeid;
        public String displayName;
        public String country;
        public String lat, lng;
    }
}
