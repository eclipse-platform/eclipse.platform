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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.usecase;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class SignaledBuilder extends IncrementalProjectBuilder {
	/** contants */
	public static final String BUILDER_ID = "org.eclipse.core.tests.resources.sigbuilder";

	/**
	 * associate IProject-&gt;SignaledBuilder
	 */
	private static HashMap<IProject, SignaledBuilder> instances = new HashMap<>(10);
	protected boolean wasExecuted;

	public static SignaledBuilder getInstance(IProject project) {
		return instances.get(project);
	}

	public SignaledBuilder() {
		reset();
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		instances.put(getProject(), this);
		try {
			monitor.beginTask("Building.", 1);
			wasExecuted = true;
			getProject().touch(null);
			return null;
		} finally {
			monitor.done();
		}
	}

	public void reset() {
		wasExecuted = false;
	}

	public boolean wasExecuted() {
		return wasExecuted;
	}
}
