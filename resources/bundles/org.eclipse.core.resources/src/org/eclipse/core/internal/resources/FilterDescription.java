/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     IBM Corporation - ongoing implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.LinkedList;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Class for describing the characteristics of filters that are stored
 * in the project description.
 */
public class FilterDescription implements IResourceFilterDescription, Comparable<FilterDescription> {

	private long id;

	/**
	 * The resource type (IResourceFilter.INCLUDE_ONLY or IResourceFilter.EXCLUDE_ALL) and/or IResourceFilter.INHERITABLE
	 */
	private int type;

	private FileInfoMatcherDescription matcherDescription;

	/**
	 * The resource that this filter is applied to
	 */
	private IResource resource;

	public FilterDescription() {
		this.type = -1;
	}

	public FilterDescription(IResource resource, int type, FileInfoMatcherDescription matcherDescription) {
		super();
		Assert.isNotNull(resource);
		this.type = type;
		this.matcherDescription = matcherDescription;
		this.resource = resource;
	}

	public boolean isInheritable() {
		return (getType() & IResourceFilterDescription.INHERITABLE) != 0;
	}

	public static LinkedList<FilterDescription> copy(LinkedList<FilterDescription> originalDescriptions, IResource resource) {
		LinkedList<FilterDescription> copy = new LinkedList<>();
		for (FilterDescription desc : originalDescriptions) {
			FilterDescription newDesc = new FilterDescription(resource, desc.getType(), desc.getFileInfoMatcherDescription());
			copy.add(newDesc);
		}
		return copy;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

	@Override
	public IResource getResource() {
		return resource;
	}

	@Override
	public FileInfoMatcherDescription getFileInfoMatcherDescription() {
		return matcherDescription;
	}

	public void setFileInfoMatcherDescription(FileInfoMatcherDescription matcherDescription) {
		this.matcherDescription = matcherDescription;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilterDescription other = (FilterDescription) obj;
		if (id != other.id)
			return false;
		return true;
	}

	/**
	 * Compare filter descriptions in a way that sorts them topologically by path.
	 */
	@Override
	public int compareTo(FilterDescription that) {
		IPath path1 = this.getResource().getProjectRelativePath();
		IPath path2 = that.getResource().getProjectRelativePath();
		int count1 = path1.segmentCount();
		int compare = count1 - path2.segmentCount();
		if (compare != 0)
			return compare;
		for (int i = 0; i < count1; i++) {
			compare = path1.segment(i).compareTo(path2.segment(i));
			if (compare != 0)
				return compare;
		}
		return 0;
	}

	@Override
	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		((Container) getResource()).removeFilter(this, updateFlags, monitor);
	}
}
