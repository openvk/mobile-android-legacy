package uk.openvk.android.legacy.ui.core.fragments.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;

import java.util.Timer;

import uk.openvk.android.legacy.BuildConfig;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.Ovk;
import uk.openvk.android.legacy.api.enumerations.HandlerMessages;
import uk.openvk.android.legacy.api.models.InstanceLink;
import uk.openvk.android.legacy.api.wrappers.OvkAPIWrapper;
import uk.openvk.android.legacy.ui.OvkAlertDialog;
import uk.openvk.android.legacy.ui.core.activities.AboutApplicationActivity;
import uk.openvk.android.legacy.ui.core.activities.AdvancedSettingsActivity;
import uk.openvk.android.legacy.ui.core.activities.DebugMenuActivity;
import uk.openvk.android.legacy.ui.core.activities.MainActivity;
import uk.openvk.android.legacy.ui.core.activities.MainSettingsActivity;
import uk.openvk.android.legacy.ui.core.activities.NetworkSettingsActivity;

/**
 * File created by Dmitry on 16.02.2023.
 */

public class MainSettingsFragment extends PreferenceFragmentCompatDividers {
    private boolean isQuiting;
    private SharedPreferences global_prefs;
    private SharedPreferences instance_prefs;
    private OvkApplication app;
    private OvkAPIWrapper ovk_api;
    private int danger_zone_multiple_tap;
    private View about_instance_view;
    private OvkAlertDialog about_instance_dlg;

    @Override
    public void onCreatePreferencesFix(Bundle bundle, String s) {
        global_prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        instance_prefs = getContext().getSharedPreferences("instance", 0);
        if(instance_prefs.getString("account_password_hash", "").length() > 0) {
            addPreferencesFromResource(R.xml.preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences_2);
        }
        setListeners();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDividerPreferences(DIVIDER_PADDING_CHILD | DIVIDER_CATEGORY_AFTER_LAST | DIVIDER_CATEGORY_BETWEEN);
    }

    private void setListeners() {
        PreferenceScreen screen = (PreferenceScreen) findPreference("main_settings");
        PreferenceGroup others = (PreferenceGroup) findPreference("cat_others");

        Preference language = findPreference("interfaceLanguage");
        if (language != null) {
            // Not implemented yet
            language.setEnabled(false);
            language.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getContext(), getResources().getString(R.string.not_implemented), Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }

        Preference notif_ringtone = findPreference("notifyRingtone");
        if (notif_ringtone != null) {
            // Not implemented yet
            notif_ringtone.setEnabled(false);
            notif_ringtone.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getContext(), getResources().getString(R.string.not_implemented), Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }

        Preference about_preference = findPreference("about");
        about_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openAboutActivity();
                return false;
            }
        });
        Preference logout_preference = findPreference("logOut");
        if (logout_preference != null) {
            //logout_preference.setSummary(account_name);
            logout_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    openLogoutConfirmationDialog();
                    return false;
                }
            });
        }

        Preference network_settings = findPreference("network_settings");
        if (network_settings != null) {
            network_settings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext().getApplicationContext(), NetworkSettingsActivity.class);
                    startActivity(intent);
                    return false;
                }
            });
        }

        Preference advanced_settings = findPreference("advanced");
        if (advanced_settings != null) {
            advanced_settings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext().getApplicationContext(), AdvancedSettingsActivity.class);
                    startActivity(intent);
                    return false;
                }
            });
        }

        final Preference about_instance = findPreference("about_instance");
        if(about_instance != null) {
            about_instance.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    openAboutInstanceDialog();
                    return false;
                }
            });
        }

        Preference debug_menu = findPreference("debug_menu");
        if (BuildConfig.BUILD_TYPE.equals("release")) {
            others.removePreference(debug_menu);
        } else {
            danger_zone_multiple_tap = 0;
            global_prefs.getBoolean("debugDangerZone", false);
            debug_menu.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (instance_prefs.getString("access_token", "").length() > 0) {
                        danger_zone_multiple_tap += 1;
                        if (danger_zone_multiple_tap == 1) {
                            Timer timer = new Timer();
                            //timer.schedule(new MainSettingsActivity.HideDangerZone(), 8000, 8000);
                        }
                        if (danger_zone_multiple_tap < 5) {
                            Intent intent = new Intent(getContext(), DebugMenuActivity.class);
                            startActivity(intent);
                        } else if (danger_zone_multiple_tap == 5) {
                            Toast.makeText(getContext(), "злой армянин кушает", Toast.LENGTH_LONG).show();
                        } else if (danger_zone_multiple_tap == 10) {
                            global_prefs.edit().putBoolean("debugDangerZone", true).commit();
                            Intent intent = new Intent(getContext(), DebugMenuActivity.class);
                            startActivity(intent);
                            danger_zone_multiple_tap = 0;
                        }
                    } else {
                        Intent intent = new Intent(getContext(), DebugMenuActivity.class);
                        startActivity(intent);
                    }
                    return false;
                }
            });
        }
    }

    private void openLogoutConfirmationDialog() {
        OvkAlertDialog logout_dlg;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = getContext().getApplicationContext().getSharedPreferences("instance", 0).edit();
                editor.putString("access_token", "");
                editor.putString("server", "");
                editor.putString("account_password_hash", "");
                editor.commit();
                Intent activity = new Intent(getContext().getApplicationContext(), MainActivity.class);
                activity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(activity);
                System.exit(0);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        logout_dlg = new OvkAlertDialog(getContext());
        logout_dlg.build(builder, "", getResources().getString(R.string.log_out_warning), null, "");
        logout_dlg.show();
    }

    private void openAboutActivity() {
        Intent intent = new Intent(getContext().getApplicationContext(), AboutApplicationActivity.class);
        startActivity(intent);
    }

    private void openAboutInstanceDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        about_instance_view = getLayoutInflater(null).inflate(R.layout.about_instance_layout, null, false);
        TextView server_name = (TextView) about_instance_view.findViewById(R.id.server_addr_label2);
        ((TextView) about_instance_view.findViewById(R.id.connection_type_label2)).setText(getResources().getString(R.string.loading));
        ((TextView) about_instance_view.findViewById(R.id.instance_version_label2)).setText(getResources().getString(R.string.loading));
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_version_ll)).setVisibility(View.GONE);
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_statistics_ll)).setVisibility(View.GONE);
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_links_ll)).setVisibility(View.GONE);
        server_name.setText(instance_prefs.getString("server", ""));
        ((TextView) about_instance_view.findViewById(R.id.rules_link)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebAddress(String.format("http://%s/about", instance_prefs.getString("server", "")));
            }
        });
        ((TextView) about_instance_view.findViewById(R.id.privacy_link)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebAddress(String.format("http://%s/privacy", instance_prefs.getString("server", "")));
            }
        });
        builder.setTitle(getResources().getString(R.string.about_instance));
        isQuiting = true;
        builder.setView(about_instance_view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isQuiting = false;
            }
        });
        about_instance_dlg = new OvkAlertDialog(getContext());
        about_instance_dlg.build(builder, getResources().getString(R.string.about_instance), "", about_instance_view);
        about_instance_dlg.show();
        //ovk_api.checkHTTPS();
        //ovk.getVersion(ovk_api);
        //ovk.aboutInstance(ovk_api);
    }

    private void openWebAddress(String server) {
    }

    public void setAboutInstanceData(Ovk ovk) {
        TextView users_counter = (TextView) about_instance_view.findViewById(R.id.instance_users_count);
        users_counter.setText(getResources().getString(R.string.instance_users_count, ovk.instance_stats.users_count));
        TextView online_users_counter = (TextView) about_instance_view.findViewById(R.id.instance_online_users_count);
        online_users_counter.setText(getResources().getString(R.string.instance_online_users_count, ovk.instance_stats.online_users_count));
        TextView active_users_counter = (TextView) about_instance_view.findViewById(R.id.instance_active_users_count);
        active_users_counter.setText(getResources().getString(R.string.instance_active_users_count, ovk.instance_stats.active_users_count));
        TextView groups_counter = (TextView) about_instance_view.findViewById(R.id.instance_groups_count);
        groups_counter.setText(getResources().getString(R.string.instance_groups_count, ovk.instance_stats.groups_count));
        TextView wall_posts_counter = (TextView) about_instance_view.findViewById(R.id.instance_wall_posts_count);
        wall_posts_counter.setText(getResources().getString(R.string.instance_wall_posts_count, ovk.instance_stats.wall_posts_count));
        TextView admins_counter = (TextView) about_instance_view.findViewById(R.id.instance_admins_count);
        admins_counter.setText(getResources().getString(R.string.instance_admins_count, ovk.instance_admins.size()));
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_statistics_ll)).setVisibility(View.VISIBLE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label2)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label3)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label4)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label5)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label6)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label7)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label8)).setVisibility(View.GONE);
        ((TextView) about_instance_view.findViewById(R.id.instance_links_label9)).setVisibility(View.GONE);
        for(int i = 0; i < ovk.instance_links.size(); i++) {
            InstanceLink link = ovk.instance_links.get(i);
            TextView textView = null;
            if(i == 0) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label2));
            } else if(i == 1) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label3));
            } else if(i == 2) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label4));
            } else if(i == 3) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label5));
            } else if(i == 4) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label6));
            } else if(i == 5) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label7));
            } else if(i == 6) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label8));
            } else if(i == 7) {
                textView = ((TextView) about_instance_view.findViewById(R.id.instance_links_label9));
            }
            if(textView != null) {
                textView.setText(Html.fromHtml(String.format("<a href=\"%s\">%s</a>", link.url, link.name)));
                textView.setVisibility(View.VISIBLE);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_links_ll)).setVisibility(View.VISIBLE);
    }


    public void setInstanceVersion(Ovk ovk) {
        TextView openvk_version_tv = (TextView) about_instance_view.findViewById(R.id.instance_version_label2);
        if(ovk.version.startsWith("OpenVK")) {
            openvk_version_tv.setText(ovk.version);
        } else {
            openvk_version_tv.setText(String.format("OpenVK %s", ovk.version));
        }
        ((LinearLayout) about_instance_view.findViewById(R.id.instance_version_ll)).setVisibility(View.VISIBLE);
    }

    public void setConnectionType(int message) {
        if(message == HandlerMessages.OVK_CHECK_HTTP) {
            TextView connection_type = (TextView) about_instance_view.findViewById(R.id.connection_type_label2);
            connection_type.setText(getResources().getString(R.string.default_connection));
        } else if(message == HandlerMessages.OVK_CHECK_HTTPS){
            TextView connection_type = (TextView) about_instance_view.findViewById(R.id.connection_type_label2);
            connection_type.setText(getResources().getString(R.string.secured_connection));
        } else {
            TextView connection_type = (TextView) about_instance_view.findViewById(R.id.connection_type_label2);
            connection_type.setText(getResources().getString(R.string.connection_error));
        }
    }
}