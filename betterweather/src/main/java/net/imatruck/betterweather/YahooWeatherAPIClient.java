package net.imatruck.betterweather;

import android.location.Location;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static net.imatruck.betterweather.LogUtils.LOGD;
import static net.imatruck.betterweather.LogUtils.LOGE;
import static net.imatruck.betterweather.LogUtils.LOGW;

/**
 * Contains calls required to use Yahoo! Weather API
 */
public class YahooWeatherAPIClient {

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

    private static final int MAX_SEARCH_RESULTS = 10;

    public static class LocationInfo {
        String woeid;
        String town;
    }

    public static BetterWeatherData getWeatherDataForLocation(LocationInfo li) throws IOException {
        HttpURLConnection connection = Utils.openUrlConnection(buildWeatherQueryUrl(li.woeid));

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
                        } else if ("text".equals(xpp.getAttributeName(i))) {
                            data.conditionText = xpp.getAttributeValue(i);
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
                        } else if ("text".equals(xpp.getAttributeName(i))) {
                            data.forecastText = xpp.getAttributeValue(i);
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
                        if ("text".equals(xpp.getAttributeName(i))) {
                            data.tomorrowForecastText = xpp.getAttributeValue(i);
                        } else if ("low".equals(xpp.getAttributeName(i))) {
                            data.tomorrowLow = xpp.getAttributeValue(i);
                        } else if ("high".equals(xpp.getAttributeName(i))) {
                            data.tomorrowHigh = xpp.getAttributeValue(i);
                        } else if ("code".equals(xpp.getAttributeName(i))) {
                            data.tomorrowForecastConditionCode = Integer.parseInt(xpp.getAttributeValue(i));
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG
                        && "location".equals(xpp.getName())) {
                    String cityOrVillage = "--";
                    String region = null;
                    String country = "--";
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("city".equals(xpp.getAttributeName(i))) {
                            cityOrVillage = xpp.getAttributeValue(i);
                        } else if ("region".equals(xpp.getAttributeName(i))) {
                            region = xpp.getAttributeValue(i);
                        } else if ("country".equals(xpp.getAttributeName(i))) {
                            country = xpp.getAttributeValue(i);
                        }
                    }

                    if (TextUtils.isEmpty(region)) {
                        // If no region is available, show the country. Otherwise, don't
                        // show country information.
                        region = country;
                    }

                    if (!TextUtils.isEmpty(li.town) && !li.town.equals(cityOrVillage)) {
                        // If a town is available and it's not equivalent to the city name,
                        // show it.
                        cityOrVillage = cityOrVillage + ", " + li.town;
                    }

                    data.location = cityOrVillage + ", " + region;
                } else if (eventType == XmlPullParser.START_TAG
                        && "wind".equals(xpp.getName())) {
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("chill".equals(xpp.getAttributeName(i))) {
                            if (xpp.getAttributeValue(i).equals(""))
                                data.windChill = -1;
                            else
                                data.windChill = Integer.parseInt(xpp.getAttributeValue(i));
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

    public static LocationInfo getLocationInfo(Location location)
            throws IOException, InvalidLocationException {
        LocationInfo li = new LocationInfo();

        HttpURLConnection connection;

        connection = Utils.openUrlConnection(buildPlaceSearchUrl(location));

        try {
            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            boolean inWoe = false;
            boolean inTown = false;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "woeid".equals(xpp.getName())) {
                    inWoe = true;
                } else if (eventType == XmlPullParser.TEXT && inWoe) {
                    li.woeid = xpp.getText();
                }

                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("locality")) {
                    for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
                        if ("type".equals(xpp.getAttributeName(i))
                                && "Town".equals(xpp.getAttributeValue(i))) {
                            inTown = true;
                        }
                    }
                } else if (eventType == XmlPullParser.TEXT && inTown) {
                    li.town = xpp.getText();
                }

                if (eventType == XmlPullParser.END_TAG) {
                    inWoe = false;
                    inTown = false;
                }

                eventType = xpp.next();
            }

            if (!TextUtils.isEmpty(li.woeid)) {
                return li;
            }

            throw new InvalidLocationException();

        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing location XML response.", e);
        } finally {
            connection.disconnect();
        }
    }

    public static String buildWeatherQueryUrl(String woeid) throws MalformedURLException {
        return "http://weather.yahooapis.com/forecastrss?w=" + woeid + "&u=" + BetterWeatherExtension.getWeatherUnits();
    }

    public static String buildPlaceSearchUrl(Location l) throws MalformedURLException {
        // GeoPlanet API
        return "http://where.yahooapis.com/v1/places.q('"
                + l.getLatitude() + "," + l.getLongitude() + "')"
                + "?appid=" + YahooWeatherAPIConfig.API_KEY;
    }

    public static String buildPlaceSearchUrl(String cN) throws MalformedURLException {
        // GeoPlanet API
        URI geocodeURI = null;
        try {
            geocodeURI = new URI(
                    "http",
                    "where.yahooapis.com",
                    "/v1/places.q('" + cN + "')",
                    "appid=" + YahooWeatherAPIConfig.API_KEY,
                    null);
        } catch (URISyntaxException e) {
            LOGW(TAG, "URI Syntax invalid");
        }
        return geocodeURI.toASCIIString();
    }

    public static String buildPlaceSearchUrlFromWoeid(String woeid) throws MalformedURLException {
        // GeoPlanet API
        URI geocodeURI = null;
        try {
            geocodeURI = new URI(
                    "http",
                    "where.yahooapis.com",
                    "/v1/place/" + woeid + "",
                    "appid=" + YahooWeatherAPIConfig.API_KEY,
                    null);
        } catch (URISyntaxException e) {
            LOGW(TAG, "URI Syntax invalid");
        }
        return geocodeURI.toASCIIString();
    }

    private static String buildPlaceSearchStartsWithUrl(String startsWith) {
        // GeoPlanet API
        startsWith = startsWith.replaceAll("[^\\w ]+", "").replaceAll(" ", "%20");
        return "http://where.yahooapis.com/v1/places.q('" + startsWith + "');"
                + "count=" + MAX_SEARCH_RESULTS
                + "?appid=" + YahooWeatherAPIConfig.API_KEY;
    }

    private static final int PARSE_STATE_NONE = 0;
    private static final int PARSE_STATE_PLACE = 1;
    private static final int PARSE_STATE_WOEID = 2;
    private static final int PARSE_STATE_NAME = 3;
    private static final int PARSE_STATE_COUNTRY = 4;
    private static final int PARSE_STATE_ADMIN1 = 5;

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

    public static class LocationSearchResult {
        String woeid;
        String displayName;
        String country;
    }

}
