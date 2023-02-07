/***************************************************************************************************
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
 * 		IBM Corporation - initial API and implementation
 * 		Sebastian Davids <sdavids@gmx.de> - bug 93374
 *      George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 **************************************************************************************************/
package org.eclipse.help.ui.internal;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.ui.internal.util.ErrorUtil;
import org.eclipse.help.ui.internal.util.FontUtils;
import org.eclipse.help.ui.internal.views.ContextHelpPart;
import org.eclipse.help.ui.internal.views.HelpTray;
import org.eclipse.help.ui.internal.views.HelpView;
import org.eclipse.help.ui.internal.views.ReusableHelpPart;
import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.help.AbstractHelpUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

/**
 * This class is an implementation of the Help UI. In is registered into the helpSupport extension
 * point, and is responsible for handling requests to display help. The methods on this class
 * interact with the actual UI component handling the display.
 * <p>
 * Most methods delegate most work to HelpDisplay class; only the UI specific ones implemented in
 * place.
 * </p>
 */
public class DefaultHelpUI extends AbstractHelpUI {

	private ContextHelpDialog f1Dialog = null;
	private static DefaultHelpUI instance;
	private static boolean openingHelpView = false;

	private static final String HELP_VIEW_ID = "org.eclipse.help.ui.HelpView"; //$NON-NLS-1$

	static class ExternalWorkbenchBrowser implements IBrowser {

		public ExternalWorkbenchBrowser() {
		}

		private IWebBrowser getExternalBrowser() throws PartInitException {
			IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
			return support.getExternalBrowser();
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isCloseSupported() {
			return false;
		}

		@Override
		public void displayURL(String url) throws Exception {
			try {
				IWebBrowser browser = getExternalBrowser();
				if (browser != null) {
					browser.openURL(new URL(url));
				}
			} catch (PartInitException pie) {
				ErrorUtil.displayErrorDialog(pie.getLocalizedMessage());
			}
		}

		@Override
		public boolean isSetLocationSupported() {
			return false;
		}

		@Override
		public boolean isSetSizeSupported() {
			return false;
		}

		@Override
		public void setLocation(int x, int y) {
		}

		@Override
		public void setSize(int width, int height) {
		}
	}

	/**
	 * Constructor.
	 */
	public DefaultHelpUI() {
		super();
		instance = this;
		// register external browser. This will cause the help system
		// to use workbench external browser instead of its own.
		BaseHelpSystem.getInstance().setBrowserInstance(new ExternalWorkbenchBrowser());
	}

	public static DefaultHelpUI getInstance() {
		return instance;
	}

	/**
	 * Displays help.
	 */
	@Override
	public void displayHelp() {
		BaseHelpSystem.getHelpDisplay().displayHelp(useExternalBrowser(null));
	}

	/**
	 * Displays search.
	 */
	@Override
	public void displaySearch() {
		search(null);
	}

	/**
	 * Displays dynamic help.
	 */
	@Override
	public void displayDynamicHelp() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell activeShell = getActiveShell();
		if (window != null && isActiveShell(activeShell, window)) {
			setIntroStandby();

			IWorkbenchPage page = window.getActivePage();
			Control c = activeShell.getDisplay().getFocusControl();
			if (page != null) {
				IWorkbenchPart activePart = page.getActivePart();
				try {
					IViewPart part = page.showView(HELP_VIEW_ID, null, IWorkbenchPage.VIEW_ACTIVATE);
					if (part != null) {
						HelpView view = (HelpView) part;
						view.showDynamicHelp(activePart, c);
					}
				} catch (PartInitException e) {
				}
			} else {
				// check the dialog
				Object data = activeShell.getData();
				if (data instanceof TrayDialog) {
					IContext context = ContextHelpPart.findHelpContext(c);
					displayContextAsHelpTray(activeShell, context);
					return;
				}
				warnNoOpenPerspective(window);
			}
		}
	}

	/**
	 * Starts the search.
	 */
	@Override
	public void search(final String expression) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell activeShell = getActiveShell();
		if (window != null && isActiveShell(activeShell, window)) {
			setIntroStandby();

			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				boolean searchFromBrowser = Platform.getPreferencesService().getBoolean
					(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SEARCH_FROM_BROWSER, false, null);
				if (searchFromBrowser) {
					String parameters = "tab=search"; //$NON-NLS-1$
					if (expression != null) {
						parameters += "&searchWord=" + URLEncoder.encode(expression, StandardCharsets.UTF_8); //$NON-NLS-1$
					}
					BaseHelpSystem.getHelpDisplay().displayHelpResource(parameters, false);
				} else {
					try {
						IViewPart part = page.showView(HELP_VIEW_ID);
						if (part != null) {
							HelpView view = (HelpView) part;
							view.startSearch(expression);
						}
					} catch (PartInitException e) {
					}
				}
			} else {
				// check the dialog
				if (activeShell != null) {
					Object data = activeShell.getData();
					if (data instanceof TrayDialog) {
						displayContextAsHelpTray(activeShell, null);
						return;
					}
					else {
						// tried to summon help from a non-tray dialog
						// not supported
						return;
					}
				}
				warnNoOpenPerspective(window);
			}
		}
	}

	public static void showIndex() {
		HelpView helpView = getHelpView();
		if (helpView != null) {
			helpView.showIndex();
		}
	}

	private static HelpView getHelpView() {
		HelpView view = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell activeShell = getActiveShell();
		if (window != null && isActiveShell(activeShell, window)) {
			setIntroStandby();
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				try {
					IViewPart part = page.showView(HELP_VIEW_ID);
					if (part != null) {
						view = (HelpView) part;
					}
				} catch (PartInitException e) {
				}
			}
		}
		return view;
	}

	private static void setIntroStandby() {
		IIntroManager introMng = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart intro = introMng.getIntro();
		if (intro != null && !introMng.isIntroStandby(intro))
			introMng.setIntroStandby(intro, true);
	}

	private void warnNoOpenPerspective(IWorkbenchWindow window) {
		MessageDialog.openInformation(window.getShell(), Messages.DefaultHelpUI_wtitle,
				Messages.DefaultHelpUI_noPerspMessage);
	}

	/**
	 * Displays a help resource specified as a url.
	 * <ul>
	 * <li>a URL in a format that can be returned by
	 * {@link  org.eclipse.help.IHelpResource#getHref() IHelpResource.getHref()}
	 * <li>a URL query in the format format <em>key=value&amp;key=value ...</em> The valid keys
	 * are: "tab", "toc", "topic", "contextId". For example,
	 * <em>toc="/myplugin/mytoc.xml"&amp;topic="/myplugin/references/myclass.html"</em> is valid.
	 * </ul>
	 */
	@Override
	public void displayHelpResource(String href) {
		BaseHelpSystem.getHelpDisplay().displayHelpResource(href, useExternalBrowser(href));
	}

	/**
	 * Displays context-sensitive help for specified context
	 *
	 * @param context
	 *            the context to display
	 * @param x
	 *            int positioning information
	 * @param y
	 *            int positioning information
	 */
	@Override
	public void displayContext(IContext context, int x, int y) {
		displayContext(context, x, y, false);
	}

	void displayContext(IContext context, int x, int y, boolean noInfopop) {
		if (context == null)
			return;
		boolean winfopop = Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_WINDOW_INFOPOP, false, null);
		boolean dinfopop = Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_DIALOG_INFOPOP, false, null)  || FontUtils.isFontTooLargeForTray();

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell activeShell = getActiveShell();
		if (window != null && isActiveShell(activeShell, window)) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				if (!noInfopop && winfopop) {
					Control c = window.getShell().getDisplay().getFocusControl();
					displayContextAsInfopop(context, x, y, c);
					return;
				}
				try {
					IWorkbenchPart activePart = page.getActivePart();
					Control c = window.getShell().getDisplay().getFocusControl();
					IContextProvider adapter = activePart.getAdapter(IContextProvider.class);
					if (adapter != null)
						context = adapter
								.getContext(c); /*
												 * If the context help has no description text and exactly one
												 * topic, go straight to the topic and skip context help.
												 */
					String contextText = context.getText();
					IHelpResource[] topics = context.getRelatedTopics();
					boolean isSingleChoiceWithoutDescription = contextText == null && topics.length == 1;
					String openMode = Platform.getPreferencesService().getString
						(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_HELP_VIEW_OPEN_MODE, IHelpBaseConstants.P_IN_PLACE, null);
					if (isSingleChoiceWithoutDescription && IHelpBaseConstants.P_IN_EDITOR.equals(openMode)) {
						showInWorkbenchBrowser(topics[0].getHref(), true);
					} else if (isSingleChoiceWithoutDescription && IHelpBaseConstants.P_IN_BROWSER.equals(openMode)) {
						BaseHelpSystem.getHelpDisplay().displayHelpResource(topics[0].getHref(), true);
					} else {
						openingHelpView = true;
						IViewPart part = page.showView(HELP_VIEW_ID);
						openingHelpView = false;
						if (part != null) {
							HelpView view = (HelpView) part;
							if (isSingleChoiceWithoutDescription) {
								view.showHelp(topics[0].getHref());
							} else {
								view.displayContext(context, activePart, c);
							}
						}
					}
					return;
				} catch (PartInitException e) {
					// ignore the exception and let
					// the code default to the context
					// help dialog
				}
			}
		}
		// check the dialog
		if (HelpTray.isAppropriateFor(activeShell) && (!dinfopop || noInfopop)) {
			displayContextAsHelpTray(activeShell, context);
			return;
		}
		// we are here either as a fallback or because of the user preferences
		displayContextAsInfopop(context, x, y, null);
	}

	@Override
	public URL resolve(String href, boolean documentOnly) {
		return BaseHelpSystem.resolve(href, documentOnly);
	}

	public String unresolve(URL url) {
		return BaseHelpSystem.unresolve(url);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.help.AbstractHelpUI#resolve(java.lang.String, boolean)
	 */
	private static Shell getActiveShell() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		return display.getActiveShell();
	}

	static boolean isActiveShell(Shell activeShell, IWorkbenchWindow window) {
		// Test if the active shell belongs to this window
		return activeShell != null && activeShell.equals(window.getShell());
	}

	private void displayContextAsInfopop(IContext context, int x, int y, Control c) {
		if (f1Dialog != null) {
			f1Dialog.close();
		}

		if (context != null) {
			/*
			 * If the context help has no description text and exactly one
			 * topic, go straight to the topic and skip context help.
			 */
			IHelpResource[] topics = context.getRelatedTopics();
			if (context.getText() == null && topics.length == 1) {
				try {
					PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(topics[0].getHref());
				}
				catch (Exception e) {
					// should never happen
				}
			}
			else {
				f1Dialog = new ContextHelpDialog(context, x, y);
				f1Dialog.open();
			}
		}
	}

	private void displayContextAsHelpTray(Shell activeShell, IContext context) {
		Control controlInFocus = activeShell.getDisplay().getFocusControl();
		TrayDialog dialog = (TrayDialog)activeShell.getData();

		DialogTray tray = dialog.getTray();
		if (tray == null) {
			tray = new HelpTray();
			dialog.openTray(tray);
		}
		if (tray instanceof HelpTray) {
			ReusableHelpPart helpPart = ((HelpTray)tray).getHelpPart();
			if (context != null) {
				IHelpResource[] topics = context.getRelatedTopics();
				if (context.getText() == null && topics.length == 1) {
					helpPart.showURL(topics[0].getHref());
				}
				else {
					helpPart.showPage(IHelpUIConstants.HV_CONTEXT_HELP_PAGE);
					helpPart.update(null, context, null, controlInFocus, true);
				}
			}
			else {
				helpPart.showPage(IHelpUIConstants.HV_FSEARCH_PAGE, true);
			}
			helpPart.setFocus();
		}
		else {
			// someone else was occupying the tray; not supported
		}
	}

	/**
	 * Returns <code>true</code> if the context-sensitive help window is currently being
	 * displayed, <code>false</code> if not.
	 */
	@Override
	public boolean isContextHelpDisplayed() {
		if (f1Dialog == null) {
			return false;
		}
		return f1Dialog.isShowing();
	}

	private boolean useExternalBrowser(String url) {
		// On non Windows platforms, use external when modal window is displayed
		if (!Constants.OS_WIN32.equalsIgnoreCase(Platform.getOS())) {
			Display display = Display.getCurrent();
			if (display != null) {
				if (insideModalParent(display))
					return true;
			}
		}

		// Use external when no help frames are to be displayed, otherwise no
		// navigation buttons.
		if (url != null) {
			if (url.indexOf("?noframes=true") > 0 //$NON-NLS-1$
					|| url.indexOf("&noframes=true") > 0) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	private boolean insideModalParent(Display display) {
		return isDisplayModal(display.getActiveShell());
	}

	public static boolean isDisplayModal(Shell activeShell) {
		while (activeShell != null) {
			if ((activeShell.getStyle() & (SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL)) > 0)
				return true;
			activeShell = (Shell) activeShell.getParent();
		}
		return false;
	}

	public static boolean showInWorkbenchBrowser(String url, boolean onlyInternal) {
		IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
		if (!onlyInternal || support.isInternalWebBrowserAvailable()) {
			try {
				IWebBrowser browser = support
						.createBrowser(
								IWorkbenchBrowserSupport.AS_EDITOR
										| IWorkbenchBrowserSupport.NAVIGATION_BAR
										| IWorkbenchBrowserSupport.STATUS,
								"org.eclipse.help.ui", Messages.ReusableHelpPart_internalBrowserTitle, url); //$NON-NLS-1$
				browser.openURL(BaseHelpSystem.resolve(url, "/help/nftopic")); //$NON-NLS-1$
				return true;
			} catch (PartInitException e) {
				Platform.getLog(DefaultHelpUI.class).error(Messages.ReusableHelpPart_internalWebBrowserError, e);
			}
		}
		return false;
	}

	/*
	 * Used to indicate to the HelpView that we are about to pass in a context
	 */

	public static boolean isOpeningHelpView() {
		return openingHelpView;
	}
}