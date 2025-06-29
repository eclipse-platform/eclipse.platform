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
package org.eclipse.compare.internal;

import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.NavigationAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;


public class CompareEditorContributor extends EditorActionBarContributor {

	public final static String FILTER_SEPARATOR = "compare.filters"; //$NON-NLS-1$
	public final static String BUILTIN_SEPARATOR = "compare.builtin"; //$NON-NLS-1$

	private IEditorPart fActiveEditorPart= null;

	private final ChangePropertyAction fIgnoreWhitespace;
	private final NavigationAction fNext;
	private final NavigationAction fPrevious;

	private final NavigationAction fToolbarNext;
	private final NavigationAction fToolbarPrevious;

	public CompareEditorContributor() {
		ResourceBundle bundle= CompareUI.getResourceBundle();

		fIgnoreWhitespace= ChangePropertyAction.createIgnoreWhiteSpaceAction(bundle, null);
		fNext= new NavigationAction(bundle, true);
		fPrevious= new NavigationAction(bundle, false);
		fToolbarNext= new NavigationAction(bundle, true);
		fToolbarPrevious= new NavigationAction(bundle, false);

		if (PlatformUI.isWorkbenchRunning()) {
			IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
			helpSystem.setHelp(fIgnoreWhitespace, ICompareContextIds.IGNORE_WHITESPACE_ACTION);
			helpSystem.setHelp(fNext, ICompareContextIds.GLOBAL_NEXT_DIFF_ACTION);
			helpSystem.setHelp(fPrevious, ICompareContextIds.GLOBAL_PREVIOUS_DIFF_ACTION);
			helpSystem.setHelp(fToolbarNext, ICompareContextIds.NEXT_DIFF_ACTION);
			helpSystem.setHelp(fToolbarPrevious, ICompareContextIds.PREVIOUS_DIFF_ACTION);
		}
	}

	@Override
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(new Separator(FILTER_SEPARATOR));
		tbm.add(new Separator(BUILTIN_SEPARATOR));
		tbm.appendToGroup(BUILTIN_SEPARATOR, fIgnoreWhitespace);
		tbm.appendToGroup(BUILTIN_SEPARATOR, fToolbarNext);
		tbm.appendToGroup(BUILTIN_SEPARATOR, fToolbarPrevious);
	}

	@Override
	public void contributeToMenu(IMenuManager menuManager) {
		// empty implementation
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {

		if (fActiveEditorPart == targetEditor) {
			return;
		}

		fActiveEditorPart= targetEditor;

		if (fActiveEditorPart != null) {
			IEditorInput input= fActiveEditorPart.getEditorInput();
			if (input instanceof CompareEditorInput compareInput) {
				fNext.setCompareEditorInput(compareInput);
				fPrevious.setCompareEditorInput(compareInput);
				// Begin fix http://bugs.eclipse.org/bugs/show_bug.cgi?id=20105
				fToolbarNext.setCompareEditorInput(compareInput);
				fToolbarPrevious.setCompareEditorInput(compareInput);
				// End fix http://bugs.eclipse.org/bugs/show_bug.cgi?id=20105
			}
		}

		if (targetEditor instanceof CompareEditor editor) {
			IActionBars actionBars= getActionBars();

			editor.setActionBars(actionBars);

			actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNext);
			actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPrevious);

			actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, fNext);
			actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, fPrevious);

			CompareConfiguration cc= editor.getCompareConfiguration();
			fIgnoreWhitespace.setCompareConfiguration(cc);

			IContributionItem[] items = actionBars.getToolBarManager()
					.getItems();
			boolean inFilters = false;
			for (IContributionItem item : items) {
				if (item.getId().equals(FILTER_SEPARATOR)) {
					inFilters = true;
				} else if (item.getId().equals(BUILTIN_SEPARATOR)) {
					break;
				} else if (inFilters) {
					if (item instanceof ActionContributionItem) {
						String definitionId = ((ActionContributionItem) item).getAction().getActionDefinitionId();
						if (definitionId != null) {
							actionBars.setGlobalActionHandler(definitionId,
									null);
						}
					}
					actionBars.getToolBarManager().remove(item);
				}
			}

			IEditorInput input = editor.getEditorInput();
			if (input instanceof CompareEditorInput
					&& ((CompareEditorInput) input).getCompareConfiguration() != null) {
				Object filterActions = ((CompareEditorInput) input)
						.getCompareConfiguration()
						.getProperty(
								ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS);
				if (filterActions instanceof List
						&& !((List<?>) filterActions).isEmpty()) {
					Iterator<?> i = ((List<?>) filterActions).iterator();
					while (i.hasNext()) {
						Object next = i.next();
						if (next instanceof ChangeCompareFilterPropertyAction) {
							actionBars.getToolBarManager().appendToGroup(
									FILTER_SEPARATOR,
									(ChangeCompareFilterPropertyAction) next);
							String definitionId = ((ChangeCompareFilterPropertyAction) next)
									.getActionDefinitionId();
							if (definitionId != null) {
								actionBars
										.setGlobalActionHandler(
												definitionId,
												(ChangeCompareFilterPropertyAction) next);
							}
						}
					}
					actionBars.getToolBarManager().markDirty();
					actionBars.getToolBarManager().update(true);
					actionBars.updateActionBars();
				}
			}
		} else {
			IActionBars actionBars= getActionBars();
			actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), null);
			actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), null);
			actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, null);
			actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, null);
		}
	}

	@Override
	public void dispose() {
		setActiveEditor(null);
		super.dispose();
		fIgnoreWhitespace.dispose();
	}
}
