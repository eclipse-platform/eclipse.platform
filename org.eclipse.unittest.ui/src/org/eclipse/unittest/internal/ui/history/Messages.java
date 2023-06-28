/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.unittest.internal.ui.history;

import org.eclipse.osgi.util.NLS;

/**
 * History messages
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.unittest.internal.ui.history.messages"; //$NON-NLS-1$
	public static String HistoryDialog_date;
	public static String HistoryDialog_export;
	public static String HistoryDialog_failures;
	public static String HistoryDialog_import;
	public static String HistoryDialog_name;
	public static String HistoryDialog_progress;
	public static String HistoryDialog_remove;
	public static String HistoryDialog_result;
	public static String HistoryDialog_selectExport;
	public static String HistoryDialog_selectImport;
	public static String HistoryDialog_title;
	public static String HistoryDialog_size;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
