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

package uk.openvk.android.legacy.core.listeners;

import android.accounts.Account;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.core.activities.AppActivity;
import uk.openvk.android.legacy.utils.AccountAuthenticator;

public class AccountsUpdateListener implements OnAccountsUpdateListener {
    private Context ctx;
    public AccountsUpdateListener(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if(ctx instanceof AppActivity) {
            SharedPreferences instance_prefs =
                    ((OvkApplication) ctx.getApplicationContext()).getAccountPreferences();
            Account newAccount = null;
            for (final Account account : accounts) {
                if (account.name.equals(instance_prefs.getString("account_name", "")) &&
                        account.type.equals("uk.openvk.android.legacy.account")) {
                    newAccount = account;
                }
            }
            if (newAccount == null) {
                instance_prefs.edit().clear().commit();
                AccountAuthenticator.openChangeAccountDialog(
                        ctx, PreferenceManager.getDefaultSharedPreferences(ctx), false
                );
            }
        }
    }
}
