package uk.openvk.android.legacy.ui.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;

@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
public class OvkSearchView extends SearchView {
    public OvkSearchView(Context context) {
        super(context);
        setStyle();
    }

    private void setStyle() {
        float dp = getResources().getDisplayMetrics().scaledDensity;
        int[] searchViewIds = Global.generateSearchViewIds(getResources());
        final ImageView search_btn = findViewById(searchViewIds[0]);
        final View search_plate = findViewById(searchViewIds[1]);
        final TextView query_tv = findViewById(searchViewIds[2]);
        final ImageView search_mag_icon = findViewById(searchViewIds[3]);
        final ImageView search_close_btn = findViewById(searchViewIds[4]);
        search_btn.setImageResource(R.drawable.ic_ab_search);
        search_plate.setBackgroundDrawable(
                getResources().getDrawable(R.drawable.login_fields)
        );
        search_mag_icon.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_ab_search_light)
        );
        search_close_btn.setImageDrawable(
                getResources().getDrawable(R.drawable.ic_search_clear)
        );
        if(((OvkApplication) getContext().getApplicationContext()).isTablet) {
            setMaxWidth((int) (320 * dp));
            search_plate.setPadding(0, (int)(4 * dp), 0, (int)(4 * dp));
            query_tv.setPadding((int)(4 * dp), (int)(2 * dp), (int)(2 * dp), (int)(6 * dp));
            search_close_btn.setPadding((int)(8 * dp), 0, (int)(8 * dp), 0);
        } else {
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setMaxWidth(
                        (int) (
                                getResources().getDisplayMetrics().widthPixels -
                                        ((44 * dp))
                        )
                );
                search_plate.setPadding(0, (int) (2 * dp), 0, (int) (2 * dp));
                query_tv.setPadding((int) (6 * dp), (int) (1 * dp), (int) (4 * dp), (int) (6 * dp));
                search_close_btn.setPadding((int) (8 * dp), 0, (int) (8 * dp), (int) (2 * dp));
            } else {
                setMaxWidth(
                        (int) (
                                getResources().getDisplayMetrics().widthPixels -
                                        ((44 * dp))
                        )
                );
                search_plate.setPadding(0, (int) (2 * dp), 0, (int) (2 * dp));
                query_tv.setPadding((int) (6 * dp), (int) (1 * dp), (int) (4 * dp), (int) (1 * dp));
                search_close_btn.setPadding((int) (8 * dp), 0, (int) (8 * dp), (int) (2 * dp));
            }
        }

        setQueryHint(getResources().getString(R.string.search));
        query_tv.setTextColor(Color.BLACK);
    }

    public OvkSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle();
    }
}
