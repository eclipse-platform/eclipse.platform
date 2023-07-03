/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
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
 *     Asma Smaoui - CEA LIST - https://bugs.eclipse.org/bugs/show_bug.cgi?id=517379
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 559885
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.AbstractItemExtensionElement;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.CheatSheetStopWatch;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.data.Item;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetHyperlinkActionFactory.CheatSheetHyperlinkAction;

public abstract class ViewItem {

	public final static byte VIEWITEM_ADVANCE = 0;
	public final static byte VIEWITEM_DONOT_ADVANCE = 1;
	private Composite bodyComp;

	protected FormText bodyText;
	protected FormText completionText;
//	protected Label bodyText;
	protected Composite bodyWrapperComposite;
	protected Composite buttonComposite;
	protected Composite completionComposite;

	private boolean buttonExpanded = true;
	private boolean completionMessageExpanded = false;
	private Label checkDoneLabel;
	private boolean completed = false;

	protected Item item;

	// Colors
	protected Color itemColor;

	private boolean isSkipped = false;
	private ExpandableComposite mainItemComposite;

	private Composite parent;
	protected CheatSheetViewer viewer;
	protected CheatSheetPage page;
	private Composite titleComposite;
	private boolean bold = true;
	private Font boldFont;
	private Font regularFont;
	private boolean initialized = false;
	protected ArrayList<SubItemCompositeHolder> listOfSubItemCompositeHolders;

	/**
	 * Constructor for ViewItem.
	 */
	public ViewItem(CheatSheetPage page, Item item, Color itemColor, CheatSheetViewer viewer) {
		super();
		this.page = page;
		this.parent = page.getForm().getBody();
		this.item = item;
		this.itemColor = itemColor;
		this.viewer = viewer;
		addItem();
	}

	//Adds the item to the main composite.
	private void addItem() {
		CheatSheetStopWatch.startStopWatch("ViewItem.addItem()"); //$NON-NLS-1$
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after getBannerBackground: "); //$NON-NLS-1$ //$NON-NLS-2$

		//		Set up the main composite for the item.******************************************
		checkDoneLabel = page.getToolkit().createLabel(parent, " "); //$NON-NLS-1$
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create checkDoneLabel: "); //$NON-NLS-1$ //$NON-NLS-2$

		mainItemComposite = page.getToolkit().createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.COMPACT);
		mainItemComposite.setBackground(itemColor);
		mainItemComposite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		String title = item.getTitle();
		if (title != null) {
			mainItemComposite.setText(ViewUtilities.escapeForLabel(title));
		}
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create mainItemComposite: "); //$NON-NLS-1$ //$NON-NLS-2$


		mainItemComposite.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				page.getForm().reflow(true);
			}
		});
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after addExpansionListener: "); //$NON-NLS-1$ //$NON-NLS-2$

		// handle item extensions here
		// check number of extensions for this item and adjust layout accordingly
		int number = 0;
		ArrayList<AbstractItemExtensionElement[]> itemExts = item.getItemExtensions();

		if((itemExts != null && itemExts.size() > 0) || item.getContextId() != null || item.getHref() != null) {
			// Set up the title composite for the item.
			titleComposite = page.getToolkit().createComposite(mainItemComposite);
			titleComposite.setBackground(itemColor);
		}

		if(itemExts != null) {
			for (int g = 0; g < itemExts.size(); g++) {
				AbstractItemExtensionElement[] eea = itemExts.get(g);
				number += eea.length;
				for (int x = 0; x < eea.length; x++) {
					eea[x].createControl(titleComposite);
				}
			}
		}
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create item extensions: "); //$NON-NLS-1$ //$NON-NLS-2$

		// don't add the help icon unless there is a context id or help link
		if(item.getContextId() != null || item.getHref() != null) {
			// adjust the layout count
			number++;
			ImageHyperlink helpButton = createButton(titleComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_HELP), this, itemColor, Messages.HELP_BUTTON_TOOLTIP);
			helpButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					// If we have a context id, handle this first and ignore an hrefs
					if(item.getContextId() != null) {
						openInfopop(e.widget);
					} else {
						// We only have an href, so let's open it in the help system
						openHelpTopic();
					}
				}
			});
		}
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create help button: "); //$NON-NLS-1$ //$NON-NLS-2$

		if(number > 0) {
			mainItemComposite.setTextClient(titleComposite);
			GridLayout layout = new GridLayout(number, false);
			GridData data = new GridData(GridData.FILL_BOTH);

			titleComposite.setLayout(layout);
			titleComposite.setLayoutData(data);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.verticalSpacing = 0;
		}
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after setTextClient: "); //$NON-NLS-1$ //$NON-NLS-2$

		//Body wrapper here.   this composite will be hidden and shown as appropriate.
		bodyWrapperComposite = page.getToolkit().createComposite(mainItemComposite);
		mainItemComposite.setClient(bodyWrapperComposite);
		TableWrapLayout wrapperLayout = new TableWrapLayout();
		bodyWrapperComposite.setLayout(wrapperLayout);
		bodyWrapperComposite.setBackground(itemColor);
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create bodyWrapperComposite: "); //$NON-NLS-1$ //$NON-NLS-2$

		bodyText = page.getToolkit().createFormText(bodyWrapperComposite, false);
		bodyText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Action copyAction = viewer.getCopyAction();
				if (copyAction != null) {
					copyAction.setEnabled(bodyText.canCopy());
				}
			}
		});
		bodyText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				Action copyAction = viewer.getCopyAction();
				if (copyAction != null) {
					copyAction.setEnabled(bodyText.canCopy());
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
				Action copyAction = viewer.getCopyAction();
				if (copyAction != null) {
					copyAction.setEnabled(false);
				}
			}
		});

		bodyText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent event) {
				String url = (String) event.getHref();
				CheatSheetHyperlinkActionFactory actionFactory = new CheatSheetHyperlinkActionFactory();
				CheatSheetHyperlinkAction cheatSheetHyperlink = actionFactory.create(url);
				cheatSheetHyperlink.execute();
			}
		});

//		bodyText = toolkit.createLabel(bodyWrapperComposite, item.getDescription(), SWT.WRAP);
		bodyText.setText(item.getDescription(), item.getDescription().startsWith(IParserTags.FORM_START_TAG), false);

		//Set up the body text portion here.
		bodyText.setBackground(itemColor);
		bodyText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after create FormText: "); //$NON-NLS-1$ //$NON-NLS-2$

		//Handle the sub-steps and regular buttons here.
		//First Check to see if there is sub steps.  If there is, don't create the button comp,
		//As it will be handled by the CoreItemWithSubs.
		//If there is no sub steps, create a button composite and Pass it to CoreItem using the handleButtons.

		if(!item.isDynamic()) {
			handleButtons();
		}
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after handleButtons(): "); //$NON-NLS-1$ //$NON-NLS-2$

		setButtonsVisible(false);
		setCollapsed();
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after setting buttons and item collapsed: "); //$NON-NLS-1$ //$NON-NLS-2$

		boldFont = mainItemComposite.getFont();
		FontData[] fontDatas = boldFont.getFontData();
		for (int i = 0; i < fontDatas.length; i++) {
			fontDatas[i].setStyle(fontDatas[i].getStyle() ^ SWT.BOLD);
		}
		regularFont = new Font(mainItemComposite.getDisplay(), fontDatas);
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after font initlization: "); //$NON-NLS-1$ //$NON-NLS-2$

		setBold(false);
		CheatSheetStopWatch.printLapTime("ViewItem.addItem()", "Time in addItem() after setBold: "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected ImageHyperlink createButtonWithText(Composite parent, Image image, ViewItem item, Color color, String linkText) {
		ImageHyperlink button = page.getToolkit().createImageHyperlink(parent, SWT.NULL);
		button.setImage(image);
		button.setData(item);
		button.setBackground(color);
		button.setText(linkText);
		button.setToolTipText(linkText);
		return button;
	}

	protected ImageHyperlink createButton(Composite parent, Image image, ViewItem item, Color color, String toolTipText) {
		ImageHyperlink button = new ImageHyperlink(parent, SWT.NULL);
		page.getToolkit().adapt(button, true, true);
		button.setImage(image);
		button.setData(item);
		button.setBackground(color);
		button.setToolTipText(toolTipText);
//		button.setFAccessibleDescription(bodyText.getText());
//		button.setFAccessibleName(button.getToolTipText());

		return button;
	}

	public void dispose() {
		if (checkDoneLabel != null)
			checkDoneLabel.dispose();
		if (bodyText != null)
			bodyText.dispose();
		if (buttonComposite != null)
			buttonComposite.dispose();
		if (completionComposite != null)
			completionComposite.dispose();
		if (bodyComp != null)
			bodyComp.dispose();
		if (bodyWrapperComposite != null)
			bodyWrapperComposite.dispose();
		if (mainItemComposite != null)
			mainItemComposite.dispose();
		if (titleComposite != null)
			titleComposite.dispose();
		if (regularFont != null)
			regularFont.dispose();

		ArrayList<AbstractItemExtensionElement[]> itemExts = item.getItemExtensions();
		if (itemExts != null) {
			for (int g = 0; g < itemExts.size(); g++) {
				AbstractItemExtensionElement[] eea = itemExts.get(g);
				for (int x = 0; x < eea.length; x++) {
					eea[x].dispose();
				}
			}
		}
	}

	/**
	 * @return
	 */
	/*package*/
	Image getCompleteImage() {
		return CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_COMPLETE);
	}

	/**
	 * @return
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * Returns the mainItemComposite.
	 * @return Composite
	 */
	/*package*/
	Composite getMainItemComposite() {
		return mainItemComposite;
	}

	/**
	 * @return
	 */
	/*package*/
	Image getSkipImage() {
		return CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_SKIP);
	}

	//Adds the buttons to the buttonComposite.
	/*package*/
	abstract void handleButtons();

	/*package*/
	boolean isBold() {
		return bold;
	}

	/**
	 * Returns the completed.
	 * @return boolean
	 */
	public boolean isCompleted() {
		return completed;
	}

	public boolean isExpanded() {
		return mainItemComposite.isExpanded();
	}

	public boolean isCompletionMessageExpanded() {
		return completionMessageExpanded;
	}

	/**
	 * Returns whether or not cheat sheet viewer containing this item is in
	 * a modal dialog.
	 *
	 * @return whether the cheat sheet viewer is in a modal dialog
	 */
	public boolean isInDialogMode() {
		return viewer.isInDialogMode();
	}

	/*package*/
	boolean isSkipped() {
		return isSkipped;
	}

	/**
	 * Open a help topic
	 */
	private void openHelpTopic() {
		if (item == null || item.getHref() == null) {
			return;
		}

		PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(item.getHref());
	}

	/**
	 * Open an infopop
	 */
	private void openInfopop(Widget widget) {
		if (item == null || item.getContextId() == null) {
			return;
		}

		IContext context = HelpSystem.getContext(item.getContextId());

		if (context != null) {
			// determine a location in the upper right corner of the widget
			Point point = widget.getDisplay().getCursorLocation();
			point = new Point(point.x + 15, point.y);
			// display the help
			PlatformUI.getWorkbench().getHelpSystem().displayContext(context, point.x, point.y);
		}
	}

	public void setAsCurrentActiveItem() {
		setColorAsCurrent(true);
		setButtonsVisible(true);
		setBold(true);
		setExpanded();
		setFocus();
	}

	protected void setFocus() {
		mainItemComposite.setFocus();
		FormToolkit.ensureVisible(getMainItemComposite());
	}

	/*package*/
	void setAsNormalCollapsed() {
		setBold(false);
		setColorAsCurrent(false);
		if (mainItemComposite.isExpanded())
			setCollapsed();
	}

	/*package*/
	void setAsNormalNonCollapsed() {
		setColorAsCurrent(false);
		setBold(false);
	}

	private void setBodyColor(Color color) {
		mainItemComposite.setBackground(color);
		setBackgroundColor(bodyWrapperComposite, color);
		setBackgroundColor(buttonComposite, color);
		setBackgroundColor(completionComposite, color);
	}

	/*
	 * Set the background color of this composite and its children.
	 * If the composite is null do nothing.
	 */
	protected void setBackgroundColor(Composite composite, Color color) {
		if (composite != null) {
			composite.setBackground(color);
			Control[] children = composite.getChildren();
			for (Control element : children) {
				element.setBackground(color);
			}
		}
	}

	/*package*/
	void setBold(boolean value) {
		if(value) {
			mainItemComposite.setFont(boldFont);
			if(initialized)
				mainItemComposite.layout();
		} else {
			mainItemComposite.setFont(regularFont);
			if(initialized)
				mainItemComposite.layout();
		}
		bold = value;
	}

	//collapse or expand the item
	/*package*/
	void setButtonsVisible(boolean isVisible) {
		if (buttonExpanded != isVisible) {
			if (listOfSubItemCompositeHolders != null) {
				for (Iterator<SubItemCompositeHolder> iter = listOfSubItemCompositeHolders.iterator(); iter
						.hasNext();) {
					iter.next().setButtonsVisible(isVisible);
				}
			} else if (buttonComposite != null) {
				buttonComposite.setVisible(isVisible);
			}
		}

		if(isVisible && initialized) {
			FormToolkit.ensureVisible(getMainItemComposite());
		}
		buttonExpanded = isVisible;
	}

	protected void setCompletionMessageExpanded(boolean isFinalItem) {
		if (hasCompletionMessage()) {
			if (completionComposite == null) {
				createCompletionComposite(isFinalItem);
			}
			if (!completionMessageExpanded) {
				completionComposite.setVisible(true);
				completionMessageExpanded = true;
			}
		}
	}

	abstract void createCompletionComposite(boolean isFinalItem);

	protected void setCompletionMessageCollapsed() {
		if (completionComposite != null) {
			if (completionMessageExpanded) {
				completionComposite.dispose();
				completionComposite = null;
				page.getForm().reflow(true);
			}
		}
		completionMessageExpanded = false;
	}

	//collapses the item
	/*package*/
	void setCollapsed() {
		if (mainItemComposite.isExpanded()) {
			mainItemComposite.setExpanded(false);
			if(initialized) {
				page.getForm().reflow(true);
				FormToolkit.ensureVisible(getMainItemComposite());
			}
		}
	}

	private void setColorAsCurrent(boolean active) {
		if (active) {
			setTitleColor(page.getActiveColor());
			setBodyColor(page.getActiveColor());
		} else {
			setTitleColor(itemColor);
			setBodyColor(itemColor);
		}
	}

	//marks the item as complete.
	public void setComplete() {
		completed = true;
		checkDoneLabel.setImage(getCompleteImage());

		if(initialized) {
			checkDoneLabel.getParent().layout();
		}
	}

	//expands the item
	public void setExpanded() {
		if (!mainItemComposite.isExpanded()) {
			mainItemComposite.setExpanded(true);
			if(initialized) {
				page.getForm().reflow(true);
				FormToolkit.ensureVisible(getMainItemComposite());
			}
		}
	}

	/*package*/
	void setIncomplete() {
		checkDoneLabel.setImage(null);
		completed = false;
		setStartImage();
	}

	/**
	 * Sets the itemColor.
	 * @param itemColor The itemColor to set
	 */
	/*package*/
	void setItemColor(Color itemColor) {
		this.itemColor = itemColor;
	}

	/*package*/
	void setOriginalColor() {
		setTitleColor(itemColor);
		setBodyColor(itemColor);
		setBold(false);
	}

	/*package*/
	abstract void setRestartImage();

	/*package*/
	void setSkipped() {
		isSkipped = true;
		checkDoneLabel.setImage(getSkipImage());

		if(initialized) {
			checkDoneLabel.getParent().layout();
		}
	}
	/*package*/
	abstract void setStartImage();

	private void setTitleColor(Color bg) {
		if(titleComposite != null) {
			titleComposite.setBackground(bg);

			Control[] titlechildren = titleComposite.getChildren();
			for (Control element : titlechildren) {
				element.setBackground(bg);
			}
		}
	}

	public void initialized() {
		initialized = true;
	}

	public boolean canCopy() {
		return (bodyText!=null && !bodyText.isDisposed())?bodyText.canCopy():false;
	}
	public void copy() {
		if (bodyText!=null && !bodyText.isDisposed())
			bodyText.copy();
	}

	abstract boolean hasCompletionMessage();

	boolean hasFocus() {
		return bodyText.isFocusControl();
	}

}
