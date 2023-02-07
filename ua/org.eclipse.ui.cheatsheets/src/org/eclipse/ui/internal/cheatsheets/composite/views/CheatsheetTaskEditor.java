/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.cheatsheets.CheatSheetListener;
import org.eclipse.ui.cheatsheets.CheatSheetViewerFactory;
import org.eclipse.ui.cheatsheets.ICheatSheetEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.state.MementoStateManager;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetViewer;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;

public class CheatsheetTaskEditor extends TaskEditor {
	private CheatSheetViewer viewer;
	private IEditableTask task;

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		viewer = (CheatSheetViewer)CheatSheetViewerFactory.createCheatSheetView();
		viewer.createPartControl(parent);
	}

	@Override
	public Control getControl() {
		return viewer.getControl();
	}


	@Override
	public void setInput(IEditableTask task, IMemento memento) {
		this.task = task;
		Dictionary<String, String> params = task.getParameters();
		String id = params.get(ICompositeCheatsheetTags.CHEATSHEET_TASK_ID);
		String path = params.get(ICompositeCheatsheetTags.CHEATSHEET_TASK_PATH);
		boolean showIntro = true;
		String showIntroParam = params.get(ICompositeCheatsheetTags.CHEATSHEET_TASK_SHOW_INTRO);
		if (showIntroParam != null) {
			showIntro = showIntroParam.equalsIgnoreCase("true"); //$NON-NLS-1$
		}

		MementoStateManager stateManager = new MementoStateManager(memento, task.getCompositeCheatSheet().getCheatSheetManager());
		if (path != null) {
			URL url;
			try {
				url = task.getInputUrl(path);
				if (id == null) {
					id = task.getId();
				}
				if (url != null) {
					viewer.setInput(id, task.getName(), url, stateManager, false);
				} else {
					errorBadUrl(path);
				}
			} catch (MalformedURLException e) {
				errorBadUrl(path);
			}
		} else if (id != null){
			viewer.setInput(id, stateManager);
		} else {
			viewer.showError(Messages.CHEATSHEET_TASK_NO_ID);
		}
		if (!showIntro) {
			viewer.advanceIntroItem();
		}
		viewer.addListener(new TaskListener());
	}

	private void errorBadUrl(String path) {
		String message = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {path}));
		viewer.showError(message);
	}

	/*
	 * Listener for the cheatsheet used by this class
	 */
	private class TaskListener extends CheatSheetListener {

		@Override
		public void cheatSheetEvent(ICheatSheetEvent event) {
			if (event.getEventType() == ICheatSheetEvent.CHEATSHEET_COMPLETED) {
				task.complete();
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		viewer.saveState(memento);
	}
}
