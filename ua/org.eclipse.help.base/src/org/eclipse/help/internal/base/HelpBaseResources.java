/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.help.internal.base;

import org.eclipse.osgi.util.NLS;

public final class HelpBaseResources extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.help.internal.base.HelpBaseResources";//$NON-NLS-1$

	private HelpBaseResources() {
		// Do not instantiate
	}

	public static String UpdatingIndex;
	public static String Preparing_for_indexing;
	public static String Writing_index;
	public static String HelpWebappNotStarted;
	public static String HelpDisplay_exceptionMessage;
	public static String no_browsers;
	public static String CustomBrowser_errorLaunching;
	public static String MozillaFactory_dataMissing;
	public static String MozillaBrowserAdapter_executeFailed;
	public static String HelpApplication_couldNotStart;
	public static String IndexToolApplication_propertyNotSet;
	public static String IndexToolApplication_cannotDelete;
	public static String IndexerJob_name;
	public static String IndexerJob_error;
	public static String selectAll;
	public static String selectWorkingSet;
	public static String WorkingSetContent;
	public static String LocalHelpPage_capabilityFiltering_name;
	public static String InfoCenter_fileNotFound;
	public static String InfoCenter_io;
	public static String WebSearch_click;
	public static String WebSearch_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, HelpBaseResources.class);
	}

	public static String InfoCenter_connecting;
	public static String InfoCenter_searching;
	public static String InfoCenter_processing;
	public static String HelpIndexBuilder_noDestinationPath;
	public static String HelpIndexBuilder_buildingIndex;
	public static String HelpIndexBuilder_indexFor;
	public static String HelpIndexBuilder_error;
	public static String HelpIndexBuilder_cannotFindDoc;
	public static String HelpIndexBuilder_errorWriting;
	public static String HelpIndexBuilder_incompleteIndex;
	public static String HelpIndexBuilder_cannotScrub;
	public static String HelpIndexBuilder_cannotCreateDest;
	public static String HelpIndexBuilder_errorExtractingId;
	public static String HelpIndexBuilder_errorParsing;
	public static String PreferenceNameDefault;
	public static String HelpSearch_QueryTooComplex;
	public static String EnabledTopicFilterName;
	public static String SearchScopeFilterName;
}
