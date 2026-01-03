/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.breakpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.BreakpointManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugTestExtension.class)
public class BreakpointTests {

	private final IBreakpointManager bpm = DebugPlugin.getDefault().getBreakpointManager();

	@AfterEach
	public void tearDown() throws Exception {
		for (IBreakpoint bp : getTestBreakpoints()) {
			bp.delete();
		}
	}

	/**
	 * Get all {@link TestBreakpoint}s known by the default
	 * {@link BreakpointManager}.
	 *
	 * @return {@link TestBreakpoint}s or empty list
	 */
	private List<TestBreakpoint> getTestBreakpoints() {
		List<TestBreakpoint> bps = new ArrayList<>();
		for (IBreakpoint bp : bpm.getBreakpoints(TestBreakpoint.MODEL)) {
			assertTrue(bp instanceof TestBreakpoint);
			bps.add((TestBreakpoint) bp);
		}
		return bps;
	}

	/**
	 * Test for bug 424561 where a breakpoint is deleted and the sequence undo
	 * (recreate breakpoint), redo (delete again), undo (recreate breakpoint)
	 * does not recreate the breakpoint.
	 */
	@Test
	public void testBug424561_undoRedoUndoGone() throws Exception {
		boolean viewVisible = true;
		BreakpointsView view = ((BreakpointsView) DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().findView(IDebugUIConstants.ID_BREAKPOINT_VIEW));
		if (view == null) {
			viewVisible = false;
			view = ((BreakpointsView) DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().showView(IDebugUIConstants.ID_BREAKPOINT_VIEW));
		}
		assertNotNull(view, "Failed to obtain breakpoint view.");

		try {
			String content = "Bug 424561";
			TestBreakpoint bp = new TestBreakpoint(content);
			IOperationHistory operationHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			IUndoContext context = DebugUITools.getBreakpointsUndoContext();

			bpm.addBreakpoint(bp);
			TestUtil.waitWhile(() -> getTestBreakpoints().isEmpty(), () -> "Breakpoint is not created");
			assertTrue(bp.getMarker().exists(), "Breakpoint marker missing");
			assertTrue(bp.isRegistered(), "Breakpoint not registered");

			DebugUITools.deleteBreakpoints(new IBreakpoint[] {
					bp }, null, null);
			assertTrue(operationHistory.canUndo(context));
			TestUtil.waitWhile(() -> !getTestBreakpoints().isEmpty(), () -> "Breakpoint is not deleted");
			assertFalse(bp.getMarker().exists(), "Breakpoint marker not removed");
			assertFalse(bp.isRegistered(), "Breakpoint still registered");

			operationHistory.undo(context, null, null);
			assertTrue(operationHistory.canRedo(context));
			TestUtil.waitWhile(() -> getTestBreakpoints().isEmpty(), () -> "Breakpoint is not recreated");
			bp = getTestBreakpoints().get(0);
			assertEquals(content, bp.getText(), "Breakpoint attributes not correctly restored");
			assertTrue(bp.getMarker().exists(), "Breakpoint marker missing");
			assertTrue(bp.isRegistered(), "Breakpoint not registered");

			operationHistory.redo(context, null, null);
			assertTrue(operationHistory.canUndo(context));
			TestUtil.waitWhile(() -> !getTestBreakpoints().isEmpty(), () -> "Breakpoint is not deleted");
			assertFalse(bp.getMarker().exists(), "Breakpoint marker not removed");
			assertFalse(bp.isRegistered(), "Breakpoint still registered");

			operationHistory.undo(context, null, null);
			assertTrue(operationHistory.canRedo(context));
			TestUtil.waitWhile(() -> getTestBreakpoints().isEmpty(), () -> "Breakpoint is not recreated");
			bp = getTestBreakpoints().get(0);
			assertEquals(content, bp.getText(), "Breakpoint attributes not correctly restored");
			assertTrue(bp.getMarker().exists(), "Breakpoint marker missing");
			assertTrue(bp.isRegistered(), "Breakpoint not registered");

			final BreakpointsView finalView = view;
			final TestBreakpoint finalBp = bp;
			TestUtil.waitWhile(() -> {
				TreeItem item = (TreeItem) finalView.getTreeModelViewer().testFindItem(finalBp);
				return item == null || item.getText() == null || !item.getText().contains(content);
			}, () -> "Breakpoint not restored in view");
		} finally {
			if (!viewVisible) {
				DebugUIPlugin.getActiveWorkbenchWindow().getActivePage().hideView(view);
			}
		}
	}
}
