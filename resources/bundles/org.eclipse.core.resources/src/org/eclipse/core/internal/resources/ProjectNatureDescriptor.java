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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.ArrayList;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 */
public class ProjectNatureDescriptor implements IProjectNatureDescriptor {
	protected String id;
	protected String label;
	protected String[] requiredNatures;
	protected String[] natureSets;
	protected String[] builderIds;
	protected String[] contentTypeIds;
	protected boolean allowLinking = true;

	//descriptors that are in a dependency cycle are never valid
	protected boolean hasCycle = false;
	//colours used by cycle detection algorithm
	protected byte colour = 0;

	/**
	 * Creates a new descriptor based on the given extension markup.
	 * @exception CoreException if the given nature extension is not correctly formed.
	 */
	protected ProjectNatureDescriptor(IExtension natureExtension) throws CoreException {
		readExtension(natureExtension);
	}

	protected void fail() throws CoreException {
		fail(NLS.bind(Messages.natures_invalidDefinition, id));
	}

	protected void fail(String reason) throws CoreException {
		throw new ResourceException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, reason, null));
	}

	/**
	 * Returns the IDs of the incremental builders that this nature claims to
	 * own.  These builders do not necessarily exist in the registry.
	 */
	public String[] getBuilderIds() {
		return builderIds;
	}

	/**
	 * Returns the IDs of the content types this nature declares to
	 * have affinity with.  These content types do not necessarily exist in the registry.
	 */
	public String[] getContentTypeIds() {
		return contentTypeIds;
	}

	/**
	 * @see IProjectNatureDescriptor#getNatureId()
	 */
	@Override
	public String getNatureId() {
		return id;
	}

	/**
	 * @see IProjectNatureDescriptor#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * @see IProjectNatureDescriptor#getRequiredNatureIds()
	 */
	@Override
	public String[] getRequiredNatureIds() {
		return requiredNatures;
	}

	/**
	 * @see IProjectNatureDescriptor#getNatureSetIds()
	 */
	@Override
	public String[] getNatureSetIds() {
		return natureSets;
	}

	/**
	 * @see IProjectNatureDescriptor#isLinkingAllowed()
	 */
	@Override
	public boolean isLinkingAllowed() {
		return allowLinking;
	}

	/**
	 * Initialize this nature descriptor based on the provided extension point.
	 */
	protected void readExtension(IExtension natureExtension) throws CoreException {
		//read the extension
		id = natureExtension.getUniqueIdentifier();
		if (id == null) {
			fail(Messages.natures_missingIdentifier);
		}
		label = natureExtension.getLabel();
		IConfigurationElement[] elements = natureExtension.getConfigurationElements();
		int count = elements.length;
		ArrayList<String> requiredList = new ArrayList<>(count);
		ArrayList<String> setList = new ArrayList<>(count);
		ArrayList<String> builderList = new ArrayList<>(count);
		ArrayList<String> contentTypeList = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			IConfigurationElement element = elements[i];
			String name = element.getName();
			if (name.equalsIgnoreCase("requires-nature")) { //$NON-NLS-1$
				String attribute = element.getAttribute("id"); //$NON-NLS-1$
				if (attribute == null)
					fail();
				requiredList.add(attribute);
			} else if (name.equalsIgnoreCase("one-of-nature")) { //$NON-NLS-1$
				String attribute = element.getAttribute("id"); //$NON-NLS-1$
				if (attribute == null)
					fail();
				setList.add(attribute);
			} else if (name.equalsIgnoreCase("builder")) { //$NON-NLS-1$
				String attribute = element.getAttribute("id"); //$NON-NLS-1$
				if (attribute == null)
					fail();
				builderList.add(attribute);
			} else if (name.equalsIgnoreCase("content-type")) { //$NON-NLS-1$
				String attribute = element.getAttribute("id"); //$NON-NLS-1$
				if (attribute == null)
					fail();
				contentTypeList.add(attribute);
			} else if (name.equalsIgnoreCase("options")) { //$NON-NLS-1$
				String attribute = element.getAttribute("allowLinking"); //$NON-NLS-1$
				//when in doubt (missing attribute, wrong value) default to allow linking
				allowLinking = !Boolean.FALSE.toString().equalsIgnoreCase(attribute);
			}
		}
		requiredNatures = requiredList.toArray(new String[requiredList.size()]);
		natureSets = setList.toArray(new String[setList.size()]);
		builderIds = builderList.toArray(new String[builderList.size()]);
		contentTypeIds = contentTypeList.toArray(new String[contentTypeList.size()]);
	}

	/**
	 * Prints out a string representation for debugging purposes only.
	 */
	@Override
	public String toString() {
		return "ProjectNatureDescriptor(" + id + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
