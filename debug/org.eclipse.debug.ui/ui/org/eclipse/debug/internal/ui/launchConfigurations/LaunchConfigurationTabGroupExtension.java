/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;


/**
 * Proxy to a launch configuration tab group element
 */
public class LaunchConfigurationTabGroupExtension {

	/**
	 * The configuration element defining this tab group.
	 */
	private IConfigurationElement fConfig = null;

	/**
	 * A list of sets of modes that this tab group supports
	 * @since 3.3
	 */
	private List<Set<String>> fModes = null;

	/**
	 * A map of mode sets to descriptions
	 * @since 3.3
	 */
	private Map<Set<String>, String> fDescriptions = null;

	/**
	 * Perspectives for each mode
	 */
	private Map<Set<String>, String> fPerspectives = null;

	/**
	 * Constructs a launch configuration tab extension based
	 * on the given configuration element
	 *
	 * @param element the configuration element defining the
	 *  attributes of this launch configuration tab extension
	 * @return a new launch configuration tab extension
	 */
	public LaunchConfigurationTabGroupExtension(IConfigurationElement element) {
		setConfigurationElement(element);
	}

	/**
	 * Sets the configuration element that defines the attributes
	 * for this launch configuration tab group extension.
	 *
	 * @param element configuration element
	 */
	private void setConfigurationElement(IConfigurationElement element) {
		fConfig = element;
	}

	/**
	 * Returns the configuration element that defines the attributes
	 * for this launch configuration tab group extension.
	 *
	 * @param configuration element that defines the attributes
	 *  for this launch configuration tab extension
	 */
	protected IConfigurationElement getConfigurationElement() {
		return fConfig;
	}

	/**
	 * Returns the set of modes specified in the configuration data, or <code>null</code>
	 * if none (i.e. default tab group)
	 *
	 * @return the set of modes specified in the configuration data, or
	 *  <code>null</code>
	 */
	protected List<Set<String>> getModes() {
		if (fModes == null) {
			fModes = new ArrayList<>();
			fPerspectives = new Hashtable<>();
			IConfigurationElement[] modes = fConfig.getChildren(IConfigurationElementConstants.LAUNCH_MODE);
			if (modes.length > 0) {
				IConfigurationElement element = null;
				String perspective = null, mode = null;
				Set<String> mset = null;
				for (IConfigurationElement m : modes) {
					element = m;
					mode = element.getAttribute(IConfigurationElementConstants.MODE);
					mset = new HashSet<>();
					mset.add(mode);
					fModes.add(mset);
					perspective = element.getAttribute(IConfigurationElementConstants.PERSPECTIVE);
					if (perspective != null) {
						fPerspectives.put(mset, perspective);
					}
				}
			}
		}
		return fModes;
	}

	/**
	 * Returns the perspective associated with the given launch
	 * mode, as specified in plug-in XML, or <code>null</code> if none.
	 *
	 * @param modes the set of launch modes
	 * @return perspective identifier, or <code>null</code>
	 */
	protected String getPerspective(Set<String> modes) {
		getModes();
		return fPerspectives.get(modes);
	}

	/**
	 * Returns the identifier of the type of launch configuration this
	 * tab group is associated with
	 *
	 * @return the identifier of the type of launch configuration this
	 *  tab group is associated with
	 */
	protected String getTypeIdentifier() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.TYPE);
	}

	/**
	 * Returns the identifier of the help context associated with this tab
	 * group, or <code>null</code> if one was not specified.
	 *
	 * @return the identifier of this tab group's help context or
	 * <code>null</code>
	 * @since 2.1
	 */
	protected String getHelpContextId() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.HELP_CONTEXT_ID);
	}

	/**
	 * Returns the identifier of the tab group
	 * @return the id of the tab group
	 *
	 * @since 3.3
	 */
	protected String getIdentifier() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.ID);
	}

	/**
	 * Returns a new tab group defined by this extension
	 *
	 * @return a new tab group defined by this extension
	 * @exception CoreException if an exception occurs instantiating
	 * 	the tab group
	 */
	public ILaunchConfigurationTabGroup newTabGroup() throws CoreException {
		return (ILaunchConfigurationTabGroup)getConfigurationElement().createExecutableExtension(IConfigurationElementConstants.CLASS);
	}

	/**
	 * Returns this tab group's description in the given mode set.
	 *
	 * @param modes the set of modes
	 * @return a description of the Launch Mode if available. If not available, attempts to return
	 * a description of the Launch Configuration. If no appropriate description is found an empty string is returned.
	 */
	public String getDescription(Set<String> modes) {
		String description = null;
		if(fDescriptions == null) {
			fDescriptions = new HashMap<>();
			IConfigurationElement[] children = fConfig.getChildren(IConfigurationElementConstants.LAUNCH_MODE);
			IConfigurationElement child = null;
			String mode = null;
			HashSet<String> set = null;
			for (IConfigurationElement c : children) {
				child = c;
				mode = child.getAttribute(IConfigurationElementConstants.MODE);
				if(mode != null) {
					set = new HashSet<>();
					set.add(mode);
				}
				description = child.getAttribute(IConfigurationElementConstants.DESCRIPTION);
				if(description != null) {
					fDescriptions.put(set, description);
				}
			}

		}
		description = fDescriptions.get(modes);
		if(description == null) {
			description = fConfig.getAttribute(IConfigurationElementConstants.DESCRIPTION);

		}
		return (description == null ? IInternalDebugCoreConstants.EMPTY_STRING : description);
	}

}

