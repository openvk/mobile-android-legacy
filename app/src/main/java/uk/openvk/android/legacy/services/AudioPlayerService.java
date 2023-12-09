package uk.openvk.android.legacy.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.api.entities.Audio;
import uk.openvk.android.legacy.core.listeners.AudioPlayerListener;
import uk.openvk.android.legacy.utils.NotificationManager;

/*  Copyleft © 2022, 2023 OpenVK Team
 *  Copyleft © 2022, 2023 Dmitry Tretyakov (aka. Tinelix)
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU Affero General Public License as published by the Free Software Foundation, either
 *  version 3 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this
 *  program. If not, see https://www.gnu.org/licenses/.
 *
 *  Source code: https://github.com/openvk/mobile-android-legacy
 */

public class AudioPlayerService extends Service implements
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener{

    private static AudioPlayerService instance;
    private NotificationManager notifMan;
    private Context ctx;
    private int currentTrackPos;
    private AudioPlayerBinder binder = new AudioPlayerBinder();
    private boolean isRunnung = false;
    private String url;
    private String title;
    private String artist;
    private MediaPlayer mp;
    private boolean isPlaying;
    private BroadcastReceiver receiver;
    public static final String ACTION_UPDATE_PLAYLIST = "uk.openvk.android.legacy.AP_UPDATE_PLAYLIST";
    public static final String ACTION_UPDATE_CURRENT_TRACKPOS = "uk.openvk.android.legacy.AP_UPDATE_CURRENT_TRACKPOS";
    public static final String ACTION_PLAYER_CONTROL = "uk.openvk.android.legacy.AP_CONTROL";
    public static final int STATUS_STARTING = 1000;
    public static final int STATUS_PLAYING = 1001;
    public static final int STATUS_PAUSED = 1002;
    public static final int STATUS_STOPPED = 1003;
    public static final int STATUS_GOTO_PREVIOUS = 1004;
    public static final int STATUS_GOTO_NEXT = 1005;
    public static final int STATUS_REPEATING = 1006;
    public static final int STATUS_SHUFFLE = 1007;

    private Audio[] playlist;
    private int playerStatus;

    public AudioPlayerService() {

    }

    public class AudioPlayerBinder extends Binder {
        public AudioPlayerListener listener;
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }

        public void setAudioPlayerListener(AudioPlayerListener listener) {
            this.listener = listener;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean isRunning() {
        return isRunnung;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        binder = new AudioPlayerBinder();
        onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Bundle data = intent.getExtras();
            if (data != null) {
                String action = data.getString("action");
                Log.d(OvkApplication.APP_TAG, String.format("Starting AudioPlayerService by ID: %s | Action: %s", startId, action));
                isRunnung = true;
                if(action != null) {
                    switch (action) {
                        case "PLAYER_CREATE":
                            if(mp == null) createMediaPlayer();
                            break;
                        case "PLAYER_GET_CURRENT_POSITION":
                            notifyPlayerStatus();
                            break;
                        case "PLAYER_START":
                            int position = data.getInt("position");
                            currentTrackPos = position;
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            ArrayList<Audio> parcelablePlaylist =
                                    data.getParcelableArrayList("playlist");
                            if(parcelablePlaylist != null) {
                                playlist = new Audio[parcelablePlaylist.size()];
                                parcelablePlaylist.toArray(playlist);
                                startPlaylistFromPosition(position);
                            }
                            break;
                        case "PLAYER_PLAY":
                            mp.start();
                            notifyPlayerStatus(AudioPlayerService.STATUS_PLAYING);
                            break;
                        case "PLAYER_PAUSE":
                            mp.pause();
                            notifyPlayerStatus(AudioPlayerService.STATUS_PAUSED);
                            break;
                        case "PLAYER_STOP":
                            mp.stop();
                            notifyPlayerStatus(AudioPlayerService.STATUS_STOPPED);
                            break;
                        case "PLAYER_PREVIOUS":
                            if(currentTrackPos > 0) {
                                currentTrackPos--;
                                startPlaylistFromPosition(currentTrackPos);
                            } else {
                                currentTrackPos = playlist.length - 1;
                                startPlaylistFromPosition(currentTrackPos);
                            }
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            break;
                        case "PLAYER_NEXT":
                            if(currentTrackPos < playlist.length - 1) {
                                currentTrackPos++;
                                startPlaylistFromPosition(currentTrackPos);
                            } else {
                                currentTrackPos = 0;
                                startPlaylistFromPosition(currentTrackPos);
                            }
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createMediaPlayer() {
        if(mp != null) {
            mp.stop();
        }
        mp = new MediaPlayer();
        if(Build.VERSION.SDK_INT >= 26)
            mp.setAudioAttributes(
                    new AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
        else
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        try {
            if (mediaPlayer.getDuration() > 0) {
                if (currentTrackPos < playlist.length - 1) {
                    int position = currentTrackPos + 1;
                    createMediaPlayer();
                    currentTrackPos = position;
                    notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                    mp.setOnCompletionListener(this);
                    mp.setOnErrorListener(this);
                    mp.setOnBufferingUpdateListener(this);
                    mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mp.start();
                            notifyPlayerStatus(STATUS_PLAYING);
                        }
                    });
                    mp.setDataSource(playlist[position].url);
                    mp.prepareAsync();
                } else {
                    mp.stop();
                    notifyPlayerStatus(STATUS_STOPPED);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e(OvkApplication.APP_TAG, "AudioPlayerService: Invalid stream");
        return false;
    }

    public int getCurrentTrackPosision() {
        return currentTrackPos;
    }

    private void startPlaylistFromPosition(int track_position) {
        try {
            if(mp.isPlaying())
                mp.release();
            mp = new MediaPlayer();
            currentTrackPos = track_position;
            if(playlist[track_position].url != null) {
                mp.setDataSource(playlist[track_position].url);
                mp.prepareAsync();
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mp.start();
                        notifyPlayerStatus(STATUS_PLAYING);
                    }
                });
                mp.setOnCompletionListener(this);
                mp.setOnErrorListener(this);
                mp.setOnBufferingUpdateListener(this);
            } else {
                Log.e(OvkApplication.APP_TAG, "AudioPlayerService: Invalid Track URL");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyPlayerStatus(int status) {
        this.playerStatus = status;
        Intent intent = new Intent();
        intent.setAction(AudioPlayerService.ACTION_PLAYER_CONTROL);
        intent.putExtra("status", status);
        intent.putExtra("track_position", currentTrackPos);
        sendBroadcast(intent);
    }

    private void notifyPlayerStatus() {
        Intent intent = new Intent();
        intent.setAction(AudioPlayerService.ACTION_UPDATE_CURRENT_TRACKPOS);
        intent.putExtra("status", this.playerStatus);
        intent.putExtra("track_position", currentTrackPos);
        sendBroadcast(intent);
    }

    public static AudioPlayerService getInstance() {
        return instance;
    }
}