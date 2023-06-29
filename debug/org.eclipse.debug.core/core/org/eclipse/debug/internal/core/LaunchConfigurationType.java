/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;


import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;

/**
 * A launch configuration type wrappers a configuration
 * element for a <code>launchConfigurationType</code>
 * extension.
 */
public class LaunchConfigurationType extends PlatformObject implements ILaunchConfigurationType {

	/**
	 * The configuration element of the extension.
	 */
	private IConfigurationElement fElement;

	/**
	 *  a listing of modes contributed to this launch configuration type
	 *
	 *  @since 3.3
	 */
	private Set<String> fModes = null;

	/**
	 * A set of sets containing all of the supported mode combinations of this type
	 *
	 * @since 3.3
	 */
	private Set<Set<String>> fModeCombinations = null;

	/**
	 * the default source path computer for this config type
	 *
	 * @since 3.3
	 */
	private ISourcePathComputer fSourcePathComputer = null;

	/**
	 * Cache for the migration delegate
	 *
	 * @since 3.3
	 */
	private ILaunchConfigurationMigrationDelegate fMigrationDelegate = null;

	/**
	 * The source locator id for this config type
	 */
	private String fSourceLocator = null;

	/**
	 * The delegates for launch configurations of this type.
	 * Delegates are instantiated lazily as required. There may
	 * be different delegates for different modes (since 3.0).
	 * Map of modes (Set of modes) to list of delegates
	 */
	private Map<Set<String>, Set<ILaunchDelegate>> fDelegates = null;

	/**
	 * The source provider cache entry
	 */
	private LaunchDelegate fSourceProvider = null;

	/**
	 * A map of preferred launch delegates for mode combinations
	 *
	 *  @since 3.3
	 */
	private Map<Set<String>, ILaunchDelegate> fPreferredDelegates = null;

	/**
	 * Constructs a new launch configuration type on the
	 * given configuration element.
	 *
	 * @param element configuration element
	 */
	protected LaunchConfigurationType(IConfigurationElement element) {
		fElement = element;
		initializePreferredDelegates();
	}

	@Override
	public String getAttribute(String attributeName) {
		return fElement.getAttribute(attributeName);
	}

	@Override
	public String getCategory() {
		return fElement.getAttribute(IConfigurationElementConstants.CATEGORY);
	}

	@Override
	public ILaunchConfigurationDelegate getDelegate() throws CoreException {
		return getDelegate(ILaunchManager.RUN_MODE);
	}

	@Override
	public ILaunchConfigurationDelegate getDelegate(String mode) throws CoreException {
		Set<String> modes = new HashSet<>();
		modes.add(mode);
		ILaunchDelegate[] delegates = getDelegates(modes);
		if (delegates.length > 0) {
			return delegates[0].getDelegate();
		}
		IStatus status = null;
		ILaunchMode launchMode = DebugPlugin.getDefault().getLaunchManager().getLaunchMode(mode);
		if (launchMode == null) {
			status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), MessageFormat.format(DebugCoreMessages.LaunchConfigurationType_7, mode));
		} else {
			status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), MessageFormat.format(DebugCoreMessages.LaunchConfigurationType_7, ((LaunchManager) DebugPlugin.getDefault().getLaunchManager()).getLaunchModeName(mode)));
		}
		throw new CoreException(status);
	}

	@Override
	public ILaunchDelegate[] getDelegates(Set<String> modes) throws CoreException {
		initializeDelegates();
		Set<ILaunchDelegate> delegates = fDelegates.get(modes);
		if (delegates == null) {
			delegates = Collections.EMPTY_SET;
		}
		return delegates.toArray(new ILaunchDelegate[delegates.size()]);
	}

	@Override
	public void setPreferredDelegate(Set<String> modes, ILaunchDelegate delegate) {
		if(fPreferredDelegates == null) {
			fPreferredDelegates = new HashMap<>();
		}
		if (delegate == null) {
			fPreferredDelegates.remove(modes);
		} else {
			fPreferredDelegates.put(modes, delegate);
		}
		((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).persistPreferredLaunchDelegate(this);
	}

	@Override
	public ILaunchDelegate getPreferredDelegate(Set<String> modes) {
		initializePreferredDelegates();
		return fPreferredDelegates.get(modes);
	}

	/**
	 * Internal use method to allow access to the listing of preferred delegates. Delegates are stored in the map by their mode set combinations.
	 * <p>
	 * preferred delegates are stored as:
	 * <pre>
	 *  Map&lt;modeset, delegate&gt;
	 * </pre>
	 * </p>
	 * @return the <code>java.util.Map</code> of preferred delegates or an empty <code>java.util.Map</code> if no preferred delegates are specified, never <code>null</code>
	 *
	 * @since 3.3
	 */
	public Map<Set<String>, ILaunchDelegate> getPreferredDelegates() {
		initializePreferredDelegates();
		return fPreferredDelegates;
	}

	/**
	 * This method is used to initialize the listing of preferred launch delegates for this type
	 *
	 * <p>
	 * Undecided if this code should live in the launch manager and have it load a listing of all preferred launch
	 * delegates that each config type could then query as needed when looking for their preferred delegate.
	 * Seems like it would be alot less work...
	 * </p>
	 * @since 3.3
	 */
	private synchronized void initializePreferredDelegates() {
		if(fPreferredDelegates == null) {
			fPreferredDelegates = new HashMap<>();
			initializeDelegates();
			LaunchManager lm = (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
			ILaunchDelegate delegate = null;
			for (Set<String> modes : fDelegates.keySet()) {
				delegate = lm.getPreferredDelegate(getIdentifier(), modes);
				if(delegate != null) {
					fPreferredDelegates.put(modes, delegate);
				}
			}
		}
	}

	/**
	 * Initializes the listing of launch delegates for this type
	 */
	private synchronized void initializeDelegates() {
		if (fDelegates == null) {
			// initialize delegate
			fDelegates = new Hashtable<>();
			LaunchDelegate[] launchDelegates = getLaunchDelegateExtensions();
			LaunchDelegate delegate = null;
			List<Set<String>> modelist = null;
			Set<ILaunchDelegate> tmp = null;
			for (LaunchDelegate launchDelegate : launchDelegates) {
				delegate = launchDelegate;
				modelist = delegate.getModes();
				for (Set<String> modes : modelist) {
					tmp = fDelegates.get(modes);
					if (tmp == null) {
						tmp = new HashSet<>();
						fDelegates.put(modes, tmp);
					}
					tmp.add(delegate);
				}
			}
		}
	}

	/**
	 * Returns all launch delegate extensions registered for this configuration type.
	 *
	 * @return all launch delegate extensions
	 */
	private LaunchDelegate[] getLaunchDelegateExtensions() {
		return ((LaunchManager) DebugPlugin.getDefault().getLaunchManager()).getLaunchDelegates(getIdentifier());
	}

	@Override
	public String getIdentifier() {
		return fElement.getAttribute(IConfigurationElementConstants.ID);
	}

	@Override
	public String getName() {
		return fElement.getAttribute(IConfigurationElementConstants.NAME);
	}

	@Override
	public String getPluginIdentifier() {
		return fElement.getContributor().getName();
	}

	@Override
	public String getSourceLocatorId() {
		if(fSourceLocator == null) {
			fSourceLocator = getAttribute(IConfigurationElementConstants.SOURCE_LOCATOR);
			//see if the cached source provider knows about it
			if(fSourceProvider != null) {
				fSourceLocator = fSourceProvider.getSourceLocatorId();
			}
			//if not provided check all the applicable delegates for one and record the delegate if found,
			//so it can be reused to try and find the source path computer
			if(fSourceLocator == null) {
				LaunchDelegate[] delegates = getLaunchDelegateExtensions();
				for (LaunchDelegate delegate : delegates) {
					fSourceLocator = delegate.getSourceLocatorId();
					if (fSourceLocator != null) {
						fSourceProvider = delegate;
						return fSourceLocator;
					}
				}
				fSourceProvider = null;
			}
		}
		return fSourceLocator;
	}

	@Override
	public ISourcePathComputer getSourcePathComputer() {
		if(fSourcePathComputer == null) {
			//get the id
			String id = fElement.getAttribute(IConfigurationElementConstants.SOURCE_PATH_COMPUTER);
			//ask if the source provider knows about it
			if(fSourceProvider != null) {
				id = fSourceProvider.getSourcePathComputerId();
			}
			if(id != null) {
				fSourcePathComputer = DebugPlugin.getDefault().getLaunchManager().getSourcePathComputer(id);
			}
			else {
			//if not provided check all the applicable delegates for one and record the delegate if found,
			//so it can be reused to try and find the source path computer
				LaunchDelegate[] delegates = getLaunchDelegateExtensions();
				for (LaunchDelegate delegate : delegates) {
					id = delegate.getSourcePathComputerId();
					if (id != null) {
						fSourceProvider = delegate;
						fSourcePathComputer = DebugPlugin.getDefault().getLaunchManager().getSourcePathComputer(id);
						if(fSourcePathComputer != null) {
							return fSourcePathComputer;
						}
					}
				}
				fSourceProvider = null;
			}

		}
		return fSourcePathComputer;
	}

	@Override
	public Set<String> getSupportedModes() {
		if(fModes == null) {
			fModes = new HashSet<>();
			LaunchDelegate[] delegates = getLaunchDelegateExtensions();
			List<Set<String>> modesets = null;
			for (LaunchDelegate delegate : delegates) {
				modesets = delegate.getModes();
				for (Set<String> modes : modesets) {
					fModes.addAll(modes);
				}
			}
		}
		return fModes;
	}

	@Override
	public Set<Set<String>> getSupportedModeCombinations() {
		if(fModeCombinations == null) {
			initializeDelegates();
			fModeCombinations = fDelegates.keySet();
		}
		return Collections.unmodifiableSet(fModeCombinations);
	}

	/**
	 * determines if the specified candidate is suitable for migration by loading its delegate.
	 * if we initialize the delegate and it has not been provided, return false instead of failing
	 * @param candidate the candidate to inspect for migration suitability
	 * @return true if the specified launch configuration is suitable for migration, false otherwise
	 * @throws CoreException if a problem is encountered
	 *
	 * @since 3.2
	 */
	public boolean isMigrationCandidate(ILaunchConfiguration candidate) throws CoreException {
		initializeMigrationDelegate();
		if(fMigrationDelegate != null) {
			return fMigrationDelegate.isCandidate(candidate);
		}
		return false;
	}

	/**
	 * This method initializes the migration delegate
	 * @throws CoreException if a problem is encountered
	 */
	private synchronized void initializeMigrationDelegate() throws CoreException {
		if(fElement.getAttribute(IConfigurationElementConstants.MIGRATION_DELEGATE) != null && fMigrationDelegate == null) {
			fMigrationDelegate = (ILaunchConfigurationMigrationDelegate) fElement.createExecutableExtension(IConfigurationElementConstants.MIGRATION_DELEGATE);
		}
	}

	@Override
	public boolean isPublic() {
		String publicString = fElement.getAttribute(IConfigurationElementConstants.PUBLIC);
		if (publicString != null) {
			if (publicString.equalsIgnoreCase("false")) { //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

	/**
	 * Migrates the specified launch configuration by loading its delegate.
	 * In the event the migration delegate has not been provided do nothing.
	 * @param candidate the candidate launch configuration to migrate
	 * @throws CoreException if a problem is encountered
	 *
	 * @since 3.2
	 */
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		initializeMigrationDelegate();
		if(fMigrationDelegate != null) {
			fMigrationDelegate.migrate(candidate);
		}
	}

	@Override
	public ILaunchConfigurationWorkingCopy newInstance(IContainer container, String name) throws CoreException {
		// validate the configuration name - see bug 275741
		IPath path = IPath.fromOSString(name);
		if (container == null) {
			// not allowed to nest in sub directory when local
			if (path.segmentCount() > 1) {
				throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugCoreMessages.LaunchConfigurationType_2));
			}
		}
		// validate the name (last segment)
		try {
			DebugPlugin.getDefault().getLaunchManager().isValidLaunchConfigurationName(path.lastSegment());
		} catch (IllegalArgumentException e) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), e.getMessage(), e));
		}
		return new LaunchConfigurationWorkingCopy(container, name, this);
	}

	@Override
	public boolean supportsMode(String mode) {
		if(fModeCombinations == null) {
			getSupportedModeCombinations();
		}
		for (Set<String> modes : fModeCombinations) {
			if(modes.size() == 1 && modes.contains(mode)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getContributorName() {
		return fElement.getContributor().getName();
	}

	@Override
	public boolean supportsModeCombination(Set<String> modes) {
		if(fModeCombinations == null) {
			getSupportedModeCombinations();
		}
		return fModeCombinations.contains(modes);
	}

	/**
	 * Called on preference import to reset preferred delegates.
	 */
	void resetPreferredDelegates() {
		fPreferredDelegates = null;
	}

	@Override
	public ILaunchConfiguration[] getPrototypes() throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(this, ILaunchConfiguration.PROTOTYPE);
	}

	@Override
	public ILaunchConfigurationWorkingCopy newPrototypeInstance(IContainer container, String name) throws CoreException {
		LaunchConfigurationWorkingCopy wc = new LaunchConfigurationWorkingCopy(container, name, this, true);
		return wc;
	}

	@Override
	public boolean supportsPrototypes() {
		String allowPrototypesString = fElement.getAttribute(IConfigurationElementConstants.ALLOW_PROTOTYPES);
		if (allowPrototypesString != null) {
			if (allowPrototypesString.equalsIgnoreCase("true")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsCommandLine() {
		String allowPrototypesString = fElement.getAttribute(IConfigurationElementConstants.ALLOW_COMMANDLINE);
		if (allowPrototypesString != null) {
			if (allowPrototypesString.equalsIgnoreCase("true")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean supportsOutputMerging() {
		String allowOutputMergingString = fElement.getAttribute(IConfigurationElementConstants.ALLOW_OUTPUT_MERGING);
		if (allowOutputMergingString != null) {
			if (allowOutputMergingString.equalsIgnoreCase("true")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}
}

