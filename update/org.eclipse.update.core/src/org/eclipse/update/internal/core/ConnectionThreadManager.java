/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Feb 19, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.eclipse.update.internal.core;

import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.*;

import org.eclipse.core.runtime.*;

/**
 * This class manages threads that are dispatched to 
 * obtained a valid input stream from an HTTP connection.
 * Since obtaining an input stream is an I/O operation
 * that may block for a long time, it is performed
 * on a separate thread to keep the UI responsive.
 * <p>
 * In case that a connection blocks and does not
 * terminate with an IOException after a timeout, 
 * active threads may accumulate. The manager will
 * refuse to create more than MAX_COUNT threads and
 * instead will throw a CoreException with a child
 * status object for each connection that is still pending.
 * <p>
 * If the connection is responsive but slow, the user
 * may cancel it. In that case, the manager will
 * close the stream to avoid resource leak.
 */
public class ConnectionThreadManager {
	// set connection timeout to 1 minute
	private static final String CONNECT_TIMEOUT = "60000";
	// set read timeout to 1 minute
	private static final String READ_TIMEOUT = "60000";
	// max number of active threads
	private static final int MAX_COUNT = 9;
	private Vector threads;

	public static class StreamRunnable implements Runnable {
		private HttpURLConnection urlConnection;
		private IOException exception;
		private InputStream is;
		private boolean disconnected;

		public StreamRunnable(HttpURLConnection urlConnection) {
			this.urlConnection = urlConnection;
		}

		public InputStream getInputStream() {
			return is;
		}

		public URL getURL() {
			return urlConnection.getURL();
		}

		public IOException getIOException() {
			return exception;
		}

		public void disconnect() {
			urlConnection.disconnect();
			disconnected = true;
		}

		public void run() {
			try {
				is = urlConnection.getInputStream();
				if (disconnected) {
					// The connection was slow, but returned
					// a valid input stream. However,
					// the user canceled the connection
					// so we must close to avoid 
					// resource leak.
					if (is != null) {
						try {
							is.close();
						} catch (IOException ex) {
							// at this point, we don't care
						} finally {
							is = null;
						}
					}
				}
			} catch (IOException e) {
				exception = e;
			}
		}
	}

	class ConnectionThread extends Thread {
		private StreamRunnable runnable;
		public ConnectionThread(StreamRunnable runnable) {
			super(runnable, "update-connection");
			this.runnable = runnable;
		}

		public StreamRunnable getRunnable() {
			return runnable;
		}
	}

	public ConnectionThreadManager() {
		// In case we are running Sun's code.
		setIfNotDefaultProperty("sun.net.client.defaultConnectTimeout", CONNECT_TIMEOUT);
		setIfNotDefaultProperty("sun.net.client.defaultReadTimeout", READ_TIMEOUT); 
	}
	
	private void setIfNotDefaultProperty(String key, String value) {
		String oldValue = System.getProperty(key);
		if (oldValue==null || oldValue.equals("-1"))
			System.setProperty(key, value);
	}

	public Thread createThread(StreamRunnable runnable) throws CoreException {
		validateExistingThreads();
		if (threads == null)
			threads = new Vector();
		Thread t = new ConnectionThread(runnable);
		t.setDaemon(true);
		threads.add(t);
		return t;
	}

	/*
	 * Removes threads that are not alive any more from the 
	 * list and ensures that there are at most MAX_COUNT threads
	 * still working.
	 */
	private void validateExistingThreads() throws CoreException {
		if (threads == null)
			return;
			
		int aliveCount = purgeTerminatedThreads();

		if (aliveCount > MAX_COUNT) {
			ArrayList children = new ArrayList();
			String pluginId =
				UpdateCore.getPlugin().getDescriptor().getUniqueIdentifier();
			for (int i = 0; i < threads.size(); i++) {
				ConnectionThread t = (ConnectionThread) threads.get(i);
				String url = t.getRunnable().getURL().toString();
				IStatus status =
					new Status(
						IStatus.ERROR,
						pluginId,
						IStatus.OK,
						Policy.bind(
							"ConnectionThreadManager.unresponsiveURL",
							url),
						null);
				children.add(status);
			}
			MultiStatus parentStatus =
				new MultiStatus(
					pluginId,
					IStatus.OK,
					(IStatus[]) children.toArray(new IStatus[children.size()]),
					Policy.bind("ConnectionThreadManager.tooManyConnections"),
					null);
			throw new CoreException(parentStatus);
		}
	}
	
	/*
	 * Removes terminated threads from the list and returns
	 * the number of those still active.
	 */
	
	private int purgeTerminatedThreads() {
		int aliveCount = 0;

		Object[] array = threads.toArray();
		for (int i = 0; i < array.length; i++) {
			Thread t = (Thread) array[i];
			if (!t.isAlive())
				threads.remove(t);
			else
				aliveCount++;
		}
		return aliveCount;
	}

	public void shutdown() {
		// We might want to kill the active threads but
		// this is not really necessary since they are all
		// daemons and will not prevent JVM to terminate.
		threads.clear();
	}
}
