/*
 * Copyright 2013 Marc-André Dufresne
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static net.imatruck.betterweather.LogUtils.LOGD;

public class WeatherRefreshReceiver extends BroadcastReceiver {

    private static final String TAG = LogUtils.makeLogTag(WeatherRefreshReceiver.class);

    public static PendingIntent getPendingIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, WeatherRefreshReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static void cancelPendingIntent(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getPendingIntent(context));
        LOGD(TAG, "Weather refresh was canceled");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.sendBroadcast(new Intent(BetterWeatherExtension.REFRESH_INTENT_FILTER));
    }

}
