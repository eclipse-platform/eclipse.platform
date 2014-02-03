/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.util.Hashtable;

/**
 * @author aniefer
 *
 */
public class LockManager {
	// lock
	private final static Object lock = new Object();

	// hashtable of locks
	private static Hashtable locks = new Hashtable();
	
	public static Object getLock(String key) {
		synchronized (lock) {
			if (locks.get(key) == null)
				locks.put(key, key);
			return locks.get(key);
		}
	}
	
	public static void returnLock(String key) {
		synchronized (lock) {
			locks.remove(key);
		}
	}
}
