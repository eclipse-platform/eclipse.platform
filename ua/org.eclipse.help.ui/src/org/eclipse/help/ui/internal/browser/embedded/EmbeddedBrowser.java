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
package org.eclipse.help.ui.internal.browser.embedded;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpApplication;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
/**
 * Help browser employing SWT Browser widget
 */
public class EmbeddedBrowser {
	private static final String BROWSER_X = "browser.x"; //$NON-NLS-1$
	private static final String BROWSER_Y = "browser.y"; //$NON-NLS-1$
	private static final String BROWSER_WIDTH = "browser.w"; //$NON-NLS-1$
	private static final String BROWSER_HEIGTH = "browser.h"; //$NON-NLS-1$
	private static final String BROWSER_MAXIMIZED = "browser.maximized"; //$NON-NLS-1$
	private static String initialTitle = getWindowTitle();
	private Shell shell;
	private Browser browser;
	private Composite statusBar;
	private Label statusBarText;
	private Label statusBarSeparator;
	private ProgressBar statusBarProgress;
	private String statusText;
	private int x, y, w, h;
	private long modalRequestTime = 0;
	private Vector<IBrowserCloseListener> closeListeners = new Vector<>(1);
	/**
	 * Constructor for main help window instance
	 */
	public EmbeddedBrowser() {
		int style = SWT.SHELL_TRIM;
		if (ProductPreferences.isRTL())
			style |= SWT.RIGHT_TO_LEFT;
		else
			style |= SWT.LEFT_TO_RIGHT;
		shell = new Shell(style);
		initializeShell(shell);
		shell.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(ControlEvent e) {
				if (!shell.getMaximized()) {
					Point location = shell.getLocation();
					x = location.x;
					y = location.y;
				}
			}

			@Override
			public void controlResized(ControlEvent e) {
				if (!shell.getMaximized()) {
					Point size = shell.getSize();
					w = size.x;
					h = size.y;
				}
			}
		});
		shell.addDisposeListener(e -> {
			// save position
			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpUIPlugin.PLUGIN_ID);
			prefs.putInt(BROWSER_X, x);
			prefs.putInt(BROWSER_Y, y);
			prefs.putInt(BROWSER_WIDTH, w);
			prefs.putInt(BROWSER_HEIGTH, h);
			prefs.putBoolean(BROWSER_MAXIMIZED, (shell.getMaximized()));
			notifyCloseListners();
			if (HelpApplication.isShutdownOnClose()) {
				HelpApplication.stopHelp();
			}
		});

		browser = new Browser(shell, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initialize(browser);

		createStatusBar(shell);
		initializeStatusBar(browser);

		// use saved location and size
		x = Platform.getPreferencesService().getInt(HelpUIPlugin.PLUGIN_ID, BROWSER_X, 0, null);
		y = Platform.getPreferencesService().getInt(HelpUIPlugin.PLUGIN_ID, BROWSER_Y, 0, null);
		w = Platform.getPreferencesService().getInt(HelpUIPlugin.PLUGIN_ID, BROWSER_WIDTH, 0, null);
		h = Platform.getPreferencesService().getInt(HelpUIPlugin.PLUGIN_ID, BROWSER_HEIGTH, 0, null);
		if (w == 0 || h == 0) {
			// first launch, use default size
			w = 1600;
			h = 900;
			x = shell.getLocation().x;
			y = shell.getLocation().y;
		}
		setSafeBounds(shell, x, y, w, h);
		if (Platform.getPreferencesService().getBoolean(HelpUIPlugin.PLUGIN_ID, BROWSER_MAXIMIZED, false, null))
			shell.setMaximized(true);
		shell.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(ControlEvent e) {
				if (!shell.getMaximized()) {
					Point location = shell.getLocation();
					x = location.x;
					y = location.y;
				}
			}

			@Override
			public void controlResized(ControlEvent e) {
				if (!shell.getMaximized()) {
					Point size = shell.getSize();
					w = size.x;
					h = size.y;
				}
			}
		});

		//
		shell.open();
		//browser.setUrl("about:blank");

		browser.addLocationListener(new LocationAdapter() {

			@Override
			public void changing(LocationEvent e) {
				// hack to know when help webapp needs modal window
				modalRequestTime = 0;
				if (e.location != null
						&& e.location.startsWith("javascript://needModal")) { //$NON-NLS-1$
					modalRequestTime = System.currentTimeMillis();
				}
				if (!e.doit && e.location != null
						&& e.location.startsWith("https://")) { //$NON-NLS-1$
					try {
						BaseHelpSystem.getHelpBrowser(true).displayURL(
								e.location);
					} catch (Exception exc) {
					}
				}
			}
		});
	}
	/**
	 * Constructor for derived help window It is either secondary browser or a
	 * help dialog
	 *
	 * @param event
	 * @param parent
	 *            Shell or null
	 */
	public EmbeddedBrowser(WindowEvent event, Shell parent) {
		if (parent == null){
			int style = SWT.SHELL_TRIM;
			if (ProductPreferences.isRTL())
				style |= SWT.RIGHT_TO_LEFT;
			else
				style |= SWT.LEFT_TO_RIGHT;
			shell = new Shell(style);
		} else
			shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		initializeShell(shell);
		Browser browser = new Browser(shell, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		initialize(browser);
		event.browser = browser;

		browser.addLocationListener(new LocationAdapter() {

			@Override
			public void changing(LocationEvent e) {
				// hack to know when help webapp needs modal window
				modalRequestTime = 0;
				if (e.location != null
						&& e.location.startsWith("javascript://needModal")) { //$NON-NLS-1$
					modalRequestTime = System.currentTimeMillis();
				}
			}
		});
	}
	private static void initializeShell(Shell s) {
		s.setText(initialTitle);
		final Image[] shellImages = createImages();
		if (shellImages != null)
			s.setImages(shellImages);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		s.setLayout(layout);
		s.addDisposeListener(e -> {
			if (shellImages != null) {
				for (int i = 0; i < shellImages.length; i++) {
					shellImages[i].dispose();
				}
			}
		});

	}
	private void initialize(Browser browser) {
		browser.addOpenWindowListener(event -> {
			if (System.currentTimeMillis() - modalRequestTime <= 1000) {
				new EmbeddedBrowser(event, shell);
			} else if (event.required) {
				new EmbeddedBrowser(event, null);
			} else {
				displayURLExternal(event);
			}
		});
		browser.addVisibilityWindowListener(new VisibilityWindowListener() {

			@Override
			public void hide(WindowEvent event) {
				Browser browser = (Browser) event.widget;
				Shell shell = browser.getShell();
				shell.setVisible(false);
			}

			@Override
			public void show(WindowEvent event) {
				Browser browser = (Browser) event.widget;
				Shell shell = browser.getShell();
				if (event.location != null)
					shell.setLocation(event.location);
				if (event.size != null) {
					Point size = event.size;
					shell.setSize(shell.computeSize(size.x, size.y));
				}
				shell.open();
			}
		});
		browser.addCloseWindowListener(event -> {
			Browser browser1 = (Browser) event.widget;
			Shell shell = browser1.getShell();
			shell.close();
		});
		browser.addTitleListener(event -> {
			if (event.title != null && event.title.length() > 0) {
				Browser browser1 = (Browser) event.widget;
				Shell shell = browser1.getShell();
				shell.setText(event.title);
			}
		});
		browser.addLocationListener(new LocationAdapter() {

			@Override
			public void changing(LocationEvent e) {
				if (!e.doit && e.location != null
						&& e.location.startsWith("https://")) { //$NON-NLS-1$
					try {
						BaseHelpSystem.getHelpBrowser(true).displayURL(
								e.location);
					} catch (Exception exc) {
					}
				}
			}
		});
	}

	private void initializeStatusBar(Browser browser) {

		// Text
		browser.addStatusTextListener(event -> {
			event.text = event.text.replaceAll("&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$
			if (!event.text.equals(statusText)) {
				statusText = event.text;
				statusBarText.setText(statusText);
			}
		});

		// Progress bar (is shown when loading a page, not on JavaScript requests;
		// unfortunately, ProgressListener doesn't tell to which request it refers, so
		// just assume that it refers to the latest page loading request)
		final AtomicReference<String> changingLocationHolder = new AtomicReference<>();
		browser.addLocationListener(new LocationListener() {

			@Override
			public void changing(LocationEvent event) {
				if (event.location == null || event.location.startsWith("javascript:")) //$NON-NLS-1$
					return;
				changingLocationHolder.set(event.location);
			}

			@Override
			public void changed(LocationEvent event) {
				if (changingLocationHolder.get() != null && changingLocationHolder.get().equals(event.location)) {
					changingLocationHolder.set(null);
				}
				if (changingLocationHolder.get() == null) {
					statusBarSeparator.setVisible(false);
					statusBarProgress.setVisible(false);
				}
			}
		});
		browser.addProgressListener(ProgressListener.changedAdapter(event -> {
			if (event.total <= 0 || changingLocationHolder.get() == null)
				return;
			statusBarProgress.setMaximum(event.total);
			statusBarProgress.setSelection(Math.min(event.current, event.total));
			if (event.current < event.total) {
				statusBarSeparator.setVisible(true);
				statusBarProgress.setVisible(true);
			}
		}));
	}

	private void createStatusBar(Composite parent) {
		statusBar = new Composite(parent, SWT.NONE);
		statusBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout =  new GridLayout(3, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginTop = 0;
		layout.marginLeft = 5;
		layout.marginRight = 5;
		layout.marginBottom = 5;
		statusBar.setLayout(layout);
		statusBarText = new Label(statusBar, SWT.NONE);
		statusBarText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		statusBarSeparator = new Label(statusBar, SWT.SEPARATOR | SWT.VERTICAL);
		statusBarSeparator.setVisible(false);
		statusBarProgress = new ProgressBar(statusBar, SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 100;
		statusBarProgress.setLayoutData(data);
		statusBarProgress.setVisible(false);
		statusBarProgress.setMinimum(0);

		/*
		 * Vertical separator labels are naturally too tall for the status bar.
		 * Size it to match the tallest of the text and progress bar.
		 */
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.heightHint = Math.max(statusBarText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y, statusBarProgress.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		statusBarSeparator.setLayoutData(data);
	}

	public void displayUrl(String url) {
		browser.setUrl(url);
		shell.setMinimized(false);
		shell.forceActive();
	}
	private void displayURLExternal(WindowEvent e) {
		final Shell externalShell = new Shell(shell, SWT.NONE);
		Browser externalBrowser = new Browser(externalShell, SWT.NONE);
		externalBrowser.addLocationListener(new LocationAdapter() {

			@Override
			public void changing(final LocationEvent e) {
				e.doit = false;
				try {
					BaseHelpSystem.getHelpBrowser(true).displayURL(e.location);
				}
				catch (Throwable t) {
					String msg = "Error opening external Web browser"; //$NON-NLS-1$
					Platform.getLog(getClass()).error(msg, t);
				}
				externalShell.getDisplay().asyncExec(() -> externalShell.dispose());
			}
		});
		e.browser = externalBrowser;
	}
	public boolean isDisposed() {
		return shell.isDisposed();
	}
	private static String getWindowTitle() {
		if (Platform.getPreferencesService().getBoolean(HelpUIPlugin.PLUGIN_ID, "windowTitlePrefix", false, null)) { //$NON-NLS-1$
			return NLS.bind(Messages.browserTitle, BaseHelpSystem
			.getProductName());
		}
		return BaseHelpSystem.getProductName();
	}
	/**
	 * Create shell images
	 */
	private static Image[] createImages() {
		String[] productImageURLs = getProductImageURLs();
		if (productImageURLs != null) {
			ArrayList<Image> shellImgs = new ArrayList<>();
			for (int i = 0; i < productImageURLs.length; i++) {
				if ("".equals(productImageURLs[i])) { //$NON-NLS-1$
					continue;
				}
				URL imageURL = null;
				try {
					imageURL = new URL(productImageURLs[i]);
				} catch (MalformedURLException mue) {
					// must be a path relative to the product bundle
					IProduct product = Platform.getProduct();
					if (product != null) {
						Bundle productBundle = product.getDefiningBundle();
						if (productBundle != null) {
							imageURL = FileLocator.find(productBundle, new Path(
									productImageURLs[i]), null);
						}
					}
				}
				Image image = null;
				if (imageURL != null) {
					image = ImageDescriptor.createFromURL(imageURL)
							.createImage();
				}
				if (image != null) {
					shellImgs.add(image);
				}
			}
			return shellImgs.toArray(new Image[shellImgs.size()]);
		}
		return new Image[0];
	}
	/**
	 * Obtains URLs to product image
	 *
	 * @return String[] with URLs as Strings or null
	 */
	private static String[] getProductImageURLs() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			String url = product.getProperty("windowImages"); //$NON-NLS-1$
			if (url != null && url.length() > 0) {
				return url.split(",\\s*"); //$NON-NLS-1$
			}
			url = product.getProperty("windowImage"); //$NON-NLS-1$
			if (url != null && url.length() > 0) {
				return new String[]{url};
			}
		}
		return null;
	}
	/**
	 * Closes the browser.
	 */
	public void close() {
		if (!shell.isDisposed())
			shell.dispose();
	}
	private static void setSafeBounds(Shell s, int x, int y, int width,
			int height) {
		Rectangle clientArea = s.getDisplay().getClientArea();
		width = Math.min(clientArea.width, width);
		height = Math.min(clientArea.height, height);
		x = Math.min(x + width, clientArea.x + clientArea.width) - width;
		y = Math.min(y + height, clientArea.y + clientArea.height) - height;
		x = Math.max(x, clientArea.x);
		y = Math.max(y, clientArea.y);
		s.setBounds(x, y, width, height);
	}
	public void setLocation(int x, int y) {
		shell.setLocation(x, y);
	}
	public void setSize(int width, int height) {
		shell.setSize(w, h);
	}
	private void notifyCloseListners() {
		for (Iterator<IBrowserCloseListener> it = closeListeners.iterator(); it.hasNext();) {
			IBrowserCloseListener listener = it.next();
			listener.browserClosed();
		}
	}

	public void addCloseListener(IBrowserCloseListener listener) {
		if (!closeListeners.contains(listener)) {
			closeListeners.add(listener);
		}
	}

	public void removeCloseListener(IBrowserCloseListener listener) {
		closeListeners.remove(listener);
	}
}
