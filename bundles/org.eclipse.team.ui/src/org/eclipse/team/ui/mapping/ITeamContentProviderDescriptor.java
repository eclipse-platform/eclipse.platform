/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A description of a single extension registered with the
 * <code>org.eclipse.team.ui.teamContentProviders</code>
 * extension point.
 *
 *  * <p>
 * This interface is not intended to be implemented by clients.
 * @since 3.2
 */
public interface ITeamContentProviderDescriptor {

	/**
	 * Return the id of the content extension registered with
	 * the <code>org.eclipse.ui.navigator.navigatorContent</code> extension point
	 * that applies to the descriptors model provider.
	 * @return id of the content extension registered with
	 * the <code>org.eclipse.ui.navigator.navigatorContent</code> extension point
	 */
	public String getContentExtensionId();

	/**
	 * Return the id of the model provider to which this content provider applies.
	 * @return the id of the model provider to which this content provider applies
	 */
	public String getModelProviderId();

	/**
	 * Return an image descriptor that can be displayed with this content
	 * extension.
	 * @return an image descriptor that can be displayed with this content
	 * extension
	 */
	public ImageDescriptor getImageDescriptor();

	/**
	 * Return a preference page that can be displayed to configure
	 * the content provider of this extension.
	 * @return a preference page that can be displayed to configure
	 * the content provider of this extension
	 * @throws CoreException if an error occurs
	 */
	public IPreferencePage createPreferencePage() throws CoreException;

	/**
	 * Return whether this content provider is enabled. If it is not
	 * enabled, it should not be included in any viewers.
	 * @return whether this content provider is enabled
	 */
	public boolean isEnabled();

	/**
	 * Returns the name specified for this content provider as specified in the extension point.
	 * @return a string containing the name or null if no name has been specified
	 */
	public String getName();

	/**
	 * Return whether the FLAT layout is supported by this content provider.
	 * @return whether the FLAT layout is supported by this content provider
	 * @see ITeamContentProviderManager#PROP_PAGE_LAYOUT
	 * @since 3.3
	 */
	public boolean isFlatLayoutSupported();

}
