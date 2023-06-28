/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


/**
 * The label and icon part of the breadcrumb item.
 *
 * @since 3.5
 */
class BreadcrumbItemDetails {

	private final Label fElementImage;
	private final Label fElementText;
	private final Composite fDetailComposite;
	private final BreadcrumbItem fParent;
	private final Composite fTextComposite;
	private final Composite fImageComposite;

	private boolean fTextVisible;
	private boolean fSelected;
	private boolean fHasFocus;


	public BreadcrumbItemDetails(BreadcrumbItem parent, Composite parentContainer) {
		fParent= parent;
		fTextVisible= true;

		fDetailComposite= new Composite(parentContainer, SWT.NONE);
		fDetailComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		GridLayout layout= new GridLayout(2, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 0;
		fDetailComposite.setLayout(layout);
		addElementListener(fDetailComposite);

		fImageComposite= new Composite(fDetailComposite, SWT.NONE);
		fImageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 1;
		layout.marginWidth= 2;
		fImageComposite.setLayout(layout);
		fImageComposite.addPaintListener(e -> {
			if (fHasFocus && !isTextVisible()) {
				e.gc.drawFocus(e.x, e.y, e.width, e.height);
			}
		});
		installFocusComposite(fImageComposite);
		addElementListener(fImageComposite);

		fElementImage= new Label(fImageComposite, SWT.NONE);
		GridData layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementImage.setLayoutData(layoutData);
		addElementListener(fElementImage);

		fTextComposite= new Composite(fDetailComposite, SWT.NONE);
		fTextComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 2;
		layout.marginWidth= 2;
		fTextComposite.setLayout(layout);
		addElementListener(fTextComposite);
		fTextComposite.addPaintListener(e -> {
			if (fHasFocus && isTextVisible()) {
				e.gc.drawFocus(e.x, e.y, e.width, e.height);
			}
		});
		installFocusComposite(fTextComposite);
		addElementListener(fTextComposite);

		fElementText= new Label(fTextComposite, SWT.NONE);

		layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementText.setLayoutData(layoutData);
		addElementListener(fElementText);

		fTextComposite.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result= fElementText.getText();
			}
		});
		fImageComposite.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result= fElementText.getText();
			}
		});

		fDetailComposite.setTabList(new Control[] { fTextComposite });

		fDetailComposite.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDetailComposite"); //$NON-NLS-1$ //$NON-NLS-2$
		fTextComposite.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDetailTextComposite"); //$NON-NLS-1$ //$NON-NLS-2$
		fImageComposite.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDetailImageComposite"); //$NON-NLS-1$ //$NON-NLS-2$
		fElementImage.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDetailImageLabel"); //$NON-NLS-1$ //$NON-NLS-2$
		fElementText.setData("org.eclipse.e4.ui.css.id", "DebugBreadcrumbItemDetailTextLabel"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Returns whether this element has the keyboard focus.
	 *
	 * @return true if this element has the keyboard focus.
	 */
	public boolean hasFocus() {
		return fHasFocus;
	}

	/**
	 * Sets the tool tip to the given text.
	 *
	 * @param text the tool tip
	 */
	public void setToolTip(String text) {
		if (isTextVisible()) {
			fElementText.getParent().setToolTipText(text);
			fElementText.setToolTipText(text);

			fElementImage.setToolTipText(text);
		} else {
			fElementText.getParent().setToolTipText(null);
			fElementText.setToolTipText(null);

			fElementImage.setToolTipText(text);
		}
	}

	/**
	 * Sets the image to the given image.
	 *
	 * @param image the image to use
	 */
	public void setImage(Image image) {
		if (image != fElementImage.getImage()) {
			fElementImage.setImage(image);
		}
	}

	/**
	 * Sets the text to the given text.
	 *
	 * @param text the text to use
	 */
	public void setText(String text) {
		if (text == null) {
			text= ""; //$NON-NLS-1$
		}
		if (!text.equals(fElementText.getText())) {
			fElementText.setText(text);
		}
	}

	/**
	 * Returns the width of this element.
	 *
	 * @return current width of this element
	 */
	public int getWidth() {
		int result= 2;

		if (fElementImage.getImage() != null) {
			result+= fElementImage.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}

		if (fTextVisible && fElementText.getText().length() > 0) {
			result+= fElementText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}

		return result;
	}

	public void setTextVisible(boolean enabled) {
		if (fTextVisible == enabled) {
			return;
		}

		fTextVisible= enabled;

		GridData data= (GridData) fTextComposite.getLayoutData();
		data.exclude= !enabled;
		fTextComposite.setVisible(enabled);

		if (fTextVisible) {
			fDetailComposite.setTabList(new Control[] { fTextComposite });
		} else {
			fDetailComposite.setTabList(new Control[] { fImageComposite });
		}

		if (fHasFocus) {
			if (isTextVisible()) {
				fTextComposite.setFocus();
			} else {
				fImageComposite.setFocus();
			}
		}
	}

	/**
	 * Tells whether this item shows a text or only an image.
	 *
	 * @return <code>true</code> if it shows a text and an image, false if it only shows the image
	 */
	public boolean isTextVisible() {
		return fTextVisible;
	}

	public void setSelected(boolean selected) {
		if (selected == fSelected) {
			return;
		}

		fSelected= selected;

	}

	public void setFocus(boolean enabled) {
		if (enabled == fHasFocus) {
			return;
		}

		fHasFocus= enabled;
		if (fHasFocus) {
			if (isTextVisible()) {
				fTextComposite.setFocus();
			} else {
				fImageComposite.setFocus();
			}
		}
	}

	/**
	 * Install focus and key listeners to the given composite.
	 *
	 * @param composite the composite which may get focus
	 */
	private void installFocusComposite(Composite composite) {
		composite.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
				int index= fParent.getViewer().getIndexOfItem(fParent);
				if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
					index++;
				} else {
					index--;
				}

				if (index > 0 && index < fParent.getViewer().getItemCount()) {
					fParent.getViewer().selectItem(fParent.getViewer().getItem(index));
				}

				e.doit= true;
			}
		});
		composite.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				BreadcrumbViewer viewer= fParent.getViewer();

				switch (e.keyCode) {
					case SWT.ARROW_LEFT:
						if (fSelected) {
							viewer.doTraverse(false);
							e.doit= false;
						} else {
							viewer.selectItem(fParent);
						}
						break;
					case SWT.ARROW_RIGHT:
						if (fSelected) {
							viewer.doTraverse(true);
							e.doit= false;
						} else {
							viewer.selectItem(fParent);
						}
						break;
					case SWT.ARROW_DOWN:
					case SWT.ARROW_UP:
					case SWT.KEYPAD_ADD:
						if (!fSelected) {
							viewer.selectItem(fParent);
						}
						openDropDown();
						e.doit= false;
						break;
					case SWT.CR:
						if (!fSelected) {
							viewer.selectItem(fParent);
						}
						viewer.fireOpen();
						break;
					default:
						if (e.character == ' ') {
							if (!fSelected) {
								viewer.selectItem(fParent);
							}
							openDropDown();
							e.doit= false;
						}
						break;
				}
			}

			private void openDropDown() {
				Shell shell = fParent.getDropDownShell();
				if (shell == null) {
					fParent.openDropDownMenu();
					shell = fParent.getDropDownShell();
				}
				shell.setFocus();
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		composite.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if (!fHasFocus) {
					fHasFocus= true;
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (fHasFocus) {
					fHasFocus= false;
				}
			}
		});
	}

	/**
	 * Add mouse listeners to the given control.
	 *
	 * @param control the control to which may be clicked
	 */
	private void addElementListener(Control control) {
		control.addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}

			@Override
			public void mouseDown(MouseEvent e) {
				BreadcrumbViewer viewer= fParent.getViewer();
				Shell shell= fParent.getDropDownShell();
				viewer.selectItem(fParent);
				if (shell == null && e.button == 1 && e.stateMask == 0) {
					fParent.getViewer().fireDoubleClick();
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
			}
		});
		control.addMenuDetectListener(e -> {
			BreadcrumbViewer viewer= fParent.getViewer();
			viewer.selectItem(fParent);
			fParent.getViewer().fireMenuDetect(e);
		});
	}
}
