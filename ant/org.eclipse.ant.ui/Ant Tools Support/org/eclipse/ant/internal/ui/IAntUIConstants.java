/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49380, 49445, bug 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui;


/**
 * Defines the constants available for client use.
 * <p>
 * This interface is not intended to be extended or implemented by clients.
 * </p>
 */
public interface IAntUIConstants {
	/**
	 * Plugin identifier for Ant ui(value <code>org.eclipse.ant.ui</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.ant.ui"; //$NON-NLS-1$;
	

	// ------- Views -------

	/**
	 * Ant View identifier (value <code>org.eclipse.ant.ui.AntView</code>).
	 */
	public static final String ANT_VIEW_ID = PLUGIN_ID + ".AntView"; //$NON-NLS-1$

	// ------- Images -------
	/**
	 * Main tab image.
	 */
	public static final String IMG_TAB_MAIN = PLUGIN_ID + ".IMG_TAB_MAIN"; //$NON-NLS-1$

	public static final String IMG_PROPERTY = PLUGIN_ID + ".IMG_PROPERTY"; //$NON-NLS-1$
	
	public static final String IMG_TAB_ANT_TARGETS = PLUGIN_ID + ".IMG_TAB_ANT_TARGETS"; //$NON-NLS-1$
	public static final String IMG_TAB_CLASSPATH = PLUGIN_ID + ".IMG_TAB_CLASSPATH"; //$NON-NLS-1$;
	
	//	Label images
	 public static final String IMG_ANT= PLUGIN_ID + ".ant"; //$NON-NLS-1$
	 public static final String IMG_ANT_PROJECT= PLUGIN_ID + ".antProject"; //$NON-NLS-1$
	 public static final String IMG_ANT_TARGET= PLUGIN_ID + ".antTarget"; //$NON-NLS-1$
	 public static final String IMG_ANT_TARGET_INTERNAL = PLUGIN_ID + ".antPrivateTarget"; //$NON-NLS-1$
	 public static final String IMG_ANT_DEFAULT_TARGET= PLUGIN_ID + ".antDefaultTarget"; //$NON-NLS-1$
	 public static final String IMG_ANT_TARGET_ERROR = PLUGIN_ID + ".antTargetError"; //$NON-NLS-1$
	 public static final String IMG_ANT_MACRODEF = PLUGIN_ID + ".antMacrodef"; //$NON-NLS-1$
     public static final String IMG_ANT_IMPORT = PLUGIN_ID + ".antImport"; //$NON-NLS-1$
     public static final String IMG_ANT_TASKDEF = PLUGIN_ID + ".antTaskdef"; //$NON-NLS-1$
     public static final String IMG_ANT_ECLIPSE_RUNTIME_OBJECT = PLUGIN_ID + ".antEclipse"; //$NON-NLS-1$
     
     //editor toolbar
     public static final String IMG_SEGMENT_EDIT = PLUGIN_ID + ".segmentEdit"; //$NON-NLS-1$
	 public static final String IMG_MARK_OCCURRENCES = PLUGIN_ID + ".markOccurrences"; //$NON-NLS-1$
     
	/**
	 * Icon for task proposal.
	 */
	public static final String IMG_TASK_PROPOSAL = PLUGIN_ID + ".html_tab_obj"; //$NON-NLS-1$
	public static final String IMG_TEMPLATE_PROPOSAL= PLUGIN_ID + ".template_obj"; //$NON-NLS-1$
	
	// Action images
	public static final String IMG_REMOVE= PLUGIN_ID + ".remove"; //$NON-NLS-1$
	public static final String IMG_REMOVE_ALL= PLUGIN_ID + ".removeAll"; //$NON-NLS-1$
	public static final String IMG_ADD= PLUGIN_ID + ".add"; //$NON-NLS-1$
	public static final String IMG_RUN= PLUGIN_ID + ".run"; //$NON-NLS-1$
	public static final String IMG_SEARCH= PLUGIN_ID + ".search"; //$NON-NLS-1$
	public static final String IMG_FILTER_INTERNAL_TARGETS= PLUGIN_ID + ".filterInternalTargets"; //$NON-NLS-1$
	public static final String IMG_FILTER_IMPORTED_ELEMENTS= PLUGIN_ID + ".filterImportedElements"; //$NON-NLS-1$
	public static final String IMG_FILTER_PROPERTIES= PLUGIN_ID + ".filterProperties"; //$NON-NLS-1$
	public static final String IMG_FILTER_TOP_LEVEL= PLUGIN_ID + ".filterTopLevel"; //$NON-NLS-1$
	public static final String IMG_SORT_OUTLINE= PLUGIN_ID + ".sortOutline"; //$NON-NLS-1$
	public static final String IMG_LINK_WITH_EDITOR= PLUGIN_ID + ".linkWithEditor"; //$NON-NLS-1$
	public static final String IMG_REFRESH = PLUGIN_ID + ".IMG_REFRESH"; //$NON-NLS-1$
	
	public static final String IMG_WIZARD_BANNER = PLUGIN_ID + ".IMG_WIZARD_BANNER"; //$NON-NLS-1$
    public static final String IMG_EXPORT_WIZARD_BANNER = PLUGIN_ID + ".IMG_EXPORT_WIZARD_BANNER"; //$NON-NLS-1$
	 
	//	Overlays
	public static final String IMG_OVR_ERROR = PLUGIN_ID + ".ovrError";  //$NON-NLS-1$
	public static final String IMG_OVR_WARNING = PLUGIN_ID + ".ovrWarning";  //$NON-NLS-1$
	public static final String IMG_OVR_IMPORT = PLUGIN_ID + ".ovrImport"; //$NON-NLS-1$
	
	public static final String IMG_ANT_TYPE= PLUGIN_ID + ".type"; //$NON-NLS-1$

	// -------- Status Codes -------------
	
	/**
	 * Status code used by the 'Run Ant' status handler which is invoked when
	 * the launch dialog is opened by the 'Run Ant' action.
	 */
	public static final int STATUS_INIT_RUN_ANT = 1000;
	
	public static final String DIALOGSTORE_LASTEXTJAR= PLUGIN_ID + ".lastextjar"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTEXTFILE= PLUGIN_ID + ".lastextfile"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTFOLDER= PLUGIN_ID + ".lastfolder"; //$NON-NLS-1$
	public static final String DIALOGSTORE_LASTANTHOME= PLUGIN_ID + ".lastanthome"; //$NON-NLS-1$
	
	/**
	 * Size of left-hand column for right-justified task name.
	 * Used for Ant Build logging.
	 * @see org.eclipse.ant.internal.launching.runtime.logger.AntProcessBuildLogger
     * @see org.eclipse.ant.internal.launching.launchConfigurations.RemoteAntBuildListener
	  */
	public static final int LEFT_COLUMN_SIZE = 15;
	
	/**
	 * Id of the IProcessFactory to use when creating processes for remote Ant builds.
	 */
	public static final String REMOTE_ANT_PROCESS_FACTORY_ID= "org.eclipse.ant.ui.remoteAntProcessFactory"; //$NON-NLS-1$
	
	/**
	 * Boolean attribute indicating if an input handler should be supplied for the build
	 * Default value is <code>true</code>.
	 */
	public static final String SET_INPUTHANDLER= PLUGIN_ID + "SET_INPUTHANDLER"; //$NON-NLS-1$	
}
