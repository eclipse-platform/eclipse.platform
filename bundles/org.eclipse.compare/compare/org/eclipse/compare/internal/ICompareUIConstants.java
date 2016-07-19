/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;


public interface ICompareUIConstants {
	public final String PREFIX = CompareUIPlugin.getPluginId() + "."; //$NON-NLS-1$
	
	public static final String DTOOL_NEXT= "dlcl16/next_nav.gif";	//$NON-NLS-1$
	public static final String ETOOL_NEXT= "elcl16/next_nav.gif";	//$NON-NLS-1$
	public static final String CTOOL_NEXT= ETOOL_NEXT;
	
	public static final String DTOOL_PREV= "dlcl16/prev_nav.gif";	//$NON-NLS-1$
	public static final String ETOOL_PREV= "elcl16/prev_nav.gif";	//$NON-NLS-1$
	public static final String CTOOL_PREV= ETOOL_PREV;
	
	public static final String HUNK_OBJ = "obj16/hunk_obj.gif"; //$NON-NLS-1$

	public static final String ERROR_OVERLAY= "ovr16/error_ov.gif"; //$NON-NLS-1$
	public static final String IS_MERGED_OVERLAY= "ovr16/merged_ov.gif"; //$NON-NLS-1$
	public static final String REMOVED_OVERLAY= "ovr16/removed_ov.gif"; //$NON-NLS-1$
	public static final String WARNING_OVERLAY= "ovr16/warning_ov.gif"; //$NON-NLS-1$

	public static final String RETARGET_PROJECT= "eview16/compare_view.gif";	//$NON-NLS-1$
	
	public static final String IGNORE_WHITESPACE_ENABLED= "etool16/ignorews_edit.gif";	//$NON-NLS-1$
	public static final String IGNORE_WHITESPACE_DISABLED= "dtool16/ignorews_edit.gif";	//$NON-NLS-1$
	
	public static final String PROP_ANCESTOR_VISIBLE = PREFIX + "AncestorVisible"; //$NON-NLS-1$
	public static final String PROP_IGNORE_ANCESTOR = PREFIX + "IgnoreAncestor"; //$NON-NLS-1$
	public static final String PROP_TITLE = PREFIX + "Title"; //$NON-NLS-1$
	public static final String PROP_TITLE_IMAGE = PREFIX + "TitleImage"; //$NON-NLS-1$
	public static final String PROP_SELECTED_EDITION = PREFIX + "SelectedEdition"; //$NON-NLS-1$
	
	public static final int COMPARE_IMAGE_WIDTH= 22;
	
	public static final String PREF_NAVIGATION_END_ACTION= PREFIX + "NavigationEndAction"; //$NON-NLS-1$
	public static final String PREF_NAVIGATION_END_ACTION_LOCAL= PREFIX + "NavigationEndActionLocal"; //$NON-NLS-1$
	public static final String PREF_VALUE_PROMPT = "prompt"; //$NON-NLS-1$
	public static final String PREF_VALUE_LOOP = "loop"; //$NON-NLS-1$
	public static final String PREF_VALUE_NEXT = "next"; //$NON-NLS-1$
	public static final String PREF_VALUE_DO_NOTHING = "doNothing"; //$NON-NLS-1$

	public static final String COMMAND_IGNORE_WHITESPACE = PREFIX + "ignoreWhiteSpace"; //$NON-NLS-1$
}
