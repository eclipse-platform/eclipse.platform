/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamMerger;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.team.core.mapping.DelegatingStorageMerger;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.mapping.IStreamMergerDelegate;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class StreamMergerDelegate implements IStreamMergerDelegate {

	public static void start() {
		TeamPlugin.getPlugin().setMergerDelegate(new StreamMergerDelegate());
	}

	@Override
	public IStorageMerger findMerger(IStorage target) {
		try {
			IContentType type = DelegatingStorageMerger.getContentType(target);
			if (type != null) {
				IStreamMerger merger = CompareUI.createStreamMerger(type);
				if (merger != null)
					return new StorageStreamMerger(merger);
			}
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		IStreamMerger merger = CompareUI.createStreamMerger(DelegatingStorageMerger.getExtension(target.getName()));
		if (merger != null)
			return new StorageStreamMerger(merger);
		return null;
	}

}
