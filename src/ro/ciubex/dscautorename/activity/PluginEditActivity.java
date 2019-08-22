package ro.ciubex.dscautorename.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.util.PluginBundleValues;

public class PluginEditActivity extends AbstractAppCompatPluginActivity {
    private static final String TAG = PluginEditActivity.class.getName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.plugin_main);

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(), 0));
        } catch (PackageManager.NameNotFoundException e) {
            ((DSCApplication) getApplication()).logE(TAG, "Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull final Bundle previousBundle,
                                               @NonNull final String previousBlurb) {
        // Nothing to do here
    }

    @Override
    @Nullable
    public Bundle getResultBundle() {
        return PluginBundleValues.generateBundle(getApplicationContext());
    }

    @Override
    @NonNull
    public String getResultBlurb(@NonNull final Bundle bundle) {
        return "Everything is fine.";
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Nothing to do here
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Nothing to do here
        return super.onOptionsItemSelected(item);
    }

}
