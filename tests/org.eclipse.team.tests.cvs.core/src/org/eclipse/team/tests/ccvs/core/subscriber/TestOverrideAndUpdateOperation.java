/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.subscriber.OverrideAndUpdateSubscriberOperation;

public class TestOverrideAndUpdateOperation extends OverrideAndUpdateSubscriberOperation {	

	private boolean prompted = false; 
	
	protected TestOverrideAndUpdateOperation(IDiffElement[] elements) {
		super(null, elements);
	}
	
	@Override
	protected boolean canRunAsJob() {
		return false;
	}
	
	@Override
	protected boolean promptForOverwrite(SyncInfoSet syncSet) {
		TestOverrideAndUpdateOperation.this.prompted = true;
		return true;
	}
	
	public boolean isPrompted() {
		return this.prompted;
	}
}
