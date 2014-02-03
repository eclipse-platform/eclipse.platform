/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.launching.launchConfigurations;

import java.util.Map;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.RuntimeProcess;

public class RemoteAntRuntimeProcess extends RuntimeProcess {

	/**
	 * Constructs a RuntimeProcess on the given system process
	 * with the given name, adding this process to the given
	 * launch.
	 * Sets the streams proxy to an AntStreamsProxy if output is captured.
	 */
	public RemoteAntRuntimeProcess(ILaunch launch, Process process, String name, Map attributes) {
		super(launch, process, name, attributes);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.RuntimeProcess#createStreamsProxy()
	 */
	protected IStreamsProxy createStreamsProxy() {
		return new AntStreamsProxy();
	}
}
