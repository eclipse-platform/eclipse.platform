/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Helper builder for cycle related tests.
 */
public class CycleBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.cyclebuilder";

	protected IProject[] beforeProjects = new IProject[0];
	protected IProject[] afterProjects = new IProject[0];
	protected int rebuildsToRequest = 0;
	private static CycleBuilder singleton;
	protected int buildCount = 0;

	/**
	 * Returns the most recently created instance.
	 */
	public static CycleBuilder getInstance() {
		return singleton;
	}

	public CycleBuilder() {
		singleton = this;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (beforeProjects != null) {
			for (IProject beforeProject : beforeProjects) {
				assertTrue(hasBeenBuilt(beforeProject), "Missing before project: " + beforeProject);
			}
		}
		if (afterProjects != null) {
			for (IProject afterProject : afterProjects) {
				assertFalse(hasBeenBuilt(afterProject), "Missing after project: " + afterProject);
			}
		}
		if (rebuildsToRequest > buildCount) {
			changeAllFiles();
			needRebuild();
		}
		//ensure that subsequent builds are always incremental
		if (buildCount > 0) {
			assertTrue(kind == IncrementalProjectBuilder.INCREMENTAL_BUILD, "Should be incremental build");
		}
		buildCount++;
		return null;
	}

	/**
	 * Method getRandomContents.
	 * @return InputStream
	 */
	protected InputStream getRandomContents() {
		return new ByteArrayInputStream("foo".getBytes());
	}

	/**
	 * Method changeSomething.
	 */
	private void changeAllFiles() throws CoreException {
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.FILE) {
				if (resource.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
					return false;
				}
				((IFile) resource).setContents(getRandomContents(), IResource.NONE, null);
			}
			return true;
		};
		getProject().accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
	}

	public int getBuildCount() {
		return buildCount;
	}

	public void resetBuildCount() {
		buildCount = 0;
	}

	public void setAfterProjects(IProject[] afterProjects) {
		this.afterProjects = afterProjects;
	}

	public void setBeforeProjects(IProject[] beforeProjects) {
		this.beforeProjects = beforeProjects;
	}

	public void setRebuildsToRequest(int rebuildsToRequest) {
		this.rebuildsToRequest = rebuildsToRequest;
	}
}
