/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui.internal;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.IUrlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.core.internal.TipManager;
import org.eclipse.tips.ui.IBrowserFunctionProvider;
import org.eclipse.tips.ui.ISwtTip;
import org.eclipse.tips.ui.internal.util.ImageUtil;

@SuppressWarnings("restriction")
public class TipComposite extends Composite implements ProviderSelectionListener {
	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final int READ_TIMER = 2000;
	private TipProvider fProvider;
	private Browser fBrowser;
	private Slider fSlider;
	private TipManager fTipManager;
	private Tip fCurrentTip;
	private Button fUnreadOnly;
	private Button fPreviousTipButton;
	private Composite fSWTComposite;
	private Composite fBrowserComposite;
	private StackLayout fContentStack;
	private Button fMultiActionMenuButton;
	private Composite fNavigationBar;
	private StackLayout fActionStack;
	private Composite fEmptyActionComposite;
	private Composite fSingleActionComposite;
	private Composite fMultiActionComposite;
	private Button fSingleActionButton;
	private Button fMultiActionButton;
	private Composite fContentComposite;
	private List<Image> fActionImages = new ArrayList<>();
	private List<BrowserFunction> fBrowserFunctions = new ArrayList<>();
	private Menu fActionMenu;
	private ToolBar ftoolBar;
	private ToolItem fStartupItem;
	private Button fNextTipButton;
	private final ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources(), this);

	/**
	 * Constructor.
	 *
	 * @param parent the parent
	 * @param style  the style
	 */
	public TipComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout_1 = new GridLayout(1, false);
		gridLayout_1.marginWidth = 2;
		gridLayout_1.marginHeight = 2;
		setLayout(gridLayout_1);

		fContentComposite = new Composite(this, SWT.NONE);
		fContentStack = new StackLayout();
		fContentComposite.setLayout(fContentStack);
		GridData gd_gridComposite = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_gridComposite.widthHint = 600;
		gd_gridComposite.heightHint = 400;
		fContentComposite.setLayoutData(gd_gridComposite);

		fBrowserComposite = new Composite(fContentComposite, SWT.NONE);
		fBrowserComposite.setLayout(new FillLayout(SWT.HORIZONTAL));

		fBrowser = new Browser(fBrowserComposite, SWT.NONE);
		fBrowser.setJavascriptEnabled(true);

		fSWTComposite = new Composite(fContentComposite, SWT.NONE);
		fSWTComposite.setLayout(new FillLayout(SWT.HORIZONTAL));

		fNavigationBar = new Composite(this, SWT.NONE);
		fNavigationBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_NavigationBar = new GridLayout(2, false);
		gl_NavigationBar.horizontalSpacing = 0;
		gl_NavigationBar.marginHeight = 0;
		gl_NavigationBar.verticalSpacing = 0;
		gl_NavigationBar.marginWidth = 0;
		fNavigationBar.setLayout(gl_NavigationBar);

		Composite preferenceBar = new Composite(fNavigationBar, SWT.NONE);
		FillLayout fl_composite_3 = new FillLayout(SWT.HORIZONTAL);
		fl_composite_3.marginWidth = 5;
		fl_composite_3.spacing = 5;
		preferenceBar.setLayout(fl_composite_3);

		final Menu menu = new Menu(getShell(), SWT.POP_UP);
		menu.addListener(SWT.Show, event -> startupMenuAboutToShow(menu));

		ftoolBar = new ToolBar(preferenceBar, SWT.FLAT | SWT.RIGHT);

		fStartupItem = new ToolItem(ftoolBar, SWT.DROP_DOWN);
		fStartupItem.setText(Messages.TipComposite_13);
		fStartupItem.addListener(SWT.Selection, event -> {
			showStartupOptions(menu);
		});

		fUnreadOnly = new Button(preferenceBar, SWT.CHECK);
		fUnreadOnly.setText(Messages.TipComposite_2);
		fUnreadOnly.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fTipManager.setServeReadTips(!fUnreadOnly.getSelection());
				fPreviousTipButton.setEnabled(fTipManager.mustServeReadTips());
				fSlider.load();
				getNextTip();
			}
		});

		Composite buttonBar = new Composite(fNavigationBar, SWT.NONE);
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_buttonBar = new GridLayout(4, true);
		gl_buttonBar.marginHeight = 0;
		buttonBar.setLayout(gl_buttonBar);

		Composite actionComposite = new Composite(buttonBar, SWT.NONE);
		fActionStack = new StackLayout();
		actionComposite.setLayout(fActionStack);
		actionComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		fSingleActionComposite = new Composite(actionComposite, SWT.NONE);
		GridLayout gl_SingleActionComposite = new GridLayout(1, false);
		gl_SingleActionComposite.marginWidth = 0;
		fSingleActionComposite.setLayout(gl_SingleActionComposite);

		fSingleActionButton = new Button(fSingleActionComposite, SWT.NONE);
		fSingleActionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fSingleActionButton.setText(Messages.TipComposite_3);
		fSingleActionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runTipAction(fCurrentTip.getActions().get(0));
			}
		});

		fMultiActionComposite = new Composite(actionComposite, SWT.NONE);
		GridLayout gl_MultiActionComposite = new GridLayout(2, false);
		gl_MultiActionComposite.marginWidth = 0;
		gl_MultiActionComposite.verticalSpacing = 0;
		gl_MultiActionComposite.horizontalSpacing = 0;
		fMultiActionComposite.setLayout(gl_MultiActionComposite);

		fMultiActionButton = new Button(fMultiActionComposite, SWT.NONE);
		fMultiActionButton.setText(Messages.TipComposite_4);
		fMultiActionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runTipAction(fCurrentTip.getActions().get(0));
			}
		});

		fMultiActionMenuButton = new Button(fMultiActionComposite, SWT.NONE);
		fMultiActionMenuButton.setImage(DefaultTipManager.getImage("icons/popup_menu.png", resourceManager)); //$NON-NLS-1$
		fMultiActionMenuButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showActionMenu();
			}
		});

		fEmptyActionComposite = new Composite(actionComposite, SWT.NONE);
		fEmptyActionComposite.setLayout(new FillLayout(SWT.HORIZONTAL));

		fPreviousTipButton = new Button(buttonBar, SWT.NONE);
		fPreviousTipButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fPreviousTipButton.setText(Messages.TipComposite_7);
		fPreviousTipButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				getPreviousTip();
			}
		});
		fPreviousTipButton.setEnabled(false);

		fNextTipButton = new Button(buttonBar, SWT.NONE);
		fNextTipButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fNextTipButton.setText(Messages.TipComposite_8);
		fNextTipButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getNextTip();
			}
		});
		fNextTipButton.setEnabled(false);

		Button btnClose = new Button(buttonBar, SWT.NONE);
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getParent().dispose();
			}
		});
		btnClose.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnClose.setText(Messages.TipComposite_9);

		Label label_1 = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		fSlider = new Slider(this, SWT.NONE);
		GridLayout gridLayout = (GridLayout) fSlider.getLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 0;
		fSlider.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
		fContentStack.topControl = fBrowserComposite;
		fSlider.addTipProviderListener(this);

		loadWaitingScript();
	}

	private void showStartupOptions(final Menu menu) {
		Rectangle rect = fStartupItem.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = ftoolBar.toDisplay(pt);
		menu.setLocation(pt.x, pt.y);
		menu.setVisible(true);
	}

	private Image getStartupItemImage(int startup) {
		switch (startup) {
		case 1:
			return DefaultTipManager.getImage("icons/lightbulb.png", resourceManager); //$NON-NLS-1$
		case 2:
			return DefaultTipManager.getImage("icons/stop.png", resourceManager); //$NON-NLS-1$
		default:
			return DefaultTipManager.getImage("icons/run_exc.png", resourceManager); //$NON-NLS-1$
		}
	}

	private void startupMenuAboutToShow(final Menu menu) {
		Arrays.asList(menu.getItems()).forEach(MenuItem::dispose);

		MenuItem item0 = new MenuItem(menu, SWT.CHECK);
		item0.setText(Messages.TipComposite_1);
		item0.setSelection(fTipManager.getStartupBehavior() == TipManager.START_DIALOG);
		item0.addListener(SWT.Selection, event -> fTipManager.setStartupBehavior(TipManager.START_DIALOG));
		item0.setImage(getStartupItemImage(TipManager.START_DIALOG));

		MenuItem item1 = new MenuItem(menu, SWT.CHECK);
		item1.setText(Messages.TipComposite_5);
		item1.setSelection(fTipManager.getStartupBehavior() == TipManager.START_BACKGROUND);
		item1.addListener(SWT.Selection, event -> fTipManager.setStartupBehavior(TipManager.START_BACKGROUND));
		item1.setImage(getStartupItemImage(TipManager.START_BACKGROUND));

		MenuItem item2 = new MenuItem(menu, SWT.CHECK);
		item2.setText(Messages.TipComposite_6);
		item2.setSelection(fTipManager.getStartupBehavior() == TipManager.START_DISABLE);
		item2.addListener(SWT.Selection, event -> fTipManager.setStartupBehavior(TipManager.START_DISABLE));
		item2.setImage(getStartupItemImage(TipManager.START_DISABLE));
	}

	private void showActionMenu() {
		Rectangle rect = fMultiActionButton.getBounds();
		Point pt = new Point(rect.x - 1, rect.y + rect.height);
		pt = fMultiActionButton.toDisplay(pt);
		fActionMenu.setLocation(pt.x, pt.y);
		fActionMenu.setVisible(true);
	}

	private void runTipAction(TipAction tipAction) {
		Job job = new Job(MessageFormat.format(Messages.TipComposite_10, tipAction.getTooltip())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					tipAction.getRunner().run();
				} catch (Exception e) {
					IStatus status = LogUtil.error(getClass(), e);
					fTipManager.log(status);
					return status;
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Sets the selected provider.
	 *
	 * @param provider the {@link TipProvider}
	 */
	public void setProvider(TipProvider provider) {
		if (provider == null) {
			return;
		}
		fNextTipButton.setEnabled(true);
		fProvider = provider;
		fSlider.setTipProvider(provider);
		getCurrentTip();
	}

	/**
	 * Schedules a TimerTask that is executed after {@value #READ_TIMER}
	 * milliseconds after which the tip is marked as read.
	 */
	private void hitTimer() {
		Tip timerTip = fCurrentTip;
		Timer timer = new Timer(Messages.TipComposite_11);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (timerTip == fCurrentTip) {
					fTipManager.setAsRead(timerTip);
					fSlider.updateButtons();
				}
				timer.cancel();
			}
		}, READ_TIMER);
	}

	private void getPreviousTip() {
		processTip(fProvider.getPreviousTip());
	}

	private void getNextTip() {
		if (fProvider.getTips().isEmpty() && !fTipManager.getProviders().isEmpty()) {
			fProvider.getNextTip(); // advance current tip
			for (TipProvider provider : fTipManager.getProviders()) {
				if (!provider.getTips().isEmpty()) {
					setProvider(provider);
					break;
				}
			}
		}
		processTip(fProvider.getNextTip());
	}

	private void getCurrentTip() {
		processTip(fProvider.getCurrentTip());
	}

	private void processTip(Tip tip) {
		fCurrentTip = tip;
		hitTimer();
		enableActionButtons(tip);
		prepareForHTML();
		loadContent(tip);
	}

	private void loadContent(Tip tip) {
		disposeBrowserFunctions();
		if (tip instanceof ISwtTip) {
			loadContentSWT(tip);
		} else if (tip instanceof IHtmlTip) {
			loadContentHtml((IHtmlTip) tip);
			applyBrowserFunctions(tip);
		} else if (tip instanceof IUrlTip) {
			loadContentUrl((IUrlTip) tip);
			applyBrowserFunctions(tip);
		} else {
			fTipManager.log(LogUtil.error(getClass(), Messages.TipComposite_12 + tip));
		}

		fContentComposite.requestLayout();
	}

	private void applyBrowserFunctions(Tip tip) {
		if (tip instanceof IBrowserFunctionProvider) {
			((IBrowserFunctionProvider) tip).getBrowserFunctions()
					.forEach((name, function) -> fBrowserFunctions.add(createBrowserFunction(name, function)));
		}
	}

	private BrowserFunction createBrowserFunction(String functionName, Function<Object[], Object> function) {
		return new BrowserFunction(getBrowser(), functionName) {
			@Override
			public Object function(Object[] arguments) {
				return function.apply(arguments);
			}
		};
	}

	private void loadContentHtml(IHtmlTip tip) {
		fBrowser.setText(getHTML(tip).trim());
	}

	private void loadContentUrl(IUrlTip tip) {
		try {
			String url = FileLocator.resolve(new URL(tip.getURL())).toString();
			fBrowser.setUrl(url);
		} catch (IOException e) {
			fTipManager.log(LogUtil.error(getClass(), e));
		}
	}

	private void loadContentSWT(Tip tip) {
		for (Control control : fSWTComposite.getChildren()) {
			control.dispose();
		}
		fContentStack.topControl = fSWTComposite;
		((ISwtTip) tip).createControl(fSWTComposite);
		fSWTComposite.requestLayout();
	}

	private void prepareForHTML() {
		fContentStack.topControl = fBrowserComposite;
		loadTimeOutScript();

		fBrowserComposite.requestLayout();
	}

	private void disposeBrowserFunctions() {
		fBrowserFunctions.forEach(BrowserFunction::dispose);
		fBrowserFunctions.clear();
	}

	/**
	 * Sets content in the browser that displays a message after 1500ms if the Tip
	 * could not load fast enough.
	 */
	private void loadTimeOutScript() {
		fBrowser.setText(getLoadingScript(500));
	}

	/**
	 * Sets content in the browser that displays a message after 1500ms if tips
	 * could not be loaded.
	 */
	private void loadWaitingScript() {
		fBrowser.setText(getWaitingScript(1500));
	}

	/**
	 * Get the timeout script in case the tips are not loading.
	 *
	 * @param timeout the timeout in milliseconds
	 * @return the script
	 */
	private static String getWaitingScript(int timeout) {
		return "<style>div{height: 90vh;display: flex;justify-content: center;align-items: center;}</style>" //$NON-NLS-1$
				+ "<div id=\"txt\"></div>" //$NON-NLS-1$
				+ "<script>var wss=function(){document.getElementById(\"txt\").innerHTML=\"" //$NON-NLS-1$
				+ Messages.TipComposite_14 //
				+ "\"};window.setTimeout(wss," //$NON-NLS-1$
				+ timeout //
				+ ");</script>"; //$NON-NLS-1$
	}

	private void enableActionButtons(Tip tip) {
		disposeActionImages();
		if (tip.getActions().isEmpty()) {
			fActionStack.topControl = fEmptyActionComposite;
		} else if (tip.getActions().size() == 1) {
			TipAction action = tip.getActions().get(0);
			fActionStack.topControl = fSingleActionComposite;
			fSingleActionButton.setImage(getActionImage(action.getTipImage()));
			fSingleActionButton.setText(action.getText());
			fSingleActionButton.setToolTipText(action.getTooltip());
		} else {
			TipAction action = tip.getActions().get(0);
			fActionStack.topControl = fMultiActionComposite;
			fMultiActionButton.setImage(getActionImage(tip.getActions().get(0).getTipImage()));
			fMultiActionButton.setText(action.getText());
			fMultiActionButton.setToolTipText(action.getTooltip());
			loadActionMenu(tip);
		}
		fEmptyActionComposite.getParent().requestLayout();
		fNavigationBar.requestLayout();
	}

	private void disposeActionImages() {
		fActionImages.forEach(Image::dispose);
	}

	private void loadActionMenu(Tip pTip) {
		if (fActionMenu != null) {
			fActionMenu.dispose();
		}
		fActionMenu = new Menu(fContentComposite.getShell(), SWT.POP_UP);
		pTip.getActions().subList(1, pTip.getActions().size()).forEach(action -> {
			MenuItem item = new MenuItem(fActionMenu, SWT.PUSH);
			item.setText(action.getText());
			item.setToolTipText(action.getTooltip());
			item.setText(action.getText());
			item.setImage(getActionImage(action.getTipImage()));
			item.addListener(SWT.Selection, e -> runTipAction(action));
		});
	}

	private Image getActionImage(TipImage tipImage) {
		if (tipImage == null) {
			return null;
		}
		try {
			Image image = new Image(getDisplay(), ImageUtil.decodeToImage(tipImage.getBase64Image()));
			if (image != null) {
				fActionImages.add(image);
				return image;
			}
		} catch (IOException e) {
			fTipManager.log(LogUtil.error(getClass(), e));
		}
		return null;
	}

	/**
	 * Get the timeout script in case a tip takes time to load.
	 *
	 * @param timeout the timeout in milliseconds
	 * @return the script
	 */
	private static String getLoadingScript(int timeout) {
		return "<style>div{position:fixed;top:50%;left:40%}</style>" //$NON-NLS-1$
				+ "<div id=\"txt\"></div>" //$NON-NLS-1$
				+ "<script>var wss=function(){document.getElementById(\"txt\").innerHTML=\"" //$NON-NLS-1$
				+ Messages.TipComposite_0 //
				+ "\"};window.setTimeout(wss," //$NON-NLS-1$
				+ timeout //
				+ ");</script>"; //$NON-NLS-1$
	}

	private String getHTML(IHtmlTip tip) {
		String encodedImage = encodeImage(tip);
		return tip.getHTML() + encodedImage;
	}

	private String encodeImage(IHtmlTip tip) {
		TipImage image = tip.getImage();
		if (image == null) {
			return EMPTY;
		}
		return encodeImageFromBase64(image);
	}

	private String encodeImageFromBase64(TipImage image) {
		int width = fBrowser.getClientArea().width;
		int height = Math.min(fBrowser.getClientArea().height / 2, (2 * (width / 3)));
		String attributes = image.getIMGAttributes(width, height).trim();
		String encoded = EMPTY + "<center> <img " // //$NON-NLS-1$
				+ attributes //
				+ " src=\"" // //$NON-NLS-1$
				+ image.getBase64Image() //
				+ "\"></center><br/>"; //$NON-NLS-1$
		return encoded;
	}

	@Override
	protected void checkSubclass() {
	}

	/**
	 * @return the {@link Browser} widget
	 */
	public Browser getBrowser() {
		return fBrowser;
	}

	/**
	 * @return the {@link Slider} widget
	 */
	public Slider getSlider() {
		return fSlider;
	}

	@Override
	public void selected(TipProvider provider) {
		setProvider(provider);
	}

	/**
	 * Sets the {@link TipManager}
	 *
	 * @param tipManager the {@link TipManager} that opened the dialog.
	 */
	public void setTipManager(TipManager tipManager) {
		fTipManager = tipManager;

		getDisplay().syncExec(() -> {
			fSlider.setTipManager(fTipManager);
			fUnreadOnly.setSelection(!fTipManager.mustServeReadTips());
			fPreviousTipButton.setEnabled(fTipManager.mustServeReadTips());
		});
	}

	@Override
	public void dispose() {
		disposeActionImages();
		super.dispose();
	}
}