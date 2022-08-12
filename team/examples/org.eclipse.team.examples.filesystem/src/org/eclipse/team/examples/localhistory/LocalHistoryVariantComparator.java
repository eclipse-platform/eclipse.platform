/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.examples.localhistory;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class LocalHistoryVariantComparator implements IResourceVariantComparator {
	@Override
	public boolean compare(IResource local, IResourceVariant remote) {
		return false;
	}

	@Override
	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		return false;
	}

	@Override
	public boolean isThreeWay() {
		return false;
	}
}
