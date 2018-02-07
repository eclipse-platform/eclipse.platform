/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.mapping;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;

public class ChangeSetAdapterFactory implements IAdapterFactory {

	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof CVSActiveChangeSet && adapterType == ResourceMapping.class) {
			CVSActiveChangeSet cs = (CVSActiveChangeSet) adaptableObject;
			return adapterType.cast(new ChangeSetResourceMapping(cs));
		}
		if (adaptableObject instanceof CVSCheckedInChangeSet && adapterType == ResourceMapping.class) {
			CVSCheckedInChangeSet cs = (CVSCheckedInChangeSet) adaptableObject;
			return adapterType.cast(new ChangeSetResourceMapping(cs));
		}
		if (adaptableObject instanceof UnassignedDiffChangeSet && adapterType == ResourceMapping.class) {
			UnassignedDiffChangeSet cs = (UnassignedDiffChangeSet) adaptableObject;
			return adapterType.cast(new ChangeSetResourceMapping(cs));
		}
		return null;
	}

	public Class<?>[] getAdapterList() {
		return new Class[] { ResourceMapping.class };
	}

}
