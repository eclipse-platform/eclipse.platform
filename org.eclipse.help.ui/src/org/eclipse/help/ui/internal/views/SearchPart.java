/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.federated.FederatedSearchEntry;
import org.eclipse.help.internal.search.federated.FederatedSearchJob;
import org.eclipse.help.internal.search.federated.LocalHelp;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchEngineResultCollector;
import org.eclipse.help.search.ISearchScope;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class SearchPart extends AbstractFormPart implements IHelpPart, IHelpUIConstants {

	public class SearchScopeObserver implements Observer {

		@Override
		public void update(Observable arg0, Object arg1) {
			ScopeSet set = scopeSetManager.getActiveSet();
			scopeSetLink.setText(set.getName());
			scopeSetManager.setActiveSet(set);
			scopeSection.layout();
			if (parent != null)
				parent.reflow();
		}

	}

	private ReusableHelpPart parent;

	@SuppressWarnings("rawtypes")
	protected static java.util.List previousSearchQueryData = new java.util.ArrayList(20);

	private static final String HREF_SEARCH_HELP = "/org.eclipse.platform.doc.user/tasks/help_search.htm"; //$NON-NLS-1$

	private static boolean SEARCH_HELP_AVAILABLE = false;

	static {
		InputStream is = HelpSystem.getHelpContent(HREF_SEARCH_HELP);
		if (is != null) {
			// don't leak the input stream
			try {
				is.close();
				SEARCH_HELP_AVAILABLE = true;
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private String id;

	private Composite container;

	private Composite filteringGroup;
	private FormText searchWordText;

	private ComboPart searchWordCombo;

	private Section scopeSection;

	private Button goButton;

	private Button shellDefaultButton;

	private Hyperlink scopeSetLink;

	private Hyperlink advancedLink;

	private Observer engineObserver;

	private ScopeSetManager scopeSetManager;

	private static final int COMBO_HISTORY_SIZE = 10;

	private JobListener jobListener;

	private boolean searchPending;

	private SearchScopeObserver scopeObserver;

	private Section alternateQuerySection;

	private FormToolkit toolkit;

	private Composite alternateQueryComposite;

	private class JobListener implements IJobChangeListener, Runnable {

		private boolean searchInProgress = false;

		@Override
		public void aboutToRun(IJobChangeEvent event) {
		}

		@Override
		public void awake(IJobChangeEvent event) {
		}

		@Override
		public void done(IJobChangeEvent event) {
			if (event.getJob().belongsTo(FederatedSearchJob.FAMILY)) {
				Job[] searchJobs = Job.getJobManager().find(FederatedSearchJob.FAMILY);
				if (searchJobs.length == 0) {
					// search finished
					searchInProgress = false;
					if (container.isDisposed())
						return;
					container.getDisplay().asyncExec(this);
					SearchResultsPart results = (SearchResultsPart) parent
							.findPart(IHelpUIConstants.HV_FSEARCH_RESULT);
					results.completed();
				}
			}
		}

		@Override
		public void running(IJobChangeEvent event) {
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
			if (!searchInProgress && event.getJob().belongsTo(FederatedSearchJob.FAMILY)) {
				searchInProgress = true;
				container.getDisplay().asyncExec(this);
			}
		}

		@Override
		public void sleeping(IJobChangeEvent event) {
		}

		@Override
		public void run() {
			searchWordCombo.getControl().setEnabled(!searchInProgress);
			if (!searchInProgress)
				goButton.setEnabled(true);
			if (searchInProgress)
				goButton.setText(Messages.SearchPart_stop);
			else
				goButton.setText(Messages.SearchPart_go);
			parent.getForm().getForm().setBusy(searchInProgress);
			goButton.getParent().layout();
		}
	}

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public SearchPart(final Composite parent, FormToolkit toolkit) {
		this.toolkit = toolkit;
		container = toolkit.createComposite(parent);
		scopeSetManager = ScopeState.getInstance().getScopeSetManager();
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		TableWrapData td;
		//createSearchExpressionDescription(parent, toolkit);
		createSearchExpressionSection(toolkit);
		// Pattern combo
		searchWordCombo = new ComboPart(container, toolkit, toolkit.getBorderStyle());
		updateSearchCombo(null);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 100;
		td.valign = TableWrapData.MIDDLE;
		searchWordCombo.getControl().setLayoutData(td);
		goButton = toolkit.createButton(container, Messages.SearchPart_go, SWT.PUSH);
		goButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleButtonPressed();
			}
		});
		goButton.setEnabled(false);
		searchWordCombo.addModifyListener(e -> goButton.setEnabled(searchWordCombo.getText().length() > 0));
		searchWordCombo.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == '\r') {
					if (goButton.isEnabled())  {
						doSearch(searchWordCombo.getText());
					}
				}
			}
		});
		searchWordCombo.getControl().addListener(SWT.FocusIn, event -> {
			shellDefaultButton = null;
			Shell shell = searchWordCombo.getControl().getShell();
			Button button = shell.getDefaultButton();
			if (button != null) {
				shellDefaultButton = button;
				shell.setDefaultButton(goButton);
			}
		});
		searchWordCombo.getControl().addListener(SWT.FocusOut, event -> {
			if (shellDefaultButton != null) {
				Shell shell = searchWordCombo.getControl().getShell();
				shell.setDefaultButton(shellDefaultButton);
				shellDefaultButton = null;
			}
		});

		createScopeSection(toolkit);

//		createAlternateQueriesSection(toolkit);

		toolkit.paintBordersFor(container);
		jobListener = new JobListener();
		Job.getJobManager().addJobChangeListener(jobListener);
	}

	private void createScopeSection(FormToolkit toolkit) {
		TableWrapData td;
		scopeSection = toolkit.createSection(container, Section.TWISTIE | Section.COMPACT
				| Section.LEFT_TEXT_CLIENT_ALIGNMENT);
		scopeSection.setText(Messages.limit_to);
		td = new TableWrapData();
		td.colspan = 2;
		td.align = TableWrapData.FILL;
		scopeSection.setLayoutData(td);
		filteringGroup = toolkit.createComposite(scopeSection);
		scopeSection.setClient(filteringGroup);
		TableWrapLayout flayout = new TableWrapLayout();
		flayout.numColumns = 2;
		filteringGroup.setLayout(flayout);
		createScopeSet(scopeSection, toolkit);
		toolkit.paintBordersFor(filteringGroup);
		scopeObserver = new SearchScopeObserver();
		scopeSetManager.addObserver(scopeObserver);
	}

	private void createSearchExpressionSection(FormToolkit toolkit) {
		TableWrapData td;
		Section searchExpressionSection = toolkit.createSection(container, Section.TWISTIE | Section.COMPACT
				| Section.LEFT_TEXT_CLIENT_ALIGNMENT);
		searchExpressionSection.setText(Messages.expression);
		td = new TableWrapData();
		td.colspan = 2;
		td.align = TableWrapData.FILL;
		searchExpressionSection.setLayoutData(td);
		Composite detailGroup = toolkit.createComposite(searchExpressionSection);
		searchExpressionSection.setClient(detailGroup);
		TableWrapLayout dgLayout = new TableWrapLayout();
		detailGroup.setLayout(dgLayout);
		//Label syntaxLabel = toolkit.createLabel(detailGroup, Messages.expression_label, SWT.WRAP);
		searchWordText = toolkit.createFormText(detailGroup, false);
		searchWordText.setImage(IHelpUIConstants.IMAGE_HELP, HelpUIResources
				.getImage(IHelpUIConstants.IMAGE_HELP));
		searchWordText.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				SearchPart.this.parent.showURL(HREF_SEARCH_HELP, true);
			}
		});
		updateSearchWordText();
		toolkit.paintBordersFor(detailGroup);
	}

	private void createAlternateQueriesSection(FormToolkit toolkit){

		TableWrapData td = new TableWrapData();
		td.colspan = 2;
		td.align = TableWrapData.FILL;

		container.setMenu(new Menu(container));

		alternateQuerySection = toolkit.createSection(container, Section.TWISTIE | Section.COMPACT
				| Section.LEFT_TEXT_CLIENT_ALIGNMENT);
		alternateQuerySection.setLayoutData(td);
		alternateQuerySection.setText(Messages.AlternateQueries);

		alternateQueryComposite = toolkit.createComposite(alternateQuerySection);
		alternateQuerySection.setClient(alternateQueryComposite);
		TableWrapLayout flayout = new TableWrapLayout();
		flayout.numColumns = 1;
		alternateQueryComposite.setLayout(flayout);
		alternateQuerySection.setExpanded(true);

//		alternateQuerySection.setVisible(false);
	}

	private void createAdvancedLink(Composite parent, FormToolkit toolkit) {
		advancedLink = toolkit.createHyperlink(parent, Messages.FederatedSearchPart_advanced, SWT.NULL);
		advancedLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				doAdvanced();
			}
		});
		TableWrapData td = new TableWrapData();
		td.colspan = 2;
		advancedLink.setLayoutData(td);
	}

	private void createScopeSet(Section section, FormToolkit toolkit) {
		scopeSetLink = toolkit.createHyperlink(section, null, SWT.WRAP);
		scopeSetLink.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				doChangeScopeSet();
			}
		});
		scopeSetLink.setToolTipText(Messages.FederatedSearchPart_changeScopeSet);
		section.setTextClient(scopeSetLink);
		ScopeSet active = scopeSetManager.getActiveSet();
		setActiveScopeSet(active);
	}

	private void updateSearchWordText() {
		StringBuilder buff = new StringBuilder();
		buff.append("<form>"); //$NON-NLS-1$
		buff.append("<p>"); //$NON-NLS-1$
		buff.append(Messages.expression_label);
			// Only add the link if available
			if (SEARCH_HELP_AVAILABLE) {
				buff.append("</p><p>"); //$NON-NLS-1$
				buff.append("<img href=\""); //$NON-NLS-1$
				buff.append(IHelpUIConstants.IMAGE_HELP);
				buff.append("\"/> "); //$NON-NLS-1$
				buff.append("<a href=\""); //$NON-NLS-1$
				buff.append(HREF_SEARCH_HELP);
				buff.append("\">"); //$NON-NLS-1$
				buff.append(Messages.SearchPart_learnMore);
				buff.append("</a>"); //$NON-NLS-1$
			}

		buff.append("</p>"); //$NON-NLS-1$
		buff.append("</form>"); //$NON-NLS-1$
		searchWordText.setText(buff.toString(), true, false);
	}

	private void setActiveScopeSet(ScopeSet set) {
		scopeSetLink.setText(set.getName());
		scopeSetManager.setActiveSet(set);
		updateMasters(set);
		scopeSection.layout();
		if (parent != null)
			parent.reflow();
	}

	private void updateMasters(ScopeSet set) {
		Control[] children = ((Composite) scopeSection.getClient()).getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			if (child instanceof Button) {
				Button master = (Button) child;
				Object data = master.getData();
				if (data != null && data instanceof EngineDescriptor) {
					EngineDescriptor ed = (EngineDescriptor) data;
					master.setSelection(set.getEngineEnabled(ed));
				}
			}
		}
	}

	private void loadEngines(final Composite container, final FormToolkit toolkit) {
		EngineDescriptorManager descManager = parent.getEngineManager();
		EngineDescriptor[] descriptors = descManager.getDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			EngineDescriptor desc = descriptors[i];
			loadEngine(desc, container, toolkit);
		}
		engineObserver = (o, arg) -> {
			EngineDescriptorManager.DescriptorEvent event = (EngineDescriptorManager.DescriptorEvent) arg;
			int kind = event.getKind();
			EngineDescriptor desc = event.getDescriptor();
			if (kind == IHelpUIConstants.ADD) {
				advancedLink.dispose();
				loadEngine(desc, container, toolkit);
				createAdvancedLink(container, toolkit);
				parent.reflow();
			} else if (kind == IHelpUIConstants.REMOVE) {
				removeEngine(desc);
			} else {
				updateEngine(desc);
			}
		};

		descManager.addObserver(engineObserver);
		updateMasters(scopeSetManager.getActiveSet());
	}

	private EngineDescriptor loadEngine(final EngineDescriptor edesc, Composite container, FormToolkit toolkit) {
		Label ilabel = toolkit.createLabel(container, null);
		ilabel.setImage(edesc.getIconImage());
		ilabel.setData(edesc);
		final Button master = toolkit.createButton(container, edesc.getLabel(), SWT.CHECK);
		master.setData(edesc);
		master.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				scopeSetManager.getActiveSet().setEngineEnabled(edesc, master.getSelection());
			}
		});
		String desc = edesc.getDescription();
		if (desc != null) {
			Label spacer = toolkit.createLabel(container, null);
			spacer.setData(edesc);
			Label dlabel = toolkit.createLabel(container, desc, SWT.WRAP);
			dlabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
			dlabel.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
			dlabel.setMenu(container.getMenu());
			dlabel.setData(edesc);
		}
		return edesc;
	}

	private void removeEngine(EngineDescriptor desc) {
		boolean reflowNeeded = false;
		Control[] children = ((Composite) scopeSection.getClient()).getChildren();
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			EngineDescriptor ed = (EngineDescriptor) child.getData();
			if (ed == desc) {
				child.setMenu(null);
				child.dispose();
				reflowNeeded = true;
			}
		}
		if (reflowNeeded)
			parent.reflow();
	}

	private void updateEngine(EngineDescriptor desc) {
		Control[] children = ((Composite) scopeSection.getClient()).getChildren();
		boolean reflowNeeded = false;
		for (int i = 0; i < children.length; i++) {
			Control child = children[i];
			EngineDescriptor ed = (EngineDescriptor) child.getData();
			if (ed == desc) {
				Button b = (Button) children[i + 1];
				b.setText(desc.getLabel());
				Label d = (Label) children[i + 3];
				d.setText(desc.getDescription());
				d.getParent().layout();
				reflowNeeded = true;
				break;
			}
		}
		if (reflowNeeded)
			parent.reflow();
	}

	public void startSearch(String text) {
		searchWordCombo.setText(text);
		doSearch(text);
	}

	private void storeSearchHistory(String expression) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95479
		HistoryScopeSet sset = scopeSetManager.findSearchSet(expression);
		if (sset == null) {
			sset = new HistoryScopeSet(expression);
			scopeSetManager.add(sset);
		}
		ScopeSet activeSet = scopeSetManager.getActiveSet();
		sset.copyFrom(activeSet);
		sset.save();
		updateSearchCombo(sset);
		searchWordCombo.setText(expression);
	}

	private void updateSearchCombo(HistoryScopeSet current) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=95479
		ScopeSet[] sets = scopeSetManager.getScopeSets(true);
		ArrayList<String> items = new ArrayList<>();
		ArrayList<HistoryScopeSet> toDelete = new ArrayList<>();
		// if (current!=null)
		// items.add(current.getExpression());
		for (int i = sets.length - 1; i >= 0; i--) {
			HistoryScopeSet sset = (HistoryScopeSet) sets[i];
			if (current != null && sset == current)
				continue;
			if (sets.length - i > COMBO_HISTORY_SIZE)
				toDelete.add(sset);
			items.add(sset.getExpression());
		}
		for (int i = 0; i < toDelete.size(); i++) {
			HistoryScopeSet sset = toDelete.get(i);
			scopeSetManager.remove(sset);
		}
		if (items.size() > 0)
			searchWordCombo.setItems(items.toArray(new String[items.size()]));
	}

	private void handleButtonPressed() {
		if (searchWordCombo.getControl().isEnabled())
			doSearch(searchWordCombo.getText());
		else {
			goButton.setEnabled(false);
			stop();
		}
	}

	private void doSearch(String text) {
		doSearch(text, false);
	}

	private void doSearch(String text, boolean fromHistory) {
		ScopeSet set = scopeSetManager.getActiveSet();
		if (!fromHistory && set instanceof HistoryScopeSet) {
			String setExpression = ((HistoryScopeSet) set).getExpression();
			if (setExpression.equals(text))
				fromHistory = true;
		}
		if (!fromHistory) {
			storeSearchHistory(text);
			boolean switchedSet = scopeSetManager.restoreLastExplicitSet();
			set = scopeSetManager.getActiveSet();
			if (switchedSet)
				setActiveScopeSet(set);
		}
		ArrayList<FederatedSearchEntry> entries = new ArrayList<>();
		final SearchResultsPart results = (SearchResultsPart) parent
				.findPart(IHelpUIConstants.HV_FSEARCH_RESULT);
		ArrayList<EngineDescriptor> eds = new ArrayList<>();
		EngineDescriptor[] engineDescriptors = parent.getEngineManager().getDescriptors();
		for (int i = 0; i < engineDescriptors.length; i++) {
			final EngineDescriptor ed = engineDescriptors[i];
			if (set.getEngineEnabled(ed) && ed.getEngine() != null) {
				ISearchScope scope = ed.createSearchScope(set.getPreferenceStore());
				FederatedSearchEntry entry = new FederatedSearchEntry(ed.getId(), ed.getLabel(), scope, ed
						.getEngine(), new ISearchEngineResultCollector() {

							@Override
					public void accept(ISearchEngineResult searchResult) {
						results.add(ed, searchResult);
					}

							@Override
					public void accept(ISearchEngineResult[] searchResults) {
						results.add(ed, searchResults);
						if (ed.getEngine() instanceof LocalHelp && !container.isDisposed())
						{
							container.getDisplay().asyncExec(new Thread() {

								@Override
								public void run(){
									if (alternateQuerySection!=null)
									{
										alternateQuerySection.dispose();
										alternateQuerySection = null;
									}
									List<String> alts = ((LocalHelp) ed.getEngine()).getAlternates();
									if (!alts.isEmpty())
									{
										createAlternateQueriesSection(toolkit);
										for (int b=0;b<alts.size();b++)
										{
											Hyperlink link = toolkit.createHyperlink(
													alternateQueryComposite, alts.get(b), SWT.NONE);
											link.addHyperlinkListener(new HyperlinkAdapter(){
												@Override
												public void linkActivated(HyperlinkEvent e) {
													if (!searchWordCombo.getControl().isDisposed()) {
														searchWordCombo.setText(((Hyperlink)e.getSource()).getText());
														doSearch(((Hyperlink)e.getSource()).getText());
													}
												}
											});
										}
									}
								}
							});
						}
					}

							@Override
					public void error(IStatus status) {
						results.error(ed, status);
					}
				});
				entries.add(entry);
				eds.add(ed);
			}
		}
		if (entries.isEmpty())
			return;
		FederatedSearchEntry[] array = entries.toArray(new FederatedSearchEntry[entries.size()]);
		if (scopeSection.isExpanded()) {
			scopeSection.setExpanded(false);
			parent.reflow();
		}
		results.clearResults();
		results.startNewSearch(text, eds);
		BaseHelpSystem.getSearchManager().search(text, array);
	}

	private void doAdvanced() {
		ScopeSet set = scopeSetManager.getActiveSet();
		PreferenceManager manager = new ScopePreferenceManager(parent.getEngineManager(), set);
		PreferenceDialog dialog = new ScopePreferenceDialog(container.getShell(), manager, parent
				.getEngineManager(), set.isEditable());
		dialog.setPreferenceStore(set.getPreferenceStore());
		dialog.create();
		dialog.getShell().setText(NLS.bind(Messages.ScopePreferenceDialog_wtitle, set.getName()));
		dialog.open();
		updateMasters(set);
	}

	private void doChangeScopeSet() {
		ScopeSetDialog dialog = new ScopeSetDialog(container.getShell(), scopeSetManager, parent
				.getEngineManager(), false);
		dialog.setInput(scopeSetManager);
		dialog.create();
		dialog.getShell().setText(Messages.ScopeSetDialog_wtitle);
		if (dialog.open() == ScopeSetDialog.OK) {
			ScopeSet set = dialog.getActiveSet();
			if (set != null) {
				setActiveScopeSet(set);
			}
			scopeSetManager.save();
			scopeSetManager.notifyObservers();
		}
	}

	@Override
	public void dispose() {
		ScopeSet activeSet = scopeSetManager.getActiveSet();
		if (activeSet != null)
			activeSet.save();
		if (engineObserver != null) {
			parent.getEngineManager().deleteObserver(engineObserver);
			engineObserver = null;
		}

		if (scopeObserver != null) {
			ScopeState.getInstance().getScopeSetManager().deleteObserver(scopeObserver);
		}

		Job.getJobManager().removeJobChangeListener(jobListener);
		stop();
		super.dispose();
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		loadEngines(filteringGroup, parent.getForm().getToolkit());
		createAdvancedLink(filteringGroup, parent.getForm().getToolkit());
		parent.hookFormText(searchWordText);
		if (memento != null)
			restorePart(memento);
	}

	private void restorePart(IMemento memento) {
		String setName = memento.getString("activeSet"); //$NON-NLS-1$
		if (setName != null) {
			ScopeSet sset = scopeSetManager.findSet(setName);
			if (sset != null)
				scopeSetManager.setActiveSet(sset);
		}
		String expression = memento.getString("expression"); //$NON-NLS-1$
		if (expression != null && expression.length() > 0) {
			searchWordCombo.setText(expression);
			searchPending = true;
			markStale();
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		if (searchPending) {
			searchPending = false;
			doSearch(searchWordCombo.getText());
		}

	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		getControl().setVisible(visible);
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return parent.fillFormContextMenu(searchWordText, manager);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return control == searchWordText || control == searchWordCombo.getControl()
				|| scopeSection.getClient() == control;
	}

	@Override
	public void setFocus() {
		searchWordCombo.getControl().setFocus();
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	@Override
	public void stop() {
		SearchResultsPart results = (SearchResultsPart) parent.findPart(IHelpUIConstants.HV_FSEARCH_RESULT);
		if (results != null) {
			results.canceling();
		}
		Job.getJobManager().cancel(FederatedSearchJob.FAMILY);
	}

	@Override
	public void toggleRoleFilter() {
	}

	@Override
	public void refilter() {
	}

	@Override
	public void saveState(IMemento memento) {
		ScopeSet sset = scopeSetManager.getActiveSet();
		if (sset != null)
			memento.putString("activeSet", sset.getName()); //$NON-NLS-1$
		memento.putString("expression", searchWordCombo.getText()); //$NON-NLS-1$
	}
}
