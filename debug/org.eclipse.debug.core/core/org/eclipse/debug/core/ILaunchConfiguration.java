/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.core;


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A launch configuration describes how to launch an application. Each launch
 * configuration is an instance of a type of launch configuration as described
 * by a launch configuration type extension. Each launch configuration has a
 * launch configuration delegate which performs the actual launching of a
 * configuration.
 * <p>
 * A launch configuration may be shared in a repository via standard VCM
 * mechanisms, or may be stored locally, essentially making the launch
 * configuration private for a single user. Thus, a launch configuration may
 * stored as a file in the workspace (shared), or as a file in the debug
 * plug-in's state location.
 * </p>
 * <p>
 * A launch configuration is a handle to its underlying storage. Methods
 * annotated as "handle-only" do not require a configuration to exist. Methods
 * that require an underlying configuration to exist throw a
 * <code>CoreException</code> when an underlying configuration is missing.
 * </p>
 * <p>
 * A launch configuration is modified by obtaining a working copy of a launch
 * configuration, modifying the working copy, and then saving the working copy.
 * </p>
 * <p>
 * Clients that define a launch configuration delegate extension implement the
 * <code>ILaunchConfigurationDelegate</code> interface.
 * </p>
 *
 * @see ILaunchConfigurationType
 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate
 * @see ILaunchConfigurationWorkingCopy
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILaunchConfiguration extends IAdaptable {

	/**
	 * The file extension for launch configuration files
	 * (value <code>"launch"</code>).
	 */
	String LAUNCH_CONFIGURATION_FILE_EXTENSION = "launch"; //$NON-NLS-1$

	/**
	 * The file extension for launch configuration prototype files (value
	 * <code>"prototype"</code>).
	 *
	 * @since 3.12
	 */
	String LAUNCH_CONFIGURATION_PROTOTYPE_FILE_EXTENSION = "prototype"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute storing an identifier of
	 * a persistable source locator extension. When this attribute is
	 * specified, a new source locator will be created automatically and
	 * associated with the launch for this configuration.
	 *
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator
	 */
	String ATTR_SOURCE_LOCATOR_ID = DebugPlugin.getUniqueIdentifier() + ".source_locator_id"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute storing a memento of a
	 * source locator. When this attribute is specified in
	 * conjunction with a source locator id, the source locator
	 * created for a launch will be initialized with this memento.
	 * When not specified, but a source locator id is specified,
	 * the source locator will be initialized to default values.
	 *
	 * @see org.eclipse.debug.core.model.IPersistableSourceLocator
	 */
	String ATTR_SOURCE_LOCATOR_MEMENTO = DebugPlugin.getUniqueIdentifier() + ".source_locator_memento"; //$NON-NLS-1$

	/**
	 * Flag indicating that only this configuration is to be modified by an operation.
	 * Any prototype children referring to this configuration will not be modified.
	 *
	 * @since 3.12
	 */
	int UPDATE_NONE = 0;

	/**
	 * Flag indicating that this prototype and any effected prototype children of this prototype
	 * should be updated when this prototype is modified.
	 *
	 * @since 3.12
	 */
	int UPDATE_PROTOTYPE_CHILDREN = 1;

	/**
	 * Type constant (bit mask value 1) which identifies a launch configuration.
	 *
	 * @see #getKind()
	 * @since 3.12
	 */
	int CONFIGURATION = 0x1;

	/**
	 * Type constant (bit mask value 2) which identifies a launch configuration prototype.
	 *
	 * @see #getKind()
	 * @since 3.12
	 */
	int PROTOTYPE = 0x2;

	/**
	 * Returns whether the contents of this launch configuration are
	 * equal to the contents of the given launch configuration.
	 *
	 * @param configuration launch configuration
	 * @return whether the contents of this launch configuration are equal to the contents
	 * of the specified launch configuration.
	 */
	boolean contentsEqual(ILaunchConfiguration configuration);

	/**
	 * Returns a copy of this launch configuration, as a
	 * working copy, with the specified name. The new
	 * working copy does not refer back to this configuration
	 * as its original launch configuration (the working copy
	 * will return <code>null</code> for <code>getOriginal()</code>).
	 * When the working copy is saved it will not affect this
	 * launch configuration.
	 *
	 * @param name the name of the copy
	 * @return a copy of this launch configuration
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while initializing the contents of the
	 * working copy from this configuration's underlying storage.</li>
	 * </ul>
	 * @see ILaunchConfigurationWorkingCopy#getOriginal()
	 */
	ILaunchConfigurationWorkingCopy copy(String name) throws CoreException;

	/**
	 * Deletes this launch configuration. This configuration's underlying
	 * storage is deleted. Has no effect if this configuration does not exist.
	 * <p>
	 * Equivalent to #delete(UPDATE_NONE)
	 * </p>
	 *
	 * @exception CoreException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>An exception occurs while deleting this
	 *                configuration's underlying storage.</li>
	 *                </ul>
	 */
	void delete() throws CoreException;

	/**
	 * Deletes this launch configuration. This configuration's underlying
	 * storage is deleted. Has no effect if this configuration does not exist.
	 * <p>
	 * When UPDATE_PROTOTYPE_CHILDREN is specified, back pointers to this
	 * prototype are cleared in any prototype children.
	 * </p>
	 *
	 * @param flag one of UPDATE_NONE or UPDATE_PROTOTYPE_CHILDREN
	 * @exception CoreException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>An exception occurs while deleting this
	 *                configuration's underlying storage or updating any
	 *                prototype children.</li>
	 *                </ul>
	 * @since 3.12
	 */
	void delete(int flag) throws CoreException;

	/**
	 * Returns whether this launch configuration's underlying
	 * storage exists. This is a handle-only method.
	 *
	 * @return whether this launch configuration's underlying
	 *  storage exists
	 */
	boolean exists();

	/**
	 * Returns the boolean-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have a boolean value</li>
	 * </ul>
	 */
	boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException;

	/**
	 * Returns the integer-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have an integer value</li>
	 * </ul>
	 */
	int getAttribute(String attributeName, int defaultValue) throws CoreException;

	/**
	 * Returns the <code>java.util.List</code>-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have a List value</li>
	 * </ul>
	 */
	List<String> getAttribute(String attributeName, List<String> defaultValue) throws CoreException;

	/**
	 * Returns the <code>java.util.Set</code>-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have a List value</li>
	 * </ul>
	 *
	 * @since 3.3
	 */
	Set<String> getAttribute(String attributeName, Set<String> defaultValue) throws CoreException;

	/**
	 * Returns the <code>java.util.Map</code>-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have a Map value</li>
	 * </ul>
	 */
	Map<String, String> getAttribute(String attributeName, Map<String, String> defaultValue) throws CoreException;

	/**
	 * Returns the string-valued attribute with the given name.
	 * Returns the given default value if the attribute is undefined.
	 *
	 * @param attributeName the name of the attribute
	 * @param defaultValue the value to use if no value is found
	 * @return the value or the default value if no value was found.
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while retrieving the attribute from
	 *  underlying storage.</li>
	 * <li>An attribute with the given name exists, but does not
	 *  have a String value</li>
	 * </ul>
	 */
	String getAttribute(String attributeName, String defaultValue) throws CoreException;

	/**
	 * Returns a map containing the attributes in this launch configuration.
	 * Returns an empty map if this configuration has no attributes.
	 * <p>
	 * Modifying the map does not affect this launch configuration's attributes.
	 * A launch configuration is modified by obtaining a working copy of that
	 * launch configuration, modifying the working copy, and then saving the working
	 * copy.
	 * </p>
	 * @return a map of attribute keys and values
	 * @exception CoreException unable to generate/retrieve an attribute map
	 * @since 2.1
	 */
	Map<String, Object> getAttributes() throws CoreException;

	/**
	 * Returns this launch configuration's type's category, or <code>null</code>
	 * if unspecified. This is a handle-only method.
	 *
	 * @return this launch configuration's type's category, or <code>null</code>
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>Unable to retrieve or instantiate this launch configuration's type.</li>
	 * </ul>
	 * @since 2.1
	 */
	String getCategory() throws CoreException;

	/**
	 * Returns the file this launch configuration is stored
	 * in, or <code>null</code> if this configuration is stored
	 * locally with the workspace. This is a handle-only method.
	 *
	 * @return the file this launch configuration is stored
	 *  in, or <code>null</code> if this configuration is stored
	 *  locally with the workspace
	 */
	IFile getFile();

	/**
	 * Returns the location of this launch configuration as a
	 * path in the local file system or <code>null</code> if it cannot
	 * be mapped to a location in the local file system. This is a handle-only method.
	 * <p>
	 * Since 3.5, this method can return <code>null</code>. For example, when a
	 * launch configuration is stored in the workspace as an {@link IFile} in
	 * an external file system ({@link EFS}).
	 * </p>
	 *
	 * @return the location of this launch configuration as a
	 *  path file system or <code>null</code> if it cannot be mapped
	 *  to a location in the local file system. Since 3.5, this method
	 *  can return <code>null</code>.
	 * @deprecated Since a launch configuration does not need to be stored in the local
	 *  file system, this attribute should no longer be used to identify a launch configuration.
	 */
	@Deprecated IPath getLocation();

	/**
	 * Returns the resources this launch configuration is associated with or <code>null</code>
	 * if none. Clients contributing launch configuration types are responsible for maintaining
	 * resource mappings as appropriate.
	 *
	 * @return the resources this launch configuration is associated with or <code>null</code>
	 * @throws CoreException unable to get the mapped resource
	 * @since 3.2
	 */
	IResource[] getMappedResources() throws CoreException;

	/**
	 * Returns a memento for this launch configuration, or <code>null</code>
	 * if unable to generate a memento for this configuration. A memento
	 * can be used to re-create a launch configuration, via the
	 * launch manager.
	 *
	 * @return a memento for this configuration
	 * @see ILaunchManager#getLaunchConfiguration(String)
	 * @exception CoreException if an exception occurs generating this
	 *  launch configuration's memento
	 */
	String getMemento() throws CoreException;

	/**
	 * Returns the name of this launch configuration. This is
	 * a handle-only method.
	 *
	 * @return the name of this launch configuration
	 */
	String getName();

	/**
	 * Returns the launch modes that have been set on this configuration.
	 * An empty set is returned if no specific launch modes have been set
	 * on a launch configuration.
	 * <p>
	 * Setting launch modes on a configuration allows a launch to be
	 * performed in mixed mode - for example, debug and profile at the
	 * same time.
	 * </p>
	 * @return this configuration's launch modes, possibly an empty set
	 * @exception CoreException if an exception occurs retrieving modes
	 * @since 3.3
	 */
	Set<String> getModes() throws CoreException;

	/**
	 * Returns the preferred launch delegate that has been set on this
	 * configuration or <code>null</code> if one is not specified.
	 *
	 * @param modes mode set for which a preferred delegate has been requested
	 * @return this configuration's preferred launch delegate for the specified mode set, or
	 * 	<code>null</code> if one is not specified
	 * @exception CoreException if an exception occurs retrieving preferred delegate
	 * @since 3.3
	 */
	ILaunchDelegate getPreferredDelegate(Set<String> modes) throws CoreException;

	/**
	 * Returns the type of this launch configuration. This is a
	 * handle-only method.
	 *
	 * @return the type of this launch configuration
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>Unable to retrieve or instantiate this launch configuration's type.</li>
	 * </ul>
	 * @see ILaunchConfigurationType
	 */
	ILaunchConfigurationType getType() throws CoreException;

	/**
	 * Returns a working copy of this launch configuration.
	 * Since 3.3, if this method is called on a working copy, a nested working
	 * copy is created (a working copy of a working copy).
	 * Changes to the working copy will be applied to this
	 * launch configuration when saved, or to the parent working copy.
	 * The working copy will refer to this launch configuration as its original
	 * launch configuration, or the working copy it was created from.
	 * <p>
	 * When a working copy (B) is created from a working copy (A), the newly
	 * created working copy (B) is initialized with the attributes from
	 * the first working copy (A). Whenever a working copy is saved, it is written
	 * back to the working copy from which it was created: in this example working
	 * copy B will write back to working copy A, and A will write back to the
	 * original launch configuration.
	 * </p>
	 * @return a working copy of this launch configuration, or a nested working copy if called
	 * on an instance of <code>ILaunchConfigurationWorkingCopy</code>
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>An exception occurs while initializing the contents of the
	 * working copy from this configuration's underlying storage.</li>
	 * </ul>
	 * @see ILaunchConfigurationWorkingCopy#getOriginal()
	 */
	ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException;

	/**
	 * Returns whether this configuration contains an attribute with the given name.
	 *
	 * @param attributeName the name of the attribute
	 * @return true if this configuration has the specified attribute false otherwise
	 * @throws CoreException if unable to retrieve attributes
	 *
	 * @since 3.4
	 */
	boolean hasAttribute(String attributeName) throws CoreException;

	/**
	 * Returns whether this launch configuration is stored
	 * locally within the workspace. This is a handle-only method.
	 *
	 * @return whether this launch configuration is stored
	 *  locally with the workspace
	 */
	boolean isLocal();

	/**
	 * Returns whether this launch configuration is a candidate for migration.
	 *
	 * @return whether this launch configuration is a candidate for migration
	 * @throws CoreException if a problem is encountered
	 * @see ILaunchConfigurationMigrationDelegate
	 * @since 3.2
	 */
	boolean isMigrationCandidate() throws CoreException ;

	/**
	 * Returns whether this launch configuration is a working
	 * copy. Launch configurations which return <code>true</code>
	 * to this method can be safely cast to
	 * <code>org.eclipse.debug.core.ILaunchConfigurationWorkingCopy</code>.
	 * This is a handle-only method.
	 *
	 * @return whether this launch configuration is a working
	 *  copy
	 */
	boolean isWorkingCopy();

	/**
	 * Launches this configuration in the specified mode by delegating to this
	 * configuration's launch configuration delegate, and returns the resulting
	 * launch.
	 * <p>
	 * Equivalent to calling
	 * <code>launch(String, IProgressMontitor, boolean)</code> with a
	 * <code>build</code> flag of <code>false</code>.
	 * </p>
	 *
	 * @param mode the mode in which to launch, one of the mode constants
	 *            defined by <code>ILaunchManager</code> - <code>RUN_MODE</code>
	 *            or <code>DEBUG_MODE</code>.
	 * @param monitor progress monitor, or <code>null</code>. A cancelable
	 *            progress monitor is provided by the Job framework. It should
	 *            be noted that the setCanceled(boolean) method should never be
	 *            called on the provided monitor or the monitor passed to any
	 *            delegates from this method; due to a limitation in the
	 *            progress monitor framework using the setCanceled method can
	 *            cause entire workspace batch jobs to be canceled, as the
	 *            canceled flag is propagated up the top-level parent monitor.
	 *            The provided monitor is not guaranteed to have been started.
	 * @return the resulting launch
	 * @exception CoreException if this method fails. Reasons include:
	 *                <ul>
	 *                <li>unable to instantiate the underlying launch
	 *                configuration delegate</li>
	 *                <li>the launch fails (in the delegate)</li>
	 *                </ul>
	 */
	ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException;

	/**
	 * Launches this configuration in the specified mode by delegating to
	 * this configuration's launch configuration delegate, and returns the
	 * resulting launch.
	 * <p>
	 * If this configuration's launch delegate implements
	 * <code>ILaunchConfigurationDelegate2</code>, the launch delegate will
	 * be consulted to provide a launch object for the launch,
	 * perform pre-launch checks, and build before the launch.
	 * If <code>build</code> is <code>true</code> and the associated launch
	 * delegate does not implement <code>ILaunchConfigurationDelegate2</code>
	 * an incremental workspace build will be performed before the launch
	 * by the debug platform.
	 * </p>
	 * <p>
	 * The resulting launch object is registered with the launch manager
	 * before passing it to this configuration's delegate launch method, for
	 * contributions (debug targets and processes).
	 * </p>
	 * <p>
	 * If the delegate contributes a source locator to the launch, that
	 * source locator is used. Otherwise an appropriate source locator is
	 * contributed to the launch  based on the values of
	 * <code>ATTR_SOURCE_LOCATOR_ID</code> and
	 * <code>ATTR_SOURCE_LOCATOR_MEMENTO</code>. If the launch is canceled (via
	 * the given progress monitor), the launch is removed from the launch
	 * manager. The launch is returned whether canceled or not. Invoking this
	 * method causes the underlying launch configuration delegate to be
	 * instantiated (if not already).
	 * </p>
	 * @param mode the mode in which to launch, one of the mode constants
	 *  defined by <code>ILaunchManager</code> - <code>RUN_MODE</code> or <code>DEBUG_MODE</code>.
	 * @param monitor progress monitor, or <code>null</code>. A cancelable progress monitor is provided by the Job
	 *  framework. It should be noted that the setCanceled(boolean) method should never be called on the provided
	 *  monitor or the monitor passed to any delegates from this method; due to a limitation in the progress monitor
	 *  framework using the setCanceled method can cause entire workspace batch jobs to be canceled, as the canceled flag
	 *  is propagated up the top-level parent monitor. The provided monitor is not guaranteed to have been started.
	 * @param build whether the workspace should be built before the launch
	 * @return resulting launch
	 * @throws CoreException if an exception occurs during the launch sequence
	 * @since 3.0
	 */
	ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException;

	/**
	 * Launches this configuration in the specified mode by delegating to
	 * this configuration's launch configuration delegate, and returns the
	 * resulting launch.
	 * <p>
	 * If this configuration's launch delegate implements
	 * <code>ILaunchConfigurationDelegate2</code>, the launch delegate will
	 * be consulted to provide a launch object for the launch,
	 * perform pre-launch checks, and build before the launch.
	 * If <code>build</code> is <code>true</code> and the associated launch
	 * delegate does not implement <code>ILaunchConfigurationDelegate2</code>
	 * an incremental workspace build will be performed before the launch
	 * by the debug platform.
	 * </p>
	 * <p>
	 * When <code>register</code> is <code>true</code>, the resulting launch object
	 * is registered with the launch manager before passing it to this configuration's delegate
	 * launch method, for contributions (debug targets and processes). When
	 * <code>register</code> is <code>false</code>, the launch is not registered with
	 * the launch manager. Clients that launch configurations without registering
	 * a launch should register appropriate debug event filters to intercept events
	 * from unregistered launches.
	 * </p>
	 * <p>
	 * If the delegate contributes a source locator to the launch, that
	 * source locator is used. Otherwise an appropriate source locator is
	 * contributed to the launch  based on the values of
	 * <code>ATTR_SOURCE_LOCATOR_ID</code> and
	 * <code>ATTR_SOURCE_LOCATOR_MEMENTO</code>. If the launch is canceled (via
	 * the given progress monitor), the launch is removed from the launch
	 * manager. The launch is returned whether canceled or not. Invoking this
	 * method causes the underlying launch configuration delegate to be
	 * instantiated (if not already).
	 * </p>
	 * @param mode the mode in which to launch, one of the mode constants
	 *  defined by <code>ILaunchManager</code> - <code>RUN_MODE</code> or <code>DEBUG_MODE</code>.
	 * @param monitor progress monitor, or <code>null</code>. A cancelable progress monitor is provided by the Job
	 *  framework. It should be noted that the setCanceled(boolean) method should never be called on the provided
	 *  monitor or the monitor passed to any delegates from this method; due to a limitation in the progress monitor
	 *  framework using the setCanceled method can cause entire workspace batch jobs to be canceled, as the canceled flag
	 *  is propagated up the top-level parent monitor. The provided monitor is not guaranteed to have been started.
	 * @param build whether the workspace should be built before the launch
	 * @param register whether to register the resulting launch with the launch manager
	 * @return resulting launch
	 * @throws CoreException if an exception occurs during the launch sequence
	 * @since 3.1
	 */
	ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException;

	/**
	 * Migrates this launch configuration to be compatible with current tooling.
	 * Has no effect if this configuration is not a candidate for migration.
	 * Migration is performed by a launch configuration migration delegate.
	 * @throws CoreException if migration fails
	 * @since 3.2
	 * @see ILaunchConfigurationMigrationDelegate
	 */
	void migrate() throws CoreException;

	/**
	 * Returns whether this launch configuration supports the
	 * specified mode. This is a handle-only method.
	 *
	 * @param mode a mode in which a configuration can be launched, one of
	 *  the mode constants defined by <code>ILaunchManager</code> - <code>RUN_MODE</code> or
	 *  <code>DEBUG_MODE</code>.
	 * @return whether this launch configuration supports the
	 *  specified mode
	 * @exception CoreException if this method fails. Reasons include:
	 * <ul>
	 * <li>Unable to retrieve this launch configuration's type.</li>
	 * </ul>
	 */
	boolean supportsMode(String mode) throws CoreException;

	/**
	 * Returns whether this launch configuration is read-only.
	 * A read-only configuration cannot be modified.
	 *
	 * @return whether this configuration is read-only
	 *
	 * @since 3.3
	 */
	boolean isReadOnly();

	/**
	 * Returns the prototype this launch configuration was created from or
	 * <code>null</code> if none.
	 *
	 * @return the prototype this launch configuration was created from or
	 *         <code>null</code> if none
	 * @throws CoreException if the prototype could not be retrieved or no
	 *             longer exists
	 * @since 3.12
	 */
	ILaunchConfiguration getPrototype() throws CoreException;

	/**
	 * Check if the given attribute has the same value than the one from its prototype.
	 * @param attribute the attribute to check
	 * @return <code>true</code> if the attribute is modified, <code>false</code> otherwise.
	 * @throws CoreException if an exception occurs while checking
	 * @since 3.12
	 */
	boolean isAttributeModified(String attribute) throws CoreException;

	/**
	 * Returns whether this configuration is a prototype.
	 *
	 * @return whether this configuration is a prototype
	 * @since 3.12
	 */
	boolean isPrototype();

	/**
	 * Returns all configurations made from this prototype, possibly an empty
	 * collection.
	 *
	 * @return all configurations made from this prototype
	 * @throws CoreException if unable to retrieve this prototype's children
	 * @since 3.12
	 */
	Collection<ILaunchConfiguration> getPrototypeChildren() throws CoreException;

	/**
	 * Returns this configuration's kind. One of CONFIGURATION or PROTOTYPE.
	 *
	 * @see #CONFIGURATION
	 * @see #PROTOTYPE
	 *
	 * @return this configuration's kind
	 * @throws CoreException if unable to retrieve this configuration's kind
	 * @since 3.12
	 */
	int getKind() throws CoreException;

	/**
	 * Get the visible attributes of this prototype (return <code>null</code> if
	 * the launch configuration is not a prototype).
	 *
	 * @return the visible attributes of this prototype (return
	 *         <code>null</code> if the launch configuration is not a
	 *         prototype).
	 * @throws CoreException if unable to retrieve this prototype's visible
	 *             attribute set.
	 * @since 3.12
	 */
	Set<String> getPrototypeVisibleAttributes() throws CoreException;

	/**
	 * Set the visibility of the given prototype's attribute.
	 *
	 * @param attribute the attribute to set
	 * @param visible the visibility
	 * @throws CoreException if unable to set this prototype's attribute
	 *             visibility
	 * @since 3.12
	 */
	void setPrototypeAttributeVisibility(String attribute, boolean visible) throws CoreException;
}
