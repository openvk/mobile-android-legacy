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
import java.util.List;

import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.client.entities.Audio;
import uk.openvk.android.legacy.core.listeners.AudioPlayerListener;
import uk.openvk.android.legacy.databases.AudioCacheDB;
import uk.openvk.android.legacy.utils.NotificationManager;

public class AudioPlayerService extends Service implements
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnErrorListener{

    private int currentTrackPos;
    private AudioPlayerBinder binder = new AudioPlayerBinder();
    private boolean isRunning = false;
    private MediaPlayer mp;
    private boolean isPlaying;
    private boolean isPrepared;
    private boolean isStarted;
    private BroadcastReceiver receiver;
    public static final String ACTION_UPDATE_PLAYLIST = "uk.openvk.android.legacy.AP_UPDATE_PLAYLIST";
    public static final String ACTION_UPDATE_CURRENT_TRACKPOS = "uk.openvk.android.legacy.AP_UPDATE_CURRENT_TRACKPOS";
    public static final String ACTION_UPDATE_SEEKPOS = "uk.openvk.android.legacy.AP_UPDATE_SEEKPOS";
    public static final String ACTION_PLAYER_CONTROL = "uk.openvk.android.legacy.AP_CONTROL";
    public static final int STATUS_STARTING_FROM_WALL = 1000;
    public static final int STATUS_STARTING = 1001;
    public static final int STATUS_PLAYING = 1002;
    public static final int STATUS_PAUSED = 1003;
    public static final int STATUS_STOPPED = 1004;
    public static final int STATUS_GOTO_PREVIOUS = 1005;
    public static final int STATUS_GOTO_NEXT = 1006;
    public static final int STATUS_REPEATING = 1007;
    public static final int STATUS_SHUFFLE = 1008;
    public static final int STATUS_SEEKING = 1009;
    List<AudioPlayerListener> listeners = new ArrayList<>();

    private Audio[] playlist;
    private int playerStatus;
    private double bufferLength;
    private int error_count;

    public AudioPlayerService() {

    }

    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        notifySeekbarStatus();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    public class AudioPlayerBinder extends Binder {
        public AudioPlayerListener listener;
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    public interface AudioPlayerListener {
        public void onChangeAudioPlayerStatus(String action, int status, int track_pos, Bundle data);
        public void onReceiveCurrentTrackPosition(int track_pos, int status);
        public void onUpdateSeekbarPosition(int position, int duration, double buffer_length);
        public void onAudioPlayerError(int what, int extra, int current_track_pos);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean isRunning() {
        return isRunning;
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
                Log.d(OvkApplication.APS_TAG, String.format("Starting AudioPlayerService by ID: %s | Action: %s", startId, action));
                isRunning = true;
                if(action != null) {
                    switch (action) {
                        case "PLAYER_CREATE":
                            if(mp == null) createMediaPlayer();
                            break;
                        case "PLAYER_GET_CURRENT_POSITION":
                            notifyPlayerStatus();
                            break;
                        case "PLAYER_GET_SEEKBAR_POSITION":
                            notifySeekbarStatus();
                            break;
                        case "PLAYER_START":
                            isPlaying = false;
                            String from = data.getString("from");
                            int position = data.getInt("position");
                            currentTrackPos = position;
                            boolean fromSearch = from != null && from.equals("search");
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            ArrayList<Audio> parcelablePlaylist =
                                    AudioCacheDB.getCachedAudiosList(this, fromSearch);
                            if(parcelablePlaylist != null) {
                                if(parcelablePlaylist.size() > 0) {
                                    Log.d(OvkApplication.APS_TAG,
                                            String.format(
                                                    "Starting playback (%s of %s) from %s...",
                                                    position + 1,
                                                    parcelablePlaylist.size(),
                                                    fromSearch ? "search results" : "playlist"
                                            )
                                    );
                                    playlist = new Audio[parcelablePlaylist.size()];
                                    parcelablePlaylist.toArray(playlist);
                                    startPlaylistFromPosition(position);
                                } else {
                                    Log.e(OvkApplication.APS_TAG, "Playlist is empty. Canceling...");
                                }
                            }
                            break;
                        case "PLAYER_START_FROM_WALL":
                            isPlaying = false;
                            position = data.getInt("position");
                            currentTrackPos = position;
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            long post_id = data.getLong("post_id");
                            parcelablePlaylist =
                                    AudioCacheDB.getAudiosListFromWall(this, post_id);
                            if(parcelablePlaylist != null) {
                                playlist = new Audio[parcelablePlaylist.size()];
                                parcelablePlaylist.toArray(playlist);
                                startPlaylistFromPosition(position);
                            } else {
                                Log.e(OvkApplication.APS_TAG, "Playlist is empty. Canceling...");
                            }
                            break;
                        case "PLAYER_PLAY":
                            mp.start();
                            notifyPlayerStatus(AudioPlayerService.STATUS_PLAYING);
                            isPlaying = true;
                            break;
                        case "PLAYER_PAUSE":
                            mp.pause();
                            notifyPlayerStatus(AudioPlayerService.STATUS_PAUSED);
                            isPlaying = false;
                            break;
                        case "PLAYER_STOP":
                            mp.stop();
                            notifyPlayerStatus(AudioPlayerService.STATUS_STOPPED);
                            isPlaying = false;
                            isPrepared = false;
                            stopSelf();
                            break;
                        case "PLAYER_PREVIOUS":
                            isPlaying = false;
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
                            isPlaying = false;
                            if(currentTrackPos < playlist.length - 1) {
                                currentTrackPos++;
                                startPlaylistFromPosition(currentTrackPos);
                            } else {
                                currentTrackPos = 0;
                                startPlaylistFromPosition(currentTrackPos);
                            }
                            notifyPlayerStatus(AudioPlayerService.STATUS_STARTING);
                            break;
                        case "PLAYER_SEEK":
                            int seek_position = data.getInt("seek_position");
                            mp.seekTo(seek_position);
                            break;
                        case "PLAYER_CONNECT":
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
        isPrepared = true;
        if(mp != null) {
            mp.reset();
        } else {
            mp = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= 26)
                mp.setAudioAttributes(
                        new AudioAttributes
                                .Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                );
            else
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        bufferLength = percent * (mediaPlayer.getDuration() / 100);
        notifySeekbarStatus();
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
                            isPlaying = true;
                            isPrepared = true;
                        }
                    });
                    mp.setDataSource(playlist[position].url);
                    if(isPrepared)
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
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.e(OvkApplication.APS_TAG, String.format("Invalid track stream (w: %s, x: %s)", what, extra));
        if((what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == MediaPlayer.MEDIA_ERROR_IO)
                || what == -38) {
            for(int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onAudioPlayerError(what, extra, currentTrackPos);
            }
            if(isPrepared && isPlaying)
                mp.reset();
            return true;
        }
        return false;
    }

    public int getCurrentTrackPosision() {
        return currentTrackPos;
    }

    private void startPlaylistFromPosition(int track_position) {
        try {
            if(playlist[track_position].id == 0) {
                track_position++;
                startPlaylistFromPosition(track_position);
                return;
            }
            createMediaPlayer();
            currentTrackPos = track_position;
            if(playlist[track_position].url != null) {
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mp.start();
                        notifyPlayerStatus(STATUS_PLAYING);
                        isPlaying = true;
                        isPrepared = true;
                    }
                });
                mp.setOnCompletionListener(this);
                mp.setOnErrorListener(this);
                mp.setOnBufferingUpdateListener(this);
                mp.setDataSource(playlist[track_position].url);
                if(isPrepared)
                    mp.prepareAsync();
            } else {
                Log.e(OvkApplication.APS_TAG, "Invalid Track URL");
                for(int i = 0; i < listeners.size(); i++) {
                    listeners.get(i).onAudioPlayerError(
                            MediaPlayer.MEDIA_ERROR_UNKNOWN,
                            0,
                            currentTrackPos);
                }
            }
        } catch (IOException e) {
            Audio audio = playlist[track_position];
            Log.e(OvkApplication.APS_TAG,
                    String.format("Can't play from URL: %s (%s - %s)",
                            audio.url, audio.artist, audio.title)
            );
            for(int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onAudioPlayerError(
                        MediaPlayer.MEDIA_ERROR_UNKNOWN,
                        0,
                        currentTrackPos);
            }
            e.printStackTrace();
        }
    }

    private void notifyPlayerStatus(int status) {
        this.playerStatus = status;
        Intent intent = new Intent();
        String action = AudioPlayerService.ACTION_PLAYER_CONTROL;
        intent.setAction(action);
        intent.putExtra("status", status);
        intent.putExtra("track_position", currentTrackPos);
        sendBroadcast(intent);
        for(int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onChangeAudioPlayerStatus(
                    action, playerStatus, currentTrackPos, intent.getExtras()
            );
        }
    }

    public void notifyPlayerStatus() {
        Intent intent = new Intent();
        String action = AudioPlayerService.ACTION_UPDATE_CURRENT_TRACKPOS;
        intent.setAction(action);
        intent.putExtra("status", this.playerStatus);
        intent.putExtra("track_position", currentTrackPos);
        sendBroadcast(intent);
        for(int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onReceiveCurrentTrackPosition(currentTrackPos, playerStatus);
        }
    }

    public void notifySeekbarStatus() {
        if(isPlaying) {
            Intent intent = new Intent();
            String action = AudioPlayerService.ACTION_UPDATE_SEEKPOS;
            intent.setAction(action);
            intent.putExtra("progress", mp.getCurrentPosition());
            intent.putExtra("duration", mp.getDuration());
            intent.putExtra("buffer_length", bufferLength);
            sendBroadcast(intent);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onUpdateSeekbarPosition(
                        mp.getCurrentPosition(), mp.getDuration(), bufferLength
                );
            }
        }
    }

    public void addListener(AudioPlayerListener listener) {
        if(listeners == null)
             listeners = new ArrayList<>();
        listeners.add(listener);
    }

    public void removeListener(AudioPlayerListener listener) {
        if(listeners != null)
            listeners.remove(listener);
    }
}
