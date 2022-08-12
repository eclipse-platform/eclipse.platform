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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import java.io.OutputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.mapping.IStorageMerger;

public class TestStorageMerger implements IStorageMerger {

	public static final String MERGE_FAILURE = "merge failed";

	@Override
	public boolean canMergeWithoutAncestor() {
		return false;
	}

	@Override
	public IStatus merge(OutputStream output, String outputEncoding,
			IStorage ancestor, IStorage target, IStorage other,
			IProgressMonitor monitor) throws CoreException {
		return new Status(IStatus.ERROR, "org.eclipse.team.tests.core", CONFLICT, MERGE_FAILURE, null);
	}

}
