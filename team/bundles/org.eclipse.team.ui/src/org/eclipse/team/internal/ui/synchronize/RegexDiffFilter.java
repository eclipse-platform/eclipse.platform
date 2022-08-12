/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.diff.DiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.core.subscribers.AbstractContentComparator;

public class RegexDiffFilter extends DiffFilter {

	AbstractContentComparator criteria;

	boolean ignoreWhiteSpace;

	/**
	 * Create a filter that does not ignore whitespace.
	 *
	 * @param pattern
	 *            regex pattern
	 */
	public RegexDiffFilter(String pattern) {
		this(false, pattern);
	}

	/**
	 * Create a filter and configure how whitespace is handled.
	 *
	 * @param ignoreWhitespace
	 *            whether whitespace should be ignored
	 * @param pattern
	 *            regex pattern
	 */
	public RegexDiffFilter(boolean ignoreWhitespace, String pattern) {
		criteria = new RegexDiffComparator(Pattern.compile(pattern,
				Pattern.DOTALL), ignoreWhitespace);
	}

	@Override
	public boolean select(IDiff diff, IProgressMonitor monitor) {
		IFileRevision remote = SyncInfoToDiffConverter.getRemote(diff);
		IResource local = ResourceDiffTree.getResourceFor(diff);
		if (local == null || local.getType() != IResource.FILE)
			return true;
		if (remote == null)
			return !local.exists();
		if (!local.exists())
			return false;
		return criteria.compare(local, remote, monitor);
	}
}