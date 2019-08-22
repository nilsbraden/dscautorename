/**
 * This file is part of DSCAutoRename application.
 * <p>
 * Copyright (C) 2014 Claudiu Ciobotariu
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
package ro.ciubex.dscautorename.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;

import ro.ciubex.dscautorename.DSCApplication;

/**
 * This is the cache file provider used to access the log files.
 *
 * @author Claudiu Ciobotariu
 */
public class CachedFileProvider extends ContentProvider {

	// The authority is the symbolic name for the provider class
	public static final String AUTHORITY = "ro.ciubex.dscautorename.provider";

	// UriMatcher used to match against incoming requests
	private UriMatcher uriMatcher;

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "*", 1);
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#openFile(android.net.Uri,
	 * java.lang.String)
	 */
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
		throws FileNotFoundException {
		if (getContext() == null)
			return null;

		if (1 == uriMatcher.match(uri)) {
			String fileLocation = getContext().getCacheDir() + File.separator
				+ DSCApplication.LOGS_FOLDER_NAME + File.separator
				+ uri.getLastPathSegment();
			File file = new File(fileLocation);
			if (file.exists()) {
				return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
			} else {
				throw new FileNotFoundException("File not exist: " + fileLocation);
			}
		} else {
			throw new FileNotFoundException("Unsupported uri: " + uri.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		if (getContext() == null)
			return null;

		if (1 == uriMatcher.match(uri)) {
			MatrixCursor cursor = null;
			File file = new File(getContext().getCacheDir() + File.separator
				+ DSCApplication.LOGS_FOLDER_NAME + File.separator
				+ uri.getLastPathSegment());
			if (file.exists()) {
				cursor = new MatrixCursor(new String[]{
					OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
				cursor.addRow(new Object[]{uri.getLastPathSegment(),
					file.length()});
			}
			return cursor;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(@NonNull Uri uri) {
		String result = null;
		if (1 == uriMatcher.match(uri)) {
			result = "text/plain";
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		// Unused...
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		// Unused...
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// Unused...
		return 0;
	}

}
