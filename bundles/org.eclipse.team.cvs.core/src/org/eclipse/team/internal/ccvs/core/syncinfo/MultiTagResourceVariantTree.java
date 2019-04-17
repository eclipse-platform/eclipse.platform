/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.internal.ccvs.core.CVSTag;

/**
 * A CVS resource variant tree that associates a different tag with each root project.
 */
public class MultiTagResourceVariantTree extends CVSResourceVariantTree {

	Map resources = new HashMap();
	
	public MultiTagResourceVariantTree(ResourceVariantByteStore cache, boolean cacheFileContentsHint) {
		super(cache, null, cacheFileContentsHint);
	}
	
	public void addResource(IResource resource, CVSTag tag) {
		Assert.isNotNull(resource);
		Assert.isNotNull(tag);
		resources.put(resource, tag);
	}
	
	@Override
	public CVSTag getTag(IResource resource) {
		return (CVSTag)resources.get(resource);
	}
}
