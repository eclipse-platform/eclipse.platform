/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.core;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A proxy to an {@link ILaunchConfigurationDelegate}. Represents a
 * launch delegate contributed to the <code>org.eclipse.debug.core.launchDelegates</code>
 * extension point.
 * @since 3.3
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILaunchDelegate {

	/**
	 * Returns this delegate's unique identifier.
	 *
	 * @return launch delegate identifier
	 */
	String getId();

	/**
	 * Returns a human readable name for this launch delegate
	 * or <code>null</code> if none.
	 *
	 * @return name or <code>null</code>
	 */
	String getName();

	/**
	 * Returns a description of this launch delegate, or
	 * <code>null</code> if none.
	 *
	 * @return description or <code>null</code>
	 */
	String getDescription();

	/**
	 * Returns the name of the plug-in that contributed this delegate.
	 *
	 * @return contributor name
	 */
	String getContributorName();

	/**
	 * Returns the underlying launch configuration.
	 * Causes the delegate to be instantiated.
	 *
	 * @return launch configuration delegate
	 * @exception CoreException if unable to instantiate the delegate
	 */
	ILaunchConfigurationDelegate getDelegate() throws CoreException;

	/**
	 * Returns the complete set of launch modes supported by this delegate as a list of sets.
	 * Each set contains one of more launch mode identifiers. When a set contains more than
	 * one launch mode, it indicates that a mixed launch mode is supported.
	 * If no modes are available an empty list is returned.
	 *
	 * @return the complete set of launch modes this delegate supports
	 */
	List<Set<String>> getModes();

	/**
	 * Returns the id of the plug-in that contributed this launch delegate.
	 *
	 * @return the id of the plug-in that contributed this launch delegate
	 */
	String getPluginIdentifier();

	/**
	 * Returns the specified perspective id for the given mode set, or null if one is not provided
	 * @param modes the set of modes to get the perspective id
	 * @return the perspective id associated with the given mode set, or <code>null</code> if none provided
	 */
	String getPerspectiveId(Set<String> modes);

}
