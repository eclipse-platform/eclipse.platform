/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui.editor.support;

import java.io.File;

import org.eclipse.ant.internal.ui.model.LocationProvider;
import org.eclipse.core.runtime.IPath;

public class TestLocationProvider extends LocationProvider {

	private File buildFile;

	public TestLocationProvider(File buildFile) {
		super(null);
		this.buildFile = buildFile;
	}

	@Override
	public IPath getLocation() {
		return IPath.fromOSString(buildFile.getAbsolutePath());
	}
}
