/*******************************************************************************
 *  Copyright (c) 2005, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;

/**
 * Model proxy for launch object.
 *
 * @since 3.3
 */
public class LaunchProxy extends AbstractModelProxy implements ILaunchesListener2 {

	private ILaunch fLaunch;

	/**
	 * Set of launch's previous children. When a child is added,
	 * its model proxy is installed.
	 */
	private Set<Object> fPrevChildren = new HashSet<>();

	/**
	 * Constructs a new model proxy for the given launch.
	 *
	 * @param launch
	 */
	public LaunchProxy(ILaunch launch) {
		fLaunch = launch;
	}

	@Override
	public void init(IPresentationContext context) {
		super.init(context);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	@Override
	public void installed(Viewer viewer) {
		// install model proxies for existing children
		installModelProxies();
	}

	@Override
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
		fPrevChildren.clear();
		fLaunch = null;
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			if (launch == fLaunch) {
				fireDelta(IModelDelta.STATE | IModelDelta.CONTENT | IModelDelta.UNINSTALL);
				break;
			}
		}
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			if (launch == fLaunch) {
				fireDelta(IModelDelta.UNINSTALL);
				break;
			}
		}
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			if (launch == fLaunch) {
				fireDelta(IModelDelta.STATE | IModelDelta.CONTENT);
				installModelProxies();
				break;
			}
		}
	}

	/**
	 * Installs model proxies for any new children in the given launch.
	 *
	 * @param launch
	 */
	protected void installModelProxies() {
		boolean changes = false;
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] allLaunches = manager.getLaunches();
		ModelDelta root = new ModelDelta(manager, 0, IModelDelta.NO_CHANGE, allLaunches.length);
		synchronized(this) {
			Object[] children = fLaunch.getChildren();
			ModelDelta launchDelta = root.addNode(fLaunch, indexOf(fLaunch, allLaunches), IModelDelta.EXPAND, children.length);
			for (Object child : children) {
				if (fPrevChildren.add(child)) {
					changes = true;
					launchDelta.addNode(child, indexOf(child, children), IModelDelta.INSTALL, -1);
				}
			}
			List<Object> childrenList = Arrays.asList(children);
			for (Iterator<Object> itr = fPrevChildren.iterator(); itr.hasNext();) {
				Object child = itr.next();
				if (!childrenList.contains(child)) {
					itr.remove();
					changes = true;
					launchDelta.addNode(child, IModelDelta.UNINSTALL);
				}
			}
		}
		if (changes) {
			fireModelChanged(root);
		}
	}

	/**
	 * Finds the index of the selected element in the given list
	 * @param element the element to get the index for
	 * @param list the list to search for the index
	 * @return the index of the specified element in the given array or -1 if not found
	 */
	protected int indexOf(Object element, Object[] list) {
		for (int i = 0; i < list.length; i++) {
			if (element == list[i]) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Convenience method to fire a delta
	 * @param flags the flags to set on the delta
	 */
	protected void fireDelta(int flags) {
		ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.NO_CHANGE);
		delta.addNode(fLaunch, flags);
		fireModelChanged(delta);
	}

}
