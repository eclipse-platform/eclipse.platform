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

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.swt.graphics.Image;

public class LaunchObjectContainerModel extends LaunchObjectModel {

	private final Set<LaunchObjectModel> children = new TreeSet<>((a, b) -> {
		if (a instanceof LaunchObjectFavoriteContainerModel) {
			return -1;
		} else if (b instanceof LaunchObjectFavoriteContainerModel) {
			return 1;
		}

		return a.getLabel().getString().compareTo(b.getLabel().getString());
	});
	private final ILaunchConfigurationType type;

	LaunchObjectContainerModel() {
		this(null, null);
	}

	LaunchObjectContainerModel(ILaunchConfigurationType type) {
		super(type.getName(), type.getIdentifier(), DebugPluginImages.getImage(type.getIdentifier()));
		this.type = type;
	}

	protected LaunchObjectContainerModel(String id, Image image) {
		super(id, null, image);
		this.type = null;
	}

	public void addChild(LaunchObjectModel model) {
		children.add(model);
	}

	public Set<LaunchObjectModel> getChildren() {
		return children;
	}

	public LaunchObjectContainerModel getContainerFor(LaunchObjectModel m) {
		for (LaunchObjectModel child : children) {
			if (child instanceof LaunchObjectContainerModel) {
				if (m.getObject().getType().equals(((LaunchObjectContainerModel) child).type)) {
					return (LaunchObjectContainerModel) child;
				}
			}
		}
		return null;
	}

}
