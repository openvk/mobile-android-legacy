package uk.openvk.android.legacy.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

/**
 * OPENVK LEGACY LICENSE NOTIFICATION
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program. If not, see https://www.gnu.org/licenses/.
 *
 * Source code: https://github.com/openvk/mobile-android-legacy
 */

public class MediaPlayer {
    private final Context ctx;

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ovkmplayer");
    }

    private native String testString();

    public MediaPlayer(Context ctx) {
        this.ctx = ctx;
        Toast.makeText(ctx, testString(), Toast.LENGTH_LONG).show();
    }
}
