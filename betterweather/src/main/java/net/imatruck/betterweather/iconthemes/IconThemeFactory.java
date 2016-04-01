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
package net.imatruck.betterweather.iconthemes;

import net.imatruck.betterweather.BetterWeatherExtension;

public class IconThemeFactory {

    public static IIconTheme getIconThemeFromSetting(String iconThemeSetting){

        if(iconThemeSetting.equals(BetterWeatherExtension.CLIMACONS_ICON_THEME)){
            return new ClimaconsIconTheme();
        }
        else if (iconThemeSetting.equals(BetterWeatherExtension.WEATHERCONS_ICON_THEME)){
            return new WeatherconsIconTheme();
        }
        else if (iconThemeSetting.equals(BetterWeatherExtension.CHAMELEON_ICON_THEME)){
            return new ChameleonIconTheme();
        }
        else if (iconThemeSetting.equals(BetterWeatherExtension.GOOGLENOW_ICON_THEME)){
            return new GoogleNowIconTheme();
        }

        return new ClimaconsIconTheme();

    }

}
