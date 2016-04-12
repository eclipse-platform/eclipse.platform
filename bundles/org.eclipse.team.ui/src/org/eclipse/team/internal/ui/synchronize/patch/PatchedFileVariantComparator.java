/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.mapping.LocalResourceVariant;

public class PatchedFileVariantComparator implements IResourceVariantComparator {

	@Override
	public boolean compare(IResource local, IResourceVariant remote) {
		if (remote instanceof LocalResourceVariant)
			// return true when matching with base
			return true;
		return false;
	}

	@Override
	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		return false;
	}

	@Override
	public boolean isThreeWay() {
		return true;
	}
}