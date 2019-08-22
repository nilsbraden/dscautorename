/**
 * This file is part of DSCAutoRename application.
 * <p>
 * Copyright (C) 2016 Claudiu Ciobotariu
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.dscautorename.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.TwoStatePreference;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ro.ciubex.dscautorename.DSCApplication;
import ro.ciubex.dscautorename.R;
import ro.ciubex.dscautorename.dialog.SelectFileNamePatternDialog;
import ro.ciubex.dscautorename.dialog.SelectFolderDialog;
import ro.ciubex.dscautorename.dialog.SelectFoldersListDialog;
import ro.ciubex.dscautorename.model.FileNameModel;
import ro.ciubex.dscautorename.model.MountVolume;
import ro.ciubex.dscautorename.model.SelectedFolderModel;
import ro.ciubex.dscautorename.preference.SeekBarPreference;
import ro.ciubex.dscautorename.provider.CachedFileProvider;
import ro.ciubex.dscautorename.task.AsyncTaskResult;
import ro.ciubex.dscautorename.task.RenameFileAsyncTask;
import ro.ciubex.dscautorename.task.SettingsFileUtilAsyncTask;
import ro.ciubex.dscautorename.util.DevicesUtils;
import ro.ciubex.dscautorename.util.Utilities;

/**
 * This is main activity class, actually is a preference activity.
 *
 * @author Claudiu Ciobotariu
 */
public class SettingsActivity extends PreferenceActivity implements
	OnSharedPreferenceChangeListener, RenameFileAsyncTask.Listener,
	DSCApplication.ProgressCancelListener, RenameShortcutUpdateListener,
	SelectFolderDialog.SelectFolderListener, SettingsFileUtilAsyncTask.Responder {
	private static final String TAG = SettingsActivity.class.getName();
	private DSCApplication mApplication;
	private Preference mAppLanguage;
	private Preference mAppTheme;
	private ListPreference mServiceTypeList;
	private Preference mRenameVideoEnabled;
	private SeekBarPreference mRenameServiceStartDelay;
	private ListPreference mDelayUnit;
	private SeekBarPreference mRenameFileDelay;
	private ListPreference mRenameFileDateType;
	private Preference mDefineFileNamePatterns;
	private Preference mSendBroadcastEnabled;
	private Preference mInvokeMediaScannerEnabled;
	private EditTextPreference mFileNameSuffixFormat;
	private Preference mEnabledFolderScanning;
	private Preference mFolderScanningPref;
	private TwoStatePreference mEnableScanForFiles;
	private Preference mToggleRenameShortcut;
	private Preference mHideRenameServiceStartConfirmation;
	private Preference mManuallyStartRename;
	private Preference mFileRenameCount;
	private Preference mRequestPermissions;
	private Preference mExportSettings;
	private Preference mImportSettings;
	private Preference mBuildVersion;
	private Preference mShowHelpPagePref;
	private Preference mSendDebugReport;
	private Preference mLicensePref;
	private Preference mPrivacyPolicyPref;
	private Preference mDonatePref;
	private Preference mAppendOriginalName;
	private SelectFoldersListDialog selectFoldersListDialog;
	private SelectFileNamePatternDialog selectFileNamePatternDialog;
	private PreferenceCategory mOtherSettings;
	private static final int ID_CONFIRMATION_INFO = -2;
	private static final int ID_CONFIRMATION_ALERT = -1;
	private static final int ID_CONFIRMATION_DONATION = 0;
	private static final int ID_CONFIRMATION_RESET_RENAME_COUNTER = 1;
	private static final int ID_CONFIRMATION_DEBUG_REPORT = 2;
	private static final int ID_CONFIRMATION_REQUEST_PERMISSIONS = 3;
	private static final int ID_CONFIRMATION_MANUAL_RENAME = 4;
	private static final int ID_CONFIRMATION_EXPORT_SETTINGS = 5;
	private static final int ID_CONFIRMATION_IMPORT_SETTINGS = 6;
	private static final int ID_CONFIRMATION_USE_INTERNAL_SELECT_FOLDER = 7;
	private static final int ID_CONFIRMATION_ENABLE_FOLDER_FILES_SCANNING = 8;
	private int confirmedActionId = -1;
	private static final int REQUEST_SEND_REPORT = 1;
	public static final int REQUEST_OPEN_DOCUMENT_TREE = 42;
	public static final int REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER = 43;
	public static final int REQUEST_OPEN_DOCUMENT_TREE_SETTINGS = 45;
	private static final int PERMISSIONS_REQUEST_CODE = 44;
	private static final int BUFFER = 1024;

	private final static int IGNORED = -1;
	private final static int DO_NOT_SHOW_IGNORED = 0;
	private final static int DO_NOT_SHOW_GRANT_URI_PERMISSION = 1;

	private boolean doNotDisplayNotGrantUriPermission;
	private boolean modifiedPreferences;

	/**
	 * Method called when the activity is created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mApplication = (DSCApplication) getApplication();
		mApplication.initVolumes();
		applyApplicationTheme();
		applyApplicationLocale();
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		initPreferences();
		initCommands();
		initPreferencesByPermissions();
		checkProVersion();
		updateShortcutFields();
		doNotDisplayNotGrantUriPermission = (mApplication.getSdkInt() < 21 || // do not check for old API
			mApplication.isDisplayNotGrantUriPermission());
	}

	/**
	 * Apply application theme.
	 */
	private void applyApplicationTheme() {
		this.setTheme(mApplication.getApplicationTheme());
	}

	/**
	 * Apply application locale.
	 */
	private void applyApplicationLocale() {
		Resources resources = getBaseContext().getResources();
		Configuration config = resources.getConfiguration();
		config.locale = DSCApplication.getLocale();
		resources.updateConfiguration(config, resources.getDisplayMetrics());
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mAppLanguage = findPreference("languageCode");
		mAppTheme = findPreference("appTheme");
		mServiceTypeList = (ListPreference) findPreference("serviceType");
		mRenameVideoEnabled = findPreference("renameVideoEnabled");
		mRenameServiceStartDelay = (SeekBarPreference) findPreference("renameServiceStartDelay");
		mDelayUnit = (ListPreference) findPreference("delayUnit");
		mRenameFileDelay = (SeekBarPreference) findPreference("renameFileDelay");
		mRenameFileDateType = (ListPreference) findPreference("renameFileDateType");
		mDefineFileNamePatterns = findPreference("definePatterns");
		mSendBroadcastEnabled = findPreference("sendBroadcastEnabled");
		mInvokeMediaScannerEnabled = findPreference("invokeMediaScannerEnabled");
		mFileNameSuffixFormat = (EditTextPreference) findPreference("fileNameSuffixFormat");
		mEnabledFolderScanning = findPreference("enabledFolderScanning");
		mFolderScanningPref = findPreference("folderScanningPref");
		mEnableScanForFiles = (TwoStatePreference) findPreference("enableScanForFilesCheck");
		mToggleRenameShortcut = findPreference("toggleRenameShortcut");
		mHideRenameServiceStartConfirmation = findPreference("hideRenameServiceStartConfirmation");
		mAppendOriginalName = findPreference("appendOriginalName");
		mManuallyStartRename = findPreference("manuallyStartRename");
		mFileRenameCount = findPreference("fileRenameCount");
		mRequestPermissions = findPreference("requestPermissions");
		mExportSettings = findPreference("exportSettings");
		mImportSettings = findPreference("importSettings");
		mBuildVersion = findPreference("buildVersion");
		mShowHelpPagePref = findPreference("showHelpPage");
		mSendDebugReport = findPreference("sendDebugReport");
		mLicensePref = findPreference("licensePref");
		mPrivacyPolicyPref = findPreference("privacyPolicyPref");
		mDonatePref = findPreference("donatePref");
		mOtherSettings = (PreferenceCategory) findPreference("otherSettings");
	}

	/**
	 * Initialize the preference commands.
	 */
	private void initCommands() {
		mDefineFileNamePatterns
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onDefineFileNamePatterns();
					return true;
				}
			});
		mFolderScanningPref
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onFolderScanningPref();
					return true;
				}
			});
		mEnableScanForFiles
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onEnableScanForFilesPref();
					return true;
				}
			});
		mToggleRenameShortcut
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onToggleRenameShortcut();
					return true;
				}
			});
		mManuallyStartRename
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onManuallyStartRename();
					return true;
				}
			});
		mFileRenameCount
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onResetFileRenameCounter();
					return true;
				}
			});
		mRequestPermissions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onRequestPermissions();
				return true;
			}
		});
		mExportSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onExportSettings();
				return true;
			}
		});
		mImportSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onImportSettings();
				return true;
			}
		});
		mBuildVersion
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onBuildVersion();
					return true;
				}
			});
		mSendDebugReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onSendDebugReport();
				return true;
			}
		});
		mLicensePref
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onLicensePref();
					return true;
				}
			});
		mPrivacyPolicyPref
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {

					onPrivacyPolicy();
					return true;
				}
			});
		mShowHelpPagePref
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {

					onShowHelpPage();
					return true;
				}
			});
		mDonatePref
			.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					onDonatePref();
					return true;
				}
			});
	}

	/**
	 * Remove the permission request preference if should not be asked for permissions.
	 */
	private void initPreferencesByPermissions() {
		if (!mApplication.shouldAskPermissions()) {
			mOtherSettings.removePreference(mRequestPermissions);
		}
	}

	/**
	 * Method used to request for application required permissions.
	 */
	@TargetApi(Build.VERSION_CODES.M)
	private void requestForPermissions(String[] permissions) {
		if (!Utilities.isEmpty(permissions)) {
			requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
		}
	}

	/**
	 * Check if the pro version is present to update the donation preference item.
	 */
	private void checkProVersion() {
		if (mApplication.isProPresent()) {
			mDonatePref.setEnabled(false);
			mDonatePref.setTitle(R.string.thank_you_title);
			mDonatePref.setSummary(R.string.thank_you_desc);
		}
	}

	/**
	 * Prepare all informations when the activity is resuming
	 */
	@Override
	protected void onResume() {
		super.onResume();
		modifiedPreferences = false;
		getPreferenceScreen().getSharedPreferences()
			.registerOnSharedPreferenceChangeListener(this);
		mApplication.updateShortcutUpdateListener(this);
		prepareSummaries();
		checkUpdateMessage();
		checkForPermissions();
		setColorPreferencesSummary(mEnableScanForFiles, Color.RED);
		checkAllSelectedFolders();
	}

	/**
	 * Method used to check for application permissions.
	 */
	@TargetApi(Build.VERSION_CODES.M)
	private void checkForPermissions() {
		if (mApplication.shouldAskPermissions()) {
			updateSettingsOptionsByPermissions();
			if (!mApplication.havePermissionsAsked()) {
				requestForPermissions(mApplication.getAllRequiredPermissions());
			}
		}
	}

	/**
	 * Check if is first time when the used open this application
	 */
	private void checkUpdateMessage() {
		if (mApplication.isFirstTime()) {
			String message = getUpdateMessage();
			if (message != null) { // show the message only if is necessary
				showConfirmationDialog(message, true, ID_CONFIRMATION_ALERT);
			}
		}
	}

	/**
	 * Get the update message from the resources.
	 *
	 * @return The update message if is present on the resources.
	 */
	private String getUpdateMessage() {
		String message = null;
		String key = "update_message";
		int id = mApplication.getApplicationContext().getResources().getIdentifier(key,
			"string", mApplication.getPackageName());
		if (id > 0) {
			message = mApplication.getApplicationContext().getString(id);
		}
		return message;
	}

	/**
	 * Unregister the preference changes when the activity is on pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (modifiedPreferences) {
			mApplication.sharedPreferencesDataChanged();
		}
		mApplication.updateShortcutUpdateListener(null);
		getPreferenceScreen().getSharedPreferences()
			.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * This method is invoked when a preference is changed
	 *
	 * @param sharedPreferences The shared preference
	 * @param key               Key of changed preference
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
										  String key) {
		boolean doPrepareSummaries = true;
		modifiedPreferences = true;
		if (DSCApplication.KEY_SERVICE_TYPE.equals(key)) {
			mApplication.resetCameraServiceInstanceCount();
			mApplication.checkRegisteredServiceType(false);
		} else if (DSCApplication.KEY_ENABLED_FOLDER_SCANNING.equals(key)) {
			mApplication.updateFolderObserverList();
		} else if (DSCApplication.KEY_LANGUAGE_CODE.equals(key) ||
			DSCApplication.KEY_APP_THEME.equals(key)) {
			doPrepareSummaries = false;
			restartActivity();
		}
		if (doPrepareSummaries) {
			prepareSummaries();
		}
	}

	/**
	 * Get selected units string: seconds or minutes.
	 *
	 * @return Selected units string.
	 */
	private String getSelectedUnits() {
		if (mApplication.getDelayUnit() == 60) {
			return mApplication.getApplicationContext().getString(R.string.minutes_unit);
		}
		return mApplication.getApplicationContext().getString(R.string.seconds_unit);
	}

	/**
	 * Get the application theme label.
	 *
	 * @return The application theme label.
	 */
	private String getSelectedThemeLabel() {
		String[] labels = mApplication.getApplicationContext().getResources().
			getStringArray(R.array.app_theme_labels);
		int themeId = mApplication.getApplicationTheme();
		if (R.style.AppThemeDark == themeId) {
			return labels[1];
		}
		return labels[0];
	}

	/**
	 * Get the application selected language.
	 *
	 * @return Selected application language.
	 */
	private String getSelectedLanguage() {
		String[] labels = mApplication.getApplicationContext().getResources().
			getStringArray(R.array.language_labels);
		String[] values = mApplication.getApplicationContext().getResources().
			getStringArray(R.array.language_values);
		String langCode = mApplication.getLanguageCode();
		int position = 0;
		for (int i = 0; i < values.length; i++) {
			if (langCode.equals(values[i])) {
				position = i;
				break;
			}
		}
		return labels[position];
	}

	/**
	 * Restart this activity.
	 */
	private void restartActivity() {
		Intent intent = getIntent();
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		mApplication.initLocale();
		finish();
		startActivity(intent);
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		Date now = new Date();
		FileNameModel[] originalArr = mApplication.getOriginalFileNamePattern();
		String newFileName = mApplication.getFileNameFormatted(originalArr[0].getAfter(), now);
		String label = mApplication.getApplicationContext().getString(
			R.string.define_file_name_pattern_desc, originalArr[0].getDemoBefore());
		mDefineFileNamePatterns.setSummary(label);

		label = "" + newFileName;
		label += mApplication.getFormattedFileNameSuffix(0);
		label += "." + originalArr[0].getDemoExtension();
		label += ", " + newFileName;
		label += mApplication.getFormattedFileNameSuffix(1);
		label += "." + originalArr[0].getDemoExtension();

		label = mApplication.getApplicationContext().getString(R.string.file_name_suffix_format_desc, label);
		mFileNameSuffixFormat.setSummary(label);
		switch (mApplication.getServiceType()) {
			case DSCApplication.SERVICE_TYPE_CAMERA:
				mServiceTypeList.setSummary(R.string.service_choice_1);
				break;
			case DSCApplication.SERVICE_TYPE_CONTENT:
				mServiceTypeList.setSummary(R.string.service_choice_2);
				break;
			case DSCApplication.SERVICE_TYPE_FILE_OBSERVER:
				mServiceTypeList.setSummary(R.string.service_choice_3);
				break;
			case DSCApplication.SERVICE_TYPE_CAMERA_SERVICE:
				mServiceTypeList.setSummary(R.string.service_choice_4);
				break;
			default:
				mServiceTypeList.setSummary(R.string.service_choice_0);
				break;
		}
		label = mApplication.getApplicationContext().getString(R.string.change_language_title_param,
			getSelectedLanguage());
		mAppLanguage.setTitle(label);

		label = mApplication.getApplicationContext().getString(R.string.app_theme_title_param,
			getSelectedThemeLabel());
		mAppTheme.setTitle(label);

		label = getSelectedUnits();
		mRenameServiceStartDelay.setUnits(label);
		mDelayUnit.setTitle(
			mApplication.getApplicationContext().getString(R.string.choose_units_title_param,
				label));
		if (mApplication.getSdkInt() >= 21) {
			mEnabledFolderScanning.setSummary(R.string.enable_filter_folder_desc_v21);
		}
		updateRenameShortcut();
		updateSelectedFolders();
		updateEnableScanForFilesCheckBox();
		// renameFileDateType
		String arr[] = mApplication.getResources().getStringArray(
			R.array.rename_file_using_labels);
		mRenameFileDateType
			.setSummary(arr[mApplication.getRenameFileDateType()]);
		label = getString(R.string.append_original_name_desc, newFileName);
		mAppendOriginalName.setSummary(label);
		mFileRenameCount.setTitle(mApplication.getString(
			R.string.file_rename_count_title,
			mApplication.getFileRenameCount()));
		mBuildVersion.setSummary(mApplication.getVersionName());
	}

	/**
	 * Update the checkbox state for the option "Enable folder scanning for files."
	 */
	private void updateEnableScanForFilesCheckBox() {
		mEnableScanForFiles.setChecked(mApplication.isEnabledFolderScanningForFiles());
	}

	/**
	 * Update selected folders.
	 */
	public void updateSelectedFolders() {
		String summary, temp;
		SelectedFolderModel[] folders = mApplication.getSelectedFolders();
		if (folders.length > 0) {
			summary = folders[0].getFullPath();
			if (folders.length > 1) {
				summary += ", ";
				temp = folders[1].getFullPath();
				summary += temp.substring(0,
					10 < temp.length() ? 10 : temp.length());
				summary += "...";
			}
			mFolderScanningPref.setSummary(summary);
		}
	}

	/**
	 * When the user click on define file name patterns preferences.
	 */
	private void onDefineFileNamePatterns() {
		if (selectFileNamePatternDialog == null) {
			selectFileNamePatternDialog = new SelectFileNamePatternDialog(this, mApplication, this);
		}
		selectFileNamePatternDialog.show();
	}

	/**
	 * Start to select a folder.
	 */
	private void onFolderScanningPref() {
		if (selectFoldersListDialog == null) {
			selectFoldersListDialog = new SelectFoldersListDialog(this, mApplication, this);
		} else {
			selectFoldersListDialog.updateSelectedFolders();
		}
		selectFoldersListDialog.show();
	}

	/**
	 * Method used when the user choose the option: Enable folder scanning for files.
	 */
	private void onEnableScanForFilesPref() {
		if (!mApplication.isEnabledFolderScanningForFiles()) {
			mEnableScanForFiles.setChecked(false);
			showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.enable_folder_scanning_confirmation),
				false,
				ID_CONFIRMATION_ENABLE_FOLDER_FILES_SCANNING);
		} else {
			modifiedPreferences = true;
			mApplication.saveBooleanValue(DSCApplication.KEY_ENABLED_SCAN_FILES, false);
			updateEnableScanForFilesCheckBox();
		}
	}

	/**
	 * Invoked when the user click on the "Create rename service shortcut"
	 * preference.
	 */
	private void onToggleRenameShortcut() {
		boolean isCreated = mApplication.isRenameShortcutCreated();
		boolean mustCreate = isCreated ? false : true;
		if (mApplication.getSdkInt() < 26) {
			createOrRemoveRenameShortcut(mustCreate);
		} else {
			createOrRemoveRenameShortcutApi26(mustCreate);
		}
	}

	/**
	 * Method invoked by the user to start rename service.
	 */
	private void onManuallyStartRename() {
		if (mApplication.getSdkInt() < Build.VERSION_CODES.LOLLIPOP_MR1 && (!mApplication.isEnabledFolderScanning()
			|| mApplication.getSelectedFolders().length == 0)) {
			showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.enable_filter_alert_v21), false,
				ID_CONFIRMATION_ALERT);
		} else {
			showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.confirmation_rename_question), false,
				ID_CONFIRMATION_MANUAL_RENAME);
		}
	}

	/**
	 * Start the rename service.
	 */
	private void startRenameServiceManually() {
		mApplication.logD(TAG, "startRenameServiceManually");
		mApplication.launchAutoRenameTask(this, true, null, true);
	}

	/**
	 * Method invoked when was pressed the fileRenameCount preference.
	 */
	private void onResetFileRenameCounter() {
		if (mApplication.getFileRenameCount() > 0) {
			showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.file_rename_count_confirmation), false,
				ID_CONFIRMATION_RESET_RENAME_COUNTER);
		}
	}

	/**
	 * Method invoked when was pressed the request permission preference.
	 */
	private void onRequestPermissions() {
		showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.request_permissions_confirmation), false,
			ID_CONFIRMATION_REQUEST_PERMISSIONS);
	}

	/**
	 * Method invoked when was pressed the Export application settings preference.
	 */
	private void onExportSettings() {
		showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.export_settings_confirmation), false,
			ID_CONFIRMATION_EXPORT_SETTINGS);
	}

	/**
	 * Method invoked when was pressed the Import application settings preference.
	 */
	private void onImportSettings() {
		showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.import_settings_confirmation), false,
			ID_CONFIRMATION_IMPORT_SETTINGS);
	}

	/**
	 * Confirmed reset file rename counter.
	 */
	private void confirmedResetFileRenameCounter() {
		mApplication.logD(TAG, "Reset file rename counter!");
		mApplication.increaseFileRenameCount(-1);
	}

	/**
	 * Show about pop up dialog message.
	 */
	private void onBuildVersion() {
		displayLocalizedAssets(R.string.about_section, "about");
	}

	/**
	 * Method invoked when the user chose to send a debug.
	 */
	private void onSendDebugReport() {
		showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.send_debug_confirmation), false,
			ID_CONFIRMATION_DEBUG_REPORT);
	}

	/**
	 * Show license info.
	 */
	private void onLicensePref() {
		displayAssets(R.string.license_title, "gpl-3.0-standalone.html");
	}

	/**
	 * Show the privacy policy
	 */
	private void onPrivacyPolicy() {
		displayAssets(R.string.privacy_policy, "privacy-policy.html");
	}

	/**
	 * Show the help file.
	 */
	private void onShowHelpPage() {
		displayLocalizedAssets(R.string.help_title, "help");
	}

	/**
	 * Search for localized resources files.
	 *
	 * @param titleId        The title id from the resources.
	 * @param fileNamePrefix The file name prefix used to identify the file form the assets folder.
	 */
	private void displayLocalizedAssets(int titleId, String fileNamePrefix) {
		String language = mApplication.getLanguageCode();
		String fileName = fileNamePrefix + language + ".html";
		try {
			if (!Arrays.asList(mApplication.getAppAssets().list("")).contains(fileName)) {
				fileName = fileNamePrefix + "-en.html";
			}
		} catch (IOException e) {
		}
		displayAssets(titleId, fileName);
	}

	/**
	 * Display an asset file content.
	 *
	 * @param titleId  The title to be displayed.
	 * @param fileName The file name used to obtain the content.
	 */
	private void displayAssets(int titleId, String fileName) {
		Intent intent = new Intent(getBaseContext(), InfoActivity.class);
		Bundle b = new Bundle();
		b.putInt(InfoActivity.TITLE, titleId);
		b.putString(InfoActivity.FILE_NAME, fileName);
		b.putBoolean(InfoActivity.HTML_MESSAGE, true);
		intent.putExtras(b);
		startActivity(intent);
	}

	/**
	 * Method invoked when was pressed the donatePref preference.
	 */
	private void onDonatePref() {
		showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.donate_confirmation), false,
			ID_CONFIRMATION_DONATION);
	}

	/**
	 * Show a confirmation popup dialog.
	 *
	 * @param message            Message of the confirmation dialog.
	 * @param messageContainLink A boolean flag which mark if the text contain links.
	 * @param confirmationId     ID of the process to be executed if confirmed.
	 */
	private void showConfirmationDialog(String message,
										boolean messageContainLink, final int confirmationId) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle(R.string.app_name);
		if (messageContainLink) {
			ScrollView scrollView = new ScrollView(this);
			SpannableString spanText = new SpannableString(message);
			Linkify.addLinks(spanText, Linkify.ALL);

			TextView textView = new TextView(this);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(spanText);

			scrollView.setPadding(14, 2, 10, 12);
			scrollView.addView(textView);
			alertDialog.setView(scrollView);
		} else {
			alertDialog.setMessage(message);
		}
		alertDialog.setCancelable(false);
		if (confirmationId == ID_CONFIRMATION_ALERT) {
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setNeutralButton(R.string.ok, null);
		} else if (confirmationId == ID_CONFIRMATION_INFO) {
			alertDialog.setIcon(android.R.drawable.ic_dialog_info);
			alertDialog.setNeutralButton(R.string.ok, null);
		} else {
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						onConfirmation(confirmationId);
					}
				});
			alertDialog.setNegativeButton(R.string.no, null);
		}
		AlertDialog alert = alertDialog.create();
		alert.show();
	}

	/**
	 * Execute proper confirmation process based on received confirmation ID.
	 *
	 * @param confirmationId Received confirmation ID.
	 */
	protected void onConfirmation(int confirmationId) {
		switch (confirmationId) {
			case ID_CONFIRMATION_DONATION:
				confirmedDonationPage();
				break;
			case ID_CONFIRMATION_RESET_RENAME_COUNTER:
				confirmedResetFileRenameCounter();
				break;
			case ID_CONFIRMATION_DEBUG_REPORT:
				confirmedSendReport(mApplication.getApplicationContext().getString(R.string.send_debug_email_title));
				break;
			case ID_CONFIRMATION_MANUAL_RENAME:
				startRenameServiceManually();
				break;
			case ID_CONFIRMATION_EXPORT_SETTINGS:
				confirmedExportSettings();
				break;
			case ID_CONFIRMATION_IMPORT_SETTINGS:
				confirmedImportSettings();
				break;
			case ID_CONFIRMATION_USE_INTERNAL_SELECT_FOLDER:
				useInternalSelectFolderDialog(0);
				break;
			case ID_CONFIRMATION_ENABLE_FOLDER_FILES_SCANNING:
				mApplication.saveBooleanValue(DSCApplication.KEY_ENABLED_SCAN_FILES, true);
				updateEnableScanForFilesCheckBox();
				modifiedPreferences = true;
				break;
			case ID_CONFIRMATION_REQUEST_PERMISSIONS: {
				String[] permissions = mApplication.getNotGrantedPermissions();
				if (Utilities.isEmpty(permissions)) {
					showConfirmationDialog(
						mApplication.getApplicationContext().getString(R.string.request_permissions_ok),
						false,
						ID_CONFIRMATION_ALERT);
				} else {
					requestForPermissions(mApplication.getNotGrantedPermissions());
				}
			}
			break;
		}
	}

	/**
	 * Access the browser to open the donation page.
	 */
	private void confirmedDonationPage() {
		String url = mApplication.getApplicationContext().getString(R.string.donate_url);
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
				"confirmedDonationPage Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Method invoked when the rename file async task is started.
	 */
	@Override
	public void onTaskStarted() {
		mApplication.createProgressDialog(this, this,
			mApplication.getApplicationContext().getString(R.string.manually_service_running));
		mApplication.showProgressDialog();
	}

	/**
	 * Method invoked from the rename task when an update is required.
	 *
	 * @param position Current number of renamed files.
	 * @param max      Maximum number of files to be renamed
	 */
	@Override
	public void onTaskUpdate(int position, int max) {
		String message = String.format(
			position == 1
				? getString(R.string.manually_file_rename_progress_1)
				: getString(R.string.manually_file_rename_progress_more),
			position, max);

		if (position == 0 && max > 0) {
			mApplication.createProgressDialog(this, this, message, max);
			mApplication.showProgressDialog();
		} else {
			mApplication.setProgressDialogMessage(message);
			mApplication.setProgressDialogProgress(position);
		}
	}

	/**
	 * Method invoked at the end of rename file async task.
	 *
	 * @param count Number of renamed files.
	 */
	@Override
	public void onTaskFinished(int count) {
		String message;
		switch (count) {
			case -1:
				message = mApplication.getApplicationContext().getString(R.string.manually_file_rename_minus_1);
				break;
			case 0:
				message = mApplication.getApplicationContext()
					.getString(R.string.manually_file_rename_count_0);
				break;
			case 1:
				message = mApplication.getApplicationContext()
					.getString(R.string.manually_file_rename_count_1);
				break;
			default:
				message = count > 0 ?
					mApplication.getApplicationContext().getString(R.string.manually_file_rename_count_more, count) :
					mApplication.getApplicationContext().getString(R.string.manually_file_rename_minus_more, (count * -1));
				break;
		}
		mApplication.hideProgressDialog();
		showAlertDialog(android.R.drawable.ic_dialog_info, message, DO_NOT_SHOW_IGNORED);
	}

	/**
	 * Display an alert dialog with a custom message.
	 *
	 * @param iconId           The resource ID for the dialog icon.
	 * @param message          Message to be displayed on an alert dialog.
	 * @param doNotShowAgainId ID for cases when the user chose to not show again this message.
	 */
	private void showAlertDialog(int iconId, String message, final int doNotShowAgainId) {
		final Context context = SettingsActivity.this;
		if (!isFinishing()) {
			final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			final LayoutInflater layoutInflater = LayoutInflater.from(context);
			final View view = layoutInflater.inflate(R.layout.do_not_show_again, null);

			if (doNotShowAgainId != DO_NOT_SHOW_IGNORED) {
				dialog.setView(view);
			}
			dialog.setTitle(R.string.app_name)
				.setMessage(message)
				.setIcon(iconId)
				.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog,
										int whichButton) {
						boolean flag = false;
						View dlgView = view.findViewById(R.id.skip);
						if (dlgView instanceof CheckBox) {
							flag = ((CheckBox) dlgView).isChecked();
						}
						doShowAlertDialog(doNotShowAgainId, flag);
					}
				});
			dialog.show();
		}
	}

	/**
	 * Invoked when the user close the Alert Dialog.
	 *
	 * @param noShowAgainId ID for cases when the user chose to not show again this message.
	 * @param checked       Boolean flag which indicate if the do not show checkbox was checked.
	 */
	private void doShowAlertDialog(int noShowAgainId, boolean checked) {
		if (DO_NOT_SHOW_GRANT_URI_PERMISSION == noShowAgainId && checked) {
			mApplication.setDisplayNotGrantUriPermission(false);
		}
	}

	@Override
	public void onProgressCancel() {
		mApplication.setRenameFileTaskCanceled(true);
	}

	/**
	 * Create or remove rename shortcut from the home screen.
	 *
	 * @param create True if the shortcut should be created.
	 */
	private void createOrRemoveRenameShortcut(boolean create) {
		String action = create ? RenameShortcutUpdateListener.INSTALL_SHORTCUT
			: RenameShortcutUpdateListener.UNINSTALL_SHORTCUT;

		Intent shortcutIntent = new Intent();
		shortcutIntent.setAction(action);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
			getActivityIntent());
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
			mApplication.getApplicationContext().getString(R.string.rename_shortcut_name));
		shortcutIntent.putExtra("duplicate", false);
		if (create) {
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(
					mApplication.getApplicationContext(),
					R.drawable.ic_manual_rename));
		}
		mApplication.getApplicationContext().sendBroadcast(shortcutIntent);
	}

	/**
	 * Create or remove rename shortcut from the home screen using ShortcutManager from API 25.
	 *
	 * @param create True if the shortcut should be created.
	 */
	@TargetApi(26)
	private void createOrRemoveRenameShortcutApi26(boolean create) {
		ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
		if (shortcutManager.isRequestPinShortcutSupported()) {
			if (create) {
				String label = mApplication.getApplicationContext().getString(R.string.rename_shortcut_name);
				ShortcutInfo shortcut = new ShortcutInfo.Builder(this, DSCApplication.ID_SHORTCUT_RENAME)
					.setShortLabel(label)
					.setLongLabel(label)
					.setIcon(Icon.createWithResource(mApplication.getApplicationContext(),
						R.drawable.ic_manual_rename))
					.setIntent(getActivityIntent())
					.build();
				try {
					shortcutManager.requestPinShortcut(shortcut, null);
					mApplication.updateShortcutPref(TYPE.INSTALL);
					updateShortcutFields();
				} catch (IllegalArgumentException e) {
					showAlertDialog(android.R.drawable.ic_dialog_info,
						mApplication.getApplicationContext().getString(R.string.create_rename_shortcut_v26_error),
						DO_NOT_SHOW_IGNORED);
				}
			} else {
				shortcutManager.disableShortcuts(Arrays.asList(DSCApplication.ID_SHORTCUT_RENAME));
				mApplication.updateShortcutPref(TYPE.UNINSTALL);
				updateShortcutFields();
			}
		}
	}

	/**
	 * Create the manually rename service shortcut intent.
	 *
	 * @return The manually rename service shortcut intent.
	 */
	private Intent getActivityIntent() {
		Intent activityIntent = new Intent(mApplication.getApplicationContext(),
			RenameDlgActivity.class);
		activityIntent.setAction(Intent.ACTION_MAIN);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		return activityIntent;
	}

	/**
	 * Update Rename service shortcut fields, descriptions and enabled/disabled
	 * properties.
	 */
	private void updateShortcutFields() {
		if (mApplication.isRenameShortcutCreated()) {
			mHideRenameServiceStartConfirmation.setEnabled(true);
			if (mApplication.getSdkInt() > 25) {
				mToggleRenameShortcut.setTitle(R.string.remove_rename_shortcut_v26);
				mToggleRenameShortcut.setSummary(R.string.remove_rename_shortcut_v26_desc);
			} else {
				mToggleRenameShortcut.setTitle(R.string.remove_rename_shortcut);
				mToggleRenameShortcut
					.setSummary(R.string.remove_rename_shortcut_desc);
			}
		} else {
			mToggleRenameShortcut.setTitle(R.string.create_rename_shortcut);
			mToggleRenameShortcut
				.setSummary(R.string.create_rename_shortcut_desc);
			mHideRenameServiceStartConfirmation.setEnabled(false);
		}
	}

	/**
	 * Method invoked by the rename shortcut broadcast.
	 */
	@Override
	public void updateRenameShortcut() {
		updateShortcutFields();
	}

	/**
	 * User just confirmed to send a report.
	 */
	private void confirmedSendReport(String emailTitle) {
		mApplication.createProgressDialog(this, this,
			mApplication.getApplicationContext().getString(R.string.send_debug_title));
		mApplication.showProgressDialog();
		String message = mApplication.getApplicationContext().getString(R.string.report_body);
		File logsFolder = mApplication.getLogsFolder();
		File archive = getLogArchive(logsFolder);
		String[] TO = {"ciubex@yahoo.com"};

		Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);

		ArrayList<Uri> uris = new ArrayList<>();
		if (archive != null && archive.exists() && archive.length() > 0) {
			uris.add(Uri.parse("content://" + CachedFileProvider.AUTHORITY
				+ "/" + archive.getName()));
		}
		if (!uris.isEmpty()) {
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		mApplication.hideProgressDialog();
		try {
			startActivityForResult(Intent.createChooser(emailIntent,
				mApplication.getApplicationContext().getString(R.string.send_report)), REQUEST_SEND_REPORT);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
				"confirmedSendReport Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Generate debugging report with mounted volumes and content of file /proc/mounts
	 */
	private void generateMountPointsReport(FileWriter writer) throws IOException {
		String filePath = "/proc/mounts";
		LineNumberReader lnr = null;
		String line;
		writer.write("---------------------------------------------------------" + '\n');
		try {
			writer.write("Content of: " + filePath + '\n');
			File file = new File(filePath);
			if (file.exists()) {
				lnr = new LineNumberReader(new FileReader(file));
				while ((line = lnr.readLine()) != null) {
					writer.write(line + '\n');
				}
			} else {
				writer.write("File: " + filePath + " does not exist!" + '\n');
			}
		} catch (IOException e) {
			mApplication.logE(TAG, e.getMessage(), e);
		} finally {
			writer.write("---------------------------------------------------------" + '\n');
			Utilities.doClose(lnr);
		}
		writer.write("List of MountedVolumes: " + mApplication.getMountedVolumes().size() + " elements." + '\n');
		for (MountVolume volume : mApplication.getMountedVolumes()) {
			writer.write(volume.toString() + '\n');
		}
		writer.write("---------------------------------------------------------" + '\n');
	}

	/**
	 * Generate debugging report with contents of media store.
	 */
	private void generateMediaStoreQueries(FileWriter writer) throws IOException {
		ContentResolver contentResolver = mApplication.getContentResolver();
		Utilities.doQuery(writer, contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		Utilities.doQuery(writer, contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
		Utilities.doQuery(writer, contentResolver, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		Utilities.doQuery(writer, contentResolver, MediaStore.Video.Media.INTERNAL_CONTENT_URI);
	}

	/**
	 * Build the logs and call the archive creator.
	 *
	 * @param logsFolder The logs folder.
	 * @return The archive file which should contain the logs.
	 */
	private File getLogArchive(File logsFolder) {
		File logFile = mApplication.getLogFile();
		File logcatFile = getLogcatFile(logsFolder);
		List<File> files = new ArrayList<>();
		if (logFile != null) {
			files.add(logFile);
		}
		files.add(logcatFile);
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
		String archiveName = "DSC_logs_" + format.format(now) + ".zip";
		return getArchives(files, logsFolder, archiveName);
	}

	/**
	 * Method used to build a ZIP archive with log files.
	 *
	 * @param files       The log files to be added.
	 * @param logsFolder  The logs folder where should be added the archive name.
	 * @param archiveName The archive file name.
	 * @return The archive file.
	 */
	private File getArchives(List<File> files, File logsFolder, String archiveName) {
		File archive = new File(logsFolder, archiveName);
		try {
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(archive);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte data[] = new byte[BUFFER];
			FileInputStream fi;
			ZipEntry entry;
			int count;
			for (File file : files) {
				if (file.exists() && file.length() > 0) {
					mApplication.logD(TAG, "Adding to archive: " + file.getName());
					fi = new FileInputStream(file);
					origin = new BufferedInputStream(fi, BUFFER);
					entry = new ZipEntry(file.getName());
					out.putNextEntry(entry);
					while ((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
					}
					Utilities.doClose(entry);
					Utilities.doClose(origin);
				}
			}
			Utilities.doClose(out);
		} catch (FileNotFoundException e) {
			mApplication.logE(TAG, "getArchives failed: FileNotFoundException", e);
		} catch (IOException e) {
			mApplication.logE(TAG, "getArchives failed: IOException", e);
		}
		return archive;
	}

	/**
	 * Generate logs file on cache directory.
	 *
	 * @param cacheFolder Cache directory where are the logs.
	 * @return File with the logs.
	 */
	private File getLogcatFile(File cacheFolder) {
		File logFile = new File(cacheFolder, "DSC_logcat.log");
		Process shell = null;
		InputStreamReader reader = null;
		FileWriter writer = null;
		char LS = '\n';
		char[] buffer = new char[BUFFER];
		String model = Build.MODEL;
		if (!model.startsWith(Build.MANUFACTURER)) {
			model = Build.MANUFACTURER + " " + model;
		}
		mApplication.logD(TAG, "Prepare Logs to be send via e-mail.");
		String oldCmd = "logcat -d -v threadtime ro.ciubex.dscautorename:v dalvikvm:v System.err:v *:s";
		String newCmd = "logcat -d -v threadtime";
		String command = newCmd;
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			if (mApplication.getSdkInt() <= 15) {
				command = oldCmd;
			}
			shell = Runtime.getRuntime().exec(command);
			reader = new InputStreamReader(shell.getInputStream());
			writer = new FileWriter(logFile);
			writer.write("Android version: " + Build.VERSION.SDK_INT +
				" (" + Build.VERSION.CODENAME + ")" + LS);
			writer.write("Device: " + model + LS);
			writer.write("Device name: " + DevicesUtils.getDeviceName(mApplication.getAppAssets()) + LS);
			writer.write("App version: " + mApplication.getVersionName() +
				" (" + mApplication.getVersionCode() + ")" + LS);
			mApplication.writeSharedPreferences(writer);
			int n;
			do {
				n = reader.read(buffer, 0, BUFFER);
				if (n == -1) {
					break;
				}
				writer.write(buffer, 0, n);
			} while (true);
			generateMediaStoreQueries(writer);
			generateMountPointsReport(writer);
			shell.waitFor();
		} catch (IOException e) {
			mApplication.logE(TAG, "getLogcatFile failed: IOException", e);
		} catch (InterruptedException e) {
			mApplication.logE(TAG, "getLogcatFile failed: InterruptedException", e);
		} catch (Exception e) {
			mApplication.logE(TAG, "getLogcatFile failed: Exception", e);
		} finally {
			Utilities.doClose(writer);
			Utilities.doClose(reader);
			if (shell != null) {
				shell.destroy();
			}
		}
		return logFile;
	}

	/**
	 * This method is invoked when a child activity is finished and this
	 * activity is showed again
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		doNotDisplayNotGrantUriPermission = true;
		if (requestCode == REQUEST_SEND_REPORT) {
//			mApplication.deleteLogFile();
		} else if (resultCode == RESULT_OK && (
			requestCode == REQUEST_OPEN_DOCUMENT_TREE ||
				requestCode == REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER ||
				requestCode == REQUEST_OPEN_DOCUMENT_TREE_SETTINGS)
		) {
			if (mApplication.getSdkInt() >= 21) {
				processActionOpenDocumentTree(requestCode, data);
			}
		}
	}

	/**
	 * Process resulted data from new API regarding selected tree.
	 *
	 * @param resultData Resulted data from selected folder.
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void processActionOpenDocumentTree(int requestCode, Intent resultData) {
		Uri uri = resultData.getData();
		int flags = resultData.getFlags();
		mApplication.logD(TAG, "Selected on OpenDocumentTree uri: " + uri);
		SelectedFolderModel selectedFolder = new SelectedFolderModel();
		selectedFolder.fromUri(uri, flags);
		MountVolume volume = mApplication.getMountVolumeByUuid(selectedFolder.getUuid());
		if (volume != null && !Utilities.isEmpty(volume.getPath())) {
			selectedFolder.setRootPath(volume.getPath());
			mApplication.logD(TAG, "Selected from OpenDocumentTree: " + selectedFolder);
			switch (requestCode) {
				case REQUEST_OPEN_DOCUMENT_TREE:
					updateSelectFoldersListDialog(selectedFolder);
					break;
				case REQUEST_OPEN_DOCUMENT_TREE_MOVE_FOLDER:
					updateSelectedFolder(selectedFolder);
					break;
				case REQUEST_OPEN_DOCUMENT_TREE_SETTINGS:
					exportOrImportSettings(selectedFolder);
					break;
			}
		}
	}

	/**
	 * Update select folder list dialog.
	 *
	 * @param selectedFolder Selected folder model.
	 */
	private void updateSelectFoldersListDialog(SelectedFolderModel selectedFolder) {
		int index = -1;
		if (selectFoldersListDialog != null) {
			index = selectFoldersListDialog.getSelectedIndex();
		}
		mApplication.setFolderScanning(index, selectedFolder);
		if (selectFoldersListDialog != null) {
			selectFoldersListDialog.updateSelectedFolders();
			mApplication.updateFolderObserverList();
		}
	}

	/**
	 * Update the pattern dialog with selected folder.
	 *
	 * @param selectedFolder The selected folder.
	 */
	private void updateSelectedFolder(SelectedFolderModel selectedFolder) {
		if (selectFileNamePatternDialog != null) {
			selectFileNamePatternDialog.updateSelectedFolder(selectedFolder);
		}
	}

	/**
	 * Callback for the result from requesting permissions.
	 *
	 * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (PERMISSIONS_REQUEST_CODE == requestCode) {
			mApplication.markPermissionsAsked();
			for (String permission : permissions) {
				mApplication.markPermissionAsked(permission);
			}
			updateSettingsOptionsByPermissions();
		}
	}

	/**
	 * Update settings options based on the allowed permissions.
	 */
	private void updateSettingsOptionsByPermissions() {
		boolean allowed;
		if (mApplication.shouldAskPermissions()) {
			// functionality
			allowed = mApplication.haveFunctionalPermissions();
			mServiceTypeList.setEnabled(allowed);
			mRenameVideoEnabled.setEnabled(allowed);
			mRenameServiceStartDelay.setEnabled(allowed);
			mDelayUnit.setEnabled(allowed);
			mRenameFileDelay.setEnabled(allowed);
			mEnabledFolderScanning.setEnabled(allowed);
			mFolderScanningPref.setEnabled(allowed);
			mEnableScanForFiles.setEnabled(allowed);
			mDefineFileNamePatterns.setEnabled(allowed);
			mSendBroadcastEnabled.setEnabled(allowed);
			mInvokeMediaScannerEnabled.setEnabled(allowed);
			mFileNameSuffixFormat.setEnabled(allowed);
			mRenameFileDateType.setEnabled(allowed);
			mAppendOriginalName.setEnabled(allowed);
			mManuallyStartRename.setEnabled(allowed);
			// shortcut
			allowed = allowed && mApplication.haveShortcutPermissions();
			mToggleRenameShortcut.setEnabled(allowed);
			mHideRenameServiceStartConfirmation.setEnabled(allowed);
			// logs
			allowed = true;// mApplication.haveLogsPermissions();
			mSendDebugReport.setEnabled(allowed);
		}
	}

	/**
	 * Set the text color for the preference summary.
	 *
	 * @param preference The preference item.
	 * @param color      The color to set.
	 */
	private void setColorPreferencesSummary(Preference preference, int color) {
		CharSequence cs = preference.getSummary();
		String plainTitle = cs.subSequence(0, cs.length()).toString();
		Spannable coloredTitle = new SpannableString(plainTitle);
		coloredTitle.setSpan(new ForegroundColorSpan(color), 0, coloredTitle.length(), 0);
		preference.setSummary(coloredTitle);
	}

	/**
	 * Check for all selected folders used if have grant URI permissions.
	 */
	private void checkAllSelectedFolders() {
		if (doNotDisplayNotGrantUriPermission) {
			return;
		}
		SelectedFolderModel folderMove;
		List<SelectedFolderModel> selectedFolders = new ArrayList<>();
		for (SelectedFolderModel folder : mApplication.getSelectedFolders()) {
			if (!selectedFolders.contains(folder)) {
				selectedFolders.add(folder);
			}
		}
		for (FileNameModel fileNameModel : mApplication.getOriginalFileNamePattern()) {
			folderMove = fileNameModel.getSelectedFolder();
			if (Utilities.isMoveFiles(folderMove)) {
				if (!selectedFolders.contains(folderMove)) {
					selectedFolders.add(folderMove);
				}
			}
		}
		if (!selectedFolders.isEmpty()) {
			List<String> list = mApplication.doGrantUriPermission(mApplication.getContentResolver(), selectedFolders);
			if (!list.isEmpty()) {
				displayNotGrantUriPermissionAlertFor(list);
			}
		}
	}

	/**
	 * Display in error message for all folders which do not have granted URI permission.
	 *
	 * @param folderList List of folders for which the application do not have access permission.
	 */
	private void displayNotGrantUriPermissionAlertFor(List<String> folderList) {
		String message;
		if (folderList.size() == 1) {
			message = mApplication.getApplicationContext().
				getString(R.string.folder_list_no_grant_permission_1, folderList.get(0));
		} else {
			StringBuilder sb = new StringBuilder();
			for (String folder : folderList) {
				if (sb.length() > 0) {
					sb.append('\n');
				}
				sb.append(folder);
			}
			message = mApplication.getApplicationContext().
				getString(R.string.folder_list_no_grant_permission_2, sb.toString());
		}
		showAlertDialog(android.R.drawable.ic_dialog_alert, message, DO_NOT_SHOW_GRANT_URI_PERMISSION);
	}

	/**
	 * Method used when is confirmed the export application settings.
	 */
	private void confirmedExportSettings() {
		confirmedActionId = ID_CONFIRMATION_EXPORT_SETTINGS;
		startToSelectFolder();
	}

	/**
	 * Method used when is confirmed the import application settings.
	 */
	private void confirmedImportSettings() {
		confirmedActionId = ID_CONFIRMATION_IMPORT_SETTINGS;
		startToSelectFolder();
	}

	/**
	 * Method used to start a folder selection by the user.
	 */
	private void startToSelectFolder() {
		if (mApplication.getSdkInt() < 21) {
			useInternalSelectFolderDialog(0);
		} else {
			startIntentActionOpenDocumentTree(SettingsActivity.REQUEST_OPEN_DOCUMENT_TREE_SETTINGS);
		}
	}

	/**
	 * Launch the internal select folder dialog.
	 *
	 * @param position The position from list of folders.
	 */
	private void useInternalSelectFolderDialog(int position) {
		new SelectFolderDialog(this, mApplication, this, position).show();
	}

	/**
	 * This method should return a selected folder for the initialization, here is not implemented.
	 *
	 * @return Null.
	 */
	@Override
	public String getSelectedFolder() {
		// ignored
		return null;
	}

	/**
	 * At the end when the user selected a folder here will be the results about selected folder.
	 *
	 * @param folderIndex    Passed position index, but will be ignored.
	 * @param selectedFolder Model of selected folder.
	 */
	@Override
	public void onFolderSelected(int folderIndex, SelectedFolderModel selectedFolder) {
		exportOrImportSettings(selectedFolder);
	}

	/**
	 * Initiate the folder chosen API
	 */
	@TargetApi(21)
	private void startIntentActionOpenDocumentTree(int requestCode) {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
		try {
			this.startActivityForResult(intent, requestCode);
		} catch (Exception e) {
			mApplication.logE(TAG, "startIntentActionOpenDocumentTree: " + e.getMessage(), e);
			showConfirmationDialog(mApplication.getApplicationContext().getString(R.string.folder_list_no_open_document_tree_support), false,
				ID_CONFIRMATION_USE_INTERNAL_SELECT_FOLDER);
		}
	}

	/**
	 * Method used after the user selected a folder.
	 *
	 * @param selectedFolder Which folder was selected by the user.
	 */
	private void exportOrImportSettings(SelectedFolderModel selectedFolder) {
		switch (confirmedActionId) {
			case ID_CONFIRMATION_EXPORT_SETTINGS:
				executeExportSettings(selectedFolder);
				break;
			case ID_CONFIRMATION_IMPORT_SETTINGS:
				executeImportSettings(selectedFolder);
				break;
		}
	}

	/**
	 * Method used to export settings.
	 *
	 * @param selectedFolder Folder where should be exported the settings.
	 */
	private void executeExportSettings(SelectedFolderModel selectedFolder) {
		new SettingsFileUtilAsyncTask(this, SettingsFileUtilAsyncTask.Operation.EXPORT, selectedFolder).execute();
	}

	/**
	 * Method used to import setting.
	 *
	 * @param selectedFolder Folder from where should be loaded the settings.
	 */
	private void executeImportSettings(SelectedFolderModel selectedFolder) {
		new SettingsFileUtilAsyncTask(this, SettingsFileUtilAsyncTask.Operation.IMPORT, selectedFolder).execute();
	}

	@Override
	public DSCApplication getDSCApplication() {
		return mApplication;
	}

	@Override
	public void startFileAsynkTask(SettingsFileUtilAsyncTask.Operation operationType) {
		mApplication.createProgressDialog(this, this,
			mApplication.getApplicationContext().getString(R.string.please_wait));
		mApplication.showProgressDialog();
	}

	@Override
	public void endFileAsynkTask(SettingsFileUtilAsyncTask.Operation operationType, AsyncTaskResult result) {
		String message = "";
		boolean restartApp = false;
		if (AsyncTaskResult.ERROR == result.resultId) {
			message = mApplication.getApplicationContext().
				getString(SettingsFileUtilAsyncTask.Operation.EXPORT == operationType ?
						R.string.export_settings_error : R.string.import_settings_error,
					result.resultMessage);
		} else {
			if (SettingsFileUtilAsyncTask.Operation.IMPORT == operationType) {
				restartApp = true;
			} else {
				message = mApplication.getApplicationContext().
					getString(R.string.export_settings_success, result.resultMessage);
			}
		}
		if (restartApp) {
			mApplication.updateMountedVolumes();
			mApplication.updateSelectedFolders();
			mApplication.checkRegisteredServiceType(false);
			mApplication.hideProgressDialog();
			restartActivity();
		} else {
			mApplication.hideProgressDialog();
			showAlertDialog(android.R.drawable.ic_dialog_alert, message, DO_NOT_SHOW_IGNORED);
		}
	}
}
