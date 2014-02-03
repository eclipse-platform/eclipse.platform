/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.ui.launching;

import org.eclipse.ant.launching.IAntLaunchConstants;

/**
 * Constant definitions for Ant launch configurations.
 * 
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 * @deprecated use {@link IAntLaunchConstants}
 */
public interface IAntLaunchConfigurationConstants {

	/**
	 * Ant launch configuration type identifier.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ID_ANT_LAUNCH_CONFIGURATION_TYPE}
	 */
	public static final String ID_ANT_LAUNCH_CONFIGURATION_TYPE = IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE;

	/**
	 * Ant builder launch configuration type identifier. Ant project builders
	 * are of this type.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE}
	 */
	public static final String ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE = IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE;
	
	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_TARGETS}
	 */
	public static final String ATTR_ANT_TARGETS = IAntLaunchConstants.ATTR_ANT_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute after a clean (full build) for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_AFTER_CLEAN_TARGETS}
	 */
	public static final String ATTR_ANT_AFTER_CLEAN_TARGETS = IAntLaunchConstants.ATTR_ANT_AFTER_CLEAN_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during a manual build for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_MANUAL_TARGETS}
	 */
	public static final String ATTR_ANT_MANUAL_TARGETS = IAntLaunchConstants.ATTR_ANT_MANUAL_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during an auto build for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_AUTO_TARGETS}
	 */
	public static final String ATTR_ANT_AUTO_TARGETS = IAntLaunchConstants.ATTR_ANT_AUTO_TARGETS;
	
	/**
	 * String attribute indicating the Ant targets to execute during a clean for an Ant builder. Default value is
	 * <code>null</code> which indicates that the default target is to be
	 * executed. Format is a comma separated listing of targets.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_CLEAN_TARGETS}
	 */
	public static final String ATTR_ANT_CLEAN_TARGETS = IAntLaunchConstants.ATTR_ANT_CLEAN_TARGETS;
	
	/**
	 * Boolean attribute indicating whether or not target specification for an Ant builder 
	 * has been updated for 3.1 
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_TARGETS_UPDATED}
	 */
	public static final String ATTR_TARGETS_UPDATED = IAntLaunchConstants.ATTR_TARGETS_UPDATED;
	
	/**
	 * Map attribute indicating the Ant properties to be defined during the
	 * build. Default value is <code>null</code> which indicates no additional
	 * properties will be defined.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_PROPERTIES}
	 */
	public static final String ATTR_ANT_PROPERTIES = IAntLaunchConstants.ATTR_ANT_PROPERTIES;					
	
	/**
	 * String attribute indicating the Ant targets to execute. Default value is
	 * <code>null</code> which indicates that no additional property files
	 * will be defined. Format is a comma separated listing of property files.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_ANT_PROPERTY_FILES}
	 */
	public static final String ATTR_ANT_PROPERTY_FILES = IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES;
	
	/**
	 * Boolean attribute indicating whether or not internal targets (targets with no
	 * description) should be hidden from the user in the launch configuration dialog.
	 * Default value is <code>false</code> which indicates that all targets will be
	 * displayed.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_HIDE_INTERNAL_TARGETS}
	 */
	public static final String ATTR_HIDE_INTERNAL_TARGETS = IAntLaunchConstants.ATTR_HIDE_INTERNAL_TARGETS;
	
	/**
	 * Integer attribute indicating which column targets should be sorted on. A
	 * value of 0 indicates target name, 1 indicates target description, and -1
	 * indicates no sort. Default value is -1.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_SORT_TARGETS}
	 */
	public static final String ATTR_SORT_TARGETS = IAntLaunchConstants.ATTR_SORT_TARGETS;

	/**
	 * Boolean attribute indicating if the default VM install should be used for the separate JRE build
	 * Default value is <code>false</code> for backwards compatibility
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ATTR_DEFAULT_VM_INSTALL}
	 */
	public static final String ATTR_DEFAULT_VM_INSTALL = IAntLaunchConstants.ATTR_DEFAULT_VM_INSTALL;

	/**
	 * Identifier for Ant processes (value <code>org.eclipse.ant.ui.antProcess</code>). This identifier is
	 * set as the value for the <code>IProcess.ATTR_PROCESS_TYPE</code>
	 * attribute in processes created by the Ant launch delegate.
	 * 
	 * @deprecated use {@link IAntLaunchConstants#ID_ANT_PROCESS_TYPE}
	 */
	public static final String ID_ANT_PROCESS_TYPE = IAntLaunchConstants.ID_ANT_PROCESS_TYPE;
}
