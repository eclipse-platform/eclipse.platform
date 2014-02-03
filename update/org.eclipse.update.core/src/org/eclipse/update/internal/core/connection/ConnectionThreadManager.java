/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core.connection;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;

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
	private static final String CONNECT_TIMEOUT = "60000"; //$NON-NLS-1$
	// set read timeout to 1 minute
	private static final String READ_TIMEOUT = "60000"; //$NON-NLS-1$
	// max number of active threads
	private static final int MAX_COUNT = 9;
	private List threads = new ArrayList(MAX_COUNT);
	
	

	public static class StreamRunnable implements Runnable {
		
		private URLConnection urlConnection;
		private IOException ioException;
		private Exception exception;
		private InputStream is;
		private boolean disconnected;

		public StreamRunnable(URLConnection urlConnection) {
			this.urlConnection = urlConnection;
		}

		public InputStream getInputStream() {
			return is;
		}

		public URL getURL() {
			return urlConnection.getURL();
		}

		public IOException getIOException() {
			return ioException;
		}
		
		public Exception getException() {
			return exception;
		}

		public void disconnect() {
			if (urlConnection instanceof HttpURLConnection)
				((HttpURLConnection)urlConnection).disconnect();
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
				ioException = e;
			} catch (Exception e) {
				exception = e;
			} finally {
				//threads.
			}
		}
	}

	
	class ConnectionThread extends Thread {
		
		private StreamRunnable runnable;
		
		public ConnectionThread(StreamRunnable runnable) {
			super(runnable, "update-connection"); //$NON-NLS-1$
			this.runnable = runnable;
		}

		public StreamRunnable getRunnable() {
			return runnable;
		}
	}

	public ConnectionThreadManager() {
		// In case we are running Sun's code.
		setIfNotDefaultProperty("sun.net.client.defaultConnectTimeout", CONNECT_TIMEOUT); //$NON-NLS-1$
		setIfNotDefaultProperty("sun.net.client.defaultReadTimeout", READ_TIMEOUT);  //$NON-NLS-1$
	}
	
	private void setIfNotDefaultProperty(String key, String value) {
		String oldValue = System.getProperty(key);
		if (oldValue==null || oldValue.equals("-1")) //$NON-NLS-1$
			System.setProperty(key, value);
	}

	public Thread getConnectionThread(StreamRunnable runnable) throws CoreException {
		
		validateExistingThreads();
	
		//if (threads == null)
		//	threads = new Vector();
		Thread t = new Thread(runnable);
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
		
		if ((threads == null) || (threads.size() == 0))
			return;
		
		int aliveCount = purgeTerminatedThreads();

		if (aliveCount > MAX_COUNT) {
			ArrayList children = new ArrayList();
			String pluginId =
				UpdateCore.getPlugin().getBundle().getSymbolicName();
			for (int i = 0; i < threads.size(); i++) {
				ConnectionThread t = (ConnectionThread) threads.get(i);
				String url = t.getRunnable().getURL().toString();
				IStatus status =
					new Status(
						IStatus.ERROR,
						pluginId,
						IStatus.OK,
						NLS.bind(Messages.ConnectionThreadManager_unresponsiveURL, (new String[] { url })),
						null);
				children.add(status);
			}
			MultiStatus parentStatus =
				new MultiStatus(
					pluginId,
					IStatus.OK,
					(IStatus[]) children.toArray(new IStatus[children.size()]),
					Messages.ConnectionThreadManager_tooManyConnections, 
					null);
			throw new CoreException(parentStatus);
		}
	}
	
	/*
	 * Removes terminated threads from the list and returns
	 * the number of those still active.
	 */
	
	private int purgeTerminatedThreads() {
		
		if (threads.size() == 0) {
			return 0;
		}
		
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
