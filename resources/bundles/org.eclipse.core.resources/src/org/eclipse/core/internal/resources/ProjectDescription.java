/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) - [245937] setLinkLocation() detects non-change
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 *     Markus Schorn (Wind River) - [306575] Save snapshot location with project
 *     Broadcom Corporation - build configurations and references
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class ProjectDescription extends ModelObject implements IProjectDescription {
	// constants
	private static final IBuildConfiguration[] EMPTY_BUILD_CONFIG_REFERENCE_ARRAY = new IBuildConfiguration[0];
	private static final ICommand[] EMPTY_COMMAND_ARRAY = new ICommand[0];
	private static final IProject[] EMPTY_PROJECT_ARRAY = new IProject[0];
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String EMPTY_STR = ""; //$NON-NLS-1$

	protected static boolean isReading = false;

	//flags to indicate when we are in the middle of reading or writing a
	// workspace description
	//these can be static because only one description can be read at once.
	protected static boolean isWriting = false;
	protected ICommand[] buildSpec = EMPTY_COMMAND_ARRAY;
	protected String comment = EMPTY_STR;

	// Build configuration + References state
	/** Id of the currently active build configuration */
	protected String activeConfiguration = IBuildConfiguration.DEFAULT_CONFIG_NAME;
	/**
	 * The 'real' build configuration names set on this project.
	 * This doesn't contain the generated 'default' build configuration with name
	 * {@link IBuildConfiguration#DEFAULT_CONFIG_NAME}
	 * when no build configurations have been defined.
	 */
	protected String[] configNames = EMPTY_STRING_ARRAY;
	// Static + Dynamic project level references
	protected IProject[] staticRefs = EMPTY_PROJECT_ARRAY;
	protected IProject[] dynamicRefs = EMPTY_PROJECT_ARRAY;
	/** Map from config name in this project -&gt; build configurations in other projects */
	protected HashMap<String, IBuildConfiguration[]> dynamicConfigRefs = new HashMap<>(1);

	// Cache of the build configurations
	protected volatile IBuildConfiguration[] cachedBuildConfigs;
	// Cached build configuration references. Not persisted.
	protected Map<String, IBuildConfiguration[]> cachedConfigRefs = Collections.synchronizedMap(new HashMap<>(1));
	/**
	 * Cached project level references. Synchronize on {@link #cachedRefsMutex} before reading or writing. Increment
	 * {@link #cachedRefsDirtyCount} whenever this is dirtied.
	 */
	protected IProject[] cachedRefs;
	/**
	 * Counts the number of times {@link #cachedRefs} has been dirtied. Can be used to determine if dynamic dependencies have
	 * changed during an operation that is intended to be atomic with respect to dynamic dependencies. Synchronize on
	 * {@link #cachedRefsMutex} before accessing.
	 */
	protected int cachedRefsDirtyCount;
	/**
	 * Mutex used to protect {@link #cachedRefs} and {@link #cachedRefsDirtyCount}.
	 */
	protected final Object cachedRefsMutex = new Object();

	/**
	 * Map of (IPath -&gt; LinkDescription) pairs for each linked resource
	 * in this project, where IPath is the project relative path of the resource.
	 */
	protected HashMap<IPath, LinkDescription> linkDescriptions = null;

	/**
	 * Map of {@literal (IPath -> LinkedList<FilterDescription>)} pairs for each filtered resource
	 * in this project, where IPath is the project relative path of the resource.
	 */
	protected HashMap<IPath, LinkedList<FilterDescription>> filterDescriptions = null;

	/**
	 * Map of (String -&gt; VariableDescription) pairs for each variable in this
	 * project, where String is the name of the variable.
	 */
	protected HashMap<String, VariableDescription> variableDescriptions = null;

	// fields
	protected URI location = null;
	protected String[] natures = EMPTY_STRING_ARRAY;
	protected URI snapshotLocation = null;

	public ProjectDescription() {
		super();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Object clone() {
		ProjectDescription clone = (ProjectDescription) super.clone();
		//don't want the clone to have access to our internal link locations table or builders
		clone.linkDescriptions = null;
		clone.filterDescriptions = null;
		if (variableDescriptions != null)
			clone.variableDescriptions = (HashMap<String, VariableDescription>) variableDescriptions.clone();
		clone.buildSpec = getBuildSpec(true);
		clone.dynamicConfigRefs = (HashMap<String, IBuildConfiguration[]>) dynamicConfigRefs.clone();
		clone.cachedConfigRefs = Collections.synchronizedMap(new HashMap<>(1));
		clone.clearCachedDynamicReferences(null);
		return clone;
	}

	/**
	 * Clear cached references for the specified build config name
	 * or all if configName is null.
	 */
	public void clearCachedDynamicReferences(String configName) {
		synchronized (cachedRefsMutex) {
			if (configName == null)
				cachedConfigRefs.clear();
			else
				cachedConfigRefs.remove(configName);
			cachedRefs = null;
			cachedRefsDirtyCount++;
		}
	}

	/**
	 * Returns a copy of the given array of build configs with all duplicates removed
	 */
	private IBuildConfiguration[] copyAndRemoveDuplicates(IBuildConfiguration[] values) {
		Set<IBuildConfiguration> set = new LinkedHashSet<>(Arrays.asList(values));
		return set.toArray(new IBuildConfiguration[set.size()]);
	}

	/**
	 * Returns a copy of the given array with all duplicates removed
	 */
	private IProject[] copyAndRemoveDuplicates(IProject[] projects) {
		IProject[] result = new IProject[projects.length];
		int count = 0;
		next: for (IProject project : projects) {
			// scan to see if there are any other projects by the same name
			for (int j = 0; j < count; j++)
				if (project.equals(result[j]))
					continue next;
			// not found
			result[count++] = project;
		}
		if (count < projects.length) {
			//shrink array
			IProject[] reduced = new IProject[count];
			System.arraycopy(result, 0, reduced, 0, count);
			return reduced;
		}
		return result;
	}

	/**
	 * Helper to turn an array of projects into an array of {@link IBuildConfiguration} to the
	 * projects' active configuration
	 * Order is preserved - the buildConfigs appear for each project in the order
	 * that the projects were specified.
	 * @param projects projects to get the active configuration from
	 * @return collection of build config references
	 */
	private Collection<BuildConfiguration> getBuildConfigReferencesFromProjects(IProject[] projects) {
		List<BuildConfiguration> refs = new ArrayList<>(projects.length);
		for (IProject project : projects)
			refs.add(new BuildConfiguration(project, null));
		return refs;
	}

	/**
	 * Helper to fetch projects from an array of build configuration references
	 * @param refs
	 * @return {@literal List<IProject>}
	 */
	private Collection<IProject> getProjectsFromBuildConfigRefs(IBuildConfiguration[] refs) {
		LinkedHashSet<IProject> projects = new LinkedHashSet<>(refs.length);
		for (IBuildConfiguration ref : refs)
			projects.add(ref.getProject());
		return projects;
	}

	public String getActiveBuildConfig() {
		return activeConfiguration;
	}

	/**
	 * Returns the union of the description's static and dynamic project references,
	 * with duplicates omitted. The calculation is optimized by caching the result
	 * Call the configuration based implementation.
	 * @see #getAllBuildConfigReferences(IProject, String, boolean)
	 */
	public IProject[] getAllReferences(IProject project, boolean makeCopy) {
		int dirtyCount;
		IProject[] projRefs;

		synchronized (cachedRefsMutex) {
			projRefs = cachedRefs;
			dirtyCount = cachedRefsDirtyCount;
		}
		// Retry this computation until we're able to proceed to the end without someone dirtying the cache.
		// This loop is here to prevent us from caching a stale result if someone dirties the cache between
		// the time we invoke getAllBuildConfigReferences and the time we can write to cachedRefs.
		while (projRefs == null) {
			IBuildConfiguration[] refs;
			if (hasBuildConfig(activeConfiguration))
				refs = getAllBuildConfigReferences(project, activeConfiguration, false);
			else if (configNames.length > 0)
				refs = getAllBuildConfigReferences(project, configNames[0], false);
			else
				// No build configuration => fall-back to default
				refs = getAllBuildConfigReferences(project, IBuildConfiguration.DEFAULT_CONFIG_NAME, false);
			Collection<IProject> l = getProjectsFromBuildConfigRefs(refs);

			synchronized (cachedRefsMutex) {
				// If nobody dirtied the cache since the start of this operation then we can cache the
				// new result and end the loop.
				if (cachedRefsDirtyCount == dirtyCount) {
					cachedRefs = l.toArray(new IProject[l.size()]);
				}
				projRefs = cachedRefs;
				dirtyCount = cachedRefsDirtyCount;
			}
		}
		//still need to copy the result to prevent tampering with the cache
		return makeCopy ? (IProject[]) projRefs.clone() : projRefs;
	}

	/**
	 * The main entrance point to fetch the full set of Project references.
	 *
	 * Returns the union of all the description's references. Includes static and dynamic
	 * project level references as well as build configuration references for the configuration
	 * with the given id.
	 * Duplicates are omitted.  The calculation is optimized by caching the result.
	 * Note that these BuildConfiguration references may have <code>null</code> name.  They must
	 * be resolved using {@link BuildConfiguration#getBuildConfig()} before use.
	 * Returns an empty array if the given configName does not exist in the description.
	 */
	public IBuildConfiguration[] getAllBuildConfigReferences(IProject project, String configName, boolean makeCopy) {
		if (!hasBuildConfig(configName))
			return EMPTY_BUILD_CONFIG_REFERENCE_ARRAY;
		IBuildConfiguration[] refs = cachedConfigRefs.get(configName);
		if (refs == null) {
			Set<IBuildConfiguration> references = new LinkedHashSet<>();
			IBuildConfiguration[] dynamicBuildConfigs = dynamicConfigRefs.containsKey(configName) ? dynamicConfigRefs.get(configName) : EMPTY_BUILD_CONFIG_REFERENCE_ARRAY;
			Collection<BuildConfiguration> dynamic;
			try {
				IBuildConfiguration buildConfig = project.getBuildConfig(configName);
				dynamic = getBuildConfigReferencesFromProjects(computeDynamicReferencesForProject(buildConfig, getBuildSpec()));
			} catch (CoreException e) {
				dynamic = Collections.emptyList();
			}
			Collection<BuildConfiguration> legacyDynamic = getBuildConfigReferencesFromProjects(dynamicRefs);
			Collection<BuildConfiguration> statik = getBuildConfigReferencesFromProjects(staticRefs);

			// Combine all references:
			// New build config references (which only come in dynamic form) trump all others.
			references.addAll(Arrays.asList(dynamicBuildConfigs));
			// We preserve the previous order of static project references before dynamic project references
			references.addAll(statik);
			references.addAll(legacyDynamic);
			references.addAll(dynamic);
			refs = references.toArray(new IBuildConfiguration[references.size()]);
			cachedConfigRefs.put(configName, refs);
		}
		return makeCopy ? (IBuildConfiguration[]) refs.clone() : refs;
	}

	/**
	 * Used by Project to get the buildConfigs on the description.
	 * @return the project configurations
	 */
	public IBuildConfiguration[] getBuildConfigs(IProject project, boolean makeCopy) {
		IBuildConfiguration[] configs = cachedBuildConfigs;
		// Ensure project is up to date in the cache
		if (configs != null && !project.equals(configs[0].getProject()))
			configs = null;
		if (configs == null) {
			if (configNames.length == 0)
				configs = new IBuildConfiguration[] {new BuildConfiguration(project)};
			else {
				configs = new IBuildConfiguration[configNames.length];
				for (int i = 0; i < configs.length; i++)
					configs[i] = new BuildConfiguration(project, configNames[i]);
			}
			cachedBuildConfigs = configs;
		}
		return makeCopy ? (IBuildConfiguration[]) configs.clone() : configs;
	}

	@Override
	public IBuildConfiguration[] getBuildConfigReferences(String configName) {
		return getBuildConfigRefs(configName, true);
	}

	public IBuildConfiguration[] getBuildConfigRefs(String configName, boolean makeCopy) {
		if (!hasBuildConfig(configName) || !dynamicConfigRefs.containsKey(configName))
			return EMPTY_BUILD_CONFIG_REFERENCE_ARRAY;

		return makeCopy ? (IBuildConfiguration[]) dynamicConfigRefs.get(configName).clone() : dynamicConfigRefs.get(configName);
	}

	/**
	 * Returns the build configuration references map
	 * @param makeCopy
	 */
	@SuppressWarnings({"unchecked"})
	public Map<String, IBuildConfiguration[]> getBuildConfigReferences(boolean makeCopy) {
		return makeCopy ? (Map<String, IBuildConfiguration[]>) dynamicConfigRefs.clone() : dynamicConfigRefs;
	}

	@Override
	public ICommand[] getBuildSpec() {
		return getBuildSpec(true);
	}

	public ICommand[] getBuildSpec(boolean makeCopy) {
		//thread safety: copy reference in case of concurrent write
		ICommand[] oldCommands = this.buildSpec;
		if (oldCommands == null)
			return EMPTY_COMMAND_ARRAY;
		if (!makeCopy)
			return oldCommands;
		ICommand[] result = new ICommand[oldCommands.length];
		for (int i = 0; i < result.length; i++)
			result[i] = (ICommand) ((BuildCommand) oldCommands[i]).clone();
		return result;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public IProject[] getDynamicReferences() {
		return getDynamicReferences(true);
	}

	public IProject[] getDynamicReferences(boolean makeCopy) {
		return makeCopy ? (IProject[]) dynamicRefs.clone() : dynamicRefs;
	}

	/**
	 * Returns the link location for the given resource name. Returns null if
	 * no such link exists.
	 */
	public URI getLinkLocationURI(IPath aPath) {
		if (linkDescriptions == null)
			return null;
		LinkDescription desc = linkDescriptions.get(aPath);
		return desc == null ? null : desc.getLocationURI();
	}

	/**
	 * Returns the filter for the given resource name. Returns null if
	 * no such filter exists.
	 */
	synchronized public LinkedList<FilterDescription> getFilter(IPath aPath) {
		if (filterDescriptions == null)
			return null;
		return filterDescriptions.get(aPath);
	}

	/**
	 * Returns the map of link descriptions (IPath (project relative path) -&gt; LinkDescription).
	 * Since this method is only used internally, it never creates a copy.
	 * Returns null if the project does not have any linked resources.
	 */
	public HashMap<IPath, LinkDescription> getLinks() {
		return linkDescriptions;
	}

	/**
	 * Returns the map of filter descriptions (IPath (project relative path) -&gt;
	 * {@literal LinkedList<FilterDescription>}). Since this method is only used
	 * internally, it never creates a copy. Returns null if the project does not
	 * have any filtered resources.
	 */
	public HashMap<IPath, LinkedList<FilterDescription>> getFilters() {
		return filterDescriptions;
	}

	/**
	 * Returns the map of variable descriptions (String (variable name) -&gt;
	 * VariableDescription). Since this method is only used internally, it never
	 * creates a copy. Returns null if the project does not have any variables.
	 */
	public HashMap<String, VariableDescription> getVariables() {
		return variableDescriptions;
	}

	/**
	 * @see IProjectDescription#getLocation()
	 * @deprecated
	 */
	@Override
	@Deprecated
	public IPath getLocation() {
		if (location == null)
			return null;
		return FileUtil.toPath(location);
	}

	@Override
	public URI getLocationURI() {
		return location;
	}

	@Override
	public String[] getNatureIds() {
		return getNatureIds(true);
	}

	public String[] getNatureIds(boolean makeCopy) {
		if (natures == null)
			return EMPTY_STRING_ARRAY;
		return makeCopy ? (String[]) natures.clone() : natures;
	}

	@Override
	public IProject[] getReferencedProjects() {
		return getReferencedProjects(true);
	}

	public IProject[] getReferencedProjects(boolean makeCopy) {
		if (staticRefs == null)
			return EMPTY_PROJECT_ARRAY;
		return makeCopy ? (IProject[]) staticRefs.clone() : staticRefs;
	}

	/**
	 * Returns the URI to load a resource snapshot from.
	 * May return <code>null</code> if no snapshot is set.
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This constant has been added as
	 * part of a work in progress. There is no guarantee that this API will
	 * work or that it will remain the same. Please do not use this API without
	 * consulting with the Platform Core team.
	 * </p>
	 * @return the snapshot location URI,
	 *   or <code>null</code>.
	 * @see IProject#loadSnapshot(int, URI, IProgressMonitor)
	 * @see #setSnapshotLocationURI(URI)
	 * @since 3.6
	 */
	public URI getSnapshotLocationURI() {
		return snapshotLocation;
	}

	@Override
	public boolean hasNature(String natureID) {
		String[] natureIDs = getNatureIds(false);
		for (String natureID2 : natureIDs)
			if (natureID2.equals(natureID))
				return true;
		return false;
	}

	/**
	 * Helper method to compare two maps of Configuration Name -&gt; IBuildConfigurationReference[]
	 * @return boolean indicating if there are differences between the two maps
	 */
	private static boolean configRefsHaveChanges(Map<String, IBuildConfiguration[]> m1, Map<String, IBuildConfiguration[]> m2) {
		if (m1.size() != m2.size())
			return true;
		for (Entry<String, IBuildConfiguration[]> e : m1.entrySet()) {
			if (!m2.containsKey(e.getKey()))
				return true;
			if (!Arrays.equals(e.getValue(), m2.get(e.getKey())))
				return true;
		}
		return false;
	}

	/**
	 * Internal method to check if the description has a given build configuration.
	 */
	boolean hasBuildConfig(String buildConfigName) {
		Assert.isNotNull(buildConfigName);
		if (configNames.length == 0)
			return IBuildConfiguration.DEFAULT_CONFIG_NAME.equals(buildConfigName);
		for (String configName : configNames)
			if (configName.equals(buildConfigName))
				return true;
		return false;
	}

	/**
	 * Returns true if any private attributes of the description have changed.
	 * Private attributes are those that are not stored in the project description
	 * file (.project).
	 */
	public boolean hasPrivateChanges(ProjectDescription description) {
		if (location == null) {
			if (description.location != null)
				return true;
		} else if (!location.equals(description.location))
			return true;

		if (!Arrays.equals(dynamicRefs, description.dynamicRefs))
			return true;

		// Build Configuration state
		if (!activeConfiguration.equals(description.activeConfiguration))
			return true;
		if (!Arrays.equals(configNames, description.configNames))
			return true;
		// Configuration level references
		if (configRefsHaveChanges(dynamicConfigRefs, description.dynamicConfigRefs))
			return true;

		return false;
	}

	/**
	 * Returns true if any public attributes of the description have changed.
	 * Public attributes are those that are stored in the project description
	 * file (.project).
	 */
	public boolean hasPublicChanges(ProjectDescription description) {
		if (!getName().equals(description.getName()))
			return true;
		if (!comment.equals(description.getComment()))
			return true;
		//don't bother optimizing if the order has changed
		if (!Arrays.equals(buildSpec, description.getBuildSpec(false)))
			return true;
		if (!Arrays.equals(staticRefs, description.getReferencedProjects(false)))
			return true;
		if (!Arrays.equals(natures, description.getNatureIds(false)))
			return true;

		HashMap<IPath, LinkedList<FilterDescription>> otherFilters = description.getFilters();
		if ((filterDescriptions == null) && (otherFilters != null))
			return otherFilters != null;
		if ((filterDescriptions != null) && !filterDescriptions.equals(otherFilters))
			return true;

		HashMap<String, VariableDescription> otherVariables = description.getVariables();
		if ((variableDescriptions == null) && (otherVariables != null))
			return true;
		if ((variableDescriptions != null) && !variableDescriptions.equals(otherVariables))
			return true;

		final HashMap<IPath, LinkDescription> otherLinks = description.getLinks();
		if (linkDescriptions != otherLinks) {
			if (linkDescriptions == null || !linkDescriptions.equals(otherLinks))
				return true;
		}

		final URI otherSnapshotLoc = description.getSnapshotLocationURI();
		if (snapshotLocation != otherSnapshotLoc) {
			if (snapshotLocation == null || !snapshotLocation.equals(otherSnapshotLoc))
				return true;
		}
		return false;
	}

	@Override
	public ICommand newCommand() {
		return new BuildCommand();
	}

	@Override
	public void setActiveBuildConfig(String configName) {
		Assert.isNotNull(configName);
		if (!configName.equals(activeConfiguration))
			clearCachedDynamicReferences(null);
		activeConfiguration = configName;
	}

	@Override
	public void setBuildSpec(ICommand[] value) {
		Assert.isLegal(value != null);
		//perform a deep copy in case clients perform further changes to the command
		ICommand[] result = new ICommand[value.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (ICommand) ((BuildCommand) value[i]).clone();
			//copy the reference to any builder instance from the old build spec
			//to preserve builder states if possible.
			for (ICommand element : buildSpec) {
				if (result[i].equals(element)) {
					((BuildCommand) result[i]).setBuilders(((BuildCommand) element).getBuilders());
					break;
				}
			}
		}
		buildSpec = result;
	}

	@Override
	public void setComment(String value) {
		comment = value;
	}

	@Deprecated
	@Override
	public void setDynamicReferences(IProject[] value) {
		Assert.isLegal(value != null);
		dynamicRefs = copyAndRemoveDuplicates(value);
		clearCachedDynamicReferences(null);
	}

	public void setBuildConfigReferences(HashMap<String, IBuildConfiguration[]> refs) {
		dynamicConfigRefs = new HashMap<>(refs);
		clearCachedDynamicReferences(null);
	}

	@Override
	public void setBuildConfigReferences(String configName, IBuildConfiguration[] references) {
		Assert.isLegal(configName != null);
		Assert.isLegal(references != null);
		if (!hasBuildConfig(configName))
			return;
		dynamicConfigRefs.put(configName, copyAndRemoveDuplicates(references));
		clearCachedDynamicReferences(configName);
	}

	@Override
	public void setBuildConfigs(String[] names) {
		// Remove references for deleted buildConfigs
		LinkedHashSet<String> buildConfigNames = new LinkedHashSet<>();

		if (names == null || names.length == 0) {
			configNames = EMPTY_STRING_ARRAY;
			buildConfigNames.add(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		} else {
			// Filter out duplicates
			for (String n : names) {
				Assert.isLegal(n != null);
				buildConfigNames.add(n);
			}

			if (buildConfigNames.size() == 1 && ((buildConfigNames.iterator().next())).equals(IBuildConfiguration.DEFAULT_CONFIG_NAME))
				configNames = EMPTY_STRING_ARRAY;
			else
				configNames = buildConfigNames.toArray(new String[buildConfigNames.size()]);
		}

		// Remove references for deleted buildConfigs
		boolean modified = dynamicConfigRefs.keySet().retainAll(buildConfigNames);
		if (modified)
			clearCachedDynamicReferences(null);
		// Clear the cached IBuildConfiguration[]
		cachedBuildConfigs = null;
	}

	/**
	 * Sets the map of link descriptions (String name -&gt; LinkDescription).
	 * Since this method is only used internally, it never creates a copy. May
	 * pass null if this project does not have any linked resources
	 */
	public void setLinkDescriptions(HashMap<IPath, LinkDescription> linkDescriptions) {
		this.linkDescriptions = linkDescriptions;
	}

	/**
	 * Sets the map of filter descriptions {@literal (String name -> LinkedList<LinkDescription>)}.
	 * Since this method is only used internally, it never creates a copy. May
	 * pass null if this project does not have any filtered resources
	 */
	public void setFilterDescriptions(HashMap<IPath, LinkedList<FilterDescription>> filterDescriptions) {
		this.filterDescriptions = filterDescriptions;
	}

	/**
	 * Sets the map of variable descriptions (String name -&gt;
	 * VariableDescription). Since this method is only used internally, it never
	 * creates a copy. May pass null if this project does not have any variables
	 */
	public void setVariableDescriptions(HashMap<String, VariableDescription> variableDescriptions) {
		this.variableDescriptions = variableDescriptions;
	}

	/**
	 * Sets the description of a link. Setting to a description of null will
	 * remove the link from the project description.
	 * @return <code>true</code> if the description was actually changed,
	 *     <code>false</code> otherwise.
	 * @since 3.5 returns boolean (was void before)
	 */
	@SuppressWarnings({"unchecked"})
	public boolean setLinkLocation(IPath path, LinkDescription description) {
		HashMap<IPath, LinkDescription> tempMap = linkDescriptions;
		if (description != null) {
			//addition or modification
			if (tempMap == null)
				tempMap = new HashMap<>(10);
			else
				//copy on write to protect against concurrent read
				tempMap = (HashMap<IPath, LinkDescription>) tempMap.clone();
			Object oldValue = tempMap.put(path, description);
			if (oldValue != null && description.equals(oldValue)) {
				//not actually changed anything
				return false;
			}
			linkDescriptions = tempMap;
		} else {
			//removal
			if (tempMap == null)
				return false;
			//copy on write to protect against concurrent access
			HashMap<IPath, LinkDescription> newMap = (HashMap<IPath, LinkDescription>) tempMap.clone();
			Object oldValue = newMap.remove(path);
			if (oldValue == null) {
				//not actually changed anything
				return false;
			}
			linkDescriptions = newMap.isEmpty() ? null : newMap;
		}
		return true;
	}

	/**
	 * Add the description of a filter. Setting to a description of null will
	 * remove the filter from the project description.
	 */
	synchronized public void addFilter(IPath path, FilterDescription description) {
		Assert.isNotNull(description);
		if (filterDescriptions == null)
			filterDescriptions = new HashMap<>(10);
		LinkedList<FilterDescription> descList = filterDescriptions.get(path);
		if (descList == null) {
			descList = new LinkedList<>();
			filterDescriptions.put(path, descList);
		}
		descList.add(description);
	}

	/**
	 * Add the description of a filter. Setting to a description of null will
	 * remove the filter from the project description.
	 */
	synchronized public void removeFilter(IPath path, FilterDescription description) {
		if (filterDescriptions != null) {
			LinkedList<FilterDescription> descList = filterDescriptions.get(path);
			if (descList != null) {
				descList.remove(description);
				if (descList.isEmpty()) {
					filterDescriptions.remove(path);
					if (filterDescriptions.isEmpty())
						filterDescriptions = null;
				}
			}
		}
	}

	/**
	 * Sets the description of a variable. Setting to a description of null will
	 * remove the variable from the project description.
	 * @return <code>true</code> if the description was actually changed,
	 *     <code>false</code> otherwise.
	 * @since 3.5
	 */
	@SuppressWarnings({"unchecked"})
	public boolean setVariableDescription(String name, VariableDescription description) {
		HashMap<String, VariableDescription> tempMap = variableDescriptions;
		if (description != null) {
			// addition or modification
			if (tempMap == null)
				tempMap = new HashMap<>(10);
			else
				// copy on write to protect against concurrent read
				tempMap = (HashMap<String, VariableDescription>) tempMap.clone();
			Object oldValue = tempMap.put(name, description);
			if (oldValue != null && description.equals(oldValue)) {
				//not actually changed anything
				return false;
			}
			variableDescriptions = tempMap;
		} else {
			// removal
			if (tempMap == null)
				return false;
			// copy on write to protect against concurrent access
			HashMap<String, VariableDescription> newMap = (HashMap<String, VariableDescription>) tempMap.clone();
			Object oldValue = newMap.remove(name);
			if (oldValue == null) {
				//not actually changed anything
				return false;
			}
			variableDescriptions = newMap.isEmpty() ? null : newMap;
		}
		return true;
	}

	/**
	 * set the filters for a given resource. Setting to a description of null will
	 * remove the filter from the project description.
	 * @return <code>true</code> if the description was actually changed,
	 *     <code>false</code> otherwise.
	 */
	synchronized public boolean setFilters(IPath path, LinkedList<FilterDescription> descriptions) {
		if (descriptions != null) {
			// addition
			if (filterDescriptions == null)
				filterDescriptions = new HashMap<>(10);
			Object oldValue = filterDescriptions.put(path, descriptions);
			if (oldValue != null && descriptions.equals(oldValue)) {
				//not actually changed anything
				return false;
			}
		} else {
			// removal
			if (filterDescriptions == null)
				return false;

			Object oldValue = filterDescriptions.remove(path);
			if (oldValue == null) {
				//not actually changed anything
				return false;
			}
			if (filterDescriptions.isEmpty())
				filterDescriptions = null;
		}
		return true;
	}

	@Override
	public void setLocation(IPath path) {
		this.location = path == null ? null : URIUtil.toURI(path);
	}

	@Override
	public void setLocationURI(URI location) {
		this.location = location;
	}

	@Override
	public void setName(String value) {
		super.setName(value);
	}

	@Override
	public void setNatureIds(String[] value) {
		natures = value.clone();
	}

	@Override
	public void setReferencedProjects(IProject[] value) {
		Assert.isLegal(value != null);
		staticRefs = copyAndRemoveDuplicates(value);
		clearCachedDynamicReferences(null);
	}

	/**
	 * Sets the location URI for a project snapshot that may be
	 * loaded automatically when the project is created in a workspace.
	 * <p>
	 * <strong>EXPERIMENTAL</strong>. This method has been added as
	 * part of a work in progress. There is no guarantee that this API will
	 * work or that it will remain the same. Please do not use this API without
	 * consulting with the Platform Core team.
	 * </p>
	 * @param snapshotLocation the location URI or
	 *    <code>null</code> to clear the setting
	 * @see IProject#loadSnapshot(int, URI, IProgressMonitor)
	 * @see #getSnapshotLocationURI()
	 * @since 3.6
	 */
	public void setSnapshotLocationURI(URI snapshotLocation) {
		this.snapshotLocation = snapshotLocation;
	}

	public URI getGroupLocationURI(IPath projectRelativePath) {
		return LinkDescription.VIRTUAL_LOCATION;
	}

	/**
	 * Updates the dynamic build configuration and reference state to that of the passed in
	 * description.
	 * Copies in:
	 * <ul>
	 * <li>Active configuration name</li>
	 * <li>Dynamic Project References</li>
	 * <li>Build configurations list</li>
	 * <li>Build Configuration References</li>
	 * </ul>
	 * @param description Project description to copy dynamic state from
	 * @return boolean indicating if anything changed requing re-calculation of WS build order
	 */
	public boolean updateDynamicState(ProjectDescription description) {
		boolean changed = false;
		if (!activeConfiguration.equals(description.activeConfiguration)) {
			changed = true;
			activeConfiguration = description.activeConfiguration;
		}
		if (!Arrays.equals(dynamicRefs, description.dynamicRefs)) {
			changed = true;
			setDynamicReferences(description.dynamicRefs);
		}
		if (!Arrays.equals(configNames, description.configNames)) {
			changed = true;
			setBuildConfigs(description.configNames);
		}
		if (configRefsHaveChanges(dynamicConfigRefs, description.dynamicConfigRefs)) {
			changed = true;
			dynamicConfigRefs = new HashMap<>(description.dynamicConfigRefs);
		}
		if (changed)
			clearCachedDynamicReferences(null);
		return changed;
	}

	/**
	 * Computes the dynamic references for the given project + configuration.
	 */
	private static IProject[] computeDynamicReferencesForProject(IBuildConfiguration buildConfig, ICommand[] buildSpec) {
		List<IProject> result = new ArrayList<>();
		for (ICommand command : buildSpec) {
			IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS, command.getBuilderName());

			if (extension == null) {
				continue;
			}

			IConfigurationElement[] configurationElements = extension.getConfigurationElements();

			if (configurationElements.length == 0) {
				continue;
			}

			IConfigurationElement element = configurationElements[0];

			Object executableExtension;
			try {
				IConfigurationElement[] children = element.getChildren("dynamicReference"); //$NON-NLS-1$
				if (children.length != 0) {
					executableExtension = children[0].createExecutableExtension("class"); //$NON-NLS-1$
					if (executableExtension instanceof IDynamicReferenceProvider) {
						IDynamicReferenceProvider provider = (IDynamicReferenceProvider) executableExtension;

						result.addAll(provider.getDependentProjects(buildConfig));
					}
				}
			} catch (CoreException e) {
				String problemElement = element.toString();
				ResourcesPlugin.getPlugin().getLog().log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Unable to load dynamic reference provider: " + problemElement, e)); //$NON-NLS-1$
			}
		}
		return result.toArray(new IProject[0]);
	}
}
