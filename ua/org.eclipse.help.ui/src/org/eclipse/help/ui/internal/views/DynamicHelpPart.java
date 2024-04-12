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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.eclipse.help.internal.search.federated.IndexerJob;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.EscapeUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DynamicHelpPart extends SectionPart implements IHelpPart {
	private static final String CANCEL_HREF = "__cancel__"; //$NON-NLS-1$

	private static final String MORE_HREF = "__more__"; //$NON-NLS-1$

	private ReusableHelpPart parent;

	private final FormText searchResults;

	private final SorterByScore resultSorter;

	private String id;

	private String phrase;

	private Job runningJob;
	private IContext context;

	private final JobListener jobListener;
	public static final int SHORT_COUNT = 8;

	class JobListener extends JobChangeAdapter {

		@Override
		public void done(IJobChangeEvent event) {
			if (event.getJob() == runningJob) {
				runningJob = null;
			}
		}
	}

	public DynamicHelpPart(Composite parent, FormToolkit toolkit) {
		super(parent, toolkit, ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE
				| ExpandableComposite.TITLE_BAR);
		// configure section
		Section section = getSection();
		section.setText(Messages.SearchPart_title);
		section.marginWidth = 5;
		section.addExpansionListener(new ExpansionAdapter() {

			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (e.getState()) {
					refilter();
				}
			}
		});
		// create 'clear' hyperlink on the section tool bar
		//ImageHyperlink clearLink = new ImageHyperlink(section, SWT.NULL);
		//toolkit.adapt(clearLink, true, true);
		/*
		clearLink.setToolTipText(HelpUIResources
				.getString("SearchPart.clearResults")); //$NON-NLS-1$
		clearLink.setImage(HelpUIResources
				.getImage(IHelpUIConstants.IMAGE_CLEAR));
		clearLink.setBackground(section.getTitleBarGradientBackground());
		clearLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				clearResults();
			}
		});
		section.setTextClient(clearLink);
		*/
		resultSorter = new SorterByScore();
		searchResults = toolkit.createFormText(section, false);
		section.setClient(searchResults);
		searchResults.setColor(IFormColors.TITLE, toolkit.getColors().getColor(
				IFormColors.TITLE));
		String topicKey = IHelpUIConstants.IMAGE_FILE_F1TOPIC;
		String nwKey = IHelpUIConstants.IMAGE_NW;
		String searchKey = IHelpUIConstants.IMAGE_HELP_SEARCH;
		searchResults.setImage(topicKey, HelpUIResources.getImage(topicKey));
		searchResults.setImage(nwKey, HelpUIResources.getImage(nwKey));
		searchResults.setImage(searchKey, HelpUIResources.getImage(searchKey));
		searchResults.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href.equals(CANCEL_HREF)) {
					if (runningJob != null) {
						runningJob.cancel();
						runningJob = null;
					}
					clearResults();
				} else if (href.equals(MORE_HREF)) {
					doMore();
				} else
					doOpenLink(e.getHref());
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
				DynamicHelpPart.this.parent.handleLinkEntered(e);
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				DynamicHelpPart.this.parent.handleLinkExited(e);
			}
		});
		searchResults.setText("", false, false); //$NON-NLS-1$
		jobListener = new JobListener();
		Job.getJobManager().addJobChangeListener(jobListener);
	}

	@Override
	public void dispose() {
		Job.getJobManager().removeJobChangeListener(jobListener);
		stop();
		super.dispose();
	}

	@Override
	public void setFocus() {
		if (searchResults!=null)
			searchResults.setFocus();
	}

	@Override
	public void stop () {
		if (runningJob!=null) {
			runningJob.cancel();
			runningJob=null;
		}
	}

	@Override
	public Control getControl() {
		return getSection();
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		parent.hookFormText(searchResults);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		getSection().setVisible(visible);
	}

	void clearResults() {
		if (runningJob != null) {
			runningJob.cancel();
			runningJob = null;
		}
		searchResults.setText("", false, false); //$NON-NLS-1$
		getManagedForm().reflow(true);
	}

	public void startSearch(String newPhrase, IContext excludeContext) {
		if (phrase!=null && phrase.equals(newPhrase))
			return;
		this.phrase = newPhrase;
		this.context = excludeContext;
		if (getSection().isExpanded())
			startInPlaceSearch(phrase, excludeContext);
	}

	private void startInPlaceSearch(final String phrase,
			final IContext excludeContext) {
		Job job = new Job(Messages.SearchPart_dynamicJob) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					try {
						Job.getJobManager().join(IndexerJob.FAMILY,
								monitor);
					} catch (InterruptedException e) {
						// TODO should we do someting here?
					}
					performSearch(phrase, excludeContext, monitor);
					return Status.OK_STATUS;
				} catch (OperationCanceledException e) {
					// it is ok to cancel the search
					return Status.OK_STATUS;
				}
			}
		};
		job.setSystem(true);
		scheduleSearch(job);
	}

	private void performSearch(String phrase, IContext excludeContext,
			IProgressMonitor monitor) {
		SearchQuery searchQuery = new SearchQuery();
		searchQuery.setSearchWord(phrase);
		SearchResults localResults = new SearchResults(null,
				DynamicHelpPart.SHORT_COUNT * 2, Platform.getNL()) {

			@Override
			public void addHits(List<SearchHit> hits, String highlightTerms) {
				// don't highlight any terms for dynamic help part
				super.addHits(hits, ""); //$NON-NLS-1$
			}
		};
		BaseHelpSystem.getSearchManager().search(searchQuery, localResults,
				monitor);
		SearchHit[] hits = localResults.getSearchHits();
		updateResults(phrase, excludeContext, new StringBuilder(), hits);
	}

	void scheduleSearch(Job job) {
		if (runningJob != null) {
			runningJob.cancel();
		}
		StringBuilder buff = new StringBuilder();
		buff.append("<form>"); //$NON-NLS-1$
		buff.append("<p><span color=\""); //$NON-NLS-1$
		buff.append(IFormColors.TITLE);
		buff.append("\">"); //$NON-NLS-1$
		buff.append(Messages.SearchResultsPart_progress);
		buff.append("</span>"); //$NON-NLS-1$
		buff.append("<a href=\""); //$NON-NLS-1$
		buff.append(CANCEL_HREF);
		buff.append("\">"); //$NON-NLS-1$
		buff.append(Messages.SearchResultsPart_cancel);
		buff.append("</a></p>"); //$NON-NLS-1$
		buff.append("</form>"); //$NON-NLS-1$
		searchResults.setText(buff.toString(), true, false);
		getManagedForm().reflow(true);
		runningJob = job;
		job.schedule();
	}

	private void updateResults(final String phrase,
			final IContext excludeContext, final StringBuilder buffer,
			final SearchHit[] hits) {
		if (getSection().isDisposed())
			return;
		getSection().getDisplay().asyncExec(() -> doUpdateResults(phrase, excludeContext, buffer, hits));
	}

	private void doUpdateResults(String phrase, IContext excludeContext, StringBuilder buff,
			SearchHit[] hits) {
		if (runningJob != null) {
			runningJob.cancel();
		}
		this.phrase = phrase;
		buff.delete(0, buff.length());
		if (hits.length > 0) {
			buff.append("<form>"); //$NON-NLS-1$
			buff.append("<p><span color=\""); //$NON-NLS-1$
			buff.append(IFormColors.TITLE);
			buff.append("\">"); //$NON-NLS-1$
			buff.append(NLS.bind(Messages.SearchResultsPart_label, phrase));
			buff.append("</span></p>"); //$NON-NLS-1$
			resultSorter.sort(null, hits);
			IHelpResource [] excludedTopics = excludeContext!=null?excludeContext.getRelatedTopics():null;

			for (int i = 0; i < hits.length; i++) {
				SearchHit hit = hits[i];
				if (hit.canOpen()) // Do not list Welcome/Cheatsheets etc.
					continue;
				if (isExcluded(hit.getHref(), excludedTopics))
					continue;
				if (i==SHORT_COUNT)
					break;
				buff.append("<li indent=\"21\" style=\"image\" value=\""); //$NON-NLS-1$
				buff.append(IHelpUIConstants.IMAGE_FILE_F1TOPIC);
				buff.append("\">"); //$NON-NLS-1$
				buff.append("<a href=\""); //$NON-NLS-1$
				String href = hit.getHref();
				buff.append(href);
				buff.append("\""); //$NON-NLS-1$
				if (hit.getToc()!=null && !Platform.getWS().equals(Platform.WS_GTK)) {
					buff.append(" alt=\""); //$NON-NLS-1$
					buff.append(EscapeUtils.escapeSpecialChars(hit.getToc().getLabel()));
					buff.append("\""); //$NON-NLS-1$
				}
				buff.append(">"); //$NON-NLS-1$
				buff.append(EscapeUtils.escapeSpecialChars(hit.getLabel()));
				buff.append("</a>"); //$NON-NLS-1$
				buff.append("</li>"); //$NON-NLS-1$
			}
			if (hits.length > 0) {
				buff.append("<p><img href=\""); //$NON-NLS-1$
				buff.append(IHelpUIConstants.IMAGE_HELP_SEARCH);
				buff.append("\"/>"); //$NON-NLS-1$
				buff.append(" <a href=\""); //$NON-NLS-1$
				buff.append(MORE_HREF);
				buff.append("\">"); //$NON-NLS-1$
				buff.append(Messages.SearchResultsPart_moreResults);
				buff.append("</a></p>"); //$NON-NLS-1$
			}
			buff.append("</form>"); //$NON-NLS-1$
			if (!searchResults.isDisposed())
				searchResults.setText(buff.toString(), true, false);
		} else
			if (!searchResults.isDisposed())
				searchResults.setText(NLS.bind(Messages.SearchResultsPart_noHits, phrase) , false, false);
		if (!searchResults.isDisposed())
			getManagedForm().reflow(true);
	}

	private boolean isExcluded(String href, IHelpResource [] excludedTopics) {
		if (excludedTopics==null) return false;
		for (int i=0; i<excludedTopics.length; i++) {
			if (href.startsWith(excludedTopics[i].getHref()))
				return true;
			if (parent.isFilteredByRoles()) {
				if (!HelpBasePlugin.getActivitySupport().isEnabled(href))
					return true;
			}
		}
		return false;
	}

	private void doMore() {
		parent.startSearch(phrase);
	}

	private void doOpenLink(Object href) {
		String url = (String) href;

		if (url.startsWith("nw:")) { //$NON-NLS-1$
			PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(url.substring(3));
		} else
			parent.showURL(url);
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return parent.fillFormContextMenu(searchResults, manager);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return searchResults.equals(control);
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	@Override
	public void toggleRoleFilter() {
		refilter();
	}

	@Override
	public void refilter() {
		if (phrase!=null && phrase.length() > 0)
			startInPlaceSearch(phrase, context);
	}

	@Override
	public void saveState(IMemento memento) {
	}
}
