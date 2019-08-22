package ro.ciubex.dscautorename.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;

public final class PluginBackgroundService
        extends JobIntentService
        implements RenameFileAsyncTask.Listener {

    private static final String TAG = PluginBackgroundService.class.getName();
    private static final int JOB_ID = 1234;


    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, PluginBackgroundService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // ACTUAL RENAMING OF FILES
        Log.d(TAG, "onHandleWork called...");
        ((DSCApplication) getApplication()).launchAutoRenameTask(this, true, null, true);
    }

    @Override
    public void onTaskStarted() {
        // TODO: Return update to plugin if possible
    }

    @Override
    public void onTaskUpdate(int position, int max, String message) {
        // TODO: Return update to plugin if possible
    }

    @Override
    public void onTaskFinished(int count) {
        // TODO: Return update to plugin if possible
        Log.d(TAG, "onTaskFinished called...");
    }

    @Override
    public boolean isFinishing() {
        return false;
    }
}
