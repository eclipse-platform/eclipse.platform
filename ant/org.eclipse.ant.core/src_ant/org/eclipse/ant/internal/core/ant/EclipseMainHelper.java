/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.core.ant;

import org.apache.tools.ant.Project;

public class EclipseMainHelper {

	protected void runProjectHelp(String buildFileLocation, Project eclipseAntProject) {
		EclipseAntMain.run(new String[]{"-f", buildFileLocation, "-p"}, eclipseAntProject); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	protected void runUsage(String buildFileLocation, Project eclipseAntProject) {
		EclipseAntMain.run(new String[]{"-f", buildFileLocation, "-h"}, eclipseAntProject); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
