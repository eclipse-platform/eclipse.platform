/*******************************************************************************
 * Copyright (c) 2007, 2018 Matthew Conway and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matthew Conway - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.variables;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

public class BuildFilesResolver implements IDynamicVariableResolver
{
	private static final char ARG_REMOVED = 'r';
	private static final char ARG_CHANGED = 'c';
	private static final char ARG_ADDED = 'a';
	private static final char ARG_DIRS = 'd';
	private static final char ARG_FILES = 'f';

	// Use a space as a separator as this is a more natural fit for sending a
	// list of files to a unix command
	private static final String FILE_LIST_SEPARATOR = " "; //$NON-NLS-1$

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException
	{
		String result = null;

		IResourceDelta buildDelta = ExternalToolBuilder.getBuildDelta();
		if (buildDelta != null)
		{
			final StringBuilder fileList = new StringBuilder();
			final Set<String> changedResources = new LinkedHashSet<>();

			// Use the argument to determine which deltas to visit - if none,
			// then defaults to all
			int deltas = 0;
			boolean dirs = false, files = false;
			if (argument != null)
			{
				// Check delta kinds
				if (argument.indexOf(ARG_ADDED) > -1)
				{
					deltas |= IResourceDelta.ADDED;
				}
				if (argument.indexOf(ARG_CHANGED) > -1)
				{
					deltas |= IResourceDelta.CHANGED;
				}
				if (argument.indexOf(ARG_REMOVED) > -1)
				{
					deltas |= IResourceDelta.REMOVED;
				}

				// Check wether to include files and/or directories
				if (argument.indexOf(ARG_DIRS) > -1)
				{
					dirs = true;
				}
				if (argument.indexOf(ARG_FILES) > -1)
				{
					files = true;
				}

			}
			if (deltas == 0)
			{
				deltas = IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED;
			}
			if (!dirs && !files)
			{
				dirs = true;
				files = true;
			}
			final int trackDeltas = deltas;
			final boolean trackDirs = dirs;
			final boolean trackFiles = files;


			buildDelta.accept((IResourceDeltaVisitor) delta -> {
				IResource resource = delta.getResource();

				// Only track files with the right kind of delta
				boolean isTracked = (delta.getKind() & trackDeltas) > 0;
				if (isTracked)
				{
					// Only track dirs if desired
					isTracked = trackDirs && resource.getType() != IResource.FILE;
					// Only track files if desired
					isTracked |= trackFiles && resource.getType() == IResource.FILE;
				}

				//  If tracking a change, then add it to the change set for inclusion in the variable's output
				if (isTracked)
				{
					String osPath = resource.getLocation().toOSString();
					if (changedResources.add(osPath))
					{
						if (fileList.length() > 0)
						{
							fileList.append(FILE_LIST_SEPARATOR);
						}

						// Since space is our separator, we need to add quotes
						// around each file to handle filenames with embedded
						// spaces. We also need to escape out embedded quotes in
						// the filename so they don't conflict with these
						// special quotes.
						//
						osPath = osPath.replaceAll("\"", "\\\\\""); //$NON-NLS-1$ //$NON-NLS-2$
						fileList.append("\"" + osPath + "\""); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				return true;
			}, deltas);
			result = fileList.toString();
		}

		return result;
	}
}