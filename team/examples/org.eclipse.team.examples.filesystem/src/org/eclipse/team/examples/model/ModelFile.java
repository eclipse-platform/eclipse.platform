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

import org.eclipse.core.resources.IFile;

public abstract class ModelFile extends ModelResource {

	protected ModelFile(IFile file) {
		super(file);
	}

	@Override
	public String getName() {
		String name = super.getName();
		int index = name.lastIndexOf(".");
		return name.substring(0, index);
	}

}
