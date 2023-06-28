/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - adapted breadcrumb for use in Debug view (Bug 252677)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.breadcrumb;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;


/**
 * The part of the breadcrumb item with the drop down menu.
 *
 * @since 3.5
 */
class BreadcrumbItemDropDown implements IBreadcrumbDropDownSite {

	private static final boolean IS_MAC_WORKAROUND= "carbon".equals(SWT.getPlatform()); //$NON-NLS-1$

	/**
	 * An arrow image descriptor. The images color is related to the list
	 * fore- and background color. This makes the arrow visible even in high contrast
	 * mode. If <code>ltr</code> is true the arrow points to the right, otherwise it
	 * points to the left.
	 */
	private final class AccessibleArrowImage extends CompositeImageDescriptor {

		private final static int ARROW_SIZE= 5;

		private final boolean fLTR;

		public AccessibleArrowImage(boolean ltr) {
			fLTR= ltr;
		}

		/*
		 * @see org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage(int, int)
		 */
		@Override
		protected void drawCompositeImage(int width, int height) {
			Display display= fParentComposite.getDisplay();
			ImageDataProvider imageProvider = zoom -> {
				Image image = new Image(display, ARROW_SIZE, ARROW_SIZE * 2);

				GC gc = new GC(image, fLTR ? SWT.LEFT_TO_RIGHT : SWT.RIGHT_TO_LEFT);
				gc.setAntialias(SWT.ON);

				Color triangleColor = createColor(SWT.COLOR_LIST_FOREGROUND, SWT.COLOR_LIST_BACKGROUND, 20, display);
				gc.setBackground(triangleColor);
				gc.fillPolygon(new int[] {
						0, 0, ARROW_SIZE, ARROW_SIZE, 0, ARROW_SIZE * 2 });
				gc.dispose();
				triangleColor.dispose();

				ImageData imageData = image.getImageData(zoom);
				image.dispose();
				int zoomedArrowSize = ARROW_SIZE * zoom / 100;
				for (int y1 = 0; y1 < zoomedArrowSize; y1++) {
					// set opaque pixels for top half of the breadcrumb arrow
					for (int x1 = 0; x1 <= y1; x1++) {
						imageData.setAlpha(fLTR ? x1 : zoomedArrowSize - x1 - 1, y1, 255);
					}
					// set transparent pixels for top half of the breadcrumbe arrow
					for (int x1 = y1 + 1; x1 < zoomedArrowSize; x1++) {
						imageData.setAlpha(fLTR ? x1 : zoomedArrowSize - x1 - 1, y1, 0);
					}
				}
				for (int y2 = 0; y2 < zoomedArrowSize; y2++) {
					// set opaque pixels for bottom half of the breadcrumb arrow
					for (int x2 = 0; x2 <= y2; x2++) {
						imageData.setAlpha(fLTR ? x2 : zoomedArrowSize - x2 - 1, zoomedArrowSize * 2 - y2 - 1, 255);
					}
					// set transparent pixels for bottom half of the breadcrumbe arrow
					for (int x2 = y2 + 1; x2 < zoomedArrowSize; x2++) {
						imageData.setAlpha(fLTR ? x2 : zoomedArrowSize - x2 - 1, zoomedArrowSize * 2 - y2 - 1, 0);
					}
				}
				return imageData;
			};
			drawImage(imageProvider, (width / 2) - (ARROW_SIZE / 2), (height / 2) - ARROW_SIZE);

		}

		/*
		 * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
		 */
		@Override
		protected Point getSize() {
			return new Point(10, 16);
		}

		private Color createColor(int color1, int color2, int ratio, Display display) {
			RGB rgb1= display.getSystemColor(color1).getRGB();
			RGB rgb2= display.getSystemColor(color2).getRGB();

			RGB blend= BreadcrumbViewer.blend(rgb2, rgb1, ratio);

			return new Color(display, blend);
		}
	}

	// Workaround for bug 258196: set the minimum size to 500 because on Linux
	// the size is not adjusted correctly in a virtual tree.
	private static final int DROP_DOWN_MIN_WIDTH= 500;
	private static final int DROP_DOWN_MAX_WIDTH= 501;

	private static final int DROP_DOWN_DEFAULT_MIN_HEIGHT= 100;
	private static final int DROP_DOWN_DEFAULT_MAX_HEIGHT= 500;

	private static final String DIALOG_SETTINGS= "BreadcrumbItemDropDown"; //$NON-NLS-1$
	private static final String DIALOG_HEIGHT= "height"; //$NON-NLS-1$
	private static final String DIALOG_WIDTH= "width"; //$NON-NLS-1$

	private final BreadcrumbItem fParent;
	private final Composite fParentComposite;
	private final ToolBar fToolBar;

	private boolean fMenuIsShown;
	private boolean fEnabled;
	private Shell fShell;
	private boolean fIsResizingProgrammatically;
	private int fCurrentWidth = -1;
	private int fCurrentHeight = -1;


	public BreadcrumbItemDropDown(BreadcrumbItem parent, Composite composite) {
		fParent= parent;
		fParentComposite= composite;
		fMenuIsShown= false;
		fEnabled= true;

		fToolBar= new ToolBar(composite, SWT.FLAT);
		fToolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		fToolBar.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result= BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip;
			}
		});
		ToolBarManager manager= new ToolBarManager(fToolBar);

		final Action showDropDownMenuAction= new Action(null, SWT.NONE) {
			@Override
			public void run() {
				Shell shell= fParent.getDropDownShell();
				if (shell != null) {
					return;
				}

				shell= fParent.getViewer().getDropDownShell();
				if (shell != null && !shell.isDisposed()) {
					shell.close();
				}

				showMenu();

				fShell.setFocus();
			}
		};

		showDropDownMenuAction.setImageDescriptor(new AccessibleArrowImage(isLeft()));
		showDropDownMenuAction.setToolTipText(BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip);
		manager.add(showDropDownMenuAction);

		manager.update(true);
		if (IS_MAC_WORKAROUND) {
			manager.getControl().addMouseListener(new MouseAdapter() {
				// see also BreadcrumbItemDetails#addElementListener(Control)
				@Override
				public void mouseDown(MouseEvent e) {
					showDropDownMenuAction.run();
				}
			});
		}
		fToolBar.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDropDownToolBar"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Return the width of this element.
	 *
	 * @return the width of this element
	 */
	public int getWidth() {
		return fToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
	}

	/**
	 * Set whether the drop down menu is available.
	 *
	 * @param enabled true if available
	 */
	public void setEnabled(boolean enabled) {
		fEnabled= enabled;

		fToolBar.setVisible(enabled);
	}

	/**
	 * Tells whether the menu is shown.
	 *
	 * @return true if the menu is open
	 */
	public boolean isMenuShown() {
		return fMenuIsShown;
	}

	/**
	 * Returns the shell used for the drop down menu if it is shown.
	 *
	 * @return the drop down shell or <code>null</code>
	 */
	public Shell getDropDownShell() {
		if (!isMenuShown()) {
			return null;
		}

		return fShell;
	}

	/**
	 * Opens the drop down menu.
	 */
	public void showMenu() {
		if (DebugUIPlugin.DEBUG_BREADCRUMB) {
			DebugUIPlugin.trace("BreadcrumbItemDropDown.showMenu()"); //$NON-NLS-1$
		}

		if (!fEnabled || fMenuIsShown) {
			return;
		}

		fMenuIsShown= true;

		fShell= new Shell(fToolBar.getShell(), SWT.RESIZE | SWT.TOOL | SWT.ON_TOP);
		if (DebugUIPlugin.DEBUG_BREADCRUMB) {
			DebugUIPlugin.trace("	creating new shell"); //$NON-NLS-1$
		}

		fShell.addControlListener(new ControlAdapter() {
			/*
			 * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse.swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(ControlEvent e) {
				if (fIsResizingProgrammatically) {
					return;
				}

				Point size= fShell.getSize();
				fCurrentWidth = size.x;
				fCurrentHeight = size.y;
				getDialogSettings().put(DIALOG_WIDTH, size.x);
				getDialogSettings().put(DIALOG_HEIGHT, size.y);
			}
		});

		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fShell.setLayout(layout);

		Composite composite= new Composite(fShell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);

		TreePath path= fParent.getPath();

		Control control = fParent.getViewer().createDropDown(composite, this, path);

		control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setShellBounds(fShell);
		fShell.setVisible(true);
		installCloser(fShell);
	}

	/**
	 * The closer closes the given shell when the focus is lost.
	 *
	 * @param shell the shell to install the closer to
	 */
	private void installCloser(final Shell shell) {
		final Listener focusListener= event -> {
			Widget focusElement= event.widget;
			boolean isFocusBreadcrumbTreeFocusWidget= focusElement == shell || focusElement instanceof Control && ((Control)focusElement).getShell() == shell;
			boolean isFocusWidgetParentShell= focusElement instanceof Control && ((Control)focusElement).getShell().getParent() == shell;

			switch (event.type) {
				case SWT.FocusIn:
					if (DebugUIPlugin.DEBUG_BREADCRUMB) {
						DebugUIPlugin.trace("focusIn - is breadcrumb tree: " + isFocusBreadcrumbTreeFocusWidget); //$NON-NLS-1$
					}

					if (!isFocusBreadcrumbTreeFocusWidget && !isFocusWidgetParentShell) {
						if (DebugUIPlugin.DEBUG_BREADCRUMB) {
							DebugUIPlugin.trace("==> closing shell since focus in other widget"); //$NON-NLS-1$
						}
						shell.close();
					}
					break;

				case SWT.FocusOut:
					if (DebugUIPlugin.DEBUG_BREADCRUMB) {
						DebugUIPlugin.trace("focusOut - is breadcrumb tree: " + isFocusBreadcrumbTreeFocusWidget); //$NON-NLS-1$
					}
					if (event.display.getActiveShell() == null) {
						if (DebugUIPlugin.DEBUG_BREADCRUMB) {
							DebugUIPlugin.trace("==> closing shell since event.display.getActiveShell() != shell"); //$NON-NLS-1$
						}
						shell.close();
					}
					break;

				default:
					Assert.isTrue(false);
			}
		};

		final Display display= shell.getDisplay();
		display.addFilter(SWT.FocusIn, focusListener);
		display.addFilter(SWT.FocusOut, focusListener);

		final ControlListener controlListener= new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
				if (!shell.isDisposed()) {
					shell.close();
				}
			}

			@Override
			public void controlResized(ControlEvent e) {
				if (!shell.isDisposed()) {
					shell.close();
				}
			}
		};
		fToolBar.getShell().addControlListener(controlListener);

		shell.addDisposeListener(e -> {
			if (DebugUIPlugin.DEBUG_BREADCRUMB) {
				DebugUIPlugin.trace("==> shell disposed"); //$NON-NLS-1$
			}

			display.removeFilter(SWT.FocusIn, focusListener);
			display.removeFilter(SWT.FocusOut, focusListener);

			if (!fToolBar.isDisposed()) {
				fToolBar.getShell().removeControlListener(controlListener);
			}
		});
		shell.addShellListener(new ShellListener() {
			@Override
			public void shellActivated(ShellEvent e) {
			}

			@Override
			public void shellClosed(ShellEvent e) {
				if (DebugUIPlugin.DEBUG_BREADCRUMB) {
					DebugUIPlugin.trace("==> shellClosed"); //$NON-NLS-1$
				}
				if (!fMenuIsShown) {
					return;
				}

				fMenuIsShown= false;
			}

			@Override
			public void shellDeactivated(ShellEvent e) {
			}

			@Override
			public void shellDeiconified(ShellEvent e) {
			}

			@Override
			public void shellIconified(ShellEvent e) {
			}
		});
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings javaSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(BreadcrumbItemDropDown.class)).getDialogSettings();
		IDialogSettings settings= javaSettings.getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings= javaSettings.addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	private int getMaxWidth() {
		try {
			return getDialogSettings().getInt(DIALOG_WIDTH);
		} catch (NumberFormatException e) {
			return DROP_DOWN_MAX_WIDTH;
		}
	}

	private int getMaxHeight() {
		try {
			return getDialogSettings().getInt(DIALOG_HEIGHT);
		} catch (NumberFormatException e) {
			return DROP_DOWN_DEFAULT_MAX_HEIGHT;
		}
	}

	/**
	 * Calculates a useful size for the given shell.
	 *
	 * @param shell the shell to calculate the size for.
	 */
	private void setShellBounds(Shell shell) {

		Rectangle rect= fParentComposite.getBounds();
		Rectangle toolbarBounds= fToolBar.getBounds();

		Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		int height= Math.max(Math.min(size.y, getMaxHeight()), DROP_DOWN_DEFAULT_MIN_HEIGHT);
		int width= Math.max(getMaxWidth(), DROP_DOWN_MIN_WIDTH);

		int imageBoundsX= 0;
		if (fParent.getImage() != null) {
			imageBoundsX= fParent.getImage().getImageData().width;
		}

		Rectangle trim= fShell.computeTrim(0, 0, width, height);
		int x= toolbarBounds.x + toolbarBounds.width + 2 + trim.x - imageBoundsX;
		if (!isLeft()) {
			x+= width;
		}

		int y = rect.y;
		if (isTop()) {
			y+= rect.height;
		} else {
			y-= height;
		}

		Point pt= new Point(x, y);
		pt= fParentComposite.toDisplay(pt);

		Rectangle monitor = Util.getClosestMonitor(shell.getDisplay(), pt).getClientArea();
		int overlap= (pt.x + width) - (monitor.x + monitor.width);
		if (overlap > 0) {
			pt.x-= overlap;
		}
		if (pt.x < monitor.x) {
			pt.x= monitor.x;
		}

		shell.setLocation(pt);
		fIsResizingProgrammatically= true;
		try {
			shell.setSize(width, height);
			fCurrentWidth = width;
			fCurrentHeight = height;
		} finally {
			fIsResizingProgrammatically= false;
		}
	}

	/**
	 * Set the size of the given shell such that more content can be shown. The shell size does not
	 * exceed a user-configurable maximum.
	 *
	 * @param shell the shell to resize
	 */
	private void resizeShell(final Shell shell) {
		int maxHeight= getMaxHeight();
		int maxWidth = getMaxWidth();

		if (fCurrentHeight >= maxHeight && fCurrentWidth >= maxWidth) {
			return;
		}

		Point preferedSize= shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		int newWidth;
		if (fCurrentWidth >= DROP_DOWN_MAX_WIDTH) {
			newWidth= fCurrentWidth;
		} else {
			// Workaround for bug 319612: Do not resize width below the
			// DROP_DOWN_MIN_WIDTH.  This can happen because the Shell.getSize()
			// is incorrectly small on Linux.
			newWidth= Math.min(Math.max(Math.max(preferedSize.x, fCurrentWidth), DROP_DOWN_MIN_WIDTH), maxWidth);
		}
		int newHeight;
		if (fCurrentHeight >= maxHeight) {
			newHeight= fCurrentHeight;
		} else {
			newHeight= Math.min(Math.max(preferedSize.y, fCurrentHeight), maxHeight);
		}

		if (newHeight != fCurrentHeight || newWidth != fCurrentWidth) {
			shell.setRedraw(false);
			try {
				fIsResizingProgrammatically= true;
				shell.setSize(newWidth, newHeight);
				fCurrentWidth = newWidth;
				fCurrentHeight = newHeight;

				Point location = shell.getLocation();
				Point newLocation = location;
				if (!isLeft()) {
					newLocation = new Point(newLocation.x - (newWidth - fCurrentWidth), newLocation.y);
				}
				if (!isTop()) {
					newLocation = new Point(newLocation.x, newLocation.y - (newHeight - fCurrentHeight));
				}
				if (!location.equals(newLocation)) {
					shell.setLocation(newLocation.x, newLocation.y);
				}
			} finally {
				fIsResizingProgrammatically= false;
				shell.setRedraw(true);
			}
		}
	}

	/**
	 * Tells whether this the breadcrumb is in LTR mode or RTL mode.  Or whether the breadcrumb
	 * is on the right-side status coolbar, which has the same effect on layout.
	 *
	 * @return <code>true</code> if the breadcrumb in left-to-right mode, <code>false</code>
	 *         otherwise
	 */
	private boolean isLeft() {
		return (fParentComposite.getStyle() & SWT.RIGHT_TO_LEFT) == 0 &&
			(fParent.getViewer().getStyle() & SWT.RIGHT) == 0;
	}

	/**
	 * Tells whether this the breadcrumb is in LTR mode or RTL mode.  Or whether the breadcrumb
	 * is on the right-side status coolbar, which has the same effect on layout.
	 *
	 * @return <code>true</code> if the breadcrumb in left-to-right mode, <code>false</code>
	 *         otherwise
	 */
	private boolean isTop() {
		return (fParent.getViewer().getStyle() & SWT.BOTTOM) == 0;
	}

	@Override
	public void close() {
		if (fShell != null && !fShell.isDisposed()) {
			fShell.close();
		}
	}

	@Override
	public void notifySelection(ISelection selection) {
		fParent.getViewer().fireMenuSelection(selection);
	}

	@Override
	public void updateSize() {
		if (fShell != null && !fShell.isDisposed()) {
			resizeShell(fShell);
		}
	}
}

