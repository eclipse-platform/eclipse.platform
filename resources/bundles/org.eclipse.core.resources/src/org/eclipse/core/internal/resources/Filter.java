/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Sergey Prigogin (Google) - bug 424972
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.Iterator;
import java.util.LinkedList;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * Class that instantiate IResourceFilter's  that are stored in the project description.
 */
public class Filter {
	/**
	 * A placeholder Filter provider that doesn't match any files or folders.
	 */
	private static class MatchNothingInfoMatcher extends AbstractFileInfoMatcher {
		public MatchNothingInfoMatcher() {
		}

		@Override
		public boolean matches(IContainer parent, IFileInfo fileInfo) {
			return false;
		}

		@Override
		public void initialize(IProject project, Object arguments) {
			// No initialization required.
		}
	}

	FilterDescription description;
	IProject project;
	AbstractFileInfoMatcher provider = null;

	public Filter(IProject project, FilterDescription description) {
		this.description = description;
		this.project = project;
	}

	public boolean match(IContainer parent, IFileInfo fileInfo) throws CoreException {
		if (provider == null) {
			IFilterMatcherDescriptor filterDescriptor = project.getWorkspace().getFilterMatcherDescriptor(getId());
			if (filterDescriptor != null)
				provider = ((FilterDescriptor) filterDescriptor).createFilter();
			if (provider == null) {
				String message = NLS.bind(Messages.filters_missingFilterType, getId());
				Policy.log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, Platform.PLUGIN_ERROR, message, new Error()));
				// Avoid further initialization attempts by instantiating a placeholder filter
				// provider that doesn't match any files or folders.
				provider = new MatchNothingInfoMatcher();
			}
			try {
				provider.initialize(project, description.getFileInfoMatcherDescription().getArguments());
			} catch (CoreException e) {
				Policy.log(e.getStatus());
				provider = null;
			}
		}
		if (provider != null)
			return provider.matches(parent, fileInfo);
		return false;
	}

	public boolean isFirst() {
		IFilterMatcherDescriptor descriptor = project.getWorkspace().getFilterMatcherDescriptor(getId());
		if (descriptor != null)
			return descriptor.isFirstOrdering();
		return false;
	}

	public Object getArguments() {
		return description.getFileInfoMatcherDescription().getArguments();
	}

	public String getId() {
		return description.getFileInfoMatcherDescription().getId();
	}

	public int getType() {
		return description.getType();
	}

	public boolean isIncludeOnly() {
		return (getType() & IResourceFilterDescription.INCLUDE_ONLY) != 0;
	}

	public boolean appliesTo(IFileInfo info) {
		if (info.isDirectory())
			return (getType() & IResourceFilterDescription.FOLDERS) != 0;
		return (getType() & IResourceFilterDescription.FILES) != 0;
	}

	public static IFileInfo[] filter(IProject project, LinkedList<Filter> includeFilters, LinkedList<Filter> excludeFilters, IContainer parent, IFileInfo[] list) throws CoreException {
		IFileInfo[] result = filterIncludes(project, includeFilters, parent, list);
		return filterExcludes(project, excludeFilters, parent, result);
	}

	public static IFileInfo[] filterIncludes(IProject project, LinkedList<Filter> filters, IContainer parent, IFileInfo[] list) throws CoreException {
		if (filters.size() > 0) {
			IFileInfo[] result = new IFileInfo[list.length];
			int outputIndex = 0;

			for (IFileInfo info : list) {
				Iterator<Filter> objIt = filters.iterator();
				boolean filtersWereApplicable = false;
				while (objIt.hasNext()) {
					Filter filter = objIt.next();
					if (filter.appliesTo(info)) {
						filtersWereApplicable = true;
						if (filter.match(parent, info)) {
							result[outputIndex++] = info;
							break;
						}
					}
				}
				if (!filtersWereApplicable)
					result[outputIndex++] = info;
			}
			if (outputIndex != result.length) {
				IFileInfo[] tmp = new IFileInfo[outputIndex];
				System.arraycopy(result, 0, tmp, 0, outputIndex);
				result = tmp;
			}
			return result;
		}
		return list;
	}

	public static IFileInfo[] filterExcludes(IProject project, LinkedList<Filter> filters, IContainer parent, IFileInfo[] list) throws CoreException {
		if (filters.size() > 0) {
			IFileInfo[] result = new IFileInfo[list.length];
			int outputIndex = 0;

			for (IFileInfo info : list) {
				Iterator<Filter> objIt = filters.iterator();
				boolean shouldBeExcluded = false;
				while (objIt.hasNext()) {
					Filter filter = objIt.next();
					if (filter.appliesTo(info)) {
						if (filter.match(parent, info)) {
							shouldBeExcluded = true;
							break;
						}
					}
				}
				if (!shouldBeExcluded)
					result[outputIndex++] = info;
			}
			if (outputIndex != result.length) {
				IFileInfo[] tmp = new IFileInfo[outputIndex];
				System.arraycopy(result, 0, tmp, 0, outputIndex);
				result = tmp;
			}
			return result;
		}
		return list;
	}
}
