/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.*;

/**
 * Internal class.
 */
public class Locker_JavaIo implements Locker {
	private File lockFile;
	private RandomAccessFile lockRAF;

	public Locker_JavaIo(File lockFile) {
		this.lockFile = lockFile;
	}

	public synchronized boolean lock() throws IOException {
		//if the lock file already exists, try to delete,
		//assume failure means another eclipse has it open
		if (lockFile.exists())
			lockFile.delete();
		if (lockFile.exists())
			return false;

		//open the lock file so other instances can't co-exist
		lockRAF = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
		lockRAF.writeByte(0);

		return true;
	}

	public synchronized void release() {
		try {
			if (lockRAF != null) {
				lockRAF.close();
				lockRAF = null;
			}
		} catch (IOException e) {
			//don't complain, we're making a best effort to clean up
		}
		if (lockFile != null)
			lockFile.delete();
	}
}
