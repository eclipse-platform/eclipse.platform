/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.internal.resources.LinkDescription;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests concurrent modification of the project description link table.
 */
public class Bug_132510 extends ResourceTest {
	public void testBug() {
		ProjectDescription desc = new ProjectDescription();
		IPath path1 = IPath.fromOSString("/a/b/");
		IPath path2 = IPath.fromOSString("/a/c/");
		LinkDescription link = new LinkDescription();
		desc.setLinkLocation(path1, link);
		HashMap<IPath, LinkDescription> linkMap = desc.getLinks();
		Iterator<LinkDescription> it = linkMap.values().iterator();
		desc.setLinkLocation(path2, link);
		try {
			it.next();
		} catch (ConcurrentModificationException e) {
			fail("4.99", e);
		}
	}

}
