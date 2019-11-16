/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.team.tests.core.mapping;

import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.core.mapping.ResourceMappingScope;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.team.ui.synchronize.ModelOperation;

public class ScopeBuildingTests extends TeamTest {

	private static final RuntimeException PROMPT_EXCEPTION = new RuntimeException();
	protected static final String TEST_MODEL_PROVIDER_ID = "id1";

	private class TestResourceMappingOperation extends ModelOperation {

		protected TestResourceMappingOperation(ResourceMapping[] selectedMappings, final ResourceMapping[] additionalMappings) {
			super(null, new SynchronizationScopeManager("", selectedMappings, ResourceMappingContext.LOCAL_CONTEXT, false) {
				@Override
				public void initialize(
						IProgressMonitor monitor) throws CoreException {
					super.initialize(monitor);
					// Add the additional test mappings to the scope
					for (ResourceMapping mapping : additionalMappings) {
						ResourceTraversal[] traversals = mapping.getTraversals(getContext(), monitor);
						((ResourceMappingScope)getScope()).addMapping(mapping, traversals);
						// TODO: The additional mappings and additional resources boolean will not be set
						// TODO: This may bring in mappings from the resources model provider
					}
				}
			});
		}
		@Override
		protected void endOperation(IProgressMonitor monitor) throws InvocationTargetException {
			ISynchronizationScopeManager manager= getScopeManager();
			manager.dispose();
			super.endOperation(monitor);
		}

		@Override
		protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			// Do nothing since we're just testing the scope build
		}
	}

	public static Test suite() {
		return suite(ScopeBuildingTests.class);
	}

	public ScopeBuildingTests() {
		super();
	}

	public ScopeBuildingTests(String name) {
		super(name);
	}

	private void expectPrompt(TestResourceMappingOperation op) {
		try {
			op.run(new NullProgressMonitor());
		} catch (InvocationTargetException e) {
			fail("Unexpected exception: " + e.getTargetException().getMessage());
		} catch (InterruptedException e) {
			fail("Unexpected interupt");
		} catch (RuntimeException e) {
			if (e == PROMPT_EXCEPTION)
				return;
			throw e;
		}
		fail("Expected prompt did not occur");
	}

	private ResourceMapping getMapping(final IProject project, final IResource[] resources, final int depth) {
		return new ResourceMapping() {

			@Override
			public ResourceTraversal[] getTraversals(ResourceMappingContext context,
					IProgressMonitor monitor) throws CoreException {
				return new ResourceTraversal[] { new ResourceTraversal(resources, depth, IResource.NONE)};
			}

			@Override
			public IProject[] getProjects() {
				return new IProject[] { project };
			}

			@Override
			public Object getModelObject() {
				return new Object();
			}

			@Override
			public String getModelProviderId() {
				return TEST_MODEL_PROVIDER_ID;
			}
			@Override
			public boolean contains(ResourceMapping mapping) {
				return false;
			}

		};
	}

	public void testAdditionalResources() throws CoreException {
		IProject project = createProject(new String[]{"file.txt", "folder1/file2.txt", "folder1/folder2/file3.txt", "folder3/"});
		ResourceMapping[] mappings = new ResourceMapping[] {
				getMapping(project, new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_INFINITE)
		};
		ResourceMapping[] additionalMappings = new ResourceMapping[] {
				getMapping(project, new IResource[] { project.getFile("file.txt")}, IResource.DEPTH_INFINITE)
		};
		TestResourceMappingOperation op = new TestResourceMappingOperation(mappings, additionalMappings);
		expectPrompt(op);
	}


}
