/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.List;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.internal.CompareUIPlugin.OpenedUnifiedDiff;
import org.eclipse.compare.internal.CompareUIPlugin.UnifiedDiffSource;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.unifieddiff.internal.HideAllDiffsRunnable;
import org.eclipse.compare.unifieddiff.internal.IUnifiedDiffFileNavigator;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Shows the changed files of a comparison of folders as unified diffs, one file
 * at a time in a single editor.
 */
public class UnifiedDiffFiles implements IUnifiedDiffFileNavigator {

	/** A changed file of the comparison and its path within it. */
	public record ChangedFile(ICompareInput compareInput, String path) {
	}

	private final CompareUIPlugin plugin;
	private final CompareEditorInput input;
	private final IWorkbenchPage page;
	private final IReusableEditor compareEditor;
	private final List<ChangedFile> files;
	private int current = -1;
	private boolean backwards;
	private OpenedUnifiedDiff shown;

	UnifiedDiffFiles(CompareUIPlugin plugin, CompareEditorInput input, IWorkbenchPage page,
			IReusableEditor compareEditor, List<ChangedFile> files) {
		this.plugin = plugin;
		this.input = input;
		this.page = page;
		this.compareEditor = compareEditor;
		this.files = files;
	}

	/** Shows the first file, whose source is already read. */
	boolean start(UnifiedDiffSource source) {
		return show(0, source, false);
	}

	public List<ChangedFile> getFiles() {
		return files;
	}

	/** The index of the file shown, or -1 before the first one is up. */
	public int getCurrent() {
		return current;
	}

	/** Moves to the file at the given index, reading its content in the background. */
	public void showFile(int index) {
		if (index >= 0 && index < files.size() && index != current) {
			load(index, index < current);
		}
	}

	@Override
	public boolean endReached(boolean next) {
		int target = current + (next ? 1 : -1);
		if (target < 0 || target >= files.size()) {
			return false;
		}
		String action = NavigationEndDialog.chooseEndAction(page.getWorkbenchWindow().getShell(), next);
		if (ICompareUIConstants.PREF_VALUE_LOOP.equals(action)) {
			return false;
		}
		if (ICompareUIConstants.PREF_VALUE_NEXT.equals(action)) {
			// the toolbar that triggered this goes away with the editor
			Display.getDefault().asyncExec(() -> load(target, !next));
		}
		return true;
	}

	@Override
	public void diffsFinished() {
		if (isShowing() && current + 1 < files.size()) {
			load(current + 1, false);
		}
	}

	@Override
	public boolean startsAtLastDiff() {
		return backwards;
	}

	private boolean isShowing() {
		ITextViewer viewer = shown == null ? null : shown.textEditor().getAdapter(ITextViewer.class);
		return viewer != null && viewer.getTextWidget() != null && !viewer.getTextWidget().isDisposed();
	}

	private void load(int index, boolean goingBack) {
		ChangedFile file = files.get(index);
		Job job = new Job(NLS.bind(CompareMessages.UnifiedDiff_preparing, file.path())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				UnifiedDiffSource source = plugin.sourceOf(file.compareInput());
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				Display.getDefault().asyncExec(() -> show(index, source, goingBack));
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return family == input || input.belongsTo(family);
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private boolean show(int index, UnifiedDiffSource source, boolean goingBack) {
		if (shown != null && !isShowing()) {
			// the user closed the editor in the meantime
			return false;
		}
		backwards = goingBack;
		OpenedUnifiedDiff opened = source == null ? null
				: plugin.openUnifiedDiff(source, input, page, compareEditor, true, this, createFileSelector(index));
		if (opened == null) {
			int skipTo = index + (goingBack ? -1 : 1);
			if (shown != null && skipTo >= 0 && skipTo < files.size()) {
				load(skipTo, goingBack);
			}
			return false;
		}
		OpenedUnifiedDiff previous = shown;
		shown = opened;
		current = index;
		if (previous != null && previous.editorPart() != opened.editorPart()) {
			leave(previous);
		}
		return true;
	}

	/** Takes the diff off the previous file, closing its editor when it was opened for the diff. */
	private void leave(OpenedUnifiedDiff previous) {
		ITextViewer viewer = previous.textEditor().getAdapter(ITextViewer.class);
		if (viewer == null || viewer.getTextWidget() == null || viewer.getTextWidget().isDisposed()) {
			return;
		}
		UnifiedDiffManager.setFileNavigator(viewer, null);
		// prompts for unsaved changes
		if (previous.openedHere() && page.closeEditor(previous.editorPart(), true)) {
			return;
		}
		new HideAllDiffsRunnable(previous.textEditor()).run();
	}

	private Action createFileSelector(int index) {
		Action selector = new Action(NLS.bind(CompareMessages.UnifiedDiff_fileOf, index + 1, files.size()),
				IAction.AS_DROP_DOWN_MENU) {
			@Override
			public void runWithEvent(Event event) {
				// the button opens the list too, not only its arrow
				if (event.widget instanceof ToolItem item) {
					Menu menu = getMenuCreator().getMenu(item.getParent());
					Rectangle bounds = item.getBounds();
					menu.setLocation(item.getParent().toDisplay(bounds.x, bounds.y + bounds.height));
					menu.setVisible(true);
				}
			}
		};
		selector.setToolTipText(CompareMessages.UnifiedDiff_selectFile_tooltip);
		selector.setMenuCreator(new IMenuCreator() {
			private Menu menu;

			@Override
			public Menu getMenu(Control parent) {
				dispose();
				menu = new Menu(parent);
				for (int i = 0; i < files.size(); i++) {
					int fileIndex = i;
					MenuItem item = new MenuItem(menu, SWT.RADIO);
					item.setText(files.get(i).path().replace("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
					item.setSelection(i == index);
					item.addListener(SWT.Selection, e -> {
						// a radio item also reports being deselected
						if (item.getSelection()) {
							showFile(fileIndex);
						}
					});
				}
				return menu;
			}

			@Override
			public Menu getMenu(Menu parent) {
				return null;
			}

			@Override
			public void dispose() {
				if (menu != null && !menu.isDisposed()) {
					menu.dispose();
				}
				menu = null;
			}
		});
		return selector;
	}
}
