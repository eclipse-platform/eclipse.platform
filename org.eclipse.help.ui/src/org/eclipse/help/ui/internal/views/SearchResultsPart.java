/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IToc;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.osgi.service.prefs.BackingStoreException;

public class SearchResultsPart extends AbstractFormPart implements IHelpPart {
	ReusableHelpPart parent;

	private Composite separator;

	private Composite container;

	private ScrolledForm innerForm;

	private String id;

	//private Action removeAllAction;

	private Action showCategoriesAction;

	private Action showDescriptionAction;

	private ArrayList<EngineResultSection> results;

	//private String phrase;

	private FormToolkit innerToolkit;

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public SearchResultsPart(Composite parent, FormToolkit toolkit,
			IToolBarManager tbm) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		innerToolkit = new FormToolkit(parent.getDisplay());
		innerToolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				toolkit.getHyperlinkGroup().getHyperlinkUnderlineMode());
		container = innerToolkit.createComposite(parent);
		container.setLayout(layout);
		separator = innerToolkit.createCompositeSeparator(container);
		separator.setVisible(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 1;
		separator.setLayoutData(gd);
		innerForm = innerToolkit.createScrolledForm(container);
		innerForm.setDelayedReflow(true);
		innerForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		final ScrollBar scrollBar = innerForm.getVerticalBar();
		scrollBar.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSeparatorVisibility();
			}
		});
		TableWrapLayout tlayout = new TableWrapLayout();
		tlayout.topMargin = 0;
		tlayout.bottomMargin = 0;
		innerForm.getBody().setLayout(tlayout);
		results = new ArrayList<>();
		contributeToToolBar(tbm);
	}

	void updateSeparatorVisibility() {
		ScrollBar scrollBar = innerForm.getVerticalBar();
		separator.setVisible(scrollBar.getSelection()>0);
	}

	private void contributeToToolBar(IToolBarManager tbm) {
		/*
		 * removeAllAction = new Action() { public void run() { clearResults(); } };
		 * removeAllAction.setImageDescriptor(HelpUIResources
		 * .getImageDescriptor(IHelpUIConstants.IMAGE_REMOVE_ALL));
		 * removeAllAction.setToolTipText("Remove all hits");
		 * removeAllAction.setId("removeAll"); tbm.insertBefore("back",
		 * removeAllAction); tbm.insertAfter("removeAll", new Separator());
		 */

		boolean descOn = Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SHOW_SEARCH_DESCRIPTION, false, null);
		boolean showCategories = Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SHOW_SEARCH_CATEGORIES, false, null);
		showCategoriesAction = new Action() {

			@Override
			public void run() {
				updateResultSections();
				IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
				pref.putBoolean(IHelpBaseConstants.P_KEY_SHOW_SEARCH_CATEGORIES, showCategoriesAction.isChecked());
				try {
					pref.flush();
				} catch (BackingStoreException e) {
				}
			}
		};
		showCategoriesAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_SHOW_CATEGORIES));
		showCategoriesAction.setChecked(showCategories);
		showCategoriesAction.setToolTipText(Messages.SearchResultsPart_showCategoriesAction_tooltip);
		showCategoriesAction.setId("categories"); //$NON-NLS-1$
		tbm.insertBefore("back", showCategoriesAction); //$NON-NLS-1$

		showDescriptionAction = new Action() {

			@Override
			public void run() {
				updateResultSections();
				IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
				pref.putBoolean(IHelpBaseConstants.P_KEY_SHOW_SEARCH_DESCRIPTION, showDescriptionAction.isChecked());
				try {
					pref.flush();
				} catch (BackingStoreException e) {
				}
			}
		};
		showDescriptionAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_SHOW_DESC));
		showDescriptionAction.setChecked(descOn);
		showDescriptionAction.setToolTipText(Messages.SearchResultsPart_showDescriptionAction_tooltip);
		showDescriptionAction.setId("description"); //$NON-NLS-1$
		tbm.insertAfter("categories", showDescriptionAction); //$NON-NLS-1$
		tbm.insertAfter("description", new Separator()); //$NON-NLS-1$
	}

	@Override
	public void dispose() {
		innerToolkit.dispose();
		super.dispose();
	}

	private void updateResultSections() {
		BusyIndicator.showWhile(container.getDisplay(), () -> {
			for (int i = 0; i < results.size(); i++) {
				EngineResultSection section = results.get(i);
				section.updateResults(false);
			}
			reflow();
		});
	}

	boolean getShowCategories() {
		return showCategoriesAction.isChecked();
	}

	boolean getShowDescription() {
		return showDescriptionAction.isChecked();
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		container.setVisible(visible);
	}

	void clearResults() {
		clearResultSections();
		separator.setVisible(false);
		reflow();
	}

	void clearResultSections() {
		for (int i = 0; i < results.size(); i++) {
			EngineResultSection section = results.get(i);
			section.dispose();
		}
		results.clear();
	}

	void startNewSearch(String phrase, ArrayList<EngineDescriptor> eds) {
		//this.phrase = phrase;
		//separator.setVisible(true);
		// locate local help engine and add it first
		EngineDescriptor localHelp = findLocalHelp(eds);
		if (localHelp!=null)
			add(localHelp);
		// add engines other than local help
		for (int i = 0; i < eds.size(); i++) {
			EngineDescriptor ed = eds.get(i);
			if (ed==localHelp)
				continue;
			add(ed);
		}
		reflow();
	}

	private EngineDescriptor findLocalHelp(ArrayList<EngineDescriptor> eds) {
		for (int i=0; i<eds.size(); i++) {
			EngineDescriptor ed = eds.get(i);
			if (ed.getEngineTypeId().equals(IHelpUIConstants.INTERNAL_HELP_ID))
				return ed;
		}
		return null;
	}

	void completed() {
		for (int i = 0; i < results.size(); i++) {
			EngineResultSection section = results.get(i);
			section.completed();
		}
	}

	void canceling() {
		for (int i = 0; i < results.size(); i++) {
			EngineResultSection section = results.get(i);
			section.canceling();
		}
	}

	void doOpenLink(Object href) {
		String url = (String) href;

		if (url.startsWith("nw:")) { //$NON-NLS-1$
			parent.showExternalURL(url.substring(3));
		} else
			parent.showURL(url);
	}

	void doCategoryLink(String href) {
		parent.showPage(IHelpUIConstants.HV_ALL_TOPICS_PAGE);
		AllTopicsPart part = (AllTopicsPart) parent
				.findPart(IHelpUIConstants.HV_TOPIC_TREE);
		if (part != null) {
			IToc[] tocs = HelpSystem.getTocs();
			IHelpResource target = null;
			for (int i = 0; i < tocs.length; i++) {
				if (tocs[i].getHref().equals(href))
					target = tocs[i];
			}
			if (target != null) {
				part.selectReveal(target);
			}
		}
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		Control focusControl = container.getDisplay().getFocusControl();
		if (focusControl != null && focusControl instanceof FormText) {
			return parent.fillFormContextMenu((FormText) focusControl, manager);
		}
		return false;
	}

	@Override
	public boolean hasFocusControl(Control control) {
		for (int i = 0; i < results.size(); i++) {
			EngineResultSection er = results.get(i);
			if (er.hasControl(control))
				return true;
		}
		return false;
	}

	public synchronized void add(EngineDescriptor ed, ISearchEngineResult match) {
		EngineResultSection ers = findEngineResult(ed);
		if (match != null)
			ers.add(match);
	}

	public synchronized void add(EngineDescriptor ed,
			ISearchEngineResult[] matches) {
		EngineResultSection ers = findEngineResult(ed);
		ers.add(matches);
	}

	public synchronized void error(EngineDescriptor ed, IStatus status) {
		EngineResultSection ers = findEngineResult(ed);
		ers.error(status);
	}

	private synchronized EngineResultSection findEngineResult(
			EngineDescriptor ed) {
		for (int i = 0; i < results.size(); i++) {
			EngineResultSection er = results.get(i);
			if (er.matches(ed))
				return er;
		}
		final EngineResultSection er = new EngineResultSection(this, ed);
		Display display = parent.getForm().getToolkit().getColors()
				.getDisplay();
		display.syncExec(() -> {
			Control c = er.createControl(innerForm.getBody(), innerToolkit);
			c.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		});
		results.add(er);
		return er;
	}

	private void add(EngineDescriptor ed) {
		final EngineResultSection er = new EngineResultSection(this, ed);
		Control c = er.createControl(innerForm.getBody(), innerToolkit);
		c.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		results.add(er);
	}

	void reflow() {
		innerForm.reflow(true);
		parent.reflow();
	}

	@Override
	public boolean setFormInput(Object input) {
		return false;
	}

	void scrollToBeginning() {
		innerForm.setOrigin(0, 0);
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	FormToolkit getToolkit() {
		return innerToolkit;
	}

	@Override
	public void stop() {
	}

	@Override
	public void toggleRoleFilter() {
		updateResultSections();
	}

	@Override
	public void refilter() {
		updateResultSections();
	}

	@Override
	public void saveState(IMemento memento) {
	}
}