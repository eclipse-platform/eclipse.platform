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
 *******************************************************************************/
package org.eclipse.help.internal.search.federated;

import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.search.ISearchScope;

public class LocalHelpScope implements ISearchScope {
	private WorkingSet workingSet;
	private boolean capabilityFiltered;
	public LocalHelpScope(WorkingSet workingSet, boolean capabilityFiltered) {
		this.workingSet = workingSet;
		this.capabilityFiltered = capabilityFiltered;
	}

	public WorkingSet getWorkingSet() {
		return workingSet;
	}

	public boolean getCapabilityFiltered() {
		return capabilityFiltered;
	}
}
