/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.MissingContentManager;
import org.eclipse.help.internal.base.util.LinkUtil;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.search.federated.IndexerJob;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.search.ISearchEngine2;
import org.eclipse.help.ui.internal.DefaultHelpUI;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubMenuManager;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.action.SubToolBarManager;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.activities.ActivityManagerEvent;
import org.eclipse.ui.activities.IActivityManagerListener;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ILayoutExtension;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ReusableHelpPart implements IHelpUIConstants, IActivityManagerListener {
	public static final int ALL_TOPICS = 1 << 1;

	public static final int CONTEXT_HELP = 1 << 2;

	public static final int SEARCH = 1 << 3;

	public static final int BOOKMARKS = 1 << 4;

	public static final int INDEX = 1 << 5;

	public static final Collator SHARED_COLLATOR = Collator.getInstance();

	private static final String PROMPT_KEY = "askShowAll"; //$NON-NLS-1$

	private static final int STATE_START = 1;

	private static final int STATE_LT = 2;

	private static final int STATE_LT_B = 3;

	private static final int STATE_LT_BR = 4;

	/*
	 * Used as a bridge from live help actions back (e.g. breadcrumb links)
	 * to the originating help part.
	 */
	private static ReusableHelpPart lastActiveInstance;

	private RoleFilter roleFilter;

	private UAFilter uaFilter;

	private ManagedForm mform;

	private final int verticalSpacing = 15;

	private final int bmargin = 5;

	private String defaultContextHelpText;

	private ArrayList<IHelpPartPage> pages;

	private Action backAction;

	private Action nextAction;

	private CopyAction copyAction;

	private Action openInfoCenterAction;

	private OpenHrefAction openAction;

	private OpenHrefAction openInHelpAction;

	private OpenHrefAction bookmarkAction;

	private Action showAllAction;

	private final ReusableHelpPartHistory history;

	private HelpPartPage currentPage;

	private final int style;

	private IMemento memento;

	private boolean showDocumentsInPlace = true;

	private int numberOfInPlaceHits = 8;

	private final IRunnableContext runnableContext;

	private IToolBarManager toolBarManager;

	private IStatusLineManager statusLineManager;

	private IActionBars actionBars;

	private EngineDescriptorManager engineManager;

	public IMenuManager menuManager;

	private abstract class BusyRunAction extends Action {
		public BusyRunAction(String name) {
			super(name);
		}

		@Override
		public void run() {
			BusyIndicator.showWhile(getControl().getDisplay(), this::busyRun);
		}

		protected abstract void busyRun();
	}

	private abstract class OpenHrefAction extends BusyRunAction {
		private Object target;

		public OpenHrefAction(String name) {
			super(name);
		}

		public void setTarget(Object target) {
			this.target = target;
		}

		public Object getTarget() {
			return target;
		}
	}

	private class CopyAction extends Action implements FocusListener,
			SelectionListener {
		private FormText target;

		public CopyAction() {
			super("copy"); //$NON-NLS-1$
		}

		public void hook(final FormText text) {
			text.addFocusListener(this);
		}

		public void unhook(FormText text) {
			text.removeFocusListener(this);
			if (target == text)
				setTarget(null);
		}

		@Override
		public void focusGained(FocusEvent e) {
			FormText text = (FormText) e.widget;
			text.addSelectionListener(this);
			setTarget(text);
		}

		@Override
		public void focusLost(FocusEvent e) {
			FormText text = (FormText) e.widget;
			text.removeSelectionListener(this);
			setTarget(null);
		}

		public void setTarget(FormText target) {
			this.target = target;
			updateState();
		}

		private void updateState() {
			setEnabled(target != null && target.canCopy());
		}

		@Override
		public void run() {
			if (target != null)
				target.copy();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			FormText text = (FormText) e.widget;
			if (text == target) {
				updateState();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	private static class PartRec {
		String id;

		boolean flexible;

		boolean grabVertical;

		IHelpPart part;

		PartRec(String id, boolean flexible, boolean grabVertical) {
			this.id = id;
			this.flexible = flexible;
			this.grabVertical = grabVertical;
		}
	}

	private class HelpPartPage implements IHelpPartPage {
		private final String id;

		private String iconId;

		Action pageAction;

		private int vspacing = verticalSpacing;

		private int horizontalMargin = 0;

		private final String text;

		private SubActionBars bars;

		private IToolBarManager subToolBarManager;

		private IMenuManager subMenuManager;

		protected ArrayList<PartRec> partRecs;

		private int nflexible;

		private boolean updateActionBars = true;

		public HelpPartPage(String id, String text) {
			this.id = id;
			this.text = text;
			partRecs = new ArrayList<>();
			if (ReusableHelpPart.this.actionBars != null) {
				// Help View
				bars = new SubActionBars(ReusableHelpPart.this.actionBars);
				subToolBarManager = bars.getToolBarManager();
				subMenuManager = bars.getMenuManager();
			} else {
				// Help Tray
				subToolBarManager = new SubToolBarManager(
						ReusableHelpPart.this.toolBarManager);
				if (ReusableHelpPart.this.menuManager != null) {
					subMenuManager = new SubMenuManager(
						ReusableHelpPart.this.menuManager);
				} else {
					subMenuManager = null;
				}
			}
		}

		public HelpPartPage(String id, String text, String iconId) {
			this(id, text);
			this.iconId = iconId;
		}

		@Override
		public void dispose() {
			if (bars != null) {
				bars.dispose();
				bars = null;
				subToolBarManager = null;
				subMenuManager = null;
			} else {
				try {
					((SubToolBarManager) subToolBarManager).disposeManager();
					if (subMenuManager != null) {
						((SubMenuManager)subMenuManager).disposeManager();
					}
				} catch (RuntimeException e) {
					// Bug 218079
				}
			}
			partRecs = null;
		}

		@Override
		public void setVerticalSpacing(int value) {
			this.vspacing = value;
		}

		@Override
		public int getVerticalSpacing() {
			return vspacing;
		}

		@Override
		public void setHorizontalMargin(int value) {
			this.horizontalMargin = value;
		}

		@Override
		public int getHorizontalMargin() {
			return horizontalMargin;
		}

		@Override
		public IToolBarManager getToolBarManager() {
			return subToolBarManager;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getText() {
			return text;
		}

		@Override
		public String getIconId() {
			return iconId;
		}

		@Override
		public void addPart(String id, boolean flexible) {
			addPart(id, flexible, false);
		}

		@Override
		public void addPart(String id, boolean flexible, boolean grabVertical) {
			partRecs.add(new PartRec(id, flexible, grabVertical));
			if (flexible)
				nflexible++;
		}

		public PartRec[] getParts() {
			return partRecs.toArray(new PartRec[partRecs.size()]);
		}

		public void refreshPage()
		{
			PartRec parts[] = getParts();
			if (parts==null)
				return;

			for (int p=0;p<parts.length;p++)
				if (parts[p]!=null && parts[p].part!=null && parts[p].part.isStale())
					parts[p].part.refresh();
		}

		@Override
		public int getNumberOfFlexibleParts() {
			return nflexible;
		}

		@Override
		public boolean canOpen() {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);

				if (rec.id.equals(IHelpUIConstants.HV_BROWSER)) {
					// Try to create a browser and watch
					// for 'no-handle' error - it means
					// that the embedded browser is not
					// available
					try {
						createRecPart(rec);
						rec.part.setVisible(false);
					} catch (SWTError error) {
						// cannot create a browser
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public void stop() {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (rec.part!=null)
					rec.part.stop();
			}
		}

		@Override
		public void saveState(IMemento memento) {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (rec.part!=null)
					rec.part.saveState(memento);
			}
		}

		@Override
		public void toggleRoleFilter() {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (rec.part != null)
					rec.part.toggleRoleFilter();
			}
		}

		@Override
		public void refilter() {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (rec.part != null)
					rec.part.refilter();
			}
		}

		@Override
		public void setVisible(boolean visible) {
			if (bars != null)
				bars.clearGlobalActionHandlers();
			ArrayList<Control> tabList = new ArrayList<>();
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (visible) {
					createRecPart(rec);
					hookGlobalAction(ActionFactory.PRINT.getId(), rec.part);
					hookGlobalAction(ActionFactory.COPY.getId(), rec.part);
					hookGlobalAction(ActionFactory.DELETE.getId(), rec.part);
					tabList.add(rec.part.getControl());
				}
				rec.part.setVisible(visible);
			}
			Composite parent = mform.getForm().getBody();
			parent.setTabList(tabList.toArray(new Control[tabList.size()]));

			if (actionBars != null) {
				actionBars.clearGlobalActionHandlers();
				if (visible) {
					Map<String, IAction> handlers = bars.getGlobalActionHandlers();
					if (handlers != null) {
						Set<String> keys = handlers.keySet();
						for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
							String key = iter.next();
							actionBars.setGlobalActionHandler(key, handlers.get(key));
						}
					}
				}
				if (pageAction != null)
					pageAction.setChecked(visible);
			}

			if (bars != null) {
				if (visible)
					bars.activate();
				else
					bars.deactivate();

				if (this.updateActionBars) {
					bars.updateActionBars();
				}
			} else {
				((SubToolBarManager) subToolBarManager).setVisible(visible);
				if (subMenuManager != null) {
					((SubMenuManager)subMenuManager).setVisible(visible);
				}
				ReusableHelpPart.this.toolBarManager.update(true);
				getControl().getParent().layout();
			}

		}

		private void hookGlobalAction(String id, IHelpPart part) {
			if (bars == null)
				return;
			IAction action = part.getGlobalAction(id);
			if (action != null)
				bars.setGlobalActionHandler(id, action);
		}

		private void createRecPart(PartRec rec) throws SWTError {
			if (rec.part == null) {
				rec.part = createPart(rec.id, subToolBarManager, subMenuManager);
			}
		}

		@Override
		public IHelpPart findPart(String id) {
			for (int i = 0; i < partRecs.size(); i++) {
				PartRec rec = partRecs.get(i);
				if (rec.id.equals(id))
					return rec.part;
			}
			return null;
		}

		@Override
		public void setFocus() {
			// Focus on the first part that is not the see also links or missing content link
			for (int focusPart = 0; focusPart < partRecs.size(); focusPart++) {
				PartRec rec = partRecs.get(focusPart);
				String partId = rec.part.getId();
				if ( partId != IHelpUIConstants.HV_SEE_ALSO && partId != IHelpUIConstants.HV_MISSING_CONTENT) {
					rec.part.setFocus();
					return;
				}
			}
		}

	}

	class HelpPartLayout extends Layout implements ILayoutExtension {

		@Override
		public int computeMaximumWidth(Composite parent, boolean changed) {
			return computeSize(parent, SWT.DEFAULT, SWT.DEFAULT, changed).x;
		}

		@Override
		public int computeMinimumWidth(Composite parent, boolean changed) {
			return computeSize(parent, 0, SWT.DEFAULT, changed).x;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint,
				boolean flushCache) {
			if (currentPage == null)
				return new Point(0, 0);
			PartRec[] parts = currentPage.getParts();
			int hmargin = currentPage.getHorizontalMargin();
			int innerWhint = wHint != SWT.DEFAULT ? wHint - 2 * hmargin : wHint;
			Point result = new Point(0, 0);
			for (int i = 0; i < parts.length; i++) {
				PartRec partRec = parts[i];
				if (!partRec.flexible) {
					Control c = partRec.part.getControl();
					Point size = c.computeSize(innerWhint, SWT.DEFAULT,
							flushCache);
					result.x = Math.max(result.x, size.x);
					result.y += size.y;
				}
				if (i < parts.length - 1)
					result.y += currentPage.getVerticalSpacing();
			}
			result.x += hmargin * 2;
			result.y += bmargin;
			return result;
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			if (currentPage == null)
				return;

			Rectangle clientArea = composite.getClientArea();

			PartRec[] parts = currentPage.getParts();
			int hmargin = currentPage.getHorizontalMargin();
			int nfixedParts = parts.length
					- currentPage.getNumberOfFlexibleParts();
			Point[] fixedSizes = new Point[nfixedParts];
			int fixedHeight = 0;
			int index = 0;
			int innerWidth = clientArea.width - hmargin * 2;
			for (int i = 0; i < parts.length; i++) {
				PartRec partRec = parts[i];
				if (!partRec.flexible) {
					Control c = partRec.part.getControl();
					Point size = c.computeSize(innerWidth, SWT.DEFAULT, false);
					fixedSizes[index++] = size;
					if (!partRec.grabVertical)
						fixedHeight += size.y;
				}
				if (i < parts.length - 1)
					fixedHeight += currentPage.getVerticalSpacing();
			}
			fixedHeight += bmargin;
			int flexHeight = clientArea.height - fixedHeight;
			int flexPortion = 0;
			if (currentPage.getNumberOfFlexibleParts() > 0)
				flexPortion = flexHeight
						/ currentPage.getNumberOfFlexibleParts();

			int usedFlexHeight = 0;
			int y = 0;
			index = 0;
			int nflexParts = 0;
			for (int i = 0; i < parts.length; i++) {
				PartRec partRec = parts[i];
				Control c = partRec.part.getControl();

				if (partRec.flexible) {
					int height;
					if (++nflexParts == currentPage.getNumberOfFlexibleParts())
						height = flexHeight - usedFlexHeight;
					else {
						height = flexPortion;
						usedFlexHeight += height;
					}
					c.setBounds(0, y, clientArea.width, height);
				} else {
					Point fixedSize = fixedSizes[index++];
					if (fixedSize.y < flexHeight && partRec.grabVertical)
						c.setBounds(hmargin, y, innerWidth, flexHeight);
					else
						c.setBounds(hmargin, y, innerWidth, fixedSize.y);
				}
				if (i < parts.length - 1)
					y += c.getSize().y + currentPage.getVerticalSpacing();
			}
		}
	}

	static class RoleFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			IHelpResource res = (IHelpResource) element;
			String href = res.getHref();
			if (href == null)
				return true;
			return HelpBasePlugin.getActivitySupport().isEnabled(href);
		}
	}

	static class UAFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return !UAContentFilter.isFiltered(element, HelpEvaluationContext.getContext());
		}
	}

	public ReusableHelpPart(IRunnableContext runnableContext) {
		this(runnableContext, getDefaultStyle());
	}

	public ReusableHelpPart(IRunnableContext runnableContext, int style) {
		this.runnableContext = runnableContext;
		history = new ReusableHelpPartHistory();
		this.style = style;
		ensureHelpIndexed();
		PlatformUI.getWorkbench().getActivitySupport().getActivityManager().addActivityManagerListener(this);
	}

	/*
	 * Used as a bridge from live help actions back (e.g. breadcrumb links)
	 * to the originating help part.
	 */
	public static ReusableHelpPart getLastActiveInstance() {
		return lastActiveInstance;
	}

	private void ensureHelpIndexed() {
		// make sure we have the index but
		// don't schedule the indexer job if one is
		// already running
		Job[] jobs = Job.getJobManager().find(IndexerJob.FAMILY);
		if (jobs.length == 0) {
			IndexerJob indexerJob = new IndexerJob();
			indexerJob.schedule();
		}
	}

	/**
	 * Adds the given page to this part.
	 *
	 * @param page the page to add
	 */
	public void addPage(IHelpPartPage page) {
		pages.add(page);
	}

	/**
	 * Adds the given part to this one. The part can then be used inside
	 * any page and referred to by id.
	 *
	 * @param id the part's unique id
	 * @param part the part to add
	 */
	public void addPart(String id, IHelpPart part) {
		part.init(this, id, memento);
		mform.addPart(part);
	}

	/**
	 * Creates a new page for this part.
	 *
	 * @param id the page's unique id
	 * @param text the page's heading, or null for none
	 * @param iconId the page's icon
	 * @return the newly created page
	 */
	public IHelpPartPage createPage(String id, String text, String iconId) {
		return new HelpPartPage(id, text, iconId);
	}

	private void definePages() {
		pages = new ArrayList<>();
		// federated search page
		HelpPartPage page = new HelpPartPage(HV_FSEARCH_PAGE,
	 			Messages.ReusableHelpPart_searchPage_name,
	 			IHelpUIConstants.IMAGE_HELP_SEARCH);
		page.setVerticalSpacing(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_MISSING_CONTENT, false);
		page.addPart(HV_FSEARCH, false);
		page.addPart(HV_FSEARCH_RESULT, true);
		pages.add(page);

		// all topics page
		page = new HelpPartPage(HV_ALL_TOPICS_PAGE,
				Messages.ReusableHelpPart_allTopicsPage_name,
				IHelpUIConstants.IMAGE_ALL_TOPICS);
		page.setVerticalSpacing(0);
		page.setHorizontalMargin(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_MISSING_CONTENT, false);
		page.addPart(HV_SCOPE_SELECT, false);
		page.addPart(HV_TOPIC_TREE, true);
		pages.add(page);

		// bookmarks page
		page = new HelpPartPage(HV_BOOKMARKS_PAGE,
				Messages.ReusableHelpPart_bookmarksPage_name,
				IHelpUIConstants.IMAGE_BOOKMARKS);
		page.setVerticalSpacing(0);
		page.setHorizontalMargin(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_BOOKMARKS_HEADER, false);
		page.addPart(HV_BOOKMARKS_TREE, true);
		pages.add(page);
		// browser page
		page = new HelpPartPage(HV_BROWSER_PAGE, null);
		page.setVerticalSpacing(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_BROWSER, true);
		pages.add(page);

		// context help page
		page = new HelpPartPage(HV_CONTEXT_HELP_PAGE,
				Messages.ReusableHelpPart_contextHelpPage_name,
				IHelpUIConstants.IMAGE_RELATED_TOPICS);
		// page.addPart(HV_CONTEXT_HELP, false);
		// page.addPart(HV_SEARCH_RESULT, false, true);
		page.setVerticalSpacing(0);
		page.setHorizontalMargin(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_MISSING_CONTENT, false);
		page.addPart(HV_RELATED_TOPICS, true);
		pages.add(page);

		// index page
		page = new HelpPartPage(HV_INDEX_PAGE,
				Messages.ReusableHelpPart_indexPage_name,
				IHelpUIConstants.IMAGE_INDEX);
		page.setVerticalSpacing(0);
		page.addPart(HV_SEE_ALSO, false);
		page.addPart(HV_MISSING_CONTENT, false);
		page.addPart(HV_SCOPE_SELECT, false);
		page.addPart(HV_INDEX_TYPEIN, false);
		page.addPart(HV_INDEX, true);
		pages.add(page);
	}

	public void init(IActionBars bars, IToolBarManager toolBarManager,
			IStatusLineManager statusLineManager, IMenuManager menuManager, IMemento memento) {
		this.memento = memento;
		this.actionBars = bars;
		this.toolBarManager = toolBarManager;
		this.menuManager = menuManager;
		this.statusLineManager = statusLineManager;
		definePages();
		makeActions();
	}

	private void makeActions() {
		backAction = new Action("back") { //$NON-NLS-1$

			@Override
			public void run() {
				doBack();
			}
		};
		backAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_BACK));
		backAction.setDisabledImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_BACK_DISABLED));
		backAction.setEnabled(false);
		backAction.setText(Messages.ReusableHelpPart_back_label);
		backAction.setToolTipText(Messages.ReusableHelpPart_back_tooltip);
		backAction.setId("back"); //$NON-NLS-1$

		nextAction = new Action("next") { //$NON-NLS-1$

			@Override
			public void run() {
				doNext();
			}
		};
		nextAction.setText(Messages.ReusableHelpPart_forward_label);
		nextAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_FORWARD));
		nextAction.setDisabledImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_FORWARD_DISABLED));
		nextAction.setEnabled(false);
		nextAction.setToolTipText(Messages.ReusableHelpPart_forward_tooltip);
		nextAction.setId("next"); //$NON-NLS-1$
		toolBarManager.add(backAction);
		toolBarManager.add(nextAction);

		openInfoCenterAction = new BusyRunAction("openInfoCenter") { //$NON-NLS-1$

			@Override
			protected void busyRun() {
				PlatformUI.getWorkbench().getHelpSystem().displayHelp();
			}
		};
		openInfoCenterAction
				.setText(Messages.ReusableHelpPart_openInfoCenterAction_label);
		openAction = new OpenHrefAction("open") { //$NON-NLS-1$

			@Override
			protected void busyRun() {
				doOpen(getTarget(), getShowDocumentsInPlace());
			}
		};
		openAction.setText(Messages.ReusableHelpPart_openAction_label);
		openInHelpAction = new OpenHrefAction("") {//$NON-NLS-1$

			@Override
			protected void busyRun() {
				doOpenInHelp(getTarget());
			}
		};
		openInHelpAction
				.setText(Messages.ReusableHelpPart_openInHelpContentsAction_label);
		copyAction = new CopyAction();
		copyAction.setText(Messages.ReusableHelpPart_copyAction_label);
		bookmarkAction = new OpenHrefAction("bookmark") { //$NON-NLS-1$

			@Override
			protected void busyRun() {
				doBookmark(getTarget());
			}
		};
		bookmarkAction.setText(Messages.ReusableHelpPart_bookmarkAction_label);
		bookmarkAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_ADD_BOOKMARK));
		if (actionBars != null && actionBars.getMenuManager() != null)
			contributeToDropDownMenu(actionBars.getMenuManager());

		roleFilter = new RoleFilter();
		uaFilter = new UAFilter();
		if (HelpBasePlugin.getActivitySupport().isUserCanToggleFiltering()) {
			showAllAction = new Action() {

				@Override
				public void run() {
					BusyIndicator.showWhile(getControl().getDisplay(),
							() -> toggleShowAll(showAllAction.isChecked()));
				}
			};
			showAllAction.setImageDescriptor(HelpUIResources
					.getImageDescriptor(IHelpUIConstants.IMAGE_SHOW_ALL));
			showAllAction
					.setToolTipText(Messages.AllTopicsPart_showAll_tooltip);
			toolBarManager.insertBefore("back", showAllAction); //$NON-NLS-1$
			toolBarManager.insertBefore("back", new Separator()); //$NON-NLS-1$
			showAllAction.setChecked(!HelpBasePlugin.getActivitySupport()
					.isFilteringEnabled());
		}
	}

	ViewerFilter getRoleFilter() {
		return roleFilter;
	}

	ViewerFilter getUAFilter() {
		return uaFilter;
	}

	@Override
	public void activityManagerChanged(ActivityManagerEvent activityManagerEvent) {
		// pages is null when the activity manager listener is added, and is set to null
		// prior to the activity manager listener being removed, so very short timeframes in
		// logic where pages could equals null entering this method
		if (pages != null){
			for (int i = 0; i < pages.size(); i++) {
				HelpPartPage page = (HelpPartPage) pages.get(i);
				page.refilter();
			}
		}
	}

	boolean isFilteredByRoles() {
		return HelpBasePlugin.getActivitySupport().isFilteringEnabled();
	}

	private void doBack() {
		String id = getCurrentPageId();
		if (id.equals(IHelpUIConstants.HV_BROWSER_PAGE)) {
			// stop the browser
			BrowserPart part = (BrowserPart) findPart(IHelpUIConstants.HV_BROWSER);
			part.stop();
		}
		HistoryEntry entry = history.prev();
		if (entry != null)
			executeHistoryEntry(entry);
	}

	private void doNext() {
		HistoryEntry entry = history.next();
		if (entry != null)
			executeHistoryEntry(entry);
	}

	private void executeHistoryEntry(HistoryEntry entry) {
		history.setBlocked(true);
		if (entry.getType() == HistoryEntry.PAGE) {
			showPage(entry.getTarget(), true);
			mform.setInput(entry.getData());
		} else if (entry.getType() == HistoryEntry.URL) {
			String relativeUrl = (String) entry.getData();
			showURL(relativeUrl != null ? relativeUrl : entry.getTarget(), true);
		}
	}

	public void createControl(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		form.getBody().setLayout(new HelpPartLayout());
		mform = new ManagedForm(toolkit, form);
		mform.getForm().setDelayedReflow(false);
		toolkit.decorateFormHeading(mform.getForm().getForm());
		MenuManager manager = new MenuManager();
		IMenuListener listener = this::contextMenuAboutToShow;
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(listener);
		Menu contextMenu = manager.createContextMenu(form.getForm());
		form.getForm().setMenu(contextMenu);
		form.addListener(SWT.Activate, event -> lastActiveInstance = ReusableHelpPart.this);
		//contributeToDropDownMenu(mform.getForm().getForm().getMenuManager());
	}

	public HelpPartPage showPage(String id) {
		String currentPageId = currentPage == null ? null : currentPage.getId();
		if (id.equals(currentPageId))
			return currentPage;
		// If navigating away from the browser page clear
		// its contents
		if (IHelpUIConstants.HV_BROWSER_PAGE.equals(currentPageId)) {
			BrowserPart part = (BrowserPart) findPart(IHelpUIConstants.HV_BROWSER);
			part.clearBrowser();
		}

		HelpPartPage page = findPage(id);
		if (page != null) {
			page.refreshPage();
			boolean success = flipPages(currentPage, page);
			return success ? page : null;
		}
		return null;
	}

	public HelpPartPage showPage(String id, boolean setFocus) {
		HelpPartPage page = this.showPage(id);
		if (page != null && setFocus)
			page.setFocus();
		return page;
	}

	public void startSearch(String phrase) {
		showPage(IHelpUIConstants.HV_FSEARCH_PAGE, true);
		SearchPart part = (SearchPart) findPart(IHelpUIConstants.HV_FSEARCH);
		if (part != null && phrase != null)
			part.startSearch(phrase);
	}

	public void showDynamicHelp(IWorkbenchPart wpart, Control c) {
		showPage(IHelpUIConstants.HV_CONTEXT_HELP_PAGE, true);
		RelatedTopicsPart part = (RelatedTopicsPart) findPart(IHelpUIConstants.HV_RELATED_TOPICS);
		if (part != null) {
			part.handleActivation(c, wpart);
		}
	}

	private boolean flipPages(HelpPartPage oldPage, HelpPartPage newPage) {
		if (!newPage.canOpen())
			return false;
		if (oldPage != null) {
			oldPage.stop();
			oldPage.updateActionBars = false;
			oldPage.setVisible(false);
			oldPage.updateActionBars = true;
		}

		newPage.updateActionBars = false;
		newPage.setVisible(true);
		newPage.updateActionBars = true;
		mform.getForm().setText(null); //(newPage.getText());
		mform.getForm().getForm().setSeparatorVisible(newPage.getText()!=null);
		Image newImage=null;
		//String iconId = newPage.getIconId();
		//if (iconId != null)
			//newImage = HelpUIResources.getImage(iconId);
		mform.getForm().setImage(newImage);

		toolBarManager.update(true);
		currentPage = newPage;
		if (mform.isStale())
			mform.refresh();
		mform.getForm().getBody().layout(true);
		mform.reflow(true);
		if (!newPage.getId().equals(IHelpUIConstants.HV_BROWSER_PAGE)) {
			if (!history.isBlocked()) {
				history.addEntry(new HistoryEntry(HistoryEntry.PAGE, newPage
						.getId(), null));
			}
			updateNavigation();
		}

		return true;
	}

	/*
	 * void addPageHistoryEntry(String id, Object data) { if
	 * (!history.isBlocked()) { history.addEntry(new
	 * HistoryEntry(HistoryEntry.PAGE, id, data)); } updateNavigation(); }
	 */
	public HelpPartPage getCurrentPage() {
		return currentPage;
	}

	public String getCurrentPageId() {
		return currentPage != null ? currentPage.getId() : null;
	}

	void browserChanged(String url) {
		if (!history.isBlocked()) {
			try {
				history.addEntry(new HistoryEntry(HistoryEntry.URL, url,
						BaseHelpSystem.unresolve(new URL(url))));
			} catch (MalformedURLException e) {
				// Do not add to history
			}
		}
		updateNavigation();
	}

	private void updateNavigation() {
		backAction.setEnabled(history.hasPrev());
		nextAction.setEnabled(history.hasNext());
		history.setBlocked(false);
	}

	public boolean isMonitoringContextHelp() {
		return currentPage != null && (currentPage.getId().equals(HV_CONTEXT_HELP_PAGE)
				|| currentPage.getId().equals(HV_BROWSER_PAGE));
	}

	public Control getControl() {
		return mform.getForm();
	}

	public ManagedForm getForm() {
		return mform;
	}

	public void reflow() {
		mform.getForm().getBody().layout();
		mform.reflow(true);
	}

	public void dispose() {
		if (lastActiveInstance == this) {
			lastActiveInstance = null;
		}
		for (int i = 0; i < pages.size(); i++) {
			HelpPartPage page = (HelpPartPage) pages.get(i);
			page.dispose();
		}
		pages = null;
		if (mform != null) {
			mform.dispose();
			mform = null;
		}
		PlatformUI.getWorkbench().getActivitySupport().getActivityManager()
				.removeActivityManagerListener(this);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.internal.intro.impl.parts.IStandbyContentPart#setFocus()
	 */
	public void setFocus() {
		if (currentPage != null)
			currentPage.setFocus();
		else
			mform.setFocus();
	}

	public void update(IWorkbenchPart part, Control control) {
		update(null, null, part, control, false);
	}

	/**
	 * Called to update the related topics page in response to a
	 * @param isExplicitRequest is true if this is the result of a direct user request such as
	 * pressing F1 and false if it is in response to a focus change listener
	 */
	public void update(IContextProvider provider, IContext context, IWorkbenchPart part, Control control,
			boolean isExplicitRequest) {
		IContext realContext = context;
		if (provider != null) {
			realContext = provider.getContext(control);
		}
		if (realContext != null) {
			String contextText = realContext.getText();
			IHelpResource[] topics = realContext.getRelatedTopics();
			if (contextText == null && topics.length == 1) {
				showURL(topics[0].getHref());
				return;
			}
		}
		// Ensure that context help is currently showing
		showPage(IHelpUIConstants.HV_CONTEXT_HELP_PAGE);
		mform.setInput(new ContextHelpProviderInput(provider, context, control, part, isExplicitRequest));
	}

	private IHelpPart createPart(String id, IToolBarManager tbm, IMenuManager menuManager) {
		IHelpPart part = null;
		Composite parent = mform.getForm().getBody();

		part = findPart(id);
		if (part != null)
			return part;

		if (id.equals(HV_TOPIC_TREE)) {
			part = new AllTopicsPart(parent, mform.getToolkit(), tbm);
		} else if (id.equals(HV_CONTEXT_HELP)) {
			part = new ContextHelpPart(parent, mform.getToolkit());
			((ContextHelpPart) part)
					.setDefaultText(getDefaultContextHelpText());
		} else if (id.equals(HV_RELATED_TOPICS)) {
			part = new RelatedTopicsPart(parent, mform.getToolkit());
			((RelatedTopicsPart) part)
					.setDefaultText(getDefaultContextHelpText());
		} else if (id.equals(HV_BROWSER)) {
			part = new BrowserPart(parent, mform.getToolkit(), tbm, menuManager);
		} else if (id.equals(HV_SEARCH_RESULT)) {
			part = new DynamicHelpPart(parent, mform.getToolkit());
		} else if (id.equals(HV_FSEARCH_RESULT)) {
			part = new SearchResultsPart(parent, mform.getToolkit(), tbm);
		} else if (id.equals(HV_SCOPE_SELECT)) {
			part = new ScopeSelectPart(parent, mform.getToolkit());
		} else if (id.equals(HV_SEE_ALSO)) {
			part = new SeeAlsoPart(parent, mform.getToolkit());
		} else if (id.equals(HV_FSEARCH)) {
			part = new SearchPart(parent, mform.getToolkit());
		} else if (id.equals(HV_BOOKMARKS_HEADER)) {
			part = new BookmarkHeaderPart(parent, mform.getToolkit());
		} else if (id.equals(HV_BOOKMARKS_TREE)) {
			part = new BookmarksPart(parent, mform.getToolkit(), tbm);
		} else if (id.equals(HV_INDEX)) {
			part = new IndexPart(parent, mform.getToolkit(), tbm);
		} else if (id.equals(HV_INDEX_TYPEIN)) {
			part = new IndexTypeinPart(parent, mform.getToolkit(), tbm);
		} else if (id.equals(HV_MISSING_CONTENT)) {
			part = new MissingContentPart(parent, mform.getToolkit());
		}
		if (part != null) {
			mform.addPart(part);
			part.init(this, id, memento);
		}
		return part;
	}

	/**
	 * @return Returns the runnableContext.
	 */
	public IRunnableContext getRunnableContext() {
		return runnableContext;
	}

	public boolean isInWorkbenchWindow() {
		return runnableContext instanceof IWorkbenchWindow;
	}

	/**
	 * @return Returns the defaultContextHelpText.
	 */
	public String getDefaultContextHelpText() {
		return defaultContextHelpText;
	}

	/**
	 * @param defaultContextHelpText
	 *            The defaultContextHelpText to set.
	 */
	public void setDefaultContextHelpText(String defaultContextHelpText) {
		this.defaultContextHelpText = defaultContextHelpText;
	}

	public void showURL(final String url) {
		BusyIndicator.showWhile(getControl().getDisplay(), () -> showURL(url, getShowDocumentsInPlace()));
	}

	public void showURL(String url, boolean replace) {
		if (url == null)
			return;
		if (url.startsWith("nw:")) { //$NON-NLS-1$
			replace = false;
			url = url.substring(3);
		}
		else if (url.startsWith("open:")) { //$NON-NLS-1$
			int col = url.indexOf(':');
			int qloc = url.indexOf('?');
			String engineId = url.substring(col+1, qloc);
			EngineDescriptor desc = getEngineManager().findEngine(engineId);
			if (desc==null)
				return;
			HashMap<String, Object> args = new HashMap<>();
			HelpURLConnection.parseQuery(url.substring(qloc+1), args);
			((ISearchEngine2)desc.getEngine()).open((String)args.get("id")); //$NON-NLS-1$
			return;
		}
		if (replace) {
			if (openInternalBrowser(url))
				return;
		}
		showExternalURL(url);
	}

	private boolean openInternalBrowser(String url) {
		String openMode = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID,
				 IHelpBaseConstants.P_KEY_HELP_VIEW_OPEN_MODE, IHelpBaseConstants.P_IN_PLACE, null);
		boolean openInEditor = IHelpBaseConstants.P_IN_EDITOR.equals(openMode);
		boolean openInBrowser = IHelpBaseConstants.P_IN_BROWSER.equals(openMode);
		Shell windowShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Shell helpShell = mform.getForm().getShell();
		boolean isDialog = (helpShell != windowShell);
		if (!isDialog && openInEditor) {
			return DefaultHelpUI.showInWorkbenchBrowser(url, true);
		}
		if (openInBrowser) {
			BaseHelpSystem.getHelpDisplay().displayHelpResource(url, false);
			return true;
		}
		showPage(IHelpUIConstants.HV_BROWSER_PAGE);
		BrowserPart bpart = (BrowserPart) findPart(IHelpUIConstants.HV_BROWSER);
		if (bpart != null) {
			bpart.showURL(BaseHelpSystem
					.resolve(url, "/help/ntopic").toString()); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	public void showExternalURL(String url) {
		if (isHelpResource(url))
			PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(url);
		else {
			try {
				String aurl = BaseHelpSystem.resolve(url, true).toString();
				/*
				/* Previous code before fix for Bug 192750
				if (aurl.endsWith("&noframes=true") || aurl.endsWith("?noframes=true")) //$NON-NLS-1$ //$NON-NLS-2$
					aurl = aurl.substring(0, aurl.length() - 14);
				DefaultHelpUI.showInWorkbenchBrowser(aurl, false);
				*/

				PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(aurl);

			} catch (Exception e) {
				ILog.of(getClass()).error("Error opening browser", e); //$NON-NLS-1$
			}
		}
	}

	public IHelpPart findPart(String id) {
		if (mform == null)
			return null;
		IFormPart[] parts = mform.getParts();
		for (int i = 0; i < parts.length; i++) {
			IHelpPart part = (IHelpPart) parts[i];
			if (part.getId().equals(id))
				return part;
		}
		return null;
	}

	public boolean isHelpResource(String url) {
		if (url == null || !url.contains("://")) //$NON-NLS-1$
			return true;
		return false;
	}

	private void contextMenuAboutToShow(IMenuManager manager) {
		IFormPart[] parts = mform.getParts();
		boolean hasContext = false;
		Control focusControl = getControl().getDisplay().getFocusControl();
		for (int i = 0; i < parts.length; i++) {
			IHelpPart part = (IHelpPart) parts[i];
			if (part.hasFocusControl(focusControl)) {
				hasContext = part.fillContextMenu(manager);
				break;
			}
		}
		if (hasContext)
			manager.add(new Separator());
		manager.add(backAction);
		manager.add(nextAction);
		manager.add(new Separator());
		manager.add(openInfoCenterAction);
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		addPageAction(manager, IHelpUIConstants.HV_CONTEXT_HELP_PAGE);
		addPageAction(manager, IHelpUIConstants.HV_ALL_TOPICS_PAGE);
		addPageAction(manager, IHelpUIConstants.HV_INDEX_PAGE);
		addPageAction(manager, IHelpUIConstants.HV_FSEARCH_PAGE);
		addPageAction(manager, IHelpUIConstants.HV_BOOKMARKS_PAGE);
	}

	private void addPageAction(IMenuManager manager, final String pageId) {
		// String cid = getCurrentPageId();
		HelpPartPage page = findPage(pageId);
		if (page == null)
			return;
		Action action = new Action(pageId, IAction.AS_CHECK_BOX) {

			@Override
			public void run() {
				BusyIndicator.showWhile(mform.getForm().getDisplay(),
						() -> showPage(pageId));
			}
		};
		action.setText(page.getText());
		String iconId = page.getIconId();
		if (iconId != null)
			action.setImageDescriptor(HelpUIResources
					.getImageDescriptor(iconId));
		manager.add(action);
		page.pageAction = action;
	}

	private HelpPartPage findPage(String id) {
		for (int i = 0; i < pages.size(); i++) {
			HelpPartPage page = (HelpPartPage) pages.get(i);
			if (page.getId().equals(id)) {
				return page;
			}
		}
		return null;
	}

	boolean fillSelectionProviderMenu(ISelectionProvider provider,
			IMenuManager manager, boolean addBookmarks) {
		boolean value = fillOpenActions(provider, manager);
		if (value && addBookmarks) {
			manager.add(new Separator());
			bookmarkAction.setTarget(provider);
			manager.add(bookmarkAction);
		}
		return true;
	}

	private boolean fillOpenActions(Object target, IMenuManager manager) {
		String href = getHref(target);
		if (href != null && !href.startsWith("__")) { //$NON-NLS-1$
			openAction.setTarget(target);
			manager.add(openAction);
			if (!href.startsWith("nw:") && !href.startsWith("open:")) { //$NON-NLS-1$ //$NON-NLS-2$
				openInHelpAction.setTarget(target);
				manager.add(openInHelpAction);
			}
			return true;
		}
		return false;
	}

	void hookFormText(FormText text) {
		copyAction.hook(text);
	}

	void unhookFormText(FormText text) {
		copyAction.unhook(text);
	}

	boolean fillFormContextMenu(FormText text, IMenuManager manager) {
		if (fillOpenActions(text, manager))
			manager.add(new Separator());
		manager.add(copyAction);
		copyAction.setTarget(text);
		if (text.getSelectedLinkHref() != null) {
			manager.add(new Separator());
			manager.add(bookmarkAction);
			bookmarkAction.setTarget(getResource(text));
		}
		return true;
	}

	IAction getCopyAction() {
		return copyAction;
	}

	private String getHref(Object target) {
		if (target instanceof ISelectionProvider) {
			ISelectionProvider provider = (ISelectionProvider) target;
			IStructuredSelection ssel = (IStructuredSelection) provider
					.getSelection();
			Object obj = ssel.getFirstElement();
			if (obj instanceof IToc)
				return null;
			if (obj instanceof IHelpResource) {
				IHelpResource res = (IHelpResource) obj;
				return res.getHref();
			}
			if (obj instanceof IIndexEntry) {
				/*
				 * if index entry has single topic
				 * it represents the topic by itself
				 */
				IHelpResource[] topics = ((IIndexEntry) obj).getTopics();
				if (topics.length == 1)
					return topics[0].getHref();
				return null;
			}
		} else if (target instanceof FormText) {
			FormText text = (FormText) target;
			Object href = text.getSelectedLinkHref();
			if (href != null)
				return href.toString();
		}
		return null;
	}

	private IHelpResource getResource(Object target) {
		if (target instanceof ISelectionProvider) {
			ISelectionProvider provider = (ISelectionProvider) target;
			IStructuredSelection ssel = (IStructuredSelection) provider
					.getSelection();
			Object obj = ssel.getFirstElement();
			if (obj instanceof ITopic) {
				return (ITopic) obj;
			} else if (obj instanceof IIndexEntry) {
				/*
				 * if index entry has single topic
				 * it represents the topic by itself
				 */
				IIndexEntry entry = (IIndexEntry) obj;
				IHelpResource[] topics = entry.getTopics();
				if (topics.length == 1) {
					final String href = topics[0].getHref();
					final String label = entry.getKeyword();
					return new IHelpResource() {

						@Override
						public String getHref() {
							return href;
						}

						@Override
						public String getLabel() {
							return label;
						}
					};
				}
				return null;
			} else if (obj instanceof IHelpResource) {
				return (IHelpResource) obj;
			}
		} else if (target instanceof FormText) {
			FormText text = (FormText) target;
			String rawHref = text.getSelectedLinkHref().toString();
			final String href = rawHref.startsWith("open") ? rawHref : //$NON-NLS-1$
				LinkUtil.stripParams(text.getSelectedLinkHref().toString());
			final String label = text.getSelectedLinkText();
			if (href != null) {
				return new IHelpResource() {

					@Override
					public String getHref() {
						return href;
					}

					@Override
					public String getLabel() {
						return label;
					}
				};
			}
		}
		return null;
	}

	private void doBookmark(Object target) {
		IHelpResource res = null;
		if (target instanceof IHelpResource) {
			res = (IHelpResource)target;
		}
		else {
			res = getResource(target);
		}
		if (res != null) {
			BaseHelpSystem.getBookmarkManager().addBookmark(res.getHref(),
					res.getLabel());
		}
	}


	private void doOpen(Object target, boolean replace) {
		String href = getHref(target);
		if (href != null)
			showURL(href, replace);
	}

	private void doOpenInHelp(Object target) {
		String href = getHref(target);
		if (href != null)
			showURL(href, false);
	}

	/**
	 * @return Returns the statusLineManager.
	 */
	public IStatusLineManager getStatusLineManager() {
		return statusLineManager;
	}

	/**
	 * @return Returns the showDocumentsInPlace.
	 */
	public boolean getShowDocumentsInPlace() {
		return showDocumentsInPlace;
	}

	/**
	 * @param showDocumentsInPlace
	 *            The showDocumentsInPlace to set.
	 */
	public void setShowDocumentsInPlace(boolean showDocumentsInPlace) {
		this.showDocumentsInPlace = showDocumentsInPlace;
	}

	/**
	 * @return Returns the style.
	 */
	public int getStyle() {
		return style;
	}

	public int getNumberOfInPlaceHits() {
		return numberOfInPlaceHits;
	}

	public void setNumberOfInPlaceHits(int numberOfInPlaceHits) {
		this.numberOfInPlaceHits = numberOfInPlaceHits;
	}

	void handleLinkEntered(HyperlinkEvent e) {
		IStatusLineManager mng = getRoot(getStatusLineManager());
		if (mng != null) {
			String label = e.getLabel();
			String href = (String) e.getHref();
			if (href != null && href.startsWith("__")) //$NON-NLS-1$
				href = null;
			if (href != null) {
				href = URLDecoder.decode(href, StandardCharsets.UTF_8);
				// Next line unnecessary following fix for Bug 78746
				//href = href.replaceAll("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (label != null && href != null) {
				String message = NLS.bind(Messages.ReusableHelpPart_status,
						label, href);
				mng.setMessage(message);
			} else if (label != null)
				mng.setMessage(label);
			else
				mng.setMessage(href);
		}
	}

	private IStatusLineManager getRoot(IStatusLineManager mng) {
		while (mng != null) {
			if (mng instanceof SubStatusLineManager) {
				SubStatusLineManager smng = (SubStatusLineManager) mng;
				IContributionManager parent = smng.getParent();
				if (parent == null)
					return smng;
				if (!(parent instanceof IStatusLineManager))
					return smng;
				mng = (IStatusLineManager) parent;
			} else
				break;
		}
		return mng;
	}

	void handleLinkExited(HyperlinkEvent e) {
		IStatusLineManager mng = getRoot(getStatusLineManager());
		if (mng != null)
			mng.setMessage(null);
	}



	private void toggleShowAll(boolean checked) {
		if (checked) {
			IPreferenceStore store = HelpUIPlugin.getDefault()
					.getPreferenceStore();
			String value = store.getString(PROMPT_KEY);
			if (value.length() == 0) {
				MessageDialogWithToggle dialog = MessageDialogWithToggle
						.openOkCancelConfirm(null,
								Messages.AskShowAll_dialogTitle,
								getShowAllMessage(),
								Messages.AskShowAll_toggleMessage, false,
								store, PROMPT_KEY);
				if (dialog.getReturnCode() != Window.OK) {
					showAllAction.setChecked(false);
					return;
				}
			}
		}
		HelpBasePlugin.getActivitySupport().setFilteringEnabled(!checked);
		for (int i = 0; i < pages.size(); i++) {
			HelpPartPage page = (HelpPartPage) pages.get(i);
			page.toggleRoleFilter();
		}
	}

	public void saveState(IMemento memento) {
		for (int i = 0; i < pages.size(); i++) {
			HelpPartPage page = (HelpPartPage) pages.get(i);
			page.saveState(memento);
		}
	}

	private String getShowAllMessage() {
		String message = HelpBasePlugin.getActivitySupport()
				.getShowAllMessage();
		if (message == null)
			return Messages.AskShowAll_message;
		StringBuilder buff = new StringBuilder();
		int state = STATE_START;

		for (int i = 0; i < message.length(); i++) {
			char c = message.charAt(i);
			switch (state) {
			case STATE_START:
				if (c == '<')
					state = STATE_LT;
				else
					buff.append(c);
				break;
			case STATE_LT:
				if (c == 'b' || c == 'B')
					state = STATE_LT_B;
				break;
			case STATE_LT_B:
				if (c == 'r' || c == 'R')
					state = STATE_LT_BR;
				break;
			case STATE_LT_BR:
				if (c == '>') {
					buff.append('\n');
				}
				state = STATE_START;
				break;
			default:
				buff.append(c);
			}
		}
		return buff.toString();
	}

	EngineDescriptorManager getEngineManager() {
		if (engineManager==null) {
			engineManager = new EngineDescriptorManager();
		}
		return engineManager;
	}

	public static int getDefaultStyle() {
		int style = ALL_TOPICS | CONTEXT_HELP | SEARCH;
		if (ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexView")) { //$NON-NLS-1$
			style |= INDEX;
		}
		if (ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "bookmarksView")) { //$NON-NLS-1$
			style |= BOOKMARKS;
		}
		return style;
	}

	public void checkRemoteStatus() {
		clearBrowser();
		showURL("/org.eclipse.help.webapp/" + MissingContentManager.REMOTE_STATUS_HELP_VIEW_HREF); //$NON-NLS-1$
		updateStatusLinks();
	}

	public void checkPlaceholderStatus() {
		clearBrowser();
		showURL("/org.eclipse.help.webapp/" + MissingContentManager.MISSING_BOOKS_HELP_VIEW_HREF); //$NON-NLS-1$
		updateStatusLinks();

	}

	private void clearBrowser() {
		IHelpPart part = findPart(HV_BROWSER);
		if ( part == null ) {
			return;
		}
		BrowserPart browserPart = (BrowserPart) part;
		browserPart.clearBrowser();
	}

	private void updateStatusLinks() {
		IHelpPart part = findPart(HV_MISSING_CONTENT);
		if ( part == null ) {
			return;
		}
		MissingContentPart mcPart = (MissingContentPart) part;
		mcPart.updateStatus();
	}
}
