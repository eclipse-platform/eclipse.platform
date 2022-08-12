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

import java.util.HashMap;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.content.IContentTypeMatcher;

public class ProjectInfo extends ResourceInfo {

	/** The description of this object */
	protected ProjectDescription description;

	/** The list of natures for this project */
	protected HashMap<String, IProjectNature> natures;

	/** The property store for this resource (used only by the compatibility fragment) */
	protected Object propertyStore;

	/** The content type matcher for this project. */
	protected IContentTypeMatcher matcher;

	/**
	 * Discards stale natures on this project after project description
	 * has changed.
	 */
	public synchronized void discardNatures() {
		natures = null;
	}

	/**
	 * Default constructor (for easier debugging)
	 */
	public ProjectInfo() {
		super();
	}

	/**
	 * Discards any stale state on this project after it has been moved.  Builder
	 * instances must be cleared because they reference the old project handle.
	 */
	public synchronized void fixupAfterMove() {
		natures = null;
		// note that the property store instance will be recreated lazily
		propertyStore = null;
		if (description != null) {
			ICommand[] buildSpec = description.getBuildSpec(false);
			for (ICommand element : buildSpec)
				((BuildCommand) element).setBuilders(null);
		}
	}

	/**
	 * Returns the description associated with this info.  The return value may be null.
	 */
	public ProjectDescription getDescription() {
		return description;
	}

	/**
	 * Returns the content type matcher associated with this info.  The return value may be null.
	 */
	public IContentTypeMatcher getMatcher() {
		return matcher;
	}

	public IProjectNature getNature(String natureId) {
		// thread safety: (Concurrency001)
		HashMap<String, IProjectNature> temp = natures;
		if (temp == null)
			return null;
		return temp.get(natureId);
	}

	/**
	 * Returns the property store associated with this info.  The return value may be null.
	 */
	@Override
	public Object getPropertyStore() {
		return propertyStore;
	}

	/**
	 * Sets the description associated with this info.  The value may be null.
	 */
	public void setDescription(ProjectDescription value) {
		if (description != null) {
			//if we already have a description, assign the new
			//build spec on top of the old one to ensure we maintain
			//any existing builder instances in the old build commands
			ICommand[] oldSpec = description.buildSpec;
			ICommand[] newSpec = value.buildSpec;
			value.buildSpec = oldSpec;
			value.setBuildSpec(newSpec);
		}
		description = value;
	}

	/**
	 * Sets the content type matcher to be associated with this info.  The value may be null.
	 */
	public void setMatcher(IContentTypeMatcher matcher) {
		this.matcher = matcher;
	}

	@SuppressWarnings({"unchecked"})
	public synchronized void setNature(String natureId, IProjectNature value) {
		// thread safety: (Concurrency001)
		if (value == null) {
			if (natures == null)
				return;
			HashMap<String, IProjectNature> temp = (HashMap<String, IProjectNature>) natures.clone();
			temp.remove(natureId);
			if (temp.isEmpty())
				natures = null;
			else
				natures = temp;
		} else {
			HashMap<String, IProjectNature> temp = natures;
			if (temp == null)
				temp = new HashMap<>(5);
			else
				temp = (HashMap<String, IProjectNature>) natures.clone();
			temp.put(natureId, value);
			natures = temp;
		}
	}

	/**
	 * Sets the property store associated with this info.  The value may be null.
	 */
	@Override
	public void setPropertyStore(Object value) {
		propertyStore = value;
	}
}
