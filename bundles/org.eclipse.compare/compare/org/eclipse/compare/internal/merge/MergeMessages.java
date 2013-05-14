/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.merge;

import org.eclipse.osgi.util.NLS;

public final class MergeMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.compare.internal.merge.MergeMessages";//$NON-NLS-1$

	private MergeMessages() {
		// Do not instantiate
	}

	public static String TextAutoMerge_inputEncodingError;
	public static String TextAutoMerge_outputEncodingError;
	public static String TextAutoMerge_outputIOError;
	public static String TextAutoMerge_conflict;

	static {
		NLS.initializeMessages(BUNDLE_NAME, MergeMessages.class);
	}
}