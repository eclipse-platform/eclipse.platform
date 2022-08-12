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
package org.eclipse.core.tests.resources.regression;

import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A builder that does not do anything.
 */
public class SimpleBuilder extends IncrementalProjectBuilder {
	/** contants */
	public static final String BUILDER_ID = "org.eclipse.core.tests.resources.simplebuilder";

	protected int triggerForLastBuild;
	protected static SimpleBuilder instance;

	public SimpleBuilder() {
		super();
		instance = this;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) {
		triggerForLastBuild = kind;
		return null;
	}

	public static SimpleBuilder getInstance() {
		return instance;
	}

	@Override
	protected void startupOnInitialize() {
	}

	public boolean wasAutoBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.AUTO_BUILD;
	}

	public boolean wasFullBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.FULL_BUILD;
	}

	public boolean wasIncrementalBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.INCREMENTAL_BUILD;
	}
}
