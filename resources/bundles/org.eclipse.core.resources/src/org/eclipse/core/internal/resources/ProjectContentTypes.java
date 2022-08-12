/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import org.eclipse.core.internal.utils.Cache;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager.ISelectionPolicy;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Manages project-specific content type behavior.
 *
 * @see ContentDescriptionManager
 * @see org.eclipse.core.runtime.content.IContentTypeManager.ISelectionPolicy
 * @since 3.1
 */
public class ProjectContentTypes {

	/**
	 * A project-aware content type selection policy.
	 * This class is also a dynamic scope context that will delegate to either
	 * project or instance scope depending on whether project specific settings were enabled
	 * for the project in question.
	 */
	private class ProjectContentTypeSelectionPolicy implements ISelectionPolicy, IScopeContext {
		// corresponding project
		private Project project;
		// cached project scope
		private IScopeContext projectScope;

		public ProjectContentTypeSelectionPolicy(Project project) {
			this.project = project;
			this.projectScope = new ProjectScope(project);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof IScopeContext))
				return false;
			IScopeContext other = (IScopeContext) obj;
			if (!getName().equals(other.getName()))
				return false;
			IPath location = getLocation();
			return location == null ? other.getLocation() == null : location.equals(other.getLocation());
		}

		private IScopeContext getDelegate() {
			if (!usesContentTypePreferences(project.getName()))
				return InstanceScope.INSTANCE;
			return projectScope;
		}

		@Override
		public IPath getLocation() {
			return getDelegate().getLocation();
		}

		@Override
		public String getName() {
			return getDelegate().getName();
		}

		@Override
		public IEclipsePreferences getNode(String qualifier) {
			return getDelegate().getNode(qualifier);
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public IContentType[] select(IContentType[] candidates, boolean fileName, boolean content) {
			return ProjectContentTypes.this.select(project, candidates, fileName, content);
		}
	}

	private static final String CONTENT_TYPE_PREF_NODE = "content-types"; //$NON-NLS-1$

	private static final String PREF_LOCAL_CONTENT_TYPE_SETTINGS = "enabled"; //$NON-NLS-1$
	private static final Preferences PROJECT_SCOPE = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE);
	private Cache contentTypesPerProject;
	private Workspace workspace;

	static boolean usesContentTypePreferences(String projectName) {
		try {
			// be careful looking up for our node so not to create any nodes as side effect
			Preferences node = PROJECT_SCOPE;
			//TODO once bug 90500 is fixed, should be simpler
			// for now, take the long way
			if (!node.nodeExists(projectName))
				return false;
			node = node.node(projectName);
			if (!node.nodeExists(Platform.PI_RUNTIME))
				return false;
			node = node.node(Platform.PI_RUNTIME);
			if (!node.nodeExists(CONTENT_TYPE_PREF_NODE))
				return false;
			node = node.node(CONTENT_TYPE_PREF_NODE);
			return node.getBoolean(PREF_LOCAL_CONTENT_TYPE_SETTINGS, false);
		} catch (BackingStoreException | IllegalStateException | IllegalArgumentException e) {
			// exception treated when retrieving the project preferences
		}
		return false;
	}

	public ProjectContentTypes(Workspace workspace) {
		this.workspace = workspace;
		// keep cache small
		this.contentTypesPerProject = new Cache(5, 30, 0.4);
	}

	/**
	 * Collect content types associated to the natures configured for the given project.
	 */
	private Set<String> collectAssociatedContentTypes(Project project) {
		String[] enabledNatures = workspace.getNatureManager().getEnabledNatures(project);
		if (enabledNatures.length == 0)
			return Collections.EMPTY_SET;
		Set<String> related = new HashSet<>(enabledNatures.length);
		for (String enabledNature : enabledNatures) {
			ProjectNatureDescriptor descriptor = (ProjectNatureDescriptor) workspace.getNatureDescriptor(enabledNature);
			if (descriptor == null)
				// no descriptor found for the nature, skip it
				continue;
			String[] natureContentTypes = descriptor.getContentTypeIds();
			related.addAll(Arrays.asList(natureContentTypes)); // collect associate content types
		}
		return related;
	}

	public void contentTypePreferencesChanged(IProject project) {
		final ProjectInfo info = (ProjectInfo) ((Project) project).getResourceInfo(false, false);
		if (info != null)
			info.setMatcher(null);
	}

	/**
	 * Creates a content type matcher for the given project. Takes natures and user settings into account.
	 */
	private IContentTypeMatcher createMatcher(Project project) {
		ProjectContentTypeSelectionPolicy projectContentTypeSelectionPolicy = new ProjectContentTypeSelectionPolicy(project);
		return Platform.getContentTypeManager().getMatcher(projectContentTypeSelectionPolicy, projectContentTypeSelectionPolicy);
	}

	@SuppressWarnings({"unchecked"})
	private Set<String> getAssociatedContentTypes(Project project) {
		final ResourceInfo info = project.getResourceInfo(false, false);
		if (info == null)
			// the project has been deleted
			return null;
		final String projectName = project.getName();
		synchronized (contentTypesPerProject) {
			Cache.Entry entry = contentTypesPerProject.getEntry(projectName);
			if (entry != null)
				// we have an entry...
				if (entry.getTimestamp() == info.getContentId())
					// ...and it is not stale, so just return it
					return (Set<String>) entry.getCached();
			// no cached information found, have to collect associated content types
			Set<String> result = collectAssociatedContentTypes(project);
			if (entry == null)
				// there was no entry before - create one
				entry = contentTypesPerProject.addEntry(projectName, result, info.getContentId());
			else {
				// just update the existing entry
				entry.setTimestamp(info.getContentId());
				entry.setCached(result);
			}
			return result;
		}
	}

	public IContentTypeMatcher getMatcherFor(Project project) throws CoreException {
		ProjectInfo info = (ProjectInfo) project.getResourceInfo(false, false);
		//fail if project has been deleted concurrently
		if (info == null)
			project.checkAccessible(project.getFlags(null));
		IContentTypeMatcher matcher = info.getMatcher();
		if (matcher != null)
			return matcher;
		matcher = createMatcher(project);
		info.setMatcher(matcher);
		return matcher;
	}

	/**
	 * Implements project specific, nature-based selection policy. No content types are vetoed.
	 *
	 * The criteria for this policy is as follows:
	 * <ol>
	 * <li>associated content types should appear before non-associated content types</li>
	 * <li>otherwise, relative ordering should be preserved.</li>
	 * </ol>
	 *
	 *  @see org.eclipse.core.runtime.content.IContentTypeManager.ISelectionPolicy
	 */
	final IContentType[] select(Project project, IContentType[] candidates, boolean fileName, boolean content) {
		// since no vetoing is done here, don't go further if there is nothing to sort
		if (candidates.length < 2)
			return candidates;
		final Set<String> associated = getAssociatedContentTypes(project);
		if (associated == null || associated.isEmpty())
			// project has no content types associated
			return candidates;
		int associatedCount = 0;
		for (int i = 0; i < candidates.length; i++)
			// is it an associated content type?
			if (associated.contains(candidates[i].getId())) {
				// need to move it to the right spot (unless all types visited so far are associated as well)
				if (associatedCount < i) {
					final IContentType promoted = candidates[i];
					// move all non-associated content types before it one one position up...
					for (int j = i; j > associatedCount; j--)
						candidates[j] = candidates[j - 1];
					// ...so there is an empty spot for the content type we are promoting
					candidates[associatedCount] = promoted;
				}
				associatedCount++;
			}
		return candidates;
	}
}