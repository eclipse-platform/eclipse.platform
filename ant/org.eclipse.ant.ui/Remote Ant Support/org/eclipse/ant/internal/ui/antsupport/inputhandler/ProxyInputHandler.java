/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.antsupport.inputhandler;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;

public class ProxyInputHandler implements InputHandler {

	@Override
	public void handleInput(InputRequest request) throws BuildException {
		new SWTInputHandler().handleInput(request);
	}
}
