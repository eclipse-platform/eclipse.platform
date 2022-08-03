/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.localhistory;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.variants.IResourceVariant;

public class LocalHistoryVariant implements IResourceVariant {

	private final IFileState state;

	public LocalHistoryVariant(IFileState state) {
		this.state = state;
	}

	@Override
	public String getName() {
		return state.getName();
	}

	@Override
	public boolean isContainer() {
		return false;
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) {
		return state;
	}

	@Override
	public String getContentIdentifier() {
		return DateFormat.getDateTimeInstance().format(new Date(state.getModificationTime()));
	}

	@Override
	public byte[] asBytes() {
		return null;
	}

	public IFileState getFileState() {
		return state;
	}
}
