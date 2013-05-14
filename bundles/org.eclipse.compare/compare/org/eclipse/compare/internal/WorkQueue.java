/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.*;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * A work queue maintains a list of tasks that need to be run.
 * If the same task is added multiple times, the last occurrence of
 * the task will be run(i.e. the task will be removed from it's
 * previous location and aded to the end of the queue.
 */
public class WorkQueue {
	
	private List runnables = new ArrayList();

	public boolean add(IRunnableWithProgress runnable) {
		if (runnables.contains(runnable))
			runnables.remove(runnable);
		return runnables.add(runnable);
	}

	public void clear() {
		runnables.clear();
	}

	public boolean contains(IRunnableWithProgress runnable) {
		return runnables.contains(runnable);
	}

	public boolean isEmpty() {
		return runnables.isEmpty();
	}

	public boolean remove(IRunnableWithProgress runnable) {
		return runnables.remove(runnable);
	}

	public int size() {
		return runnables.size();
	}
	public IRunnableWithProgress remove() {
		return (IRunnableWithProgress)runnables.remove(0);
	}


}
