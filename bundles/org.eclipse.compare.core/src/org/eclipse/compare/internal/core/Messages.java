/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.compare.internal.core.messages"; //$NON-NLS-1$
	public static String Activator_1;
	public static String FileDiffResult_0;
	public static String FileDiffResult_1;
	public static String FileDiffResult_2;
	public static String FileDiffResult_3;
	public static String Patcher_0;
	public static String Patcher_1;
	public static String Patcher_2;
	public static String WorkspacePatcher_0;
	public static String WorkspacePatcher_1;
	public static String RangeComparatorLCS_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/*
	 * (non Javadoc) Cannot be instantiated.
	 */
	private Messages() {
		// nothing to do
	}
}
