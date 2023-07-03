/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.model;

/**
 * The concrete implementation of an editable task
 */

import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.cheatsheets.composite.parser.EditableTaskParseStrategy;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ITaskParseStrategy;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.TaskEditor;

public class EditableTask extends AbstractTask implements IEditableTask {

	private TaskEditor editor;

	private ITaskParseStrategy parserStrategy;

	private boolean editorInitialized = false;

	private boolean underReview = false;

	public EditableTask(CompositeCheatSheetModel model, String id, String name, String kind) {
		super(model, id, name, kind);
		parserStrategy = new EditableTaskParseStrategy();
	}

	@Override
	public ITaskParseStrategy getParserStrategy() {
		return parserStrategy;
	}

	@Override
	public ICompositeCheatSheetTask[] getSubtasks() {
		return EMPTY;
	}

	public void setStarted() {
		if (state==NOT_STARTED) {
			setState(IN_PROGRESS);
		}
	}

	public void setEditor(TaskEditor editor) {
		this.editor = editor;
	}

	public TaskEditor getEditor() {
		return editor;
	}

	public void reset() {
		setStateNoNotify(NOT_STARTED);
		editorInitialized = false;
	}

	public void setInput(IMemento memento) {
		if (editor != null) {
			editor.setInput(this, memento);
			editorInitialized = true;
		}
	}

	public boolean isEditorInitialized() {
		return editorInitialized;
	}

	public void setUnderReview(boolean underReview) {
		this.underReview = underReview;
	}

	public boolean isUnderReview() {
		return underReview;
	}

}
