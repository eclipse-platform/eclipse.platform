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
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.LinkUtil;
import org.eclipse.help.internal.util.URLCoder;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.FontUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.service.prefs.BackingStoreException;

public class BrowserPart extends AbstractFormPart implements IHelpPart {
	private final static String QUERY = "BrowserPartQuery:"; //$NON-NLS-1$
	private final static String HIGHLIGHT_ON = "highlight-on"; //$NON-NLS-1$
	private final static String HELP_VIEW_SCALE = "help_view_scale"; //$NON-NLS-1$
	private final static String EMPTY_PAGE = "<html><head></head><body></body></html>"; //$NON-NLS-1$

	private ReusableHelpPart parent;

	private Browser browser;

	private String id;

	private int lastProgress = -1;

	private static final int SCALE_MAX = 250;

	private static final int SCALE_MIN = 50;

	private static final int SCALE_INCREMENT = 10;

	private int fontScalePercentage = 100;

	private String url;

	private Action showExternalAction;

	private Action syncTocAction;

	private Action highlightAction;

	private Action bookmarkAction;

	private Action printAction;

	private Action magnifyAction;

	private Action reduceAction;

	private String statusURL;

	private String title;

	public BrowserPart(final Composite parent, FormToolkit toolkit,
			final IToolBarManager tbm, IMenuManager menuManager) {
		browser = new Browser(parent, SWT.NULL);
		browser.addLocationListener(new LocationListener() {

			@Override
			public void changing(LocationEvent event) {
				if (redirectLink(event.location))
					event.doit = false;
				if (!event.doit && event.location != null
						&& event.location.startsWith("https://")) { //$NON-NLS-1$
					try {
						BaseHelpSystem.getHelpBrowser(true).displayURL(
								event.location);
					} catch (Exception exc) {
					}
				}
			}

			@Override
			public void changed(LocationEvent event) {
				String url = event.location;
				boolean isResult = url.contains("resultof"); //$NON-NLS-1$
				BrowserPart.this.parent.browserChanged(url);
				BrowserPart.this.url = url;
				updateSyncTocAction();
				BrowserPart.this.highlightAction.setEnabled(isResult);
			}
		});
		browser.addProgressListener(new ProgressListener() {

			@Override
			public void changed(ProgressEvent e) {
				if (e.current == e.total)
					return;
				IStatusLineManager slm = BrowserPart.this.parent
						.getStatusLineManager();
				IProgressMonitor monitor = slm != null ? slm
						.getProgressMonitor() : null;
				if (lastProgress == -1) {
					lastProgress = 0;
					if (monitor != null) {
						monitor.beginTask("", e.total); //$NON-NLS-1$
						slm.setCancelEnabled(true);
					}
				} else if (monitor != null && monitor.isCanceled()) {
					browser.stop();
					return;
				}
				if (monitor != null)
					monitor.worked(e.current - lastProgress);
				lastProgress = e.current;
			}

			@Override
			public void completed(ProgressEvent e) {
				IStatusLineManager slm = BrowserPart.this.parent
						.getStatusLineManager();
				IProgressMonitor monitor = slm != null ? slm
						.getProgressMonitor() : null;
				if (monitor != null) {
					slm.setCancelEnabled(false);
					monitor.done();
				}
				lastProgress = -1;
				if (fontScalePercentage != 100) {
					rescale();
				}
				String value = executeQuery("document.title"); //$NON-NLS-1$
				BrowserPart.this.title = value != null ? value : "N/A"; //$NON-NLS-1$
			}
		});
		browser.addStatusTextListener(event -> {
			if (processQuery(event.text))
				return;
			IStatusLineManager statusLine = BrowserPart.this.parent.getStatusLineManager();
			if (statusLine != null)
				statusLine.setMessage(event.text);
			if (event.text.contains("://")) //$NON-NLS-1$
				statusURL = event.text;
		});
		browser.addOpenWindowListener(event -> {
			if (statusURL != null) {
				try {
					String relativeURL = BaseHelpSystem.unresolve(new URL(statusURL));
					if (BrowserPart.this.parent.isHelpResource(relativeURL)) {
						BrowserPart.this.parent.showExternalURL(relativeURL);
						event.required = true;
					}
				} catch (MalformedURLException e) {
					Platform.getLog(getClass()).error("Malformed URL: " + statusURL, e); //$NON-NLS-1$
				}
			}
		});
		contributeToToolBar(tbm);
		contributeToMenu(menuManager);
	}

	private String executeQuery(String domValue) {
		return (String)browser.evaluate("return " + domValue + ';'); //$NON-NLS-1$
	}

	private boolean processQuery(String text) {
		if (text.startsWith(QUERY)) {
			browser.setData("query", text.substring(QUERY.length())); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	private void contributeToToolBar(IToolBarManager tbm) {
		boolean highlight = Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, HIGHLIGHT_ON, true, null);
		showExternalAction = new Action() {

			@Override
			public void run() {
				BusyIndicator.showWhile(browser.getDisplay(), () -> {
					try {
						parent.showExternalURL(BaseHelpSystem.unresolve(new URL(url)));
					} catch (MalformedURLException e) {
						Platform.getLog(getClass()).error("Malformed URL: " + statusURL, e); //$NON-NLS-1$
					}
				});
			}
		};
		showExternalAction
				.setToolTipText(Messages.BrowserPart_showExternalTooltip);
		showExternalAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_NW));
		syncTocAction = new Action() {

			@Override
			public void run() {
				doSyncToc();
			}
		};
		syncTocAction.setToolTipText(Messages.BrowserPart_syncTocTooltip);
		syncTocAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_SYNC_TOC));
		syncTocAction.setEnabled(false);
		bookmarkAction = new Action() {

			@Override
			public void run() {
				String href = LinkUtil.stripParams(BaseHelpSystem.unresolve(url));
				BaseHelpSystem.getBookmarkManager().addBookmark(href, title);
			}
		};
		bookmarkAction.setToolTipText(Messages.BrowserPart_bookmarkTooltip);
		bookmarkAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_ADD_BOOKMARK));

		highlightAction = new Action() {

			@Override
			public void run() {
				IScopeContext instanceScope = InstanceScope.INSTANCE;
				IEclipsePreferences prefs = instanceScope.getNode(HelpBasePlugin.PLUGIN_ID);
				prefs.putBoolean(HIGHLIGHT_ON, highlightAction.isChecked());
				if (browser.getUrl().contains("resultof")) browser.execute("setHighlight(" +highlightAction.isChecked()+");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		};
		highlightAction.setChecked(highlight);
		highlightAction.setToolTipText(Messages.BrowserPart_highlightTooltip);
		highlightAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_HIGHLIGHT));

		printAction = new Action(ActionFactory.PRINT.getId()) {

			@Override
			public void run() {
				doPrint();
			}
		};
		printAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_HELP_PRINT));
		printAction.setToolTipText(Messages.BrowserPart_printTooltip);

		tbm.insertBefore("back", showExternalAction); //$NON-NLS-1$
		tbm.insertBefore("back", syncTocAction); //$NON-NLS-1$
		tbm.insertBefore("back", printAction); //$NON-NLS-1$
		tbm.insertBefore("back", bookmarkAction); //$NON-NLS-1$
		tbm.insertBefore("back", highlightAction); //$NON-NLS-1$
		tbm.insertBefore("back", new Separator()); //$NON-NLS-1$
		enableButtons();
	}

	private void contributeToMenu(IMenuManager menuManager) {
		if (FontUtils.canRescaleHelpView()) {
			fontScalePercentage = Platform.getPreferencesService().getInt(HelpBasePlugin.PLUGIN_ID,
					HELP_VIEW_SCALE, 100, null);
			if (menuManager != null) {
				addMenuActions(menuManager);
			}
		}
	}

	private void addMenuActions(IMenuManager menuManager) {
		magnifyAction = new Action() {

			@Override
			public void run() {
				doMagnify(SCALE_INCREMENT);
			}
		};
		magnifyAction.setToolTipText(Messages.BrowserPart_magnifyTooltip);
		magnifyAction.setText(Messages.BrowserPart_magnifyTooltip);
		magnifyAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_MAGNIFY));
		magnifyAction.setDisabledImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_D_MAGNIFY));

		reduceAction = new Action() {

			@Override
			public void run() {
				doMagnify(-SCALE_INCREMENT);
			}
		};
		reduceAction.setToolTipText(Messages.BrowserPart_reduceTooltip);
		reduceAction.setText(Messages.BrowserPart_reduceTooltip);
		reduceAction
				.setImageDescriptor(HelpUIResources.getImageDescriptor(IHelpUIConstants.IMAGE_REDUCE));
		reduceAction.setDisabledImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_D_REDUCE));
		menuManager.add(new Separator());
		menuManager.add(reduceAction);
		menuManager.add(magnifyAction);
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		if (memento != null) {
			String href = memento.getString("BrowserPart.url"); //$NON-NLS-1$
			if (href != null)
				showURL(BaseHelpSystem.resolve(href, "/help/ntopic").toString()); //$NON-NLS-1$
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Control getControl() {
		return browser;
	}

	@Override
	public void setVisible(boolean visible) {
		if (browser != null) {
			browser.setVisible(visible);
		}
	}

	@Override
	public void setFocus() {
		if (browser != null)
			browser.setFocus();
	}

	public void showURL(String url) {
		if (browser != null && url != null) {
			browser.setUrl(url);
		}
	}

	public void clearBrowser() {
		if (browser != null) {
			browser.setText(EMPTY_PAGE);
		}
	}

	@Override
	public void stop() {
		if (browser != null && !browser.isDisposed()) {
			browser.stop();
		}
	}

	private void doPrint() {
		browser.execute("window.print();"); //$NON-NLS-1$
	}

	private void doSyncToc() {
		String href = BaseHelpSystem.unresolve(this.url);
		int ix = href.indexOf("?resultof="); //$NON-NLS-1$
		if (ix >= 0) {
			href = href.substring(0, ix);
		}
		parent.showPage(IHelpUIConstants.HV_ALL_TOPICS_PAGE);
		AllTopicsPart part = (AllTopicsPart) parent
				.findPart(IHelpUIConstants.HV_TOPIC_TREE);
		if (part != null) {
			part.selectReveal(href);
		}
	}

	private void updateSyncTocAction() {
		String href = BaseHelpSystem.unresolve(this.url);
		syncTocAction.setEnabled(parent.isHelpResource(href));
	}

	private boolean redirectLink(final String url) {
		if (url.contains("/topic/")) { //$NON-NLS-1$
			if (!url.contains("noframes")) { //$NON-NLS-1$
				return true;
			}
		} else if (url.indexOf("livehelp/?pluginID=")>0) { //$NON-NLS-1$
			processLiveAction(url);
			return true;
		} else if (url.indexOf("helpview:") == 0) { //$NON-NLS-1$
			HelpviewProtocol.handleProtocolCall(url, parent);
			return true;
		}
		return false;
	}

	private void processLiveAction(String url) {
		if (!Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, "activeHelp", false, null)) { //$NON-NLS-1$
			return;
		}

		String query = null;
		try {
			URL u = new URL(url);
			query = u.getQuery();
		} catch (MalformedURLException mue) {
		}
		if (query == null)
			return;
		StringTokenizer st = new StringTokenizer(query, "=&"); //$NON-NLS-1$
		if (st.countTokens() < 6) {
			return;
		}
		st.nextToken();
		String pluginId = URLCoder.decode(st.nextToken());
		st.nextToken();
		String className = URLCoder.decode(st.nextToken());
		st.nextToken();
		String arg = URLCoder.decode(st.nextToken());
		if (pluginId == null || className == null || arg == null)
			return;
		BaseHelpSystem.runLiveHelp(pluginId, className, arg);
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return browser.equals(control);
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.PRINT.getId()))
			return printAction;
		return null;
	}

	private void enableButtons() {
		if (magnifyAction != null) {
			magnifyAction.setEnabled(fontScalePercentage < SCALE_MAX);
			reduceAction.setEnabled(fontScalePercentage > SCALE_MIN);
		}
	}

	private void doMagnify(int percent) {
		fontScalePercentage += percent;
		IScopeContext instanceScope = InstanceScope.INSTANCE;
		IEclipsePreferences prefs = instanceScope.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.putInt(HELP_VIEW_SCALE, fontScalePercentage);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
		rescale();
	}

	public void rescale() {
		browser.execute(FontUtils.getRescaleScript(fontScalePercentage));
		enableButtons();
	}

	@Override
	public void toggleRoleFilter() {
	}

	@Override
	public void refilter() {
		showURL(this.url);
	}

	@Override
	public void saveState(IMemento memento) {
		if (url != null) {
			String href = BaseHelpSystem.unresolve(url);
			memento.putString("BrowserPart.url", href); //$NON-NLS-1$
		}
	}
}
