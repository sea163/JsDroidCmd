package com.jsdroid.uiautomator2;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.app.IActivityManager.ContentProviderHolder;
import android.app.UiAutomation;
import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Binder;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;

/**
 * @hide
 */
public class ShellUiAutomatorBridge extends UiAutomatorBridge {

	private static final String LOG_TAG = ShellUiAutomatorBridge.class
			.getSimpleName();

	public ShellUiAutomatorBridge(UiAutomation uiAutomation) {
		super(uiAutomation);
	}

	public Display getDefaultDisplay() {
		return DisplayManagerGlobal.getInstance().getRealDisplay(
				Display.DEFAULT_DISPLAY);
	}

	public long getSystemLongPressTime() {
		long longPressTimeout = 2000;
		try {
			IContentProvider provider = null;
			Cursor cursor = null;
			IActivityManager activityManager = ActivityManagerNative
					.getDefault();
			String providerName = Settings.Secure.CONTENT_URI.getAuthority();
			IBinder token = new Binder();
			try {
				ContentProviderHolder holder = activityManager
						.getContentProviderExternal(providerName,
								UserHandle.USER_OWNER, token);
				if (holder == null) {
					throw new IllegalStateException("Could not find provider: "
							+ providerName);
				}
				provider = holder.provider;
				cursor = provider.query(null, Settings.Secure.CONTENT_URI,
						new String[] { Settings.Secure.VALUE }, "name=?",
						new String[] { Settings.Secure.LONG_PRESS_TIMEOUT },
						null, null);
				if (cursor.moveToFirst()) {
					longPressTimeout = cursor.getInt(0);
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
				if (provider != null) {
					activityManager.removeContentProviderExternal(providerName,
							token);
				}
			}
		} catch (Throwable e) {
		}
		return longPressTimeout;
	}

	@Override
	public int getRotation() {
		IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager
				.getService(Context.WINDOW_SERVICE));
		int ret = -1;
		try {
			ret = (int) IWindowManager.class.getMethod("getRotation")
					.invoke(wm);
			return ret;
		} catch (Throwable e) {
		}
		try {
			ret = (int) IWindowManager.class.getMethod(
					"getDefaultDisplayRotation").invoke(wm);
			return ret;
		} catch (Throwable e) {
		}
		return ret;
	}

	@Override
	public boolean isScreenOn() {
		IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager
				.getService(Context.POWER_SERVICE));
		boolean ret = false;
		try {
			ret = (boolean) IPowerManager.class.getMethod("isInteractive")
					.invoke(pm);
			return ret;
		} catch (Throwable e) {
		}
		try {
			ret = (boolean) IPowerManager.class.getMethod("isScreenOn").invoke(
					pm);
			return ret;
		} catch (Throwable e) {
		}
		return ret;
	}

	public Context getContext() {
		return ActivityThread.currentActivityThread().getApplication();
	}
}
