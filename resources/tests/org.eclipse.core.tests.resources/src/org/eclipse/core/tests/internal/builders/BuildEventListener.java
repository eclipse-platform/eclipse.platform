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
package org.eclipse.core.tests.internal.builders;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

/**
 * An event listener that tracks what events were received.
 */
public class BuildEventListener implements IResourceChangeListener {

	private int buildKind;
	private boolean postBuild;
	private boolean postChange;
	private boolean preBuild;
	private Object source;

	public int getBuildKind() {
		return buildKind;
	}

	public Object getSource() {
		return source;
	}

	public boolean hadPostBuild() {
		return postBuild;
	}

	public boolean hadPostChange() {
		return postChange;
	}

	public boolean hadPreBuild() {
		return preBuild;
	}

	/**
	 * Resets this listener back to the state it was in before any events occurred.
	 */
	public void reset() {
		postBuild = preBuild = postChange = false;
		buildKind = 0;
		source = null;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.PRE_BUILD :
				preBuild = true;
			case IResourceChangeEvent.POST_BUILD :
				postBuild = true;
			case IResourceChangeEvent.POST_CHANGE :
				postChange = true;
		}
		buildKind = event.getBuildKind();
		source = event.getSource();
	}
}
