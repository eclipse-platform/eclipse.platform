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
package org.eclipse.ant.internal.core;

import org.eclipse.ant.core.AntCorePlugin;


/**
 * Common constants.
 */
public interface IAntCoreConstants {

	// default values
	public static final String DEFAULT_BUILD_FILENAME = "build.xml"; //$NON-NLS-1$

	// preferences
	public static final String PREFERENCE_TASKS = "tasks"; //$NON-NLS-1$
	public static final String PREFERENCE_TYPES = "types"; //$NON-NLS-1$
	
	public static final String PREFIX_TASK = "task."; //$NON-NLS-1$
	public static final String PREFIX_TYPE = "type."; //$NON-NLS-1$
	
	/**
	 * Preferences
	 * @since 3.0
	 */
	public static final String PREFERENCE_ANT_HOME_ENTRIES = "ant_home_entries"; //$NON-NLS-1$
	public static final String PREFERENCE_ADDITIONAL_ENTRIES = "additional_entries"; //$NON-NLS-1$
	
	public static final String PREFERENCE_CLASSPATH_CHANGED = "classpath_changed"; //$NON-NLS-1$
	
	/**
	 * Preferences
	 * @since 2.1
	 */
	public static final String PREFERENCE_ANT_HOME = "ant_home"; //$NON-NLS-1$
	public static final String PREFERENCE_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String PREFERENCE_PROPERTY_FILES = "propertyfiles"; //$NON-NLS-1$
	public static final String PREFIX_PROPERTY = "property."; //$NON-NLS-1$

	/**
	 * Constant for the word 'default'
	 * <br><br>
	 * Value is: <code>default</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String DEFAULT = "default"; //$NON-NLS-1$

	/**
	 * Constant for the word 'dir'
	 * <br><br>
	 * Value is: <code>dir</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String DIR = "dir"; //$NON-NLS-1$
	
	/**
	 * Constant for the empty {@link String}
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	
	/**
	 * Constant for the word 'file'
	 * <br><br>
	 * Value is: <code>file</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String FILE = "file"; //$NON-NLS-1$

	/**
	 * Constant representing a file URL protocol
	 * <br><br>
	 * Value is: <code>file:</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$
	
	/**
	 * Constant for the word 'name'
	 * <br><br>
	 * Value is: <code>name</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String NAME = AntCorePlugin.NAME;

	/**
	 * Constant for the word 'value'
	 * <br><br>
	 * Value is: <code>value</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String VALUE = AntCorePlugin.VALUE;

	/**
	 * Constant for the word 'description'
	 * <br><br>
	 * Value is: <code>description</code>
	 * 
	 * @since org.eclipse.ant.core 3.2.200
	 */
	public static final String DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * Constant for the encoding <code>UTF-8</code>
	 * <br><br>
	 * Value is: <code>UTF-8</code>
	 * 
	 * @since org.eclipse.ant.core 3.3.0
	 */
	public static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

	/**
	 * The name of the XML build file extension.
	 * <br><br>
	 * Value is: <code>xml</code>
	 * @since 3.8
	 */
	public static final String XML_EXTENSION = "xml"; //$NON-NLS-1$
}
