/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.launching;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;

/**
 * Constant definitions for Ant launch configurations.
 * 
 * @since 1.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAntLaunchConstants {

	/**
	 * Ant launch configuration type identifier.
	 */
	public static final String ID_ANT_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ant.AntLaunchConfigurationType"; //$NON-NLS-1$

	public static final String MAIN_TYPE_NAME= "org.eclipse.ant.internal.launching.remote.InternalAntRunner"; //$NON-NLS-1$
	
	/**
	 * Ant builder launch configuration type identifier. Ant project builders
	 * are of this type.
	 */
	public static final String ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE = "org.eclipse.ant.AntBuilderLaunchConfigurationType"; //$NON-NLS-1$

	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 */
	public static final String ATTR_ANT_TARGETS = IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_ANT_TARGETS"; //$NON-NLS-1$

	/**
	 * String attribute indicating the Ant targets to execute after a clean
	 * (full build) for an Ant builder. Default value is <code>null</code> which
	 * indicates that the default target is to be executed. Format is a comma
	 * separated listing of targets.
	 */
	public static final String ATTR_ANT_AFTER_CLEAN_TARGETS = "org.eclipse.ant.ui.ATTR_ANT_AFTER_CLEAN_TARGETS"; //$NON-NLS-1$

	/**
	 * String attribute indicating the Ant targets to execute during a manual
	 * build for an Ant builder. Default value is <code>null</code> which
	 * indicates that the default target is to be executed. Format is a comma
	 * separated listing of targets.
	 */
	public static final String ATTR_ANT_MANUAL_TARGETS = "org.eclipse.ant.ui.ATTR_ANT_MANUAL_TARGETS"; //$NON-NLS-1$

	/**
	 * String attribute indicating the Ant targets to execute during an auto
	 * build for an Ant builder. Default value is <code>null</code> which
	 * indicates that the default target is to be executed. Format is a comma
	 * separated listing of targets.
	 */
	public static final String ATTR_ANT_AUTO_TARGETS = "org.eclipse.ant.ui.ATTR_ANT_AUTO_TARGETS"; //$NON-NLS-1$

	/**
	 * String attribute indicating the Ant targets to execute during a clean for
	 * an Ant builder. Default value is <code>null</code> which indicates that
	 * the default target is to be executed. Format is a comma separated listing
	 * of targets.
	 */
	public static final String ATTR_ANT_CLEAN_TARGETS = "org.eclipse.ant.ui.ATTR_ANT_CLEAN_TARGETS"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating whether or not target specification for an
	 * Ant builder has been updated for 3.1
	 */
	public static final String ATTR_TARGETS_UPDATED = "org.eclipse.ant.ui.ATTR_TARGETS_UPDATED"; //$NON-NLS-1$

	/**
	 * Map attribute indicating the Ant properties to be defined during the
	 * build. Default value is <code>null</code> which indicates no additional
	 * properties will be defined.
	 */
	public static final String ATTR_ANT_PROPERTIES = IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_ANT_PROPERTIES"; //$NON-NLS-1$					

	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that no additional property files will
	 * be defined. Format is a comma separated listing of property files.
	 */
	public static final String ATTR_ANT_PROPERTY_FILES = IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_ANT_PROPERTY_FILES"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating whether or not internal targets (targets
	 * with no description) should be hidden from the user in the launch
	 * configuration dialog. Default value is <code>false</code> which indicates
	 * that all targets will be displayed.
	 */
	public static final String ATTR_HIDE_INTERNAL_TARGETS = IExternalToolConstants.UI_PLUGIN_ID + ".ATTR_HIDE_INTERNAL_TARGETS"; //$NON-NLS-1$

	/**
	 * Integer attribute indicating which column targets should be sorted on. A
	 * value of 0 indicates target name, 1 indicates target description, and -1
	 * indicates no sort. Default value is -1.
	 */
	public static final String ATTR_SORT_TARGETS = IExternalToolConstants.UI_PLUGIN_ID + "ATTR_SORT_TARGETS"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating if the default VM install should be used for
	 * the separate JRE build Default value is <code>false</code> for backwards
	 * compatibility
	 */
	public static final String ATTR_DEFAULT_VM_INSTALL = "org.eclipse.ant.ui.DEFAULT_VM_INSTALL"; //$NON-NLS-1$

	/**
	 * Identifier for Ant processes (value
	 * <code>org.eclipse.ant.ui.antProcess</code>). This identifier is set as
	 * the value for the <code>IProcess.ATTR_PROCESS_TYPE</code> attribute in
	 * processes created by the Ant launch delegate.
	 */
	public static final String ID_ANT_PROCESS_TYPE = "org.eclipse.ant.ui.antProcess"; //$NON-NLS-1$

}
