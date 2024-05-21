/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.resources.refresh.win32;

/**
 * Hooks for native methods involved with win32 auto-refresh callbacks.
 */
public class Win32Natives {
	/* general purpose */
	/**
	 * A general use constant expressing the value of an
	 * invalid handle.
	 */
	public static final long INVALID_HANDLE_VALUE;
	/**
	 * An error constant which indicates that the previous function
	 * succeeded.
	 */
	public static final int ERROR_SUCCESS;
	/**
	 * An error constant which indicates that a handle is or has become
	 * invalid.
	 */
	public static final int ERROR_INVALID_HANDLE;

	/** Access is denied. */
	public static final int ERROR_ACCESS_DENIED = 5;

	/**
	 * A constant which indicates the maximum number of objects
	 * that can be passed into WaitForMultipleObjects.
	 */
	public static final int MAXIMUM_WAIT_OBJECTS;

	/* wait return values */
	/**
	 * A constant used returned WaitForMultipleObjects when the function times out.
	 */
	public static final int WAIT_TIMEOUT;
	/**
	 * A constant used by WaitForMultipleObjects to indicate the object which was
	 * signaled.
	 */
	public static final int WAIT_OBJECT_0;
	/**
	 * A constant which indicates that some objects which
	 * were waiting to be signaled are an abandoned mutex
	 * objects.
	 */
	public static final int WAIT_ABANDONED_0;
	/**
	 * A constant returned by WaitForMultipleObjects which indicates
	 * that the wait failed.
	 */
	public static final int WAIT_FAILED;

	/* wait notification filter masks */
	/**
	 * Change filter for monitoring file rename, creation or deletion.
	 */
	public static final int FILE_NOTIFY_CHANGE_FILE_NAME;
	/**
	 * Change filter for monitoring directory creation or deletion.
	 */
	public static final int FILE_NOTIFY_CHANGE_DIR_NAME;
	/**
	 * Change filter for monitoring file size changes.
	 */
	public static final int FILE_NOTIFY_CHANGE_SIZE;
	/**
	 * Change filter for monitoring the file write timestamp
	 */
	public static final int FILE_NOTIFY_CHANGE_LAST_WRITE;

	/*
	 * Make requests to set the constants.
	 */
	static {
		System.loadLibrary("win32refresh"); //$NON-NLS-1$
		INVALID_HANDLE_VALUE = INVALID_HANDLE_VALUE();
		ERROR_SUCCESS = ERROR_SUCCESS();
		ERROR_INVALID_HANDLE = ERROR_INVALID_HANDLE();

		MAXIMUM_WAIT_OBJECTS = MAXIMUM_WAIT_OBJECTS();

		WAIT_TIMEOUT = WAIT_TIMEOUT();
		WAIT_OBJECT_0 = WAIT_OBJECT_0();
		WAIT_ABANDONED_0 = WAIT_ABANDONED_0();
		WAIT_FAILED = WAIT_FAILED();

		FILE_NOTIFY_CHANGE_FILE_NAME = FILE_NOTIFY_CHANGE_FILE_NAME();
		FILE_NOTIFY_CHANGE_DIR_NAME = FILE_NOTIFY_CHANGE_DIR_NAME();
		FILE_NOTIFY_CHANGE_SIZE = FILE_NOTIFY_CHANGE_SIZE();
		FILE_NOTIFY_CHANGE_LAST_WRITE = FILE_NOTIFY_CHANGE_LAST_WRITE();
	}

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
		return FindFirstChangeNotificationW(lpPathName, bWatchSubtree, dwNotifyFilter);
	}

	/**
	 * Creates a change notification object for the given path. This notification object
	 * allows the client to monitor changes to the directory and the subtree
	 * under the directory using FindNextChangeNotification or
	 * WaitForMultipleObjects.
	 *
	 * @param lpPathName The path to the directory to be monitored. Cannot be <code>null</code>,
	 *  or longer than 2^15 - 1 characters.
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
	private static native long FindFirstChangeNotificationW(String lpPathName, boolean bWatchSubtree, int dwNotifyFilter);

	/**
	 * Stops and disposes of the change notification object that corresponds to the given
	 * handle.  The handle cannot be used in future calls to FindNextChangeNotification or
	 * WaitForMultipleObjects
	 *
	 * @param hChangeHandle a handle which was created with FindFirstChangeNotification
	 * @return boolean <code>true</code> if the method succeeds, <code>false</code>
	 * otherwise.
	 */
	public static native boolean FindCloseChangeNotification(long hChangeHandle);

	/**
	 * Requests that the next change detected be signaled.  This method should only be
	 * called after FindFirstChangeNotification or WaitForMultipleObjects.  Once this
	 * method has been called on a given handle, further notification requests can be made
	 * through the WaitForMultipleObjects call.
	 * @param hChangeHandle a handle which was created with FindFirstChangeNotification
	 * @return boolean <code>true</code> if the method succeeds, <code>false</code> otherwise.
	 */
	public static native boolean FindNextChangeNotification(long hChangeHandle);

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
	public static native int WaitForMultipleObjects(int nCount, long[] lpHandles, boolean bWaitAll, int dwMilliseconds);

	/**
	 * Answers the last error set in the current thread.
	 * @return int the last error
	 */
	public static native int GetLastError();

	/**
	 * Returns the constant FILE_NOTIFY_CHANGE_LAST_WRITE.
	 * @return int
	 */
	private static native int FILE_NOTIFY_CHANGE_LAST_WRITE();

	/**
	 * Returns the constant FILE_NOTIFY_CHANGE_DIR_NAME.
	 * @return int
	 */
	private static native int FILE_NOTIFY_CHANGE_DIR_NAME();

	/**
	 * Returns the constant FILE_NOTIFY_CHANGE_SIZE.
	 * @return int
	 */
	private static native int FILE_NOTIFY_CHANGE_SIZE();

	/**
	 * Returns the constant FILE_NOTIFY_CHANGE_FILE_NAME.
	 * @return int
	 */
	private static native int FILE_NOTIFY_CHANGE_FILE_NAME();

	/**
	 * Returns the constant MAXIMUM_WAIT_OBJECTS.
	 * @return int
	 */
	private static native int MAXIMUM_WAIT_OBJECTS();

	/**
	 * Returns the constant WAIT_OBJECT_0.
	 * @return int
	 */
	private static native int WAIT_OBJECT_0();

	/**
	 * Returns the constant WAIT_ABANDONED_0.
	 * @return int
	 */
	private static native int WAIT_ABANDONED_0();

	/**
	 * Returns the constant WAIT_FAILED.
	 * @return int
	 */
	private static native int WAIT_FAILED();

	/**
	 * Returns the constant WAIT_TIMEOUT.
	 * @return int
	 */
	private static native int WAIT_TIMEOUT();

	/**
	 * Returns the constant ERROR_INVALID_HANDLE.
	 * @return int
	 */
	private static native int ERROR_INVALID_HANDLE();

	/**
	 * Returns the constant ERROR_SUCCESS.
	 * @return int
	 */
	private static native int ERROR_SUCCESS();

	/**
	 * Returns the constant INVALID_HANDLE_VALUE.
	 * @return long
	 */
	private static native long INVALID_HANDLE_VALUE();

}
