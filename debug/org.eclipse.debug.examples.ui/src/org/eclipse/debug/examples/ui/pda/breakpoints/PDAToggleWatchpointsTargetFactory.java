/*******************************************************************************
 * Copyright (c) 2008, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.breakpoints;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Toggle breakpoints target factory for creating PDA watchpoints.  It allows the
 * user the select the type of watchpoint that will be created when the watchpoint
 * is toggles inside the editor or variables view.
 */
public class PDAToggleWatchpointsTargetFactory implements IToggleBreakpointsTargetFactory {

	private static final String TOGGLE_WATCHPOINT_TARGET_ACCESS = "org.eclipse.debug.examples.ui.pda.watchpoint_access"; //$NON-NLS-1$
	private static final String TOGGLE_WATCHPOINT_TARGET_MODIFICATION = "org.eclipse.debug.examples.ui.pda.watchpoint_modification"; //$NON-NLS-1$
	private static final String TOGGLE_WATCHPOINT_TARGET_BOTH = "org.eclipse.debug.examples.ui.pda.watchpoint_both"; //$NON-NLS-1$

	private static Set<String> TOGGLE_WATCHPOINTS_TARGETS = new LinkedHashSet<>();

	private final Map<String, IToggleBreakpointsTarget> fToggleWatchpointTargets = new HashMap<>(3);

	static {
		TOGGLE_WATCHPOINTS_TARGETS.add(TOGGLE_WATCHPOINT_TARGET_BOTH);
		TOGGLE_WATCHPOINTS_TARGETS.add(TOGGLE_WATCHPOINT_TARGET_ACCESS);
		TOGGLE_WATCHPOINTS_TARGETS.add(TOGGLE_WATCHPOINT_TARGET_MODIFICATION);
	}

	@Override
	public IToggleBreakpointsTarget createToggleTarget(String targetID) {
		IToggleBreakpointsTarget target = fToggleWatchpointTargets.get(targetID);
		if (target == null) {
			if (TOGGLE_WATCHPOINT_TARGET_BOTH.equals(targetID)) {
				target = new PDAToggleWatchpointsTarget(true, true);
			} else if (TOGGLE_WATCHPOINT_TARGET_ACCESS.equals(targetID)) {
				target = new PDAToggleWatchpointsTarget(true, false);
			} else if (TOGGLE_WATCHPOINT_TARGET_MODIFICATION.equals(targetID)) {
				target = new PDAToggleWatchpointsTarget(false, true);
			} else {
				return null;
			}
			fToggleWatchpointTargets.put(targetID, target);
		}
		return target;
	}

	@Override
	public String getDefaultToggleTarget(IWorkbenchPart part, ISelection selection) {
		return TOGGLE_WATCHPOINT_TARGET_BOTH;
	}

	@Override
	public Set<String> getToggleTargets(IWorkbenchPart part, ISelection selection) {
		return TOGGLE_WATCHPOINTS_TARGETS;
	}

	@Override
	public String getToggleTargetName(String targetID) {
		if (TOGGLE_WATCHPOINT_TARGET_BOTH.equals(targetID)) {
			return "Watchpoints (Read/Write)"; //$NON-NLS-1$
		} else if (TOGGLE_WATCHPOINT_TARGET_ACCESS.equals(targetID)) {
			return "Watchpoints (Read)"; //$NON-NLS-1$
		} else if (TOGGLE_WATCHPOINT_TARGET_MODIFICATION.equals(targetID)) {
			return "Watchpoints (Write)"; //$NON-NLS-1$
		} else {
			return null;
		}
	}

	@Override
	public String getToggleTargetDescription(String targetID) {
		return getToggleTargetName(targetID);
	}
}
