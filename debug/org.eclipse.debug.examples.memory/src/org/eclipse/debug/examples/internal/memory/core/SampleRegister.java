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
package org.eclipse.debug.examples.internal.memory.core;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegister;
import org.eclipse.debug.core.model.IRegisterGroup;

public class SampleRegister extends SampleVariable implements IRegister {

	private SampleRegisterGroup fGroup;

	SampleRegister(SampleStackFrame frame, SampleRegisterGroup group, String name) {
		super(frame, name);
		fGroup = group;
	}

	@Override
	public IRegisterGroup getRegisterGroup() throws DebugException {
		return fGroup;
	}

}
