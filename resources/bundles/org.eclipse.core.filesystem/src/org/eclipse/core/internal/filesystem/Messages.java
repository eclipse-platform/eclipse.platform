/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.internal.filesystem;

import org.eclipse.osgi.util.NLS;

/**
 * Provides translatable messages for the file system bundle
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.internal.filesystem.messages"; //$NON-NLS-1$

	public static String copying;
	public static String couldnotDelete;
	public static String couldnotDeleteReadOnly;
	public static String couldNotLoadLibrary;
	public static String couldNotMove;
	public static String couldNotRead;
	public static String couldNotWrite;
	public static String deleteProblem;
	public static String deleting;
	public static String failedCreateWrongType;
	public static String failedCreateAccessDenied;
	public static String failedMove;
	public static String failedReadDuringWrite;
	public static String fileExists;
	public static String fileNotFound;
	public static String moving;
	public static String noFileSystem;
	public static String noImplDelete;
	public static String noImplWrite;
	public static String noScheme;
	public static String notAFile;
	public static String readOnlyParent;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
