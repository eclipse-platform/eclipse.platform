/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.resources;

import java.io.ByteArrayInputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * A benchmark for bug 552185.
 *
 * Not included in actual tests, since performance is only printed and not
 * asserted.
 */
public class Bug552185PerformanceTest {

	public void testBug552185Performance() throws Exception {
		// run inside a WorkspaceJob, in case there are listeners on workspace changes
		WorkspaceJob testJob = new WorkspaceJob(Bug552185PerformanceTest.class.getName()) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				try {
					runBenchmark(monitor);
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Benchmark failed.", e);
				}

				return Status.OK_STATUS;
			}

		};
		testJob.schedule();
		testJob.join();
	}

	static void runBenchmark(IProgressMonitor monitor) throws CoreException {
		runBenchmark(1, 10_000, monitor); // 1 directory with 10k files
		runBenchmark(10_000, 1, monitor); // 10k directories with 1 file each
		runBenchmark(10, 1_000, monitor); // 10 directories with 1k files each
		runBenchmark(1, 100_000, monitor); // 1 directory with 100k files
		runBenchmark(100_000, 1, monitor); // 100k directories with 1 file each
	}

	static void runBenchmark(int directoriesCount, int fileCountPerDirectory, IProgressMonitor monitor)
			throws CoreException {
		Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		boolean local = false;

		IProject project = root.getProject("TestBug552185ResourcesIO");

		try {
			project.create(monitor);
			project.open(monitor);
			System.out.println(
					"Creating " + directoriesCount + " directories with " + fileCountPerDirectory + " files each");
			long start = System.currentTimeMillis();

			SubMonitor subMonitor = SubMonitor.convert(monitor, directoriesCount);
			for (int i = 0; i < directoriesCount; ++i) {
				IFolder folder = project.getFolder("folder" + i);
				SubMonitor subMonitor2 = SubMonitor.convert(subMonitor, "Creating directory " + folder.getName(),
						fileCountPerDirectory);
				folder.create(IResource.FORCE, local, subMonitor);
				for (int j = 0; j < fileCountPerDirectory; ++j) {
					subMonitor2.checkCanceled();
					IFile file = folder.getFile("file" + j);
					String content = "file content " + j;
					ByteArrayInputStream contentStream = new ByteArrayInputStream(content.getBytes());
					file.create(contentStream, IResource.FORCE, subMonitor2);
					subMonitor2.worked(1);
				}
				subMonitor.worked(1);
			}
			subMonitor.done();

			long end = System.currentTimeMillis();
			long elapsed = end - start;
			System.out.println("Elapsed: " + elapsed + " ms");
		} finally {
			if (project.exists()) {
				project.delete(IResource.FORCE, monitor);
			}
		}
	}
}
