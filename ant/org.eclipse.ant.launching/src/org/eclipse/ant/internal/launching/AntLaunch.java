/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * Stores link descriptors for Launch for further use of TaskLinkManager
 */
public class AntLaunch extends Launch {
	List linkDescriptors;

	public AntLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
		super(launchConfiguration, mode, locator);
		linkDescriptors = new ArrayList();
	}

	public void addLinkDescriptor(String line, String fileName, int lineNumber, int offset, int length) {
		if (fileName!= null && fileName.trim().length() > 0) {
			synchronized (linkDescriptors) {
				linkDescriptors.add(new LinkDescriptor(line, fileName, lineNumber, offset, length));
			}
		}
	}

	public void removeLinkDescriptor(LinkDescriptor ld) {
		synchronized (linkDescriptors) {
			linkDescriptors.remove(ld);
		}
	}

	public List getLinkDescriptors() {
		synchronized (linkDescriptors) {
			return new ArrayList(linkDescriptors);
		}
	}

	public void clearLinkDescriptors() {
		synchronized (linkDescriptors) {
			linkDescriptors.clear();
		}
	}

}
