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
import java.net.URLEncoder;
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
        String smallLocation;
        String largeLocation;
    }
    public static String sLang;

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
                    data.smallLocation = li.smallLocation;
                    data.largeLocation = li.largeLocation;
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
            boolean name = false;
            boolean country = false;
            boolean admin1 = false;
            boolean admin2 = false;
            boolean admin3 = false;
            String[] addrs = {"", "", "", "", ""};
            String smallLocation = "";
            String largeLocation = "";

            li.smallLocation = "";
            li.largeLocation = "";

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "woeid".equals(xpp.getName())) {
                    inWoe = true;
                } else if (eventType == XmlPullParser.TEXT && inWoe) {
                    li.woeid = xpp.getText();
                }

                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("name")) {
                    name = true;
                } else if (eventType == XmlPullParser.TEXT && name) {
                    addrs[0] = xpp.getText();
                }
                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("country")) {
                    country = true;
                } else if (eventType == XmlPullParser.TEXT && country) {
                    addrs[4] = xpp.getText();
                }
                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("admin1")) {
                    admin1 = true;
                } else if (eventType == XmlPullParser.TEXT && admin1) {
                    addrs[3] = xpp.getText();
                }
                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("admin2")) {
                    admin2 = true;
                } else if (eventType == XmlPullParser.TEXT && admin2) {
                    addrs[2] = xpp.getText();
                }
                if (eventType == XmlPullParser.START_TAG && xpp.getName().startsWith("admin3")) {
                    admin3 = true;
                } else if (eventType == XmlPullParser.TEXT && admin3) {
                    addrs[1] = xpp.getText();
                    for(int i=0; i<addrs.length; i++) {
                        if (TextUtils.isEmpty(addrs[i])) {
                            continue;
                        } else {
                            smallLocation = addrs[i];
                            largeLocation = "";
                            for(int j=i+1; j<addrs.length; j++) {
                                if (TextUtils.isEmpty(addrs[j])) {
                                    continue;
                                }
                                if (!smallLocation.equals(addrs[j])) {
                                    largeLocation = addrs[j];
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    li.smallLocation = smallLocation;
                    li.largeLocation = largeLocation;
                }

                if (eventType == XmlPullParser.END_TAG) {
                    inWoe = false;
                    name = false;
                    country = false;
                    admin1 = false;
                    admin2 = false;
                    admin3 = false;
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
                + "?lang=" + sLang + "&appid=" + YahooWeatherAPIConfig.API_KEY;
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
        try {
            startsWith = URLEncoder.encode(startsWith, "UTF-8");
        }
        catch(Exception e) {}
        return "http://where.yahooapis.com/v1/places.q('" + startsWith + "');"
                + "count=" + MAX_SEARCH_RESULTS
                + "?lang=" + sLang + "&appid=" + YahooWeatherAPIConfig.API_KEY;
    }

    private static final int PARSE_STATE_NONE = 0;
    private static final int PARSE_STATE_PLACE = 1;
    private static final int PARSE_STATE_WOEID = 2;
    private static final int PARSE_STATE_NAME = 3;
    private static final int PARSE_STATE_COUNTRY = 4;
    private static final int PARSE_STATE_ADMIN1 = 5;
    private static final int PARSE_STATE_ADMIN2 = 6;
    private static final int PARSE_STATE_ADMIN3 = 7;

    public static List<LocationSearchResult> findLocationsAutocomplete(String startsWith) {
        LOGD(TAG, "Autocompleting locations starting with '" + startsWith + "'");

        List<LocationSearchResult> results = new ArrayList<LocationSearchResult>();

        HttpURLConnection connection = null;
        try {
            connection = Utils.openUrlConnection(buildPlaceSearchStartsWithUrl(startsWith));
            XmlPullParser xpp = sXmlPullParserFactory.newPullParser();
            xpp.setInput(new InputStreamReader(connection.getInputStream()));

            LocationSearchResult result = null;
            String[] addrs = {"", "", "", "", "", ""};

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
                                for(int i=0; i<addrs.length; i++)
                                    addrs[i]= "";
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
                            } else if ("admin2".equals(tagName)) {
                                state = PARSE_STATE_ADMIN2;
                            } else if ("admin3".equals(tagName)) {
                                state = PARSE_STATE_ADMIN3;
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
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName)) {
                        String smallLocation = "";
                        String largeLocation = "";

                        for(int i=0; i<addrs.length; i++) {
                            if (TextUtils.isEmpty(addrs[i])) {
                                continue;
                            }
                            else {
                                smallLocation = addrs[i];
                                largeLocation = "";
                                for(int j=i+1; j<addrs.length; j++) {
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
