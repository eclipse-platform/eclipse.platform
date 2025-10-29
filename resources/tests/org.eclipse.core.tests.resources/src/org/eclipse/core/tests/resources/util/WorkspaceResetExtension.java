/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.resources.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.FreezeMonitor;
import org.eclipse.core.tests.resources.TestUtil;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Restores a clean workspace with a default description and an empty resource
 * tree after test execution.
 */
public class WorkspaceResetExtension implements AfterEachCallback, BeforeEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		// Wait for any pending refresh operation, in particular from startup
		waitForRefresh();
		TestUtil.log(IStatus.INFO, getTestName(context), "setUp");
		assertNotNull(getWorkspace(), "Workspace was not set up");
		FreezeMonitor.expectCompletionInAMinute();
		waitForRefresh();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		TestUtil.log(IStatus.INFO, getTestName(context), "tearDown");
		try {
			restoreCleanWorkspace();
		} finally {
			FreezeMonitor.done();
			assertWorkspaceFolderEmpty();
		}
	}

	private String getTestName(ExtensionContext context) {
		String className = context.getRequiredTestClass().getSimpleName();
		String methodName = context.getRequiredTestMethod().getName();
		String displayName = context.getDisplayName();
		if (!displayName.contains(methodName)) {
			methodName += displayName;
		}
		return className + "." + methodName;
	}

	private void restoreCleanWorkspace() {
		List<CoreException> exceptions = new ArrayList<>();
		try {
			restoreWorkspaceDescription();
		} catch (CoreException e) {
			exceptions.add(e);
		}
		// Wait for any build job that may still be executed
		waitForBuild();
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				getWorkspace().getRoot().delete(true, true, createTestMonitor());
			}, null);
		} catch (CoreException e) {
			exceptions.add(e);
		}
		try {
			getWorkspace().save(true, null);
		} catch (CoreException e) {
			exceptions.add(e);
		}
		// don't leak builder jobs, since they may affect subsequent tests
		waitForBuild();
		if (!exceptions.isEmpty()) {
			IllegalStateException composedException = new IllegalStateException("Failures when cleaning up workspace");
			exceptions.forEach(exception -> composedException.addSuppressed(exception));
			throw composedException;
		}
	}

	private void restoreWorkspaceDescription() throws CoreException {
		getWorkspace().setDescription(Workspace.defaultWorkspaceDescription());
	}

	private void assertWorkspaceFolderEmpty() {
		final String metadataDirectoryName = ".metadata";
		File workspaceLocation = getWorkspace().getRoot().getLocation().toFile();
		File[] remainingFilesInWorkspace = workspaceLocation
				.listFiles(file -> !file.getName().equals(metadataDirectoryName));
		assertThat(remainingFilesInWorkspace).as("check workspace folder is empty").isEmpty();
	}

}
