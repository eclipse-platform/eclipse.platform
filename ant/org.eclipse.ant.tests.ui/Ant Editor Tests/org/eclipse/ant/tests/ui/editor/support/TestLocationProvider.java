/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.support;

import java.io.File;

import org.eclipse.ant.internal.ui.model.LocationProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


public class TestLocationProvider extends LocationProvider {

	private File buildFile;
	
	public TestLocationProvider(File buildFile) {
		super(null);
		this.buildFile= buildFile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.outline.ILocationProvider#getLocation()
	 */
	public IPath getLocation() {
		return new Path(buildFile.getAbsolutePath());
	}
}
