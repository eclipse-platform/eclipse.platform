/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.remote;


import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.Project;

/**
 * This class exists so that the Ant integration has backwards compatibility
 * with Ant releases previous to 1.6. DemuxInputStream is a new class to Ant 1.6.
 */
class DemuxInputStreamSetter {

	protected void remapSystemIn(Project project) {
		System.setIn(new DemuxInputStream(project));
	}
}
