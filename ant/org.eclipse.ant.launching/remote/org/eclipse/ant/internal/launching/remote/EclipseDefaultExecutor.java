/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.remote;

import java.util.Arrays;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Executor;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.DefaultExecutor;

public class EclipseDefaultExecutor extends DefaultExecutor {

	private static final EclipseSingleCheckExecutor SUB_EXECUTOR = new EclipseSingleCheckExecutor();

	@SuppressWarnings("unused")
	@Override
	public void executeTargets(Project project, String[] targetNames) throws BuildException {
		Vector<String> v = new Vector<>();
		v.addAll(Arrays.asList(targetNames));
		project.addReference(IAntCoreConstants.TARGET_VECTOR_NAME, v);
		super.executeTargets(project, targetNames);
	}

	@Override
	public Executor getSubProjectExecutor() {
		return SUB_EXECUTOR;
	}
}
