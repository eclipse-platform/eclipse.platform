/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.DiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

public class ContentComparisonDiffFilter extends DiffFilter {
	ContentComparator criteria = new ContentComparator(false);

	/**
	 * Create a filter that does not ignore whitespace.
	 */
	public ContentComparisonDiffFilter() {
		this(false);
	}
	/**
	 * Create a filter and configure how whitespace is handled.
	 * @param ignoreWhitespace whether whitespace should be ignored
	 */
	public ContentComparisonDiffFilter(boolean ignoreWhitespace) {
		criteria = new ContentComparator(ignoreWhitespace);
	}

	/**
	 * Compare the contents of the local file and its variant.
	 * This is used by the <code>select</code> method to compare the
	 * contents of two non-null files.
	 * @param local a local file
	 * @param remote a resource variant of the file
	 * @param monitor a progress monitor
	 * @return whether the contents of the two files are equal
	 */
	public boolean compareContents(IFile local, IFileRevision remote, IProgressMonitor monitor) {
		Assert.isNotNull(local);
		Assert.isNotNull(remote);
		return criteria.compare(local, remote, monitor);
	}

	@Override
	public boolean select(IDiff diff, IProgressMonitor monitor) {
		IFileRevision remote = SyncInfoToDiffConverter.getRemote(diff);
		IResource local = ResourceDiffTree.getResourceFor(diff);
		if (local == null) return true;
		if (local.getType() != IResource.FILE) return false;
		if (remote == null) return !local.exists();
		if (!local.exists()) return false;
		return compareContents((IFile)local, remote, monitor);
	}
}