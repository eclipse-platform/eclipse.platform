/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.internal.ccvs.core.CVSProjectSetCapability.LoadInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;

public class CVSRepositoryLocationMatcher {

	private static final String EXTSSH = "extssh"; //$NON-NLS-1$
	private static final String PSERVER = "pserver"; //$NON-NLS-1$
	private static final String EXT = "ext"; //$NON-NLS-1$

	private static Comparator<ICVSRepositoryLocation> COMPATIBLE_LOCATIONS_COMPARATOR = (rl1, rl2) -> {
		String name1 = rl1.getMethod().getName();
		String name2 = rl2.getMethod().getName();

		if (!name1.equals(name2) && isCompatible(rl1, rl2, false)) {
			if (name1.equals(EXTSSH))
				return -1;
			if (name2.equals(EXTSSH))
				return 1;
			if (name1.equals(PSERVER))
				return -1;
			if (name2.equals(PSERVER))
				return 1;
			if (name1.equals(EXT))
				return -1;
			if (name2.equals(EXT))
				return 1;
		}
		return name1.compareTo(name2);
	};

	public static Map<IProject, List<ICVSRepositoryLocation>> prepareSuggestedRepositoryLocations(
			IProject[] projects, final Map<IProject, LoadInfo> infoMap) {
		List<IProject> confirmedProjectsList = Arrays.asList(projects);
		Set<ICVSRepositoryLocation> projectSetRepositoryLocations = new HashSet<>();
		for (Iterator i = infoMap.keySet().iterator(); i.hasNext();) {
			IProject project = (IProject) i.next();
			if (confirmedProjectsList.contains(project)) {
				LoadInfo loadInfo = infoMap.get(project);
				projectSetRepositoryLocations.add(loadInfo.repositoryLocation);
			}
		}

		// none of projects from project sets is confirmed to overwrite
		if (projectSetRepositoryLocations.isEmpty()) {
			return null;
		}

		List<ICVSRepositoryLocation> knownRepositories = Arrays
				.asList(KnownRepositories.getInstance().getRepositories());

		if (knownRepositories.isEmpty()) {
			// There are no known repositories so use repository location from
			// the project set.
			Map result = new HashMap();
			for (Iterator i = projectSetRepositoryLocations.iterator(); i
					.hasNext();) {
				ICVSRepositoryLocation projectSetRepositoryLocation = (ICVSRepositoryLocation) i
						.next();
				ArrayList<ICVSRepositoryLocation> list = new ArrayList<>(1);
				list.add(projectSetRepositoryLocation);
				result.put(projectSetRepositoryLocation, list);
			}
			return result;
		} else if (knownRepositories.containsAll(projectSetRepositoryLocations)) {
			// All repositories are known, no need to prompt for additional
			// information.
			return Collections.emptyMap();
		} else {
			// Not all repositories from the project set are known.
			Map result = new HashMap();

			for (Iterator i = projectSetRepositoryLocations.iterator(); i
					.hasNext();) {
				ICVSRepositoryLocation projectSetRepositoryLocation = (ICVSRepositoryLocation) i
						.next();

				List<ICVSRepositoryLocation> matching = new ArrayList<>();
				List<ICVSRepositoryLocation> compatible = new ArrayList<>();
				List<ICVSRepositoryLocation> list = new ArrayList<>();
				for (Iterator j = knownRepositories.iterator(); j.hasNext();) {
					ICVSRepositoryLocation knownRepositoryLocation = (ICVSRepositoryLocation) j
							.next();
					// There can be more than one perfect match (i.e. two
					// known, matching repositories with different user names)
					if (CVSRepositoryLocationMatcher.isMatching(
							projectSetRepositoryLocation,
							knownRepositoryLocation)) {
						matching.add(knownRepositoryLocation);
					} else if (CVSRepositoryLocationMatcher.isCompatible(
							knownRepositoryLocation,
							projectSetRepositoryLocation, false)) {
						compatible.add(knownRepositoryLocation);
					} else {
						list.add(knownRepositoryLocation);
					}
				}

				// Sort compatible repository locations starting from extssh,
				// followed by pserver and finally ext.
				Collections.sort(compatible, COMPATIBLE_LOCATIONS_COMPARATOR);

				// Add compatible repos before others
				list.addAll(0, compatible);

				if (matching.isEmpty()) {
					// If no matching repo locations found add the one
					// from the project set first.
					list.add(0, projectSetRepositoryLocation);
				} else if (matching.size() == 1) {
					// There is only one matching, known repository
					// so there is no need to ask for any additional info.
					// Don't add it to the 'resultMap'
					list.clear();
					list.addAll(matching);
					result.put(projectSetRepositoryLocation, list);
					continue;
				} else {
					// There is more than one matching, known repository, so
					// ask which one we should use during the import
					list.addAll(0, matching);
				}

				result.put(projectSetRepositoryLocation, list);
			}
			return result;
		}
	}

	/**
	 * Same check as in
	 * org.eclipse.team.internal.ccvs.ui.CVSProjectPropertiesPage class.
	 * 
	 * @see org.eclipse.team.internal.ccvs.ui.CVSProjectPropertiesPage#isCompatible
	 * 
	 * @param location1
	 *            First repository location to match
	 * @param location2
	 *            Second repository location to match
	 * @param equalIsCompatible
	 *            If equal means compatible
	 * @return <code>true</code> if given repository location are compatible,
	 *         otherwise <code>false</code> is returned.
	 */
	public static boolean isCompatible(ICVSRepositoryLocation location1,
			ICVSRepositoryLocation location2, boolean equalIsCompatible) {
		if (!location1.getHost().equals(location2.getHost()))
			return false;
		if (!location1.getRootDirectory().equals(location2.getRootDirectory()))
			return false;
		if (!equalIsCompatible && location1.equals(location2))
			return false;
		return true;
	}

	/**
	 * Checks whether two repository locations match (i.e. they are compatible,
	 * they use the same connection method and they use the same port) .
	 * 
	 * @param location1
	 *            First repository location to match
	 * @param location2
	 *            Second repository location to match
	 * @return <code>true</code> if given repository location are matching
	 *         according to the rule above, otherwise <code>false</code> is
	 *         returned.
	 */
	public static boolean isMatching(ICVSRepositoryLocation location1,
			ICVSRepositoryLocation location2) {
		if (isCompatible(location1, location2, true)
				&& location2.getMethod() == location1.getMethod()
				&& location2.getPort() == location1.getPort())
			return true;
		return false;
	}

	static boolean isPromptRequired(Map suggestedRepositoryLocations) {
		if (suggestedRepositoryLocations == null)
			return false;
		for (Iterator i = suggestedRepositoryLocations.values().iterator(); i
				.hasNext();) {
			List list = (List) i.next();
			if (!list.isEmpty())
				return true;
		}
		return false;
	}

}