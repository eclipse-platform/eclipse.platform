/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IUAElement;

/**
 * This class is used to test topics created using the IIndex API
 */

public class UserIndex implements IIndex {

	private List<IIndexEntry> entries = new ArrayList<>();
	private boolean enabled;

	public UserIndex(boolean enabled) {
		this.enabled = true;
	}

	@Override
	public IUAElement[] getChildren() {
		return getEntries();
	}

	public void addEntry(IIndexEntry child) {
		entries.add(child);
	}

	@Override
	public IIndexEntry[] getEntries() {
		return entries.toArray(new IIndexEntry[0]);
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return enabled;
	}

}
