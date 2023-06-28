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
package org.eclipse.debug.internal.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * Provides a proxy to a launchConfigurationTabs extension point
 *
 * @since 3.3
 */
public final class LaunchConfigurationTabExtension {

	/**
	 * The configuration element backing this proxy
	 */
	IConfigurationElement fElement = null;
	private Set<String> fDelegates = null;

	/**
	 * Constructor
	 * @param element the <code>IConfigurationElement</code> for this proxy
	 */
	public LaunchConfigurationTabExtension(IConfigurationElement element) {
		fElement = element;
	}

	/**
	 * Returns the unique id of the tab
	 * @return the unique id of the tab
	 */
	public String getIdentifier() {
		return fElement.getAttribute(IConfigurationElementConstants.ID);
	}

	/**
	 * Returns the human readable name for the tab, not to be confused with the name that appears on the tab itself
	 * @return the name of the tab
	 */
	public String getName() {
		return fElement.getAttribute(IConfigurationElementConstants.NAME);
	}

	/**
	 * Returns the instantiated class of this tab
	 * @return the instantiated class of this tab
	 */
	public ILaunchConfigurationTab getTab() {
		try {
			Object object = fElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
			if(object instanceof ILaunchConfigurationTab) {
				return (ILaunchConfigurationTab) object;
			}
		} catch (CoreException e) {DebugUIPlugin.log(e);}
		return null;
	}

	/**
	 * Returns the unique id of the <code>ILaunchConfigurationTabGroup</code> that this tab contributes to
	 * @return the id of the <code>ILaunchConfigurationTabGroup</code> this tab contributes to
	 */
	public String getTabGroupId() {
		return fElement.getAttribute(IConfigurationElementConstants.GROUP);
	}

	/**
	 * This method returns the id of the tab that this tab should be placed immediately after.
	 * @return the id of the relative tab or <code>null</code> if one has not been specified
	 *
	 */
	public String getRelativeTabId() {
		IConfigurationElement[] elems = fElement.getChildren(IConfigurationElementConstants.PLACEMENT);
		if(elems.length == 1) {
			return elems[0].getAttribute(IConfigurationElementConstants.AFTER);
		}
		return null;
	}

	/**
	 * Returns the id of the plugin that contributed this tab extension
	 * @return the id of the plugin tat contributed this tab
	 */
	public String getPluginIdentifier() {
		return fElement.getContributor().getName();
	}

	/**
	 * Returns a set of strings of the launch delegates that this tab contribution is associated with
	 * @return the set of strings of the associated launch delegates, which can be an empty collection, never <code>null</code>.
	 */
	public Set<String> getDelegateSet() {
		if(fDelegates == null) {
			fDelegates = new HashSet<>();
			IConfigurationElement[] children = fElement.getChildren(IConfigurationElementConstants.ASSOCIATED_DELEGATE);
			String id = null;
			for (IConfigurationElement child : children) {
				id = child.getAttribute(IConfigurationElementConstants.DELEGATE);
				if(id != null) {
					fDelegates.add(id);
				}
			}
		}
		return fDelegates;
	}
}
