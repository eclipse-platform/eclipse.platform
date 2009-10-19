/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;

/**
 * Constant definitions for Ant launch configurations.
 * <p>
 * Constant definitions only; not to be implemented.
 * </p>
 * @since 3.0
 */
public interface IAntLaunchConfigurationConstants {

	// ------- Common Ant Launch Configuration Attributes -------
	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 */
	public static final String ATTR_ANT_TARGETS = IAntLaunchConstants.ATTR_ANT_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute after a clean (full build) for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * @since 3.1
	 */
	public static final String ATTR_ANT_AFTER_CLEAN_TARGETS = IAntLaunchConstants.ATTR_ANT_AFTER_CLEAN_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during a manual build for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * @since 3.1
	 */
	public static final String ATTR_ANT_MANUAL_TARGETS = IAntLaunchConstants.ATTR_ANT_MANUAL_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during an auto build for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * @since 3.1
	 */
	public static final String ATTR_ANT_AUTO_TARGETS = IAntLaunchConstants.ATTR_ANT_AUTO_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during a clean for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * @since 3.1
	 */
	public static final String ATTR_ANT_CLEAN_TARGETS = IAntLaunchConstants.ATTR_ANT_CLEAN_TARGETS;
	
	/**
	 * Boolean attribute indicating whether or not target specification for an Ant builder has been updated for
	 * 3.1 
	 * 
	 * @since 3.1
	 */
	public static final String ATTR_TARGETS_UPDATED = IAntLaunchConstants.ATTR_TARGETS_UPDATED;
	
	/**
	 * Map attribute indicating the Ant properties to be defined during the
	 * build. Default value is <code>null</code> which indicates no additional
	 * properties will be defined.
	 */
	public static final String ATTR_ANT_PROPERTIES = IAntLaunchConstants.ATTR_ANT_PROPERTIES;					
	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that no additional property files
	 * will be defined. Format is a comma separated listing of property files.
	 */
	public static final String ATTR_ANT_PROPERTY_FILES = IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES;
	/**
	* String attribute indicating the custom runtime classpath to use for an Ant
	* build. Default value is <code>null</code> which indicates that the global
	* classpath is to be used. Format is a comma separated listing of URLs.
	* @deprecated no longer supported
	*/
	public static final String ATTR_ANT_CUSTOM_CLASSPATH = IExternalToolConstants.PLUGIN_ID + ".ATTR_ANT_CUSTOM_CLASSPATH"; //$NON-NLS-1$
	/**
	 * String attribute indicating the custom Ant home to use for an Ant build.
	 * Default value is <code>null</code> which indicates that no Ant home is to
	 * be set 
	 * @deprecated no longer supported
	 */
	public static final String ATTR_ANT_HOME = IExternalToolConstants.PLUGIN_ID + ".ATTR_ANT_HOME"; //$NON-NLS-1$
	/**
	 * Boolean attribute indicating whether or not internal targets (targets with no
	 * description) should be hidden from the user in the launch configuration dialog.
	 * Default value is <code>false</code> which indicates that all targets will be
	 * displayed.
	 * 
	 * @since 3.0
	 */
	public static final String ATTR_HIDE_INTERNAL_TARGETS = IAntLaunchConstants.ATTR_HIDE_INTERNAL_TARGETS;
	/**
	 * Integer attribute indicating which column targets should be sorted on. A
	 * value of 0 indicates target name, 1 indicates target description, and -1
	 * indicates no sort. Default value is -1.
	 * 
	 * @since 3.0
	 */
	public static final String ATTR_SORT_TARGETS = IAntLaunchConstants.ATTR_SORT_TARGETS;


	// ------- Launch configuration types --------
	/**
	 * Ant launch configuration type identifier.
	 */
	public static final String ID_ANT_LAUNCH_CONFIGURATION_TYPE = IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE;

	/**
	 * Ant builder launch configuration type identifier. Ant project builders
	 * are of this type.
	 */
	public static final String ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE = IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE;

	/**
	 * Identifier for Ant processes (value <code>org.eclipse.ant.ui.antProcess</code>). This identifier is
	 * set as the value for the <code>IProcess.ATTR_PROCESS_TYPE</code>
	 * attribute in processes created by the Ant launch delegate.
	 */
	public static final String ID_ANT_PROCESS_TYPE = IAntLaunchConstants.ID_ANT_PROCESS_TYPE;
}
