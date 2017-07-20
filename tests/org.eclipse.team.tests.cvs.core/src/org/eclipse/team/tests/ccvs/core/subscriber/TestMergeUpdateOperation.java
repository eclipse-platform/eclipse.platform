/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.subscriber;


import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.subscriber.MergeUpdateOperation;
import org.junit.Assert;


class TestMergeUpdateOperation extends MergeUpdateOperation {
	boolean allowOverwrite = false;

	public TestMergeUpdateOperation(IDiffElement[] elements, boolean allowOverwrite) {
		super(null, elements, false /* prompt before update */);
		this.allowOverwrite = allowOverwrite;
	}

	protected boolean promptForOverwrite(SyncInfoSet syncSet) {
		if (allowOverwrite) return true;
		if (syncSet.isEmpty()) return true;
		IResource[] resources = syncSet.getResources();
		Assert.fail(resources[0].getFullPath().toString() + " failed to merge properly");
		return false;
	}
	
	protected boolean canRunAsJob() {
		return false;
	}
}
