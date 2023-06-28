/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.internal.memory.engine;

import org.eclipse.debug.core.model.MemoryByte;

/**
 * For testing addressable size > 1. Group each addressable unit in a MemoryByte
 * array.
 *
 */
public class SampleMemoryUnit {

	MemoryByte[] fBytes;

	public SampleMemoryUnit(MemoryByte[] bytes) {
		fBytes = bytes;
	}

	public MemoryByte[] getBytes() {
		return fBytes;
	}

	public void setBytes(MemoryByte[] bytes) {
		fBytes = bytes;
	}

}
