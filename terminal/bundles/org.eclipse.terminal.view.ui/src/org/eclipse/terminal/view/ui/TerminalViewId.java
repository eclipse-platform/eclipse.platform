/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361363] [TERMINALS] Implement "Pin&Clone" for the "Terminals" view
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Encapsulates the pair of mandatory primary terminal view id and optional secondary id
 *
 * @see IUIConstants#ID
 * @see IViewReference#getSecondaryId()
 */
public record TerminalViewId(String primary, Optional<String> secondary) {

	public TerminalViewId() {
		this(IUIConstants.ID);
	}

	public TerminalViewId(String primary) {
		this(primary, Optional.empty());
	}

	public TerminalViewId(String primary, String secondary) {
		this(primary, Optional.ofNullable(secondary));
	}

	public TerminalViewId next() {
		return new TerminalViewId(primary, nextSecondaryId(primary));
	}

	private static String nextSecondaryId(String primary) {
		Map<String, IViewReference> terminalViews = new HashMap<>();
		int maxNumber = 0;
		for (IViewReference ref : viewReferences()) {
			if (ref.getId().equals(primary)) {
				if (ref.getSecondaryId() != null) {
					terminalViews.put(ref.getSecondaryId(), ref);
					int scondaryIdInt = Integer.parseInt(ref.getSecondaryId());
					if (scondaryIdInt > maxNumber) {
						maxNumber = scondaryIdInt;
					}
				} else {
					// add the one with secondaryId == null with 0 by default
					terminalViews.put(Integer.toString(0), ref);
				}
			}
		}
		if (terminalViews.size() == 0) {
			return null;
		}

		int i = 0;
		for (; i < maxNumber; i++) {
			String secondaryIdStr = Integer.toString(i);
			if (!terminalViews.keySet().contains(secondaryIdStr)) {
				// found a free slot
				if (i == 0) {
					return null;
				}
				return Integer.toString(i);
			}
		}
		// add a new one
		return Integer.toString(i + 1);
	}

	private static List<IViewReference> viewReferences() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return Collections.emptyList();
		}
		return Optional.of(PlatformUI.getWorkbench()).map(IWorkbench::getActiveWorkbenchWindow)
				.map(IWorkbenchWindow::getActivePage).map(IWorkbenchPage::getViewReferences).map(Arrays::asList)
				.orElseGet(Collections::emptyList);
	}

}
