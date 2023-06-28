/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;

/**
 * Manages source selections and decorated editors for launch views.
 */
public class DecorationManager {

	// map of targets to lists of active decorations
	private static Map<IDebugTarget, List<Decoration>> fDecorations = new HashMap<>(10);

	/**
	 * Adds the given decoration for the given stack frame.
	 *
	 * @param decoration
	 * @param frame
	 */
	public static void addDecoration(Decoration decoration) {
		synchronized (fDecorations) {
			IDebugTarget target = decoration.getThread().getDebugTarget();
			List<Decoration> list = fDecorations.get(target);
			if (list == null) {
				list = new ArrayList<>();
				fDecorations.put(target, list);
			}
			list.add(decoration);
		}
	}

	/**
	 * Removes any decorations for the given debug target.
	 *
	 * @param target to remove editor decorations for
	 */
	public static void removeDecorations(IDebugTarget target) {
		doRemoveDecorations(target, null);
	}

	/**
	 * Removes any decorations for the given thread
	 *
	 * @param thread thread to remove decorations for
	 */
	public static void removeDecorations(IThread thread) {
		doRemoveDecorations(thread.getDebugTarget(), thread);
	}

	private static void doRemoveDecorations(IDebugTarget target, IThread thread) {
		ArrayList<Decoration> decorationsToRemove = new ArrayList<>();
		synchronized (fDecorations) {
			List<Decoration> list = fDecorations.get(target);
			if (list != null) {
				ListIterator<Decoration> iterator = list.listIterator();
				while (iterator.hasNext()) {
					Decoration decoration = iterator.next();
					if (thread == null || thread.equals(decoration.getThread())) {
						decorationsToRemove.add(decoration);
						iterator.remove();
					}
				}
				if (list.isEmpty()) {
					fDecorations.remove(target);
				}
			}
		}
		Iterator<Decoration> iter = decorationsToRemove.iterator();
		while (iter.hasNext()) {
			Decoration decoration = iter.next();
			decoration.remove();
		}
	}

}
