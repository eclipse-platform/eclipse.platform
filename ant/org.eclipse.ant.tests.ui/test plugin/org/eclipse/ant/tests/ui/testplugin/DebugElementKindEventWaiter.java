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
package org.eclipse.ant.tests.ui.testplugin;

import org.eclipse.debug.core.DebugEvent;

/**
 * Waits for a type of event on a kind of element.  Compare this to SpecificDebugElementEventWaiter which is
 * used to wait for a type of event on a specific debug element object.
 */

public class DebugElementKindEventWaiter extends DebugEventWaiter {
	
	protected Class fElementClass;
	
	public DebugElementKindEventWaiter(int eventKind, Class elementClass) {
		super(eventKind);
		fElementClass = elementClass;
	}
	
	public boolean accept(DebugEvent event) {
		Object o = event.getSource();
		return super.accept(event) && fElementClass.isInstance(o);
	}

}


