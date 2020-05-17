/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.examples.model;

import org.eclipse.core.resources.ResourcesPlugin;

public class ModelWorkspace extends ModelContainer {

	protected ModelWorkspace() {
		super(ResourcesPlugin.getWorkspace().getRoot());
	}

	@Override
	public String getName() {
		return "Model Root";
	}

	@Override
	public ModelObject getParent() {
		return null;
	}

	public static Object getRoot() {
		return new ModelWorkspace();
	}

}
