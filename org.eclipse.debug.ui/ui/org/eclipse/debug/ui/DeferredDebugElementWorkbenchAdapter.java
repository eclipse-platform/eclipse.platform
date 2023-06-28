/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;


/**
 * Common function for a deferred workbench adapter for a debug element.
 * <p>
 * Clients may subclass this class to provide custom adapters for elements in a debug
 * model. The debug platform provides <code>IDeferredWorkbenchAdapters</code> for the standard debug
 * elements. Clients may override the default content in the debug view by providing an
 * <code>IWorkbenchAdapter</code> or <code>IDeferredWorkbenchAdapter</code> for a debug
 * element.
 * </p>
 * @since 3.1
 * @deprecated deferred custom content in the debug views is no longer supported by
 * 	 {@link IDeferredWorkbenchAdapter}. Deferred custom content is currently supported
 * 	 by a provisional internal viewer framework.
 */
@Deprecated
public abstract class DeferredDebugElementWorkbenchAdapter extends DebugElementWorkbenchAdapter implements IDeferredWorkbenchAdapter {

	/**
	 * An empty collection of children
	 */
	protected static final Object[] EMPTY = new Object[0];

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public ISchedulingRule getRule(Object object) {
		return null;
	}

	@Override
	public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		Object[] children = getChildren(object);
		if (monitor.isCanceled()) {
			return;
		}
		if (children.length > 0) {
			collector.add(children, monitor);
		}
		collector.done();
	}


}
