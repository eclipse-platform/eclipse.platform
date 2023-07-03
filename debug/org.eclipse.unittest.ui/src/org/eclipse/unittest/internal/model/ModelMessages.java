/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.unittest.internal.model;

import org.eclipse.osgi.util.NLS;

/**
 * Unit Test Model messages
 */
public class ModelMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.unittest.internal.model.ModelMessages"; //$NON-NLS-1$

	public static String UnitTestModel_could_not_instantiate_support;
	public static String UnitTestModel_could_not_import;
	public static String UnitTestModel_could_not_export;
	public static String UnitTestModel_could_not_read;
	public static String UnitTestModel_could_not_write;
	public static String UnitTestModel_importing_from_url;
	public static String TestRunHandler_lines_read;

	public static String TestingSession_finished_status;
	public static String TestingSession_name_format;
	public static String TestingSession_starting_status;
	public static String TestingSession_stopped_status;

	public static String TestRunSession_unrootedTests;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ModelMessages.class);
	}

	private ModelMessages() {
	}
}
