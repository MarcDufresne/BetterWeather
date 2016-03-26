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
 * Copyright 2013-2014 Marc-André Dufresne
 *
 * This file was modified by Marc-André Dufresne to include several
 * more features.
 */

package net.imatruck.betterweather.settings;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.imatruck.betterweather.R;
import net.imatruck.betterweather.YahooPlacesAPIClient;
import net.imatruck.betterweather.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

import static net.imatruck.betterweather.YahooPlacesAPIClient.LocationSearchResult;

/**
 * A preference that allows the user to choose a location, using the Yahoo! GeoPlanet API.
 */
public class WeatherLocationPreference extends Preference {

    private static final String TAG = LogUtils.makeLogTag(WeatherLocationPreference.class);

    private static final int LOCATION_REQUEST_CODE = 12;

    public WeatherLocationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeatherLocationPreference(Context context) {
        super(context);
    }

    public WeatherLocationPreference(Context context, AttributeSet attrs,
                                     int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setValue(String value) {
        if (value == null) {
            value = "";
        }

        if (callChangeListener(value)) {
            persistString(value);
            notifyChanged();
        }
    }

    public static CharSequence getDisplayValue(Context context, String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            return context.getString(R.string.pref_weather_location_automatic);
        }
        String[] locationDetails = value.split("/");
        return locationDetails[1];
    }

    public static String getWoeidFromValue(String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            if(value.matches("\\d+,[0-9a-zA-Z,. ]*"))
                return value.substring(0, value.indexOf(","));
            return null;
        }

        String[] locationDetails = value.split("/");
        return locationDetails[0];
    }

    /**
     * This method is for getting location name.
     * @param value pref location string.
     * @return location name saved in setting.
     */
    public static String getDisplayNameFromValue(String value) {
        if (TextUtils.isEmpty(value) || value.indexOf('/') < 0) {
            if(value.matches("\\d+,[0-9a-zA-Z,. ]*"))
                return value.substring(value.indexOf(",")+1).trim();
            return null;
        }

        String[] locationDetails = value.split("/");
        return locationDetails[1];
    }

    public static String getLatFromValue(String value) {
        // if value is a form like "11111/SEOUL, KOREA/12.3333/45.6666", in which
        // SEOUL, KOREA are not ASCIIs but Korean characters, then
        // if(!value.matches("\\d+/[0-9a-zA-Z,. ]*/\\d+[,.]?\\d*/\\d+[,.]?\\d*"))
        // fails to match lat/lon, so changed loosely.
        if(!value.matches("\\d+/[^/]*/-?\\d+[,.]?\\d*/-?\\d+[,.]?\\d*"))
            return "0";

        String[] locationDetails = value.split("/");
        return locationDetails[2];
    }

    public static String getLngFromValue(String value) {
        if(!value.matches("\\d+/[^/]*/-?\\d+[,.]?\\d*/-?\\d+[,.]?\\d*"))
            return "0";

        String[] locationDetails = value.split("/");
        return locationDetails[3];
    }

    @Override
    protected void onClick() {
        super.onClick();

        LocationChooserDialogFragment fragment = LocationChooserDialogFragment.newInstance();
        fragment.setPreference(this);

        Activity activity = (Activity) getContext();
        activity.getFragmentManager().beginTransaction()
                .add(fragment, getFragmentTag())
                .commit();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        Activity activity = (Activity) getContext();
        LocationChooserDialogFragment fragment = (LocationChooserDialogFragment) activity
                .getFragmentManager().findFragmentByTag(getFragmentTag());
        if (fragment != null) {
            // re-bind preference to fragment
            fragment.setPreference(this);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString("") : (String) defaultValue);
    }

    public String getFragmentTag() {
        return "location_chooser_" + getKey();
    }

    /**
     * Dialog fragment that pops up when touching the preference.
     */
    public static class LocationChooserDialogFragment extends DialogFragment implements
            TextWatcher,
            LoaderManager.LoaderCallbacks<List<LocationSearchResult>> {
        /**
         * Time between search queries while typing.
         */
        private static final int QUERY_DELAY_MILLIS = 500;

        private WeatherLocationPreference mPreference;

        private SearchResultsListAdapter mSearchResultsAdapter;
        private ListView mSearchResultsList;

        public LocationChooserDialogFragment() {
        }

        public static LocationChooserDialogFragment newInstance() {
            return new LocationChooserDialogFragment();
        }

        public void setPreference(WeatherLocationPreference preference) {
            mPreference = preference;
            tryBindList();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            tryBindList();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context layoutContext = new ContextThemeWrapper(getActivity(),
                    android.R.style.Theme_DeviceDefault_Light_Dialog);

            LayoutInflater layoutInflater = LayoutInflater.from(layoutContext);
            View rootView = layoutInflater.inflate(R.layout.dialog_weather_location_chooser, null);
            TextView searchView = (TextView) rootView.findViewById(R.id.location_query);
            searchView.addTextChangedListener(this);

            // Set up apps
            mSearchResultsList = (ListView) rootView.findViewById(android.R.id.list);
            mSearchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> listView, View view,
                                        int position, long itemId) {
                    String value = mSearchResultsAdapter.getPrefValueAt(position);
                    if (value == null || "".equals(value)) {
                        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                        }
                    }
                    mPreference.setValue(value);
                    dismiss();
                }
            });

            tryBindList();

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(rootView)
                    .create();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            return dialog;
        }

        private void tryBindList() {
            if (mPreference == null) {
                return;
            }

            if (isAdded() && mSearchResultsAdapter == null) {
                mSearchResultsAdapter = new SearchResultsListAdapter();
            }

            if (mSearchResultsAdapter != null && mSearchResultsList != null) {
                mSearchResultsList.setAdapter(mSearchResultsAdapter);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            mQuery = charSequence.toString();
            if (mRestartLoaderHandler.hasMessages(0)) {
                return;
            }

            mRestartLoaderHandler.sendMessageDelayed(
                    mRestartLoaderHandler.obtainMessage(0),
                    QUERY_DELAY_MILLIS);
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }

        private String mQuery;

        private Handler mRestartLoaderHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle args = new Bundle();
                args.putString("query", mQuery);
                getLoaderManager().restartLoader(0, args, LocationChooserDialogFragment.this);
            }
        };

        @Override
        public Loader<List<LocationSearchResult>> onCreateLoader(int id, Bundle args) {
            final String query = args.getString("query");
            return new ResultsLoader(query, getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<LocationSearchResult>> loader,
                                   List<LocationSearchResult> results) {
            mSearchResultsAdapter.changeArray(results);
        }

        @Override
        public void onLoaderReset(Loader<List<LocationSearchResult>> loader) {
            mSearchResultsAdapter.changeArray(null);
        }

        private class SearchResultsListAdapter extends BaseAdapter {
            private List<LocationSearchResult> mResults;

            private SearchResultsListAdapter() {
                mResults = new ArrayList<LocationSearchResult>();
            }

            public void changeArray(List<LocationSearchResult> results) {
                if (results == null) {
                    results = new ArrayList<LocationSearchResult>();
                }

                mResults = results;
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                return Math.max(1, mResults.size());
            }

            @Override
            public Object getItem(int position) {
                if (position == 0 && mResults.size() == 0) {
                    return null;
                }

                return mResults.get(position);
            }

            public String getPrefValueAt(int position) {
                if (position == 0 && mResults.size() == 0) {
                    return "";
                }

                LocationSearchResult result = mResults.get(position);
                return result.woeid + "/" + result.displayName + "/" + result.lat + "/" + result.lng;
            }

            @Override
            public long getItemId(int position) {
                if (position == 0 && mResults.size() == 0) {
                    return -1;
                }

                return mResults.get(position).woeid.hashCode();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup container) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(R.layout.list_item_weather_location_result, container, false);
                }

                if (position == 0 && mResults.size() == 0) {
                    ((TextView) convertView.findViewById(android.R.id.text1))
                            .setText(R.string.pref_weather_location_automatic);
                    ((TextView) convertView.findViewById(android.R.id.text2))
                            .setText(R.string.pref_weather_location_automatic_description);
                } else {
                    LocationSearchResult result = mResults.get(position);
                    ((TextView) convertView.findViewById(android.R.id.text1))
                            .setText(result.displayName);
                    ((TextView) convertView.findViewById(android.R.id.text2))
                            .setText(result.country);
                }

                return convertView;
            }
        }
    }

    /**
     * Loader that fetches location search results from {@link net.imatruck.betterweather.weatherapi.YahooWeatherAPIClient}.
     */
    private static class ResultsLoader extends AsyncTaskLoader<List<LocationSearchResult>> {
        private String mQuery;
        private List<LocationSearchResult> mResults;

        public ResultsLoader(String query, Context context) {
            super(context);
            mQuery = query;
        }

        @Override
        public List<LocationSearchResult> loadInBackground() {
            return YahooPlacesAPIClient.findLocationsAutocomplete(mQuery);
        }

        @Override
        public void deliverResult(List<LocationSearchResult> apps) {
            mResults = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mResults != null) {
                deliverResult(mResults);
            }

            if (takeContentChanged() || mResults == null) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        @Override
        protected void onReset() {
            super.onReset();
            onStopLoading();
        }
    }
}
