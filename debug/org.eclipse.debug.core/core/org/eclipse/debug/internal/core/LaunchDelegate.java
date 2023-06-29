/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * Proxy to a launch delegate extension.
 * <p>
 * Clients can contribute launch delegates through the
 * <code>launchDelegates</code> extension point
 * </p>
 * <p>
 * Example contribution of the local java launch delegate
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.launchDelegates"&gt;
 *    &lt;launchDelegate
 *          delegate="org.eclipse.jdt.launching.JavaLaunchDelegate"
 *          id="org.eclipse.jdt.launching.localJavaApplicationDelegate"
 *          modes="run, debug"
 *          name="%localJavaApplication"
 *          type="org.eclipse.jdt.launching.localJavaApplication"&gt;
 *        &lt;modeCombination
 *          modes="run, profile"&gt;
 *          perspective="com.example.Perspective"&gt;
 *        &lt;/modeCombination&gt;
 *    &lt;/launchDelegate&gt;
 * </pre>
 *
 * Clients are NOT intended to subclass this class
 *
 * @see IConfigurationElementConstants
 *
 * @since 3.3
 */
public final class LaunchDelegate implements ILaunchDelegate {

	/**
	 * The configuration element for this delegate
	 */
	private IConfigurationElement fElement = null;

	/**
	 * The cached delegate. Remains null until asked for, then persisted
	 */
	private ILaunchConfigurationDelegate fDelegate = null;

	//a listing of sets of
	private List<Set<String>> fLaunchModes = null;
	private String fType = null;
	private HashMap<Set<String>, String> fPerspectiveIds = null;

	/**
	 * Constructor
	 * @param element the configuration element to associate with this launch delegate
	 */
	public LaunchDelegate(IConfigurationElement element) {
		fElement = element;
	}

	@Override
	public ILaunchConfigurationDelegate getDelegate() throws CoreException {
		if(fDelegate == null) {
			Object obj = fElement.createExecutableExtension(IConfigurationElementConstants.DELEGATE);
			if(obj instanceof ILaunchConfigurationDelegate) {
				fDelegate = (ILaunchConfigurationDelegate)obj;
			} else {
				throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, MessageFormat.format(DebugCoreMessages.LaunchDelegate_3, new Object[] { getId() }), null));
			}
		}
		return fDelegate;
	}

	@Override
	public String getId() {
		return fElement.getAttribute(IConfigurationElementConstants.ID);
	}

	/**
	 * Returns the id of the associated <code>ILaunchConfigurationType</code> or <code>null</code> if none provided
	 * @return the id of the <code>ILaunchConfigurationType</code> associated with this delegate
	 */
	public String getLaunchConfigurationTypeId() {
		if(fType == null) {
			//fall back to single association if no appliesTo
			fType = fElement.getAttribute(IConfigurationElementConstants.TYPE);
			if(fType == null) {
				//the case when we have passed a launch configuration type to the launch delegate
				fType = fElement.getAttribute(IConfigurationElementConstants.ID);
			}
		}
		return fType;
	}

	/**
	 * Simple method to parse mode strings (separated by commas)
	 * @param element the config element to read the mode string from
	 * @return a set of the parsed strings or an empty collection
	 * @since 3.3
	 */
	private Set<String> parseModes(IConfigurationElement element) {
		HashSet<String> set = new HashSet<>();
		String modes = element.getAttribute(IConfigurationElementConstants.MODES);
		if (modes != null) {
			String[] strings = modes.split(","); //$NON-NLS-1$
			for (String string : strings) {
				set.add(string.trim());
			}
		}
		return set;
	}

	@Override
	public List<Set<String>> getModes() {
		if(fLaunchModes == null) {
			fLaunchModes = new ArrayList<>();
			fPerspectiveIds = new HashMap<>();
			IConfigurationElement[] children = fElement.getChildren(IConfigurationElementConstants.MODE_COMBINATION);
			Set<String> modeset = null;
			for (IConfigurationElement child : children) {
				modeset = parseModes(child);
				fLaunchModes.add(modeset);
				fPerspectiveIds.put(modeset, child.getAttribute(IConfigurationElementConstants.PERSPECTIVE));
			}
			//try to get the modes from the old definition and make each one
			//a separate set of one element
			modeset = null;
			String modes = fElement.getAttribute(IConfigurationElementConstants.MODES);
			if (modes != null) {
				String[] strings = modes.split(","); //$NON-NLS-1$
				for (String string : strings) {
					modeset = new HashSet<>();
					modeset.add(string.trim());
					fLaunchModes.add(modeset);
				}
			}
		}
		return fLaunchModes;
	}

	/**
	 * Returns the human readable name for this launch delegate
	 * @return the human readable name for this launch delegate, or <code>null</code> if none
	 */
	@Override
	public String getName() {
		//try a delegateName attribute first, in the event this delegate was made from an ILaunchConfigurationType
		String name = fElement.getAttribute(IConfigurationElementConstants.DELEGATE_NAME);
		if(name == null) {
			name = fElement.getAttribute(IConfigurationElementConstants.NAME);
			if (name == null) {
				name = getContributorName();
			}
			name = name.trim();
			if (Character.isUpperCase(name.charAt(0))) {
				name = MessageFormat.format(DebugCoreMessages.LaunchDelegate_1, new Object[] { name });
			} else {
				name = MessageFormat.format(DebugCoreMessages.LaunchDelegate_2, new Object[] { name });
			}
		}
		return name;
	}

	/**
	 * Returns the contributor name of this delegate (plug-in name).
	 *
	 * @return contributor name
	 */
	@Override
	public String getContributorName() {
		return fElement.getContributor().getName();
	}

	/**
	 * Returns the associated source locator id or <code>null</code>
	 * @return the associated source locator id or <code>null</code> if not provided
	 */
	public String getSourceLocatorId() {
		return fElement.getAttribute(IConfigurationElementConstants.SOURCE_LOCATOR);
	}

	/**
	 * Returns the associated source path computer id or <code>null</code>
	 * @return the associated source path computer id or <code>null</code> if not provided
	 */
	public String getSourcePathComputerId() {
		return fElement.getAttribute(IConfigurationElementConstants.SOURCE_PATH_COMPUTER);
	}

	@Override
	public String getDescription() {
		String desc = fElement.getAttribute(IConfigurationElementConstants.DELEGATE_DESCRIPTION);
		if(desc == null) {
			return DebugCoreMessages.LaunchDelegate_0;
		}
		return desc;
	}

	@Override
	public String getPluginIdentifier() {
		return fElement.getContributor().getName();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		return obj instanceof ILaunchDelegate && getId() != null && getId().equals(((ILaunchDelegate)obj).getId());
	}

	@Override
	public int hashCode() {
		String id = getId();
		return id == null ? 0 : id.hashCode();
	}

	@Override
	public String getPerspectiveId(Set<String> modes) {
		if(fPerspectiveIds == null) {
			getModes();
		}
		return fPerspectiveIds.get(modes);
	}
}
