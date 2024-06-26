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

package uk.openvk.android.legacy.ui.list.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.R;
import uk.openvk.android.client.entities.InstanceAdmin;
import uk.openvk.android.client.entities.InstanceStatistics;

public class InstanceCountersAdapter extends BaseAdapter {

    private final InstanceStatistics statistics;
    private final int admins;
    private Context ctx;
    
    public InstanceCountersAdapter(Context ctx, InstanceStatistics statistics,
                                   ArrayList<InstanceAdmin> admins) {
        this.ctx = ctx;
        this.statistics = statistics;
        this.admins = admins.size();
    }
    
    @Override
    public int getCount() {
        return 6;
    }

    @Override
    public Long getItem(int position) {
        if(position == 0) {
            return statistics.users_count;
        } else if(position == 1) {
            return statistics.online_users_count;
        } else if(position == 2) {
            return statistics.active_users_count;
        } else if(position == 3) {
            return statistics.groups_count;
        } else if(position == 4) {
            return statistics.wall_posts_count;
        } else if(position == 5) {
            return (long) admins;
        } else {
            return 0L;
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(ctx);
            convertView = inflater.inflate(R.layout.list_item_instance_counter, null);
        }
        if(convertView != null) {
            TextView counter = convertView.findViewById(R.id.instance_count);
            TextView label = convertView.findViewById(R.id.instance_label);
            long count = (Long) getItem(position);
            if(count >= 1000000000L) {
                counter.setText(String.format("%.1fB", (double)count / (double)1000000000));
            } else if(count >= 1000000L) {
                counter.setText(String.format("%.1fM", (double)count / (double)1000000));
            } else if(count >= 1000L) {
                counter.setText(String.format("%.1fK", (double)count / (double)1000));
            } else {
                counter.setText(String.format("%s", count));
            }
            int res_id = 0;
            if(position == 0) {
                res_id = R.plurals.instance_users;
            } else if(position == 1) {
                res_id = R.string.instance_online_users;
            } else if(position == 2) {
                res_id = R.plurals.instance_active_users;
            } else if(position == 3) {
                res_id = R.plurals.profile_groups;
            } else if(position == 4) {
                res_id = R.plurals.instance_posts;
            } else if(position == 5) {
                res_id = R.plurals.instance_administrators;
            }

            int end_number = Global.getEndNumberFromLong(getItem(position));

            if(position == 1) {
                label.setText(ctx.getResources().getString(res_id));
            } else {
                if(count >= 1000L) {
                    end_number = 9;
                }
                String plural_str = Global.getPluralQuantityString(
                        ctx,
                        res_id,
                        end_number
                );
                if(plural_str.length() == 0) {
                    plural_str = ctx.getResources().getQuantityString(res_id, end_number);
                }
                label.setText(plural_str);
            }
        }
        return convertView;
    }
}
