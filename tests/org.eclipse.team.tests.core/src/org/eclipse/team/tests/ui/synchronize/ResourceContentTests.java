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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.mapping.ResourceModelContentProvider;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ResourceContentTests extends TeamTest {

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

	public static Test suite() {
		return suite(ResourceContentTests.class);
	}

	private ResourceModelContentProvider provider;

	public ResourceContentTests() {
		super();
	}

	public ResourceContentTests(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		initializeProvider(null, null, null);
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		cleanupProvider();
		super.tearDown();
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
		if (!resourceSet.isEmpty()) {
			fail("Tree entries were missing for " + toString(resourceSet));
		}
		if (!paths.isEmpty()) {
			fail("Tree entries were found for " + toString(paths));
		}
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
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	public void testFileContent() throws CoreException {
		String[] files = new String[] {"file.txt", "file2.txt", "folder1/file3.txt", "folder1/folder2/file4.txt"};
		IProject project = createProject(files);
		files = new String[] {".project", "file.txt", "file2.txt", "folder1/file3.txt", "folder1/folder2/file4.txt"};
		assertContentMatches(project, files);
	}

	public void testFileChange() throws CoreException {
//		String[] files = new String[] {"file.txt", "file2.txt", "folder1/file3.txt", "folder1/folder2/file4.txt"};
//		IProject project = createProject(files);

	}



}
