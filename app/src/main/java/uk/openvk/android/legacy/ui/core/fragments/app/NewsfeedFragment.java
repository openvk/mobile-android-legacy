package uk.openvk.android.legacy.ui.core.fragments.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.json.JSONArray;

import java.util.ArrayList;

import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.attachments.Attachment;
import uk.openvk.android.legacy.api.attachments.PhotoAttachment;
import uk.openvk.android.legacy.ui.core.activities.AppActivity;
import uk.openvk.android.legacy.ui.core.listeners.OnScrollListener;
import uk.openvk.android.legacy.ui.list.adapters.NewsfeedAdapter;
import uk.openvk.android.legacy.api.models.WallPost;
import uk.openvk.android.legacy.ui.view.InfinityScrollView;

public class NewsfeedFragment extends Fragment {
    private View headerView;
    private int param = 0;
    public TextView titlebar_title;
    public String state;
    public JSONArray newsfeed;
    public String send_request;
    public SharedPreferences global_sharedPreferences;
    private NewsfeedAdapter newsfeedAdapter;
    private RecyclerView newsfeedView;
    private ListView newsfeedListView;
    private LinearLayoutManager llm;
    private ArrayList<WallPost> wallPosts;
    public boolean loading_more_posts = false;
    private int pastComplVisiblesItems;
    private Parcelable recyclerViewState;
    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.newsfeed_layout, container, false);
        adjustLayoutSize(getContext().getResources().getConfiguration().orientation);
        return view;
    }

    public void createAdapter(Context ctx, ArrayList<WallPost> wallPosts) {
        this.wallPosts = wallPosts;
        newsfeedView = (RecyclerView) view.findViewById(R.id.news_listview);
        if(newsfeedAdapter == null) {
            newsfeedAdapter = new NewsfeedAdapter(ctx, this.wallPosts);
            llm = new LinearLayoutManager(ctx);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            newsfeedView.setLayoutManager(llm);
            newsfeedView.setAdapter(newsfeedAdapter);
        } else {
            newsfeedAdapter.setArray(wallPosts);
            newsfeedAdapter.notifyDataSetChanged();
        }
        SwipeRefreshLayout p2r_news_view = view.findViewById(R.id.refreshable_layout);
        p2r_news_view.setProgressBackgroundColorSchemeResource(R.color.ovk_color);
        p2r_news_view.setColorSchemeResources(android.R.color.white);
        p2r_news_view.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getActivity().getClass().getSimpleName().equals("AppActivity")) {
                    if (((AppActivity) getActivity()).ab_layout.getNewsfeedSelection() == 0) {
                        ((AppActivity) getActivity()).refreshPage("subscriptions_newsfeed");
                    } else {
                        ((AppActivity) getActivity()).refreshPage("global_newsfeed");
                    }
                }
            }
        });
        adjustLayoutSize(getContext().getResources().getConfiguration().orientation);
    }

    public void updateItem(WallPost item, int position) {
        if(newsfeedAdapter != null) {
            newsfeedView = (RecyclerView) view.findViewById(R.id.news_listview);
            wallPosts.set(position, item);
            newsfeedAdapter.notifyItemChanged(position);
        }
    }

    public void updateAllItems() {
        if(newsfeedAdapter != null) {
            newsfeedView = (RecyclerView) view.findViewById(R.id.news_listview);
            newsfeedAdapter.notifyDataSetChanged();
        }
    }

    public void loadAvatars() {
        if(newsfeedAdapter != null) {
            newsfeedView = (RecyclerView) view.findViewById(R.id.news_listview);
            for (int i = 0; i < getCount(); i++) {
                try {
                    WallPost item = wallPosts.get(i);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(String.format("%s/photos_cache/newsfeed_avatars/avatar_%s", getContext().getCacheDir(), item.author_id), options);
                    if (bitmap != null) {
                        item.avatar = bitmap;
                    }
                    wallPosts.set(i, item);
                } catch (OutOfMemoryError err) {
                    err.printStackTrace();
                }
            }
            newsfeedAdapter.notifyDataSetChanged();
        }
    }

    private void loadPhotos() {
        newsfeedView = (RecyclerView) view.findViewById(R.id.news_listview);
        try {
            if(llm == null) {
                llm = new LinearLayoutManager(getContext());
                llm.setOrientation(LinearLayoutManager.VERTICAL);
            }
            int visibleItemCount = llm.getChildCount();
            int totalItemCount = llm.getItemCount();
            int firstVisibleItemPosition = llm.findFirstVisibleItemPosition();
            int lastVisibleItemPosition = llm.findLastVisibleItemPosition();
            for (int i = 0; i < totalItemCount; i++) {
                WallPost item = wallPosts.get(i);
                try {
                    if(item.repost != null) {
                        if (item.repost.newsfeed_item.attachments.size() > 0) {
                            if (item.repost.newsfeed_item.attachments.get(0).type.equals("photo")) {
                                PhotoAttachment photoAttachment = ((PhotoAttachment) item.repost.newsfeed_item.attachments.get(0).getContent());
                                Attachment attachment = item.repost.newsfeed_item.attachments.get(0);
                                if (i < firstVisibleItemPosition || i > lastVisibleItemPosition) {
                                    if(photoAttachment.photo != null) {
                                        photoAttachment.photo.recycle();
                                        photoAttachment.photo = null;
                                        System.gc();
                                    }
                                } else {
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                    if (photoAttachment.url.length() > 0) {
                                        Bitmap bitmap = BitmapFactory.decodeFile(String.format("%s/photos_cache/newsfeed_photo_attachments/newsfeed_attachment_o%sp%s", getContext().getCacheDir(),
                                                item.repost.newsfeed_item.owner_id, item.repost.newsfeed_item.post_id), options);
                                        if (bitmap != null) {
                                            photoAttachment.photo = bitmap;
                                            attachment.status = "done";
                                            item.repost.newsfeed_item.attachments.set(0, attachment);
                                        }
                                    }
                                }
                                wallPosts.set(i, item);
                            }
                        }
                    }
                    if(item.attachments.size() > 0) {
                        if(item.attachments.get(0).type.equals("photo")) {
                            PhotoAttachment photoAttachment = ((PhotoAttachment) item.attachments.get(0).getContent());
                            Attachment attachment = item.attachments.get(0);
                            if (i < firstVisibleItemPosition || i > lastVisibleItemPosition) {
                                if(photoAttachment.photo != null) {
                                    photoAttachment.photo.recycle();
                                    photoAttachment.photo = null;
                                    System.gc();
                                }
                            } else {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                if (photoAttachment.url.length() > 0) {
                                    Bitmap bitmap = BitmapFactory.decodeFile(String.format("%s/photos_cache/newsfeed_photo_attachments/newsfeed_attachment_o%sp%s", getContext().getCacheDir(), item.owner_id, item.post_id), options);
                                    if (bitmap != null) {
                                        photoAttachment.photo = bitmap;
                                        attachment.status = "done";
                                        item.attachments.set(0, attachment);
                                    } else if(photoAttachment.url.length() > 0) {
                                        attachment.status = "error";
                                    }
                                }
                            }
                            wallPosts.set(i, item);
                        } else if(!item.attachments.get(0).type.equals("poll") && !item.attachments.get(0).type.equals("video")) {
                            item.attachments.get(0).status = "not_supported";
                        }
                    }
                } catch (OutOfMemoryError error) {
                    Log.e("OpenVK Legacy", "Bitmap error: Out of memory");
                } catch (Exception ex) {
                    if(ex.getMessage() != null) {
                        Log.e("OpenVK Legacy", String.format("Bitmap error: %s", ex.getMessage()));
                    } else {
                        Log.e("OpenVK Legacy", String.format("Bitmap error: %s", ex.getClass().getSimpleName()));
                    }
                    ex.printStackTrace();
                }
            }
            newsfeedAdapter.notifyDataSetChanged();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setScrollingPositions(final Context ctx, final boolean load_photos, final boolean infinity_scroll) {
        loading_more_posts = false;
        if(load_photos) {
            loadPhotos();
        }
        final InfinityScrollView scrollView = view.findViewById(R.id.scrollView);
        scrollView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(InfinityScrollView infinityScrollView, int x, int y, int old_x, int old_y) {
                View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
                if (!loading_more_posts) {
                    if (diff == 0) {
                        if (ctx.getClass().getSimpleName().equals("AppActivity")) {
                            loading_more_posts = true;
                            ((AppActivity) ctx).loadMoreNews();
                        }
                    }
                }
            }
        });
    }


    public int getCount() {
        try {
            return newsfeedView.getAdapter().getItemCount();
        } catch (NullPointerException npE) {
            return 0;
        }
    }

    public void select(int position, String item, int value) {
        if(item.equals("likes")) {
            if(value == 1) {
                wallPosts.get(position).counters.isLiked = true;
            } else {
                wallPosts.get(position).counters.isLiked = false;
            }
            newsfeedAdapter.notifyDataSetChanged();
        }
    }

    public void select(int position, String item, String value) {
        if(item.equals("likes")) {
            if(value.equals("add")) {
                wallPosts.get(position).counters.isLiked = true;
            } else {
                wallPosts.get(position).counters.isLiked = false;
            }
            newsfeedAdapter.notifyDataSetChanged();
        }
    }

    public void adjustLayoutSize(int orientation) {
        if(newsfeed != null) {
            try {
                if (((OvkApplication) getContext().getApplicationContext()).isTablet) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (600 * (getResources().getDisplayMetrics().density)), ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                        newsfeedView.setLayoutParams(layoutParams);
                    } else {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                        newsfeedView.setLayoutParams(layoutParams);
                    }
                } else {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (480 * (getResources().getDisplayMetrics().density)), ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                        newsfeedView.setLayoutParams(layoutParams);
                    } else {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                        newsfeedView.setLayoutParams(layoutParams);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}