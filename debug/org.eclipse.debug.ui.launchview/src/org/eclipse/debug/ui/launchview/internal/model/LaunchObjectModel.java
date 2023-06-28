/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.model;

import org.eclipse.debug.ui.launchview.services.ILaunchObject;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

public class LaunchObjectModel implements Comparable<LaunchObjectModel> {

	private final String id;
	private final Image image;

	private final ILaunchObject object;
	private final String internalId;

	LaunchObjectModel(ILaunchObject obj) {
		this.id = obj.getId();
		this.internalId = obj.getId();
		this.image = obj.getImage();
		this.object = obj;
	}

	LaunchObjectModel(String id, String internalId, Image image) {
		this.id = id;
		this.internalId = internalId;
		this.image = image;
		this.object = null;
	}

	public StyledString getLabel() {
		if (object == null) {
			return new StyledString(id);
		}
		return object.getLabel();
	}

	public Image getImage() {
		return image;
	}

	public ILaunchObject getObject() {
		return object;
	}

	@Override
	public String toString() {
		return uniqueId() + "(" + getObject() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public int compareTo(LaunchObjectModel o) {
		return uniqueId().compareTo(o.uniqueId());
	}

	public String uniqueId() {
		if (id == null && internalId == null) {
			return "root"; //$NON-NLS-1$
		} else if (internalId != null) {
			return internalId;
		} else if (object == null || object.getType() == null) {
			return id;
		}
		return object.getType().getIdentifier() + "." + id; //$NON-NLS-1$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (uniqueId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LaunchObjectModel other = (LaunchObjectModel) obj;
		return uniqueId().equals(other.uniqueId());
	}

}
