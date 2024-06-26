/*
 *  Copyleft © 2022, 2023, 2024 OpenVK Team
 *  Copyleft © 2022, 2023, 2024 Dmitry Tretyakov (aka. Tinelix)
 *
 *  This file is part of OpenVK Legacy for Android.
 *
 *  OpenVK Legacy for Android is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this
 *  program. If not, see https://www.gnu.org/licenses/.
 *
 *  Source code: https://github.com/openvk/mobile-android-legacy
 */

package uk.openvk.android.legacy.databases;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Vector;

import uk.openvk.android.client.entities.Audio;
import uk.openvk.android.client.entities.User;
import uk.openvk.android.legacy.databases.base.CacheDatabase;
import uk.openvk.android.legacy.services.AudioPlayerService;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class AudioCacheDB extends CacheDatabase {
    public static Vector<String> cachedIDs = new Vector<>();
    public static Vector<String> cacheReqs = new Vector<>();
    private static Context ctx;
    public static String prefix = "audio";

    public void putTrack(Context ctx, Audio track, boolean forced, boolean intoSearchResults) {
        CacheOpenHelper helper2 = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
        SQLiteDatabase db2 = helper2.getWritableDatabase();
        if (!isExist(ctx, track.id, intoSearchResults)) {
            if (!cachedIDs.contains(String.valueOf(track.owner_id) + "_" + track.id)) {
                cachedIDs.add(String.valueOf(track.owner_id) + "_" + track.id);
            }
            Intent intent = new Intent(AudioPlayerService.ACTION_UPDATE_PLAYLIST);
            ctx.getApplicationContext().sendBroadcast(intent);
            try {
                ContentValues values2 = new ContentValues();
                values2.put("audio_id", track.id);
                values2.put("owner_id", track.owner_id);
                values2.put("artist", track.artist);
                values2.put("duration", track.getDurationInSeconds());
                values2.put("lastplay", System.currentTimeMillis() / 1000);
                values2.put("user", forced);
                values2.put("lyrics", track.lyrics);
                String table_name = "tracks";
                if(intoSearchResults) {
                    table_name = "search_results";
                }
                db2.insert(table_name, null, values2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (forced) {
            CacheOpenHelper helper = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
            SQLiteDatabase db = helper.getWritableDatabase();
            try {
                ContentValues values = new ContentValues();
                values.put("user", true);
                String table_name = "tracks";
                if(intoSearchResults) {
                    table_name = "search_results";
                }
                db.update(table_name, values,
                        "audio_id=" + track.id
                                + " and owner_id=" + track.owner_id,
                        null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        db2.close();
        helper2.close();
    }

    private boolean isExist(Context ctx, long track_id, boolean inSearchResults) {
        boolean result = false;
        CacheOpenHelper helper = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            String table_name = "tracks";
            if(inSearchResults) {
                table_name = "search_results";
            }
            Cursor cursor = db.query(table_name, new String[]{"count(*)"},
                    "`audio_id`=" + track_id,
                    null, null, null, null);
            result = cursor.getCount() > 0 && cursor.moveToFirst() && cursor.getInt(0) > 0;
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.close();
        helper.close();
        return result;
    }

    public static void fillDatabase(Context ctx2, ArrayList<Audio> audios,
                                    boolean clear, boolean intoSearchResults) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx2, getCurrentDatabaseName(ctx2, "audio"));

        Cursor cursor = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            if (clear) {
                cachedIDs.clear();
            }
            CacheDatabaseTables.createAudioTracksTable(db, clear);
            String table_name = "tracks";
            if(intoSearchResults) {
                table_name = "search_results";
            }
            cursor = db.query(table_name, new String[]{"owner_id", "audio_id"},
                    null, null, null, null, null);
            cursor.moveToFirst();

            for (int i = 0; i < audios.size(); i++) {
                Audio track = audios.get(i);
                ContentValues values = new ContentValues();
                values.put("audio_id", track.id);
                values.put("owner_id", track.owner_id);
                values.put("title", track.title);
                values.put("artist", track.artist);
                values.put("duration", track.getDurationInSeconds());
                values.put("lastplay", 0);
                values.put("user", true);
                values.put("lyrics", track.lyrics);
                values.put("url", track.url);
                values.put("status", track.status);
                db.insert(table_name, null, values);
                String track_name = String.format("%s_%s", track.id, track.owner_id);
                cachedIDs.add(track_name);
            }
            db.close();
            helper.close();
            cursor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void fillDatabaseFromWall(Context ctx2, ArrayList<Audio> audios,
                                            long post_id, boolean clear) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx2, getCurrentDatabaseName(ctx2, "audio"));

        Cursor cursor = null;
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            if (clear) {
                cachedIDs.clear();
            }
            CacheDatabaseTables.createAudioTracksTable(db, clear);
            cursor = db.query("wall_tracks", new String[]{"owner_id", "audio_id", "post_id"},
                    null, null, null, null, null);
            cursor.moveToFirst();

            for (int i = 0; i < audios.size(); i++) {
                Audio track = audios.get(i);
                ContentValues values = new ContentValues();
                values.put("audio_id", track.id);
                values.put("post_id", post_id);
                values.put("owner_id", track.owner_id);
                values.put("title", track.title);
                values.put("artist", track.artist);
                values.put("duration", track.getDurationInSeconds());
                values.put("lastplay", 0);
                values.put("user", true);
                values.put("lyrics", track.lyrics);
                values.put("url", track.url);
                values.put("status", track.status);
                db.insert("wall_tracks", null, values);
                String track_name = String.format("%s_%s", track.id, track.owner_id);
                cachedIDs.add(track_name);
            }
            db.close();
            helper.close();
            cursor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static ArrayList<Audio> getCachedAudiosList(Context ctx2, boolean fromSearchResults) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx2, getCurrentDatabaseName(ctx2, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        ArrayList<Audio> list = new ArrayList<>();
        try {
            String table_name = "tracks";
            if(fromSearchResults) {
                table_name = "search_results";
            }
            Cursor cursor = db.query(table_name, null, null,
                    null, null, null, "user desc");
            cursor.moveToFirst();
            int i = 0;
            do {
                Audio track = new Audio();
                track.sender = new User();
                track.sender.id = cursor.getInt(0);
                track.owner_id = cursor.getInt(0);
                track.id = cursor.getInt(1);
                track.title = cursor.getString(2);
                track.artist = cursor.getString(3);
                track.setDuration(cursor.getInt(4));
                track.lyrics = cursor.getLong(7);
                track.url = cursor.getString(8);
                list.add(track);
                i++;
            } while (cursor.moveToNext());
            cursor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.close();
        helper.close();
        ctx = ctx2;
        deleteOldTrack(ctx);
        return list;
    }

    public static ArrayList<Audio> getPersonalAudiosList(Context ctx2, long owner_id) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx2, getCurrentDatabaseName(ctx2, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        ArrayList<Audio> list = new ArrayList<>();
        try {
            Cursor cursor = db.query("tracks", null,
                    String.format("owner_id = %s", owner_id),
                    null, null, null, "user desc");
            cursor.moveToFirst();
            int i = 0;
            do {
                Audio track = new Audio();
                track.sender = new User();
                track.sender.id = cursor.getInt(1);
                track.owner_id = cursor.getInt(1);
                track.id = cursor.getInt(1);
                track.title = cursor.getString(2);
                track.artist = cursor.getString(3);
                track.setDuration(cursor.getInt(4));
                track.lyrics = cursor.getLong(7);
                track.url = cursor.getString(8);
                list.add(track);
                i++;
            } while (cursor.moveToNext());
            cursor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.close();
        helper.close();
        ctx = ctx2;
        deleteOldTrack(ctx);
        return list;
    }

    private static void deleteOldTrack(Context ctx) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            Cursor cursor = db.query("tracks",
                    null, "user=0", null, null, null,
                    "lastplay asc");
            cursor.moveToFirst();
            if (cursor.getCount() > 10) {
                int nDel = cursor.getCount() - 10;
                String where = "";
                for (int i = 0; i < nDel; i++) {
                    where = String.valueOf(where) + "or(owner_id=" + cursor.getInt(0) +
                            " AND audio_id=" + cursor.getInt(1) + ")";
                    cachedIDs.remove(String.valueOf(cursor.getInt(0)) + "_" + cursor.getInt(1));
                    cursor.moveToNext();
                }
                cursor.close();
                db.delete("tracks", where.substring(2), null);
                Intent intent = new Intent(AudioPlayerService.ACTION_UPDATE_PLAYLIST);
                intent.putExtra("reload_cached_list", true);
                ctx.getApplicationContext().sendBroadcast(intent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.close();
        helper.close();
    }

    public static void clear(Context ctx, boolean fromSearchOnly) {
        if(getCurrentDatabaseName(ctx, prefix) != null) {
            CacheOpenHelper helper = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
            SQLiteDatabase db = helper.getWritableDatabase();
            try {
                if (fromSearchOnly) {
                    db.delete("search_results", null, null);
                } else {
                    db.delete("tracks", null, null);
                }
                cachedIDs.clear();
                Intent intent = new Intent(AudioPlayerService.ACTION_UPDATE_PLAYLIST);
                intent.putExtra("reload_cached_list", true);
                ctx.getApplicationContext().sendBroadcast(intent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            db.close();
            helper.close();
        }
    }

    public static void updatePlayTime(Context ctx, int owner_id, int audio_id) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx, getCurrentDatabaseName(ctx, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("lastplay", (System.currentTimeMillis() / 1000));
            db.update("files", values,
                    "audio_id=" + audio_id + " and owner_id=" + owner_id,
                    null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.close();
        helper.close();
    }

    public static ArrayList<Audio> getAudiosListFromWall(Context ctx2, long post_id) {
        CacheOpenHelper helper = new CacheOpenHelper(ctx2, getCurrentDatabaseName(ctx2, prefix));
        SQLiteDatabase db = helper.getWritableDatabase();
        ArrayList<Audio> list = new ArrayList<>();
        try {
            Cursor cursor = db.query("wall_tracks", null,
                    String.format("post_id = %s", post_id),
                    null, null, null, "user desc");
            cursor.moveToFirst();
            int i = 0;
            do {
                Audio track = new Audio();
                track.sender = new User();
                track.sender.id = cursor.getInt(0);
                track.owner_id = cursor.getInt(0);
                track.id = cursor.getInt(1);
                track.title = cursor.getString(2);
                track.artist = cursor.getString(3);
                track.setDuration(cursor.getInt(4));
                track.lyrics = cursor.getLong(8);
                track.url = cursor.getString(9);
                list.add(track);
                i++;
            } while (cursor.moveToNext());
            cursor.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        db.close();
        helper.close();
        ctx = ctx2;
        deleteOldTrack(ctx);
        return list;
    }
}
