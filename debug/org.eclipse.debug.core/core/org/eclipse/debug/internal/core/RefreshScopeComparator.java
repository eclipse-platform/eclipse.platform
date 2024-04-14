/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.RefreshUtil;

/**
 * Compares refresh scope attributes as the format has changed from a working
 * set memento to an XML memento of resource paths. Avoids migrating attribute
 * to new format until something else in the configuration changes.
 *
 * @since 3.6
 */
public class RefreshScopeComparator implements Comparator<String> {
	private static IResource[] toResources(String memento) {
		try {
			return RefreshUtil.toResources(memento);
		} catch (CoreException e) {
			return null;
		}
	}

	private static final Comparator<IResource> RESOURCE = Comparator.nullsFirst(Comparator.comparing(IResource::toString));
	private static final Comparator<IResource[]> ARRAY = Comparator.nullsFirst((IResource[] s1, IResource[] s2) -> Arrays.compare(s1, s2, RESOURCE));
	private static final Comparator<String> MEMENTO = Comparator.nullsFirst(Comparator.comparing(RefreshScopeComparator::toResources, ARRAY));

	@Override
	public int compare(String o1, String o2) {
		return MEMENTO.compare(o1, o2);
	}

}
