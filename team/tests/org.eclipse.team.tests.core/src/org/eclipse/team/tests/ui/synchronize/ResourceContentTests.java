/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ui.synchronize;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.mapping.ResourceModelContentProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceContentTests {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	public static class TestableResourceModelContentProvider extends ResourceModelContentProvider {

		private final ISynchronizationScope scope;
		private final ISynchronizationContext context;
		private final ISynchronizePageConfiguration configuration;

		public TestableResourceModelContentProvider(ISynchronizationScope scope, ISynchronizationContext context, ISynchronizePageConfiguration configuration) {
			this.scope = scope;
			this.context = context;
			this.configuration = configuration;
		}

		@Override
		public ISynchronizePageConfiguration getConfiguration() {
			return configuration;
		}

		@Override
		public ISynchronizationContext getContext() {
			return context;
		}

		@Override
		public ISynchronizationScope getScope() {
			return scope;
		}
	}

	private ResourceModelContentProvider provider;

	@Before
	public void setUp() throws Exception {
		initializeProvider(null, null, null);
	}

	@After
	public void tearDown() throws Exception {
		cleanupProvider();
	}

	private void initializeProvider(ISynchronizationScope scope, ISynchronizationContext context, ISynchronizePageConfiguration configuration) {
		cleanupProvider();
		provider = new TestableResourceModelContentProvider(scope, context, configuration);
	}

	private void cleanupProvider() {
		if (provider != null)
			provider.dispose();
		provider = null;
	}

	private void assertContentMatches(IProject project, String[] leaves) {
		assertContentsMatch(asResources(project, leaves));
	}

	private void assertContentsMatch(IResource[] resources) {
		Set paths = getPaths(ResourcesPlugin.getWorkspace().getRoot());
		Set<Object> resourceSet = new HashSet<>();
		Collections.addAll(resourceSet, resources);
		for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
			TreePath path = (TreePath) iterator.next();
			Object o = path.getLastSegment();
			// Just remove the object, we'll check for any remaining resources below
			if (resourceSet.remove(o)) {
				iterator.remove();
			}
		}
		assertFalse("Tree entries were missing for " + toString(resourceSet), resourceSet.isEmpty());
		assertFalse("Tree entries were found for " + toString(paths), paths.isEmpty());
	}

	private Set getPaths(Object root) {
		Set<Object> result = new HashSet<>();
		Object[] elements = provider.getElements(root);
		for (Object object : elements) {
			TreePath path = new TreePath(new Object[] { object });
			Set childPaths = getPaths(provider, path);
			result.addAll(childPaths);
		}
		return result;
	}

	private Set getPaths(ResourceModelContentProvider provider, TreePath path) {
		Object[] children = provider.getChildren(path);
		Set<TreePath> result = new HashSet<>();
		if (children.length == 0)
			result.add(path);
		for (Object object : children) {
			TreePath childPath = path.createChildPath(object);
			Set childPaths = getPaths(provider, childPath);
			result.addAll(childPaths);
		}
		return result;
	}

	private String toString(Set set) {
		StringBuilder buffer = new StringBuilder();
		boolean addComma = false;
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			Object resource = iterator.next();
			buffer.append(toString(resource));
			if (addComma)
				buffer.append(", ");
			addComma = true;
		}
		return buffer.toString();
	}

	private String toString(Object object) {
		if (object instanceof IResource) {
			return ((IResource)object).getFullPath().toString();
		}
		if (object instanceof TreePath) {
			return toString(((TreePath)object).getLastSegment());
		}
		return object.toString();
	}

	private IResource[] asResources(IProject project, String[] resourcePaths) {
		List<IResource> resources = new ArrayList<>();
		for (String path : resourcePaths) {
			if (path.endsWith("/")) {
				resources.add(project.getFolder(path));
			} else {
				resources.add(project.getFile(path));
			}
		}
		return resources.toArray(new IResource[resources.size()]);
	}

	@Test
	public void testFileContent() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		String[] files = new String[] {"file.txt", "file2.txt", "folder1/file3.txt", "folder1/folder2/file4.txt"};
		buildResources(project, files);
		assertContentMatches(project, files);
	}

}
