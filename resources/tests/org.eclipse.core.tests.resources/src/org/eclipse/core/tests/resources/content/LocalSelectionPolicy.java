/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager.ISelectionPolicy;
import org.eclipse.core.tests.resources.AutomatedResourceTests;

/**
 * Selection policy that filters out any content types that do not
 * belong to the runtime plug-in or this test plug-in.
 */
public class LocalSelectionPolicy implements ISelectionPolicy {
	@Override
	public IContentType[] select(IContentType[] candidates, boolean fileName, boolean content) {
		List<IContentType> result = new ArrayList<>(candidates.length);
		for (IContentType candidate : candidates) {
			String namespace = getNamespace(candidate.getId());
			if (namespace.equals(AutomatedResourceTests.PI_RESOURCES_TESTS) || namespace.equals(Platform.PI_RUNTIME)) {
				result.add(candidate);
			}
		}
		return result.toArray(new IContentType[result.size()]);
	}

	private static String getNamespace(String id) {
		int lastDot = id.lastIndexOf('.');
		if (lastDot <= 1) {
			throw new IllegalArgumentException("lastDot ==" + lastDot);
		}
		return id.substring(0, lastDot);
	}
}
