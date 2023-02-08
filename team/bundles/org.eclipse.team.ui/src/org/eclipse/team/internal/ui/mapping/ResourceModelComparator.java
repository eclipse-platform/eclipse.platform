/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Sorter for use by Common Navigator
 */
public class ResourceModelComparator extends ResourceComparator {

	public ResourceModelComparator() {
		super(NAME);
	}

	@Override
	protected int compareNames(IResource r1, IResource r2) {
		if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)
				&& r1 instanceof IFolder
				&& r2 instanceof IFolder) {
			return getComparator().compare(r1.getProjectRelativePath().toString(), r2.getProjectRelativePath().toString());
		}
		return super.compareNames(r1, r2);
	}

	protected String getLayout() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT);
	}
}
