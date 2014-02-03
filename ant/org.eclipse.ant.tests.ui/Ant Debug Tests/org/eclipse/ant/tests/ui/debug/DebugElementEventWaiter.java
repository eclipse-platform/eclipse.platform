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
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter;
import org.eclipse.debug.core.DebugEvent;

/**
 * Waits for an event on a specific element
 */

public class DebugElementEventWaiter extends DebugEventWaiter {
	
	protected Object fElement;
	
	public DebugElementEventWaiter(int kind, Object element) {
		super(kind);
		fElement = element;
	}
	
	public boolean accept(DebugEvent event) {
		return super.accept(event) && fElement == event.getSource();
	}

}
