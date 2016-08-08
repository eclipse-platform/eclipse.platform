/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.CVSException;

/**
 * Listener that accumulates test errors
 */
public class CVSTestLogListener implements ILogListener {
	Map<String, List<IStatus>> errors = new HashMap<>();
	
	@Override
	public void logging(IStatus status, String plugin) {
		List<IStatus> pluginErrors = errors.get(plugin);
		if (pluginErrors == null) {
			pluginErrors = new ArrayList<>();
			errors.put(plugin, pluginErrors);
		}
		pluginErrors.add(status);
	}

	public void checkErrors() throws CoreException {
		if (errors.isEmpty())
			return;
		List<IStatus> allErrors = new ArrayList<>();
		for (List<IStatus> pluginErrors : errors.values()) {
			allErrors.addAll(pluginErrors);
		}
		errors.clear();
		if (allErrors.isEmpty()) return;
		IStatus status = null;
		if (allErrors.size() == 1) {
			status = allErrors.get(0);
			if (!status.isMultiStatus()) {
				throw new CVSException(status);
			}
		}
		if (status == null) {
			status = new MultiStatus("org.eclipse.team.tests.cvs.core", 0, 
					allErrors.toArray(new IStatus[allErrors.size()]), 
					"Errors were logged during this test. Check the log file for details", null);
		}
		throw new CoreException(status);
	}
}
