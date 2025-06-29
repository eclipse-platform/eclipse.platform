/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.terminal;

import java.util.Map;

import org.eclipse.cdt.utils.spawner.Spawner;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;

public class PtyRuntimeProcess extends RuntimeProcess {

	PtyRuntimeProcess(ILaunch launch, Process process, String name, Map<String, String> attributes) {
		super(launch, process, name, attributes);
	}

	public Spawner getSpawner() {
		Process systemProcess = super.getSystemProcess();
		if (systemProcess instanceof Spawner) {
			return (Spawner) systemProcess;
		}
		return null;
	}

}
