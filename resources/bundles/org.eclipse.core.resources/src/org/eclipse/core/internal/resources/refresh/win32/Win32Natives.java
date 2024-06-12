/*******************************************************************************
 * Copyright (c) 2002, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Mikael Barbero (Eclipse Foundation) - 286681 handle WAIT_ABANDONED_0 return value
 *     Hannes Wellmann - Migrate Win32Natives to use JNA instead of JNI to avoid native binaries and platform-specific fragments
 *******************************************************************************/

package org.eclipse.core.internal.resources.refresh.win32;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.WString;

/**
 * Hooks for native methods involved with win32 auto-refresh callbacks.
 */
public class Win32Natives {

	// All constants and native methods are defined with the exact same name
	// as in the native windows.h or referenced header files.

	/* general purpose */
	/**
	 * A general use constant expressing the value of an
	 * invalid handle.
	 */
	public static final long INVALID_HANDLE_VALUE = -1; // handleapi.h
	/**
	 * An error constant which indicates that the previous function
	 * succeeded.
	 */
	public static final int ERROR_SUCCESS = 0; // winerror.h
	/**
	 * An error constant which indicates that a handle is or has become
	 * invalid.
	 */
	public static final int ERROR_INVALID_HANDLE = 6; // winerror.h

	/** Access is denied. */
	public static final int ERROR_ACCESS_DENIED = 5; // winerror.h

	/**
	 * A constant which indicates the maximum number of objects
	 * that can be passed into WaitForMultipleObjects.
	 */
	public static final int MAXIMUM_WAIT_OBJECTS = 64; // winnt.h

	/* wait return values */
	/**
	 * A constant used returned WaitForMultipleObjects when the function times out.
	 */
	public static final int WAIT_TIMEOUT = 258; // winerror.h
	/**
	 * A constant used by WaitForMultipleObjects to indicate the object which was
	 * signaled.
	 */
	public static final int WAIT_OBJECT_0 = 0x0; // winbase.h
	/**
	 * A constant which indicates that some objects which
	 * were waiting to be signaled are an abandoned mutex
	 * objects.
	 */
	public static final int WAIT_ABANDONED_0 = 0x80; // winbase.h
	/**
	 * A constant returned by WaitForMultipleObjects which indicates
	 * that the wait failed.
	 */
	public static final int WAIT_FAILED = 0xffffffff; // winbase.h

	/* wait notification filter masks */
	/**
	 * Change filter for monitoring file rename, creation or deletion.
	 */
	public static final int FILE_NOTIFY_CHANGE_FILE_NAME = 0x01; // winnt.h
	/**
	 * Change filter for monitoring directory creation or deletion.
	 */
	public static final int FILE_NOTIFY_CHANGE_DIR_NAME = 0x02; // winnt.h
	/**
	 * Change filter for monitoring file size changes.
	 */
	public static final int FILE_NOTIFY_CHANGE_SIZE = 0x08; // winnt.h
	/**
	 * Change filter for monitoring the file write timestamp
	 */
	public static final int FILE_NOTIFY_CHANGE_LAST_WRITE = 0x10; // winnt.h

	private static class WindowsH {
		static {
			Native.register(NativeLibrary
					.getInstance("Kernel32" /* , W32APIOptions.DEFAULT_OPTIONS < type-conversion unnecessary> */)); //$NON-NLS-1$
		}
		// Direct-mappings are faster than interface-mappings
		// and avoiding type-conversions is faster again.
		// https://github.com/java-native-access/jna/blob/master/www/FunctionalDescription.md#direct-mapping

		// WinNT's type 'HANDLE is expressed as long
		// (in winnt.h it is defined as 'typedef HANDLE *PHANDLE;')

		// Methods from fileapi.h header

		static native long FindFirstChangeNotificationW(WString lpPathName, int bWatchSubtree, int dwNotifyFilter);

		static native int FindCloseChangeNotification(long hChangeHandle);

		static native int FindNextChangeNotification(long hChangeHandle);

		// Methods from synchapi.h

		static native int WaitForMultipleObjects(int nCount, long[] lpHandles, int bWaitAll, int dwMilliseconds);

		// direct type conversion methods

		static WString fromString(String value) {
			return new WString(value);
		}

		static int fromBoolean(boolean value) {
			return value ? 1 : 0;
		}

		static boolean toBoolean(int value) {
			return value != 0;
		}
	}

	private static final String LONG_PATH_PREFIX = "\\\\?\\"; //$NON-NLS-1$

	/**
	 * Creates a change notification object for the given path. The notification
	 * object allows the client to monitor changes to the directory and the
	 * subtree under the directory using FindNextChangeNotification or
	 * WaitForMultipleObjects.
	 * <p>
	 * The path must be no longer than 2^15 - 1 characters, if the given path is too
	 * long {@link #ERROR_INVALID_HANDLE} is returned.
	 *
	 * @param lpPathName The path to the directory to be monitored.
	 * @param bWatchSubtree If <code>true</code>, specifies that the entire
	 * 	tree under the given path should be monitored. If <code>false</code>
	 *  specifies that just the named path should be monitored.
	 * @param dwNotifyFilter Any combination of FILE_NOTIFY_CHANGE_FILE_NAME,
	 *  FILE_NOTIFY_CHANGE_DIR_NAME,   FILE_NOTIFY_CHANGE_ATTRIBUTES,
	 *  FILE_NOTIFY_CHANGE_SIZE,  FILE_NOTIFY_CHANGE_LAST_WRITE, or
	 *  FILE_NOTIFY_CHANGE_SECURITY.
	 * @return long The handle to the find change notification object or
	 *  ERROR_INVALID_HANDLE  if the attempt fails.
	 */
	public static long FindFirstChangeNotification(String lpPathName, boolean bWatchSubtree, int dwNotifyFilter) {
		WString wPathName = WindowsH
				.fromString(!lpPathName.startsWith(LONG_PATH_PREFIX) ? LONG_PATH_PREFIX + lpPathName : lpPathName);
		return WindowsH.FindFirstChangeNotificationW(wPathName, WindowsH.fromBoolean(bWatchSubtree), dwNotifyFilter);
	}

	/**
	 * Stops and disposes of the change notification object that corresponds to the given
	 * handle.  The handle cannot be used in future calls to FindNextChangeNotification or
	 * WaitForMultipleObjects
	 *
	 * @param hChangeHandle a handle which was created with FindFirstChangeNotification
	 * @return boolean <code>true</code> if the method succeeds, <code>false</code>
	 * otherwise.
	 */
	public static boolean FindCloseChangeNotification(long hChangeHandle) {
		return WindowsH.toBoolean(WindowsH.FindCloseChangeNotification(hChangeHandle));
	}

	/**
	 * Requests that the next change detected be signaled.  This method should only be
	 * called after FindFirstChangeNotification or WaitForMultipleObjects.  Once this
	 * method has been called on a given handle, further notification requests can be made
	 * through the WaitForMultipleObjects call.
	 * @param hChangeHandle a handle which was created with FindFirstChangeNotification
	 * @return boolean <code>true</code> if the method succeeds, <code>false</code> otherwise.
	 */
	public static boolean FindNextChangeNotification(long hChangeHandle) {
		return WindowsH.toBoolean(WindowsH.FindNextChangeNotification(hChangeHandle));
	}

	/**
	 * Returns when one of the following occurs.
	 * <ul>
	 *   <li>One of the objects is signaled, when bWaitAll is <code>false</code></li>
	 *   <li>All of the objects are signaled, when bWaitAll is <code>true</code></li>
	 *   <li>The timeout interval of dwMilliseconds elapses.</li>
	 * </ul>
	 * @param nCount The number of handles, cannot be greater than MAXIMUM_WAIT_OBJECTS.
	 * @param lpHandles The array of handles to objects to be waited upon cannot contain
	 * duplicate handles.
	 * @param bWaitAll If <code>true</code> requires all objects to be signaled before this
	 * method returns.  If <code>false</code>, indicates that only one object need be
	 * signaled for this method to return.
	 * @param dwMilliseconds A timeout value in milliseconds.  If zero, the function tests
	 * the objects and returns immediately.  If INFINITE, the function will only return
	 * when the objects have been signaled.
	 * @return int WAIT_TIMEOUT when the function times out before recieving a signal.
	 * WAIT_OBJECT_0 + n when a signal for the handle at index n.  WAIT_FAILED when this
	 * function fails.
	 */
	public static int WaitForMultipleObjects(int nCount, long[] lpHandles, boolean bWaitAll, int dwMilliseconds) {
		return WindowsH.WaitForMultipleObjects(nCount, lpHandles, WindowsH.fromBoolean(bWaitAll), dwMilliseconds);
	}

	/**
	 * Answers the last error set in the current thread.
	 * @return int the last error
	 */
	public static int GetLastError() {
		return Native.getLastError();
	}

}
