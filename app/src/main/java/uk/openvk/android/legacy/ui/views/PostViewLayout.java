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

package uk.openvk.android.legacy.ui.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;
import uk.openvk.android.client.attachments.Attachment;
import uk.openvk.android.client.entities.Poll;
import uk.openvk.android.client.entities.Video;
import uk.openvk.android.client.entities.Comment;
import uk.openvk.android.client.entities.OvkLink;
import uk.openvk.android.client.entities.Photo;
import uk.openvk.android.legacy.core.activities.VideoPlayerActivity;
import uk.openvk.android.legacy.core.activities.WallPostActivity;
import uk.openvk.android.legacy.ui.list.adapters.CommentsListAdapter;
import uk.openvk.android.client.entities.WallPost;
import uk.openvk.android.legacy.ui.views.attach.PollAttachView;
import uk.openvk.android.legacy.ui.views.attach.VideoAttachView;

@SuppressWarnings("ConstantConditions")
public class PostViewLayout extends LinearLayout {
    private final String instance;
    private ImageLoader imageLoader;
    private ImageLoaderConfiguration imageLoaderConfig;
    private DisplayImageOptions displayimageOptions;
    private View headerView;
    private int param = 0;
    public TextView titlebar_title;
    public String state;
    public JSONArray newsfeed;
    public String send_request;
    public SharedPreferences global_prefs;
    private CommentsListAdapter commentsAdapter;
    private RecyclerView commentsView;
    private LinearLayoutManager llm;
    private ArrayList<Comment> comments;

    public PostViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        View view =  LayoutInflater.from(getContext()).inflate(
                R.layout.layout_post_view, null);

        this.addView(view);

        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        view.setLayoutParams(layoutParams);
        instance = PreferenceManager.getDefaultSharedPreferences(
                getContext()).getString("current_instance", "");
        global_prefs = android.support.v7.preference
                .PreferenceManager.getDefaultSharedPreferences(getContext());
        this.displayimageOptions =
                new DisplayImageOptions.Builder().bitmapConfig(Bitmap.Config.ARGB_8888).build();
        this.imageLoaderConfig =
                new ImageLoaderConfiguration.Builder(context.getApplicationContext()).
                        defaultDisplayImageOptions(displayimageOptions)
                        .memoryCacheSize(16777216) // 16 MB memory cache
                        .writeDebugLogs()
                        .build();
        if (ImageLoader.getInstance().isInited()) {
            ImageLoader.getInstance().destroy();
        }
        this.imageLoader = ImageLoader.getInstance();
        imageLoader.init(imageLoaderConfig);
    }

    public void createAdapter(Context ctx, ArrayList<Comment> comments) {
        TextView no_comments_text = findViewById(R.id.no_comments_text);
        this.comments = comments;
        commentsAdapter = new CommentsListAdapter(ctx, comments);
        commentsView = findViewById(R.id.comments_list);
        if(comments.size() > 0) {
            no_comments_text.setVisibility(GONE);
            commentsView.setVisibility(VISIBLE);
        } else {
            no_comments_text.setVisibility(VISIBLE);
            commentsView.setVisibility(GONE);
        }
        llm = new LinearLayoutManager(ctx);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        commentsView.setLayoutManager(llm);
        commentsView.setAdapter(commentsAdapter);
    }


    public int getCount() {
        try {
            return commentsView.getAdapter().getItemCount();
        } catch (NullPointerException npE) {
            return 0;
        }
    }

    public void loadAvatars() {
        if(commentsAdapter != null) {
            commentsView = findViewById(R.id.comments_list);
            for (int i = 0; i < getCount(); i++) {
                try {
                    Comment item = comments.get(i);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(
                            String.format("%s/%s/photos_cache/comment_avatars/avatar_%s",
                                    getContext().getCacheDir(), instance, item.author_id), options);
                    if (bitmap != null) {
                        item.avatar = bitmap;
                    } else {
                        Log.e(OvkApplication.APP_TAG, String.format(
                                "'%s/%s/photos_cache/comment_avatars/avatar_%d' not found",
                                getContext().getCacheDir(), instance, item.author_id));
                    }
                    comments.set(i, item);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            commentsAdapter.notifyDataSetChanged();
        }
    }

    public void setPost(WallPost item, final Context ctx) {
        ((TextView) findViewById(R.id.wall_view_poster_name)).setText(item.author_name);
        ArrayList<WallPost> posts = new ArrayList<WallPost>();
        posts.add(item);

        ((PostAttachmentsView) findViewById(R.id.post_attach_container))
                .loadAttachments(ctx, posts, item, imageLoader, item.attachments, 0);
        if(!item.is_explicit || !global_prefs.getBoolean("safeViewing", true)) {
            if (item.text.length() > 0) {
                String text = item.text;
                Pattern pattern = Pattern.compile("\\[(.+?)\\]|" +
                        "((http|https)://)(www.)?[a-zA-Z0-9@:%._\\+~#?&//=]" +
                        "{1,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)");
                Matcher matcher = pattern.matcher(text);
                boolean regexp_search = matcher.find();
                int regexp_results = 0;
                text = item.text.replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">")
                        .replaceAll("&amp;", "&")
                        .replaceAll("&quot;", "\"");
                while (regexp_search) {
                    if (regexp_results == 0) {
                        text = text.replace("\n", "<br>");
                    }
                    String block = matcher.group();
                    if (block.startsWith("[") && block.endsWith("]")) {
                        OvkLink link = new OvkLink();
                        String[] markup = block
                                .replace("[", "").replace("]", "")
                                .split("\\|");
                        link.screen_name = markup[0];
                        if (markup.length == 2) {
                            if (markup[0].startsWith("id")) {
                                link.url = String.format("openvk://ovk/id%s", markup[0]);
                                link.name = markup[1];
                            } else if (markup[0].startsWith("club")) {
                                link.url = String.format("openvk://ovk/club%s", markup[0]);
                                link.name = markup[1];
                            }
                            link.name = markup[1];
                            if (markup[0].startsWith("id") || markup[0].startsWith("club")) {
                                text = text.replace(block,
                                        String.format("<a href=\"%s\">%s</a>", link.url, link.name));
                            }
                        }
                    } else if (block.startsWith("https://") || block.startsWith("http://")) {
                        text = text.replace(block, String.format("<a href=\"%s\">%s</a>", block, block));
                    }
                    regexp_results = regexp_results + 1;
                    regexp_search = matcher.find();
                }
                if (regexp_results > 0) {
                    ((TextView) findViewById(R.id.post_view)).setAutoLinkMask(0);
                    ((TextView) findViewById(R.id.post_view)).setText(Html.fromHtml(text));
                } else {
                    ((TextView) findViewById(R.id.post_view)).setText(text);
                }
                ((TextView) findViewById(R.id.post_view)).setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                findViewById(R.id.post_view).setVisibility(GONE);
            }
            ((TextView) findViewById(R.id.wall_view_time)).setText(Global.formatTimestamp(ctx, item.dt.getTime()));

            if (item.avatar != null) {
                ((ImageView) findViewById(R.id.wall_user_photo)).setImageBitmap(item.avatar);
            }

        } else {
            TextView error_label = findViewById(R.id.error_label);
            error_label.setText(ctx.getResources().getString(R.string.post_load_nsfw));
            error_label.setVisibility(View.VISIBLE);
            (findViewById(R.id.post_view)).setVisibility(GONE);
        }

        if (item.counters != null) {
            ((TextView) findViewById(R.id.wall_view_like)).setText(String.format("%s",
                    item.counters.likes));
        }

    }

    public void loadWallAvatar(long author_id, String where) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = null;
            if(where.equals("newsfeed")) {
                bitmap = BitmapFactory.decodeFile(
                        String.format("%s/%s/photos_cache/newsfeed_avatars/avatar_%s",
                        getContext().getCacheDir(), instance, author_id), options);
            } else {
                bitmap = BitmapFactory.decodeFile(
                        String.format("%s/%s/photos_cache/wall_avatars/avatar_%s",
                        getContext().getCacheDir(), instance, author_id), options);
            }
            if (bitmap != null) {
                ((ImageView) findViewById(R.id.wall_user_photo)).setImageBitmap(bitmap);
                Log.e(OvkApplication.APP_TAG,
                        String.format("'%s/%s/photos_cache/wall_avatars/avatar_%d' not found",
                        getContext().getCacheDir(), instance, author_id));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void adjustLayoutSize(int orientation) {
        if (((OvkApplication) getContext().getApplicationContext()).isTablet) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.LayoutParams layoutParams = new LayoutParams((int)
                        (600 * (getResources().getDisplayMetrics().density)),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                findViewById(R.id.post_with_comments_view_ll)
                        .setLayoutParams(layoutParams);
            } else {
                LinearLayout.LayoutParams layoutParams = new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                findViewById(R.id.post_with_comments_view_ll)
                        .setLayoutParams(layoutParams);
            }
        } else {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.LayoutParams layoutParams = new LayoutParams((int)
                        (480 * (getResources().getDisplayMetrics().density)),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                findViewById(R.id.post_with_comments_view_ll)
                        .setLayoutParams(layoutParams);
            } else {
                LinearLayout.LayoutParams layoutParams = new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                findViewById(R.id.post_with_comments_view_ll).setLayoutParams(layoutParams);
            }
        }
    }

    public void loadPhotos() {
        if(commentsAdapter != null) {
            commentsView = findViewById(R.id.comments_list);
            for (int i = 0; i < getCount(); i++) {
                try {
                    Comment item = comments.get(i);
                    if(item.attachments.size() > 0) {
                        for (int attachment_index = 0; attachment_index <
                                item.attachments.size(); attachment_index++) {
                            if (item.attachments.get(attachment_index).type.equals("photo")) {
                                Photo photo = ((Photo) item.attachments.get(0));
                                Attachment attachment = item.attachments.get(0);
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                if (photo.url.length() > 0) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(
                                            String.format(
                                                    "%s/%s/photos_cache/comment_photos/comment_photo_o%sp%s",
                                                    getContext().getCacheDir(), instance,
                                                    item.author_id, item.id), options);
                                    if (bitmap != null) {
                                        photo.bitmap = bitmap;
                                        attachment.status = "done";
                                        item.attachments.set(i, attachment);
                                    } else if (photo.url.length() > 0) {
                                        Log.e(OvkApplication.APP_TAG,
                                                "Loading photo error in comments");
                                        attachment.status = "error";
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e(OvkApplication.APP_TAG,
                                "No photo attachments");
                    }
                    comments.set(i, item);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            commentsAdapter.notifyDataSetChanged();
        }
    }
}