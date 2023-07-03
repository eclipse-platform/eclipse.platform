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
package org.eclipse.help.ui.internal;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.ui.internal.views.HelpTray;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * ContextHelpDialog
 */
public class ContextHelpDialog {
	//private static ImageRegistry imgRegistry = null;

	private Color backgroundColour = null;

	private IContext context;

	private Color foregroundColour = null;

	private Color linkColour = null;

	private static HyperlinkHandler linkManager = new HyperlinkHandler();

	protected Shell parentShell;

	protected Shell shell;

	protected String infopopText;

	/**
	 * Listener for hyperlink selection.
	 */
	class LinkListener extends HyperlinkAdapter {
		IHelpResource topic;

		public LinkListener(IHelpResource topic) {
			this.topic = topic;
		}

		@Override
		public void linkActivated(Control c) {
			launchLinks(topic);
		}

	}

	/**
	 * Constructor:
	 *
	 * @param context
	 *            an array of String or an array of IContext
	 * @param x
	 *            the x mouse location in the current display
	 * @param y
	 *            the y mouse location in the current display
	 */
	ContextHelpDialog(IContext context, int x, int y) {
		this.context = context;
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		backgroundColour = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		foregroundColour = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		linkColour = display.getSystemColor(SWT.COLOR_BLUE);
		parentShell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();

		if (parentShell != null) {
			boolean isModal = 0 < (parentShell.getStyle() & (SWT.APPLICATION_MODAL
					| SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL));
			if (HelpUIPlugin.DEBUG_INFOPOP) {
				System.out
						.println("ContextHelpDialog.ContextHelpDialog(): ParentShell: " //$NON-NLS-1$
								+ parentShell.toString() + " is " //$NON-NLS-1$
								+ (isModal ? "modal" : "modeless")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		shell = new Shell(parentShell, SWT.NONE);
		if (HelpUIPlugin.DEBUG_INFOPOP) {
			System.out
					.println("ContextHelpDialog.ContextHelpDialog(): Shell is:" //$NON-NLS-1$
							+ shell.toString());
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpUIConstants.F1_SHELL);

		shell.addListener(SWT.Deactivate, e -> {
			if (HelpUIPlugin.DEBUG_INFOPOP) {
				System.out.println("ContextHelpDialog shell deactivate listener: SWT.Deactivate called. "); //$NON-NLS-1$
			}
			close();
		});

		shell.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				if (HelpUIPlugin.DEBUG_INFOPOP) {
					System.out.println(
							"ContextHelpDialog: shell traverse listener: SWT.TRAVERSE_ESCAPE called. "); //$NON-NLS-1$
				}
				e.doit = true;
			}
		});

		shell.addControlListener(new ControlAdapter() {

			@Override
			public void controlMoved(ControlEvent e) {
				if (HelpUIPlugin.DEBUG_INFOPOP) {
					System.out
							.println("ContextHelpDialog: shell control adapter called."); //$NON-NLS-1$
				}
				Rectangle clientArea = shell.getClientArea();
				shell.redraw(clientArea.x, clientArea.y, clientArea.width,
						clientArea.height, true);
				shell.update();
			}
		});
		if (HelpUIPlugin.DEBUG_INFOPOP) {
			System.out
					.println("ContextHelpDialog.ContextHelpDialog(): Focus owner is: " //$NON-NLS-1$
							+ Display.getCurrent().getFocusControl().toString());
		}
		linkManager
				.setHyperlinkUnderlineMode(HyperlinkHandler.UNDERLINE_ALWAYS);
		createContents(shell);
		shell.pack();
		// Correct x and y of the shell if it not contained within the screen
		int width = shell.getBounds().width;
		int height = shell.getBounds().height;

		Rectangle screen = display.getClientArea();
		// check lower boundaries
		x = x >= screen.x ? x : screen.x;
		y = y >= screen.y ? y : screen.y;
		// check upper boundaries
		x = x + width <= screen.width ? x : screen.width - width;
		y = y + height <= screen.height ? y : screen.height - height;
		shell.setLocation(x, y);

		initAccessible(shell);
	}

	public synchronized void close() {
		try {
			if (HelpUIPlugin.DEBUG_INFOPOP) {
				System.out.println("ContextHelpDialog.close()"); //$NON-NLS-1$
			}
			if (shell != null) {
				shell.close();
				if (!shell.isDisposed())
					shell.dispose();
				shell = null;
			}
		} catch (Throwable ex) {
		}
	}

	protected Control createContents(Composite contents) {
		initAccessible(contents);
		contents.setBackground(backgroundColour);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		contents.setLayout(layout);
		contents.setLayoutData(new GridData(GridData.FILL_BOTH));
		// create the dialog area and button bar
		createInfoArea(contents);
		Control c = createLinksArea(contents);
		if (c != null) {
			// links exist, make them the only focusable controls
			contents.setTabList(new Control[] { c });
		}
		return contents;
	}

	private Control createInfoArea(Composite parent) {
		// Create the text field.
		String styledText = null;
		if (context instanceof IContext2) {
			styledText = ((IContext2) context).getStyledText();
		}
		if (styledText == null && context.getText() != null) {
			styledText = context.getText();
			styledText= styledText.replaceAll("<b>","<@#\\$b>"); //$NON-NLS-1$ //$NON-NLS-2$
			styledText= styledText.replaceAll("</b>", "</@#\\$b>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (styledText == null) { // no description found in context objects.
			styledText = Messages.ContextHelpPart_noDescription;
		}
		Description text = new Description(parent, SWT.MULTI | SWT.READ_ONLY);
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				if (HelpUIPlugin.DEBUG_INFOPOP) {
					System.out.println(
							"ContextHelpDialog text TraverseListener.handleEvent(): SWT.TRAVERSE_ESCAPE."); //$NON-NLS-1$
				}
				e.doit = true;
			}
		});

		text.getCaret().setVisible(false);
		text.setBackground(backgroundColour);
		text.setForeground(foregroundColour);
		text.setFont(parent.getFont());
		int linkWidth = getLinksWidth(text);
		StyledLineWrapper content = new StyledLineWrapper(styledText, text,
				linkWidth + 70);
		text.setContent(content);
		text.setStyleRanges(content.getStyles());

		infopopText = text.getText();
		initAccessible(text);

		return text;
	}

	/**
	 * Measures the longest label of related links
	 *
	 * @param text
	 * @return
	 */
	private int getLinksWidth(Description text) {
		int linkWidth = 0;
		IHelpResource relatedTopics[] = context.getRelatedTopics();
		if (relatedTopics != null) {
			GC gc = new GC(text);
			for (int i = 0; i < relatedTopics.length; i++) {
				linkWidth = Math.max(linkWidth, gc.textExtent(relatedTopics[i]
						.getLabel()).x);
			}
			gc.dispose();
		}
		return linkWidth;
	}

	private Control createLink(Composite parent, IHelpResource topic) {
		Label image = new Label(parent, SWT.NONE);
		image.setImage(getImage());
		image.setBackground(backgroundColour);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
		//data.horizontalIndent = 4;
		image.setLayoutData(data);
		HyperlinkLabel link = new HyperlinkLabel(parent, SWT.NONE);
		link.setText(topic.getLabel());
		link.setBackground(backgroundColour);
		link.setForeground(linkColour);
		link.setFont(parent.getFont());
		linkManager.registerHyperlink(link, new LinkListener(topic));
		return link;
	}

	private Control createLinksArea(Composite parent) {
		IHelpResource[] relatedTopics = context.getRelatedTopics();
		if (relatedTopics == null)
			return null;
		// Create control
		Composite composite = new Composite(parent, SWT.NONE);
		initAccessible(composite);

		composite.setBackground(backgroundColour);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 2;
		layout.marginWidth = 0;
		layout.verticalSpacing = 3;
		layout.horizontalSpacing = 2;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());
		GridData data = new GridData(GridData.FILL_BOTH
				| GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.VERTICAL_ALIGN_CENTER);
		composite.setLayoutData(data);
		// Create separator.
		Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBackground(backgroundColour);
		label.setForeground(foregroundColour);
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING
				| GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		// Create related links
		for (int i = 0; i < relatedTopics.length; i++) {
			if (!UAContentFilter.isFiltered(relatedTopics[i], HelpEvaluationContext.getContext())) {
				createLink(composite, relatedTopics[i]);
			}
		}

		// create dynamic help link if current context allows dynamic help
		IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (DefaultHelpUI.isActiveShell(parentShell, wbWindow) || HelpTray.isAppropriateFor(parentShell)) {
			// Create separator.
			label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			label.setBackground(backgroundColour);
			label.setForeground(foregroundColour);
			data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING
					| GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 2;
			label.setLayoutData(data);

			// create link to the dynamic help
			createDynamicHelpLink(composite);
		}

		return composite;
	}

	private Control createDynamicHelpLink(Composite parent) {
		Label image = new Label(parent, SWT.NONE);
		Image img = HelpUIResources.getImage(IHelpUIConstants.IMAGE_DHELP);
		image.setImage(img);
		image.setBackground(backgroundColour);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
		//data.horizontalIndent = 4;
		image.setLayoutData(data);
		HyperlinkLabel link = new HyperlinkLabel(parent, SWT.NONE);
		link.setText(Messages.ContextHelpDialog_showInDynamicHelp);
		link.setBackground(backgroundColour);
		link.setForeground(linkColour);
		link.setFont(parent.getFont());
		linkManager.registerHyperlink(link, new HyperlinkAdapter() {

			@Override
			public void linkActivated(Control label) {
				openDynamicHelp();
			}
		});
		return link;
	}

	/**
	 * Called when related link has been chosen Opens help viewer with list of
	 * all related topics
	 */
	protected void launchLinks(IHelpResource selectedTopic) {
		close();
		if (HelpUIPlugin.DEBUG_INFOPOP) {
			System.out.println("ContextHelpDialog.launchLinks(): closed shell"); //$NON-NLS-1$
		}
		BaseHelpSystem.getHelpDisplay().displayHelp(
				context,
				selectedTopic,
				DefaultHelpUI.isDisplayModal(parentShell)
				&& !Constants.OS_WIN32.equalsIgnoreCase(Platform
								.getOS()));
	}

	private void openDynamicHelp() {
		BusyIndicator.showWhile(shell.getDisplay(), () -> {
			close();
			DefaultHelpUI.getInstance().displayContext(context, 0, 0, true);
		});
	}

	public synchronized void open() {
		try {
			shell.open();
			if (HelpUIPlugin.DEBUG_INFOPOP) {
				System.out
						.println("ContextHelpDialog.open(): Focus owner after open is: " //$NON-NLS-1$
								+ Display.getCurrent().getFocusControl()
										.toString());
			}
		} catch (Throwable e) {
			ILog.of(getClass()).error("An error occurred when opening context-sensitive help pop-up.", e); //$NON-NLS-1$
		}
	}

	private Image getImage() {
		return HelpUIResources.getImage(IHelpUIConstants.IMAGE_FILE_F1TOPIC);
	}

	public boolean isShowing() {
		return (shell != null && !shell.isDisposed() && shell.isVisible());
	}

	private void initAccessible(final Control control) {
		Accessible accessible = control.getAccessible();
		accessible.addAccessibleListener(new AccessibleAdapter() {

			@Override
			public void getName(AccessibleEvent e) {
				e.result = infopopText;
			}

			@Override
			public void getHelp(AccessibleEvent e) {
				e.result = control.getToolTipText();
			}
		});

		accessible.addAccessibleControlListener(new AccessibleControlAdapter() {

			@Override
			public void getChildAtPoint(AccessibleControlEvent e) {
				Point pt = control.toControl(new Point(e.x, e.y));
				e.childID = (control.getBounds().contains(pt)) ? ACC.CHILDID_MULTIPLE
						: ACC.CHILDID_NONE;
			}

			@Override
			public void getLocation(AccessibleControlEvent e) {
				Rectangle location = control.getBounds();
				Point pt = control.toDisplay(new Point(location.x, location.y));
				e.x = pt.x;
				e.y = pt.y;
				e.width = location.width;
				e.height = location.height;
			}

			@Override
			public void getChildCount(AccessibleControlEvent e) {
				e.detail = 1;
			}

			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_LABEL;
			}

			@Override
			public void getState(AccessibleControlEvent e) {
				e.detail = ACC.STATE_READONLY;
			}
		});
	}

	public static class Description extends StyledText {
		/**
		 * @param parent
		 * @param style
		 */
		public Description(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public boolean setFocus() {
			return false;
		}

		@Override
		public boolean isFocusControl() {
			return false;
		}
	}

}
