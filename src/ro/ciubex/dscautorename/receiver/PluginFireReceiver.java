package ro.ciubex.dscautorename.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import ro.ciubex.dscautorename.service.PluginBackgroundService;
import ro.ciubex.dscautorename.util.PluginBundleValues;

public class PluginFireReceiver extends AbstractPluginSettingReceiver {
    private static final String TAG = PluginFireReceiver.class.getName();

    @Override
    protected boolean isBundleValid(@NonNull final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return false;
    }

    @Override
    protected void firePluginSetting(@NonNull final Context context, @NonNull final Bundle bundle) {
        try {
            Log.d(TAG, "PluginFireReceiver.firePluginSetting() called...");
            PluginBackgroundService.enqueueWork(context, new Intent());
        } catch (Exception e) {
            Log.d(TAG, "PluginFireReceiver.firePluginSetting() EXCEPTION...");
            e.printStackTrace();
        }
    }

}
