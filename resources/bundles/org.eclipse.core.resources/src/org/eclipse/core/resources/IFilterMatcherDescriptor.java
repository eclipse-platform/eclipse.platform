/*******************************************************************************
 * Copyright (c) 2009 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp(Freescale Semiconductor) - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.resources;

import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;

/**
 * A filter descriptor contains information about a filter type
 * obtained from the plug-in manifest (<code>plugin.xml</code>) files.
 * <p>
 * Filter descriptors are platform-defined objects that exist
 * independent of whether that filter's bundle has been started.
 * </p>
 *
 * @see AbstractFileInfoMatcher
 * @see IWorkspace#getFilterMatcherDescriptor(String)
 * @see IWorkspace#getFilterMatcherDescriptors()
 * @since 3.6
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IFilterMatcherDescriptor {

	/**
	 * An argument filter type constant (value "filter"), denoting that this
	 * filter takes another filter as argument.
	 */
	String ARGUMENT_TYPE_FILTER_MATCHER = "filterMatcher"; //$NON-NLS-1$
	/**
	 * An argument filter type constant (value "filters"), denoting that this
	 * filter takes an array of other filters as argument.
	 */
	String ARGUMENT_TYPE_FILTER_MATCHERS = "filterMatchers"; //$NON-NLS-1$
	/**
	 * An argument filter type constant (value "none"), denoting that this
	 * filter does not take any arguments.
	 */
	String ARGUMENT_TYPE_NONE = "none"; //$NON-NLS-1$
	/**
	 * An argument filter type constant (value "string"), denoting that this
	 * filter takes a string argument
	 */
	String ARGUMENT_TYPE_STRING = "string"; //$NON-NLS-1$

	/**
	 * Returns the argument type expected by this filter. The result will be one of the
	 * <code>ARGUMENT_TYPE_*</code> constants declared on this class.
	 * @return The argument type of this filter extension
	 */
	String getArgumentType();

	/**
	 * Returns a translated, human-readable description for this filter extension.
	 * @return The human-readable filter description
	 */
	String getDescription();

	/**
	 * Returns the fully qualified id of the filter extension.
	 * @return The fully qualified id of the filter extension.
	 */
	String getId();

	/**
	 * Returns a translated, human-readable name for this filter extension.
	 * @return The human-readable filter name
	 */
	String getName();

	/**
	 * TODO What is this?
	 */
	boolean isFirstOrdering();

}