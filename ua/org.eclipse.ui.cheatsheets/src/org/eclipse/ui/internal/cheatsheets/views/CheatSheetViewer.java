/*******************************************************************************
 *  Copyright (c) 2002, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Hofmann, Perspectix AG - https://bugs.eclipse.org/bugs/show_bug.cgi?id=291750
 *     Asma Smaoui - CEA LIST - https://bugs.eclipse.org/bugs/show_bug.cgi?id=517379
 *     Christoph Läubrich - Bug 552773 - Simplify logging in platform code base
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.ui.internal.views.HelpTray;
import org.eclipse.help.ui.internal.views.IHelpPartPage;
import org.eclipse.help.ui.internal.views.ReusableHelpPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.CheatSheetListener;
import org.eclipse.ui.cheatsheets.ICheatSheetEvent;
import org.eclipse.ui.cheatsheets.ICheatSheetViewer;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.CheatSheetStopWatch;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.actions.IMenuContributor;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.views.CompositeCheatSheetPage;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetParser;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetSaveHelper;
import org.eclipse.ui.internal.cheatsheets.data.ICheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.data.ParserInput;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.eclipse.ui.internal.cheatsheets.state.DefaultStateManager;
import org.eclipse.ui.internal.cheatsheets.state.ICheatSheetStateManager;
import org.eclipse.ui.internal.cheatsheets.state.NoSaveStateManager;
import org.eclipse.ui.internal.cheatsheets.state.TrayStateManager;
import org.osgi.framework.Bundle;

public class CheatSheetViewer implements ICheatSheetViewer, IMenuContributor {

	//CS Elements
	private CheatSheetElement contentElement;
	private ParserInput parserInput;
	private String currentID;
	private int currentItemNum;
	// Used to indicate if an invalid cheat sheet id was specified via setInput.
	private boolean invalidCheatSheetId = false;
	// Used to indicate if a null cheat sheet id was specified via setInput.
	private boolean nullCheatSheetId = false;

	private CheatSheetParser parser;
	private ICheatSheet model;
	private CheatSheetManager manager;
	private final CheatSheetSaveHelper saveHelper;

	private CheatSheetExpandRestoreAction expandRestoreAction;
	private Action copyAction;

	//ITEMS
	private ViewItem currentItem;

	//Lists
	private ArrayList<String> expandRestoreList = new ArrayList<>();
	private ArrayList<ViewItem> viewItemList = new ArrayList<>();

	//Composites
	protected Composite control;

	private Cursor busyCursor;

	// The page currently displayed, may be a CheatSheetPage, CompositeCheatSheetPage
	// or ErrorPage
	private Page currentPage;
	private Label howToBegin;
	private final boolean inDialog;
	private Listener listener;

	private ICheatSheetStateManager stateManager; // The state manager to use when saving
	private ICheatSheetStateManager preTrayManager; // The state manager in use before a tray was opened
	private String restorePath;

	private int dialogReturnCode;
	private boolean isRestricted;

	/**
	 * The constructor.
	 *
	 * @param inDialog whether or not this viewer will be placed in a modal dialog
	 */
	public CheatSheetViewer(boolean inDialog) {
		currentItemNum = -1;
		this.inDialog = inDialog;
		saveHelper = new CheatSheetSaveHelper();
	}

	public void advanceIntroItem() {
		if (getViewItemAtIndex(0) == null) {
			return;  // Cheat Sheet has no items or was not opened correctly
		}
		resetItemState();
		/* LP-item event */
		// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_DEACTIVATED, introItem);

		currentItemNum = 1;
		ViewItem nextItem = getViewItemAtIndex(currentItemNum);
		if (nextItem.item.isDynamic()) {
			nextItem.handleButtons();
		}
		nextItem.setAsCurrentActiveItem();
		currentItem = nextItem;
		/* LP-item event */
		// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_ACTIVATED, nextItem);
		collapseAllButCurrent(false);

		saveCurrentSheet();
		Display.getDefault().asyncExec(() -> currentPage.form.showControl(currentItem.getMainItemComposite()));
	}

	/**
	 * Reset the state of all the items in this cheatsheet
	 */
	private void resetItemState() {
		IntroItem introItem = (IntroItem) getViewItemAtIndex(0);
		boolean isStarted = introItem.isCompleted();

		expandRestoreList = new ArrayList<>();
		if(expandRestoreAction != null)
			expandRestoreAction.setCollapsed(false);

		clearBackgrounds();
		clearIcons();
		collapseAllButtons();
		if(isStarted)
			initManager();

		for (ViewItem item : viewItemList) {
			if (item instanceof CoreItem) {
				CoreItem c = (CoreItem) item;
				ArrayList<SubItemCompositeHolder> l = c.getListOfSubItemCompositeHolders();
				if (l != null)
					for (int j = 0; j < l.size(); j++) {
						l.get(j).setSkipped(false);
						l.get(j).setCompleted(false);
					}
			}
		}

		if (isStarted)
			getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_RESTARTED);
		else
			getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_STARTED);

		isStarted = true;
		introItem.setAsNormalCollapsed();
		introItem.setComplete();
		introItem.setRestartImage();
	}

	/*package*/
	/*
	 * This function can do one of three things
	 * 1. If this item has a completion message which has not been displayed, display it
	 * 2. Otherwise if this is the final item return to the introduction
	 * 3. If neither condition 1 or 2 is satisfied move to the next item
	 */
	public void advanceItem(ImageHyperlink link, boolean markAsCompleted) {
		currentItem = (ViewItem) link.getData();
		int indexNextItem = getIndexOfItem(currentItem) +1;
		boolean isFinalItem = indexNextItem >= viewItemList.size();

		if (markAsCompleted
				&& currentItem.hasCompletionMessage()
				&& !currentItem.isCompletionMessageExpanded()) {
			currentItem.setCompletionMessageExpanded(isFinalItem);
			currentItem.setComplete();
			if (isFinalItem) {
				getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_COMPLETED);
			}
			saveCurrentSheet();
			return;
		}

		if (indexNextItem < currentItemNum) {
			ViewItem vi = getViewItemAtIndex(currentItemNum);
			vi.setAsNormalNonCollapsed();
		}
		if (currentItem != null) {
			//set that item to it's original color.
			currentItem.setAsNormalCollapsed();
			//set that item as complete.
			if (markAsCompleted) {
				if (!currentItem.isCompleted()) {
					currentItem.setComplete();
				}
				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_COMPLETED, currentItem);
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_DEACTIVATED, currentItem);
			} else {
				currentItem.setSkipped();
				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_SKIPPED, currentItem);
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_DEACTIVATED, currentItem);
			}
		}
		if (!isFinalItem) {
			ViewItem nextItem = getViewItemAtIndex(indexNextItem);
			currentItemNum = indexNextItem;
			if (nextItem != null) {
				//Handle lazy button instantiation here.
				if (nextItem.item.isDynamic()) {
					((CoreItem) nextItem).handleButtons();
				}
				nextItem.setAsCurrentActiveItem();
				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_ACTIVATED, nextItem);
				currentItem = nextItem;
			}

			Display.getDefault().asyncExec(() -> currentPage.form.showControl(currentItem.getMainItemComposite()));
		} else if (indexNextItem == viewItemList.size()) {
			if (!currentItem.isCompletionMessageExpanded()) { // The event will already have been fired
				getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_COMPLETED);
			}
			showIntroItem();
		}

		saveCurrentSheet();
	}

	private void showIntroItem() {
		ViewItem item = getViewItemAtIndex(0);
		item.setAsCurrentActiveItem();
		currentItem = item;
	}

	public void advanceSubItem(ImageHyperlink link, boolean markAsCompleted, int subItemIndex) {
		Label l = null;
		ArrayList<SubItemCompositeHolder> list = null;
		SubItemCompositeHolder sich = null;
		CoreItem ciws = null;

		currentItem = (ViewItem) link.getData();

		if (currentItem instanceof CoreItem)
			ciws = (CoreItem) currentItem;

		if (ciws != null) {
			list = ciws.getListOfSubItemCompositeHolders();
			sich = list.get(subItemIndex);
			l = sich.getCheckDoneLabel();
		}

		if (l != null) {
			if (markAsCompleted) {
				sich.setCompleted(true);
				sich.setSkipped(false);
				/* LP-subitem event */
				// fireManagerSubItemEvent(ICheatSheetItemEvent.ITEM_COMPLETED, ciws, subItemID);
			} else {
				sich.setSkipped(true);
				sich.setCompleted(false);
				/* LP-subitem event */
				// fireManagerSubItemEvent(ICheatSheetItemEvent.ITEM_SKIPPED, ciws, subItemID);
			}
			ciws.refreshItem();
		}

		boolean allAttempted = checkAllAttempted(list);
		boolean anySkipped = checkContainsSkipped(list);

		if (allAttempted && !anySkipped) {
			advanceItem(link, true);
			return;
		} else if (allAttempted && anySkipped) {
			advanceItem(link, false);
			return;
		}

		setFocus();
		saveCurrentSheet();
	}

	private boolean checkAllAttempted(ArrayList<SubItemCompositeHolder> list) {
		for (int i = 0; i < list.size(); i++) {
			SubItemCompositeHolder s = list.get(i);
			if (s.isCompleted() || s.isSkipped()) {
				continue;
			}
			return false;
		}
		return true;
	}

	private boolean checkContainsSkipped(ArrayList<SubItemCompositeHolder> list) {
		for (int i = 0; i < list.size(); i++) {
			SubItemCompositeHolder s = list.get(i);
			if (s.isSkipped()) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean loadState() {
		try {
			Properties props = stateManager.getProperties();

			manager = stateManager.getCheatSheetManager();

			// There is a bug which causes the background of the buttons to
			// remain white, even though the color is set. So instead of calling
			// clearBackgrounds() only the following line should be needed. D'oh!
			// ((ViewItem) viewItemList.get(0)).setOriginalColor();
			clearBackgrounds();

			if (props == null) {
				getViewItemAtIndex(0).setAsCurrentActiveItem();
				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_ACTIVATED, items[0]);
				return true;
			}

			boolean buttonIsDown = (Integer.parseInt((String) props.get(IParserTags.BUTTON)) == 0) ? false : true;
			int itemNum = Integer.parseInt((String) props.get(IParserTags.CURRENT));
			ArrayList completedStatesList = (ArrayList) props.get(IParserTags.COMPLETED);
			ArrayList expandedStatesList = (ArrayList) props.get(IParserTags.EXPANDED);
			expandRestoreList = (ArrayList<String>) props.get(IParserTags.EXPANDRESTORE);
			String cid = (String) props.get(IParserTags.ID);
			Hashtable completedSubItems = (Hashtable) props.get(IParserTags.SUBITEMCOMPLETED);
			Hashtable<String, String> skippedSubItems = (Hashtable<String, String>) props
					.get(IParserTags.SUBITEMSKIPPED);

			ArrayList completedSubItemsItemList = new ArrayList<>();
			ArrayList skippedSubItemsItemList = new ArrayList<>();

			Enumeration e = completedSubItems.keys();
			while (e.hasMoreElements())
				completedSubItemsItemList.add(e.nextElement());

			Enumeration e2 = skippedSubItems.keys();
			while (e2.hasMoreElements())
				skippedSubItemsItemList.add(e2.nextElement());

			if (cid != null)
				currentID = cid;

			if (itemNum >= 0) {
				currentItemNum = itemNum;

				currentItem = getViewItemAtIndex(itemNum);

				CheatSheetStopWatch.startStopWatch("CheatSheetViewer.checkSavedState()"); //$NON-NLS-1$
				for (int i = 0; i < viewItemList.size(); i++) {

					ViewItem item = getViewItemAtIndex(i);
					if (i > 0 && item.item.isDynamic() && i <= currentItemNum) {
						 item.handleButtons();
						 item.setOriginalColor();
					}

					if (completedStatesList.contains(Integer.toString(i))) {
						item.setComplete();
						item.setRestartImage();
					} else {
						if (i < currentItemNum) {
							item.setSkipped();
						}
					}
					if (expandedStatesList.contains(Integer.toString(i))) {
						item.setExpanded();
					} else {
						item.setCollapsed();
					}
					if (i > currentItemNum) {
						item.setButtonsVisible(false);
						item.setCompletionMessageCollapsed();
					} else {
						item.setButtonsVisible(true);
						if (i >currentItemNum || item.isCompleted()) {
							item.setCompletionMessageExpanded(i + 1 >= viewItemList.size());
						} else {
							item.setCompletionMessageCollapsed();
						}
					}
					if (expandRestoreList.contains(Integer.toString(i))) {
						item.setCollapsed();
					}
					if (completedSubItemsItemList.contains(Integer.toString(i))) {
						String subItemNumbers = (String) completedSubItems.get(Integer.toString(i));
						StringTokenizer st = new StringTokenizer(subItemNumbers, ","); //$NON-NLS-1$
						if (item instanceof CoreItem) {
							CoreItem coreitemws = (CoreItem) item;
							ArrayList<SubItemCompositeHolder> subItemCompositeHolders = coreitemws
									.getListOfSubItemCompositeHolders();
							if (subItemCompositeHolders != null) {
								while (st.hasMoreTokens()) {
									String token = st.nextToken();
									subItemCompositeHolders.get(Integer.parseInt(token)).setCompleted(true);
									ArrayList<SubItemCompositeHolder> l = subItemCompositeHolders;
									SubItemCompositeHolder s = l.get(Integer.parseInt(token));
									if (s != null && s.getStartButton() != null) {
										s.getStartButton().setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_RESTART));
										s.getStartButton().setToolTipText(Messages.RESTART_TASK_TOOLTIP);
									}

								}
							}
						}
					}
					if (skippedSubItemsItemList.contains(Integer.toString(i))) {
						String subItemNumbers = skippedSubItems.get(Integer.toString(i));
						StringTokenizer st = new StringTokenizer(subItemNumbers, ","); //$NON-NLS-1$
						if (item instanceof CoreItem) {
							CoreItem coreitemws = (CoreItem) item;
							while (st.hasMoreTokens()) {
								String token = st.nextToken();
								coreitemws.getListOfSubItemCompositeHolders().get(Integer.parseInt(token))
										.setSkipped(true);
							}
						}
					}
					CheatSheetStopWatch.printLapTime("CheatSheetViewer.checkSavedState()", "Time in CheatSheetViewer.checkSavedState() after loop #"+i+": "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				CheatSheetStopWatch.printLapTime("CheatSheetViewer.checkSavedState()", "Time in CheatSheetViewer.checkSavedState() after loop: "); //$NON-NLS-1$ //$NON-NLS-2$

				if (buttonIsDown) {
					if(expandRestoreAction != null)
						expandRestoreAction.setCollapsed(true);
				}

				// If the last item is the current one and it is complete then
				// we should collapse the last item and set the focus on intro.
				// For all other cases, set the current item as the active item.
				if(viewItemList.size()-1 == itemNum && currentItem.isCompleted()) {
					currentItem.setCollapsed();
					getViewItemAtIndex(0).getMainItemComposite().setFocus();

					// The cheat sheet has been restored but is also completed so fire both events
					getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_RESTORED);
					getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_COMPLETED);
				} else {
					currentItem.setAsCurrentActiveItem();

					// If the intro item is completed, than the cheat sheet has been restored.
					if(getViewItemAtIndex(0).isCompleted())
						getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_RESTORED);
				}

				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_ACTIVATED, currentItem);
			} else {
				getViewItemAtIndex(0).setAsCurrentActiveItem();
				currentItem = getViewItemAtIndex(0);
				/* LP-item event */
				// fireManagerItemEvent(ICheatSheetItemEvent.ITEM_ACTIVATED, items[0]);
			}

			return true;
		} catch(Exception e) {
			// An exception while restoring the saved state data usually only occurs if
			// the cheat sheet has been modified since this previous execution. This most
			// often occurs during development of cheat sheets and as such an end user is
			// not as likely to encounter this.

			boolean reset = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					Messages.CHEATSHEET_STATE_RESTORE_FAIL_TITLE,
					Messages.CHEATSHEET_STATE_RESET_CONFIRM);

			if (reset) {
				restart();
				return true;
			}

			// Log the exception
			String stateFile = saveHelper.getStateFile(currentID).toOSString();
			String message = NLS.bind(Messages.ERROR_APPLYING_STATE_DATA_LOG, (new Object[] {stateFile, currentID}));
			CheatSheetPlugin.getPlugin().getLog().error(message, e);

			// Set the currentID to null so it is not saved during internalDispose()
			currentID = null;

			internalDispose();

			// Reinitialize a few variables because there is no currentItem or currentPage now
			parserInput = null;
			currentItem = null;
			currentItemNum = -1;
			currentPage = null;
			expandRestoreList = new ArrayList<>();
			viewItemList = new ArrayList<>();

			// Create the errorpage to show the user
			createErrorPage(Messages.ERROR_APPLYING_STATE_DATA);

			return false;
		}
	}

	private void clearBackgrounds() {
		for (ViewItem item : viewItemList) {
			item.setOriginalColor();
		}
	}

	private void clearIcons() {
		for (ViewItem item : viewItemList) {
			item.setOriginalColor();
			if (item.isCompleted() || item.isExpanded() || item.isSkipped())
					item.setIncomplete();
		}
	}

	private void collapseAllButCurrent(boolean fromAction) {
		expandRestoreList = new ArrayList<>();
		try {
			ViewItem current = getViewItemAtIndex(currentItemNum);
			for (ListIterator<ViewItem> iter = viewItemList.listIterator(viewItemList.size()); iter.hasPrevious();) {
				ViewItem item = iter.previous();
				if (item != current && item.isExpanded()) {
					item.setCollapsed();
					if (fromAction)
						expandRestoreList.add(Integer.toString(getIndexOfItem(item)));
				}
			}
		} catch (Exception e) {
		}
	}

	private void collapseAllButtons() {
		for (Iterator<ViewItem> iter = viewItemList.listIterator(1); iter.hasNext();) {
			ViewItem item = iter.next();
			item.setButtonsVisible(false);
			item.setCompletionMessageCollapsed();
		}
	}

	private void createErrorPage(String message) {
		setCollapseExpandButtonEnabled(false);
		if(message != null) {
			currentPage = new ErrorPage(message);
		} else {
			currentPage = new ErrorPage();
		}
		currentPage.createPart(control);
		control.layout(true);
	}

	private void showStartPage() {
		setCollapseExpandButtonEnabled(false);
		internalDispose();

		howToBegin = new Label(control, SWT.WRAP);
		howToBegin.setText(Messages.INITIAL_VIEW_DIRECTIONS);
		howToBegin.setLayoutData(new GridData(GridData.FILL_BOTH));
		currentPage = null;
		control.layout(true);
	}

	private void createErrorPage(IStatus status) {
		setCollapseExpandButtonEnabled(false);
		currentPage = new ErrorPage(status);
		currentPage.createPart(control);
		control.layout(true);
	}

	/**
	 * Creates the SWT controls for this workbench part.
	 * <p>
	 * Clients should not call this method (the workbench calls this method at
	 * appropriate times).
	 * </p>
	 * <p>
	 * For implementors this is a multi-step process:
	 * </p>
	 * <ol>
	 *   <li>Create one or more controls within the parent.</li>
	 *   <li>Set the parent layout as needed.</li>
	 *   <li>Register any global actions with the <code>IActionService</code>.</li>
	 *   <li>Register any popup menus with the <code>IActionService</code>.</li>
	 *   <li>Register a selection provider with the <code>ISelectionService</code>
	 *     (optional). </li>
	 * </ol>
	 *
	 * @param parent the parent control
	 */
	@Override
	public void createPartControl(Composite parent) {
		control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.numColumns = 1;
		control.setLayout(layout);

		control.addDisposeListener(e -> dispose());

		showStartPage();

		Display display = parent.getDisplay();

		busyCursor = display.getSystemCursor(SWT.CURSOR_WAIT);

		if(contentElement != null) {
			initCheatSheetView();
		}
	}

	/**
	 * Called when any TrayDialog is opened. The viewer must react by disabling
	 * itself and moving the cheat sheet to the dialog's tray if the current item
	 * was flagged as one that opens a modal dialog.
	 *
	 * @param dialog the dialog that was opened
	 */
	private void dialogOpened(final TrayDialog dialog) {
		if (isActive()) {
			HelpTray tray = (HelpTray)dialog.getTray();
			if (tray == null) {
				tray = new HelpTray();
				dialog.openTray(tray);
			}
			ReusableHelpPart helpPart = tray.getHelpPart();
			IHelpPartPage page = helpPart.createPage(CheatSheetHelpPart.ID, null, null);
			page.setVerticalSpacing(0);
			page.setHorizontalMargin(0);
			ICheatSheetStateManager trayManager = new TrayStateManager();
			preTrayManager = stateManager;
			stateManager = trayManager;
			saveCurrentSheet();      // Save the state into the tray manager
			helpPart.addPart(CheatSheetHelpPart.ID, new CheatSheetHelpPart(helpPart.getForm().getForm().getBody(), helpPart.getForm().getToolkit(), page.getToolBarManager(), contentElement, trayManager));
			page.addPart(CheatSheetHelpPart.ID, true);
			helpPart.addPage(page);
			helpPart.showPage(CheatSheetHelpPart.ID);

			/*
			 * Disable the viewer until the tray is closed, then show it again.
			 */
			control.setVisible(false);
			Display.getCurrent().removeFilter(SWT.Show, listener);

			helpPart.getControl().addListener(SWT.Dispose, event -> {
				control.setVisible(true);
				Display.getCurrent().addFilter(SWT.Show, listener);
				if (preTrayManager != null) {
					loadState(); // Load from the tray manager
					stateManager = preTrayManager;
					preTrayManager = null;
				}
				dialogReturnCode = dialog.getReturnCode();
			});
		}
	}

	/**
	 * Disposes of this cheat sheet viewer.
	 */
	private void dispose() {
		internalDispose();
	}

	/*
	 * Returns the cheat sheet being viewed.
	 */
	public ICheatSheet getCheatSheet() {
		return model;
	}

	@Override
	public String getCheatSheetID() {
		if(getContent() != null) {
			return getContent().getID();
		}

		return null;
	}

	/**
	 * Returns the current content.
	 *
	 * @return CheatSheetElement
	 */
	/*package*/ CheatSheetElement getContent() {
		return contentElement;
	}

	@Override
	public Control getControl() {
		return control;
	}

	private int getIndexOfItem(ViewItem item) {
		int index = viewItemList.indexOf(item);
		if(index != -1) {
			return index;
		}
		return 0;
	}

	/*package*/ CheatSheetManager getManager() {
		if (manager == null) {
			getNewManager();
		}
		return manager;
	}

	private CheatSheetManager getNewManager(){
		manager = new CheatSheetManager(contentElement);
		return manager;
	}

	private CheatSheetManager initManager(){
		CheatSheetManager csManager = getManager();
		csManager.setData(new Hashtable<>());
		return csManager;
	}

	private ViewItem getViewItemAtIndex(int index) {
		if (viewItemList != null && !viewItemList.isEmpty()) {
			return viewItemList.get(index);
		}
		return null;
	}

	/**
	 * Returns whether or not this viewer contains the given Control, which
	 * is currently in focus.
	 *
	 * @param control the Control currently in focus
	 * @return whether this viewer contains the given Control or not
	 */
	public boolean hasFocusControl(Control control) {
		return (control == this.control) || (currentPage.getControl() == control);
	}

	/**
	 * If in a dialog-opening step, will add the appropriate listener for
	 * the cheatsheet to jump into the dialog's tray once opened.
	 *
	 * Should be called before executing any action.
	 */
	private void hookDialogListener() {
		/*
		 * org.eclipse.help.ui is an optional dependency; only perform this
		 * step is this plugin is present.
		 */
		if (!inDialog && isInDialogItem() && (Platform.getBundle("org.eclipse.help.ui") != null)) { //$NON-NLS-1$
			listener = event -> {
				if (isTrayDialog(event.widget)) {
					dialogOpened((TrayDialog) ((Shell) event.widget).getData());
				}
			};
			Display.getCurrent().addFilter(SWT.Show, listener);
		}
	}

	/**
	 * Removes the dialog-opening listener, if it was added.
	 *
	 * Should be called after executing any action.
	 */
	private void unhookDialogListener() {
		if (listener != null) {
			Display.getCurrent().removeFilter(SWT.Show, listener);
		}
	}

	/*
	 * return true if a cheat sheet was opened successfully
	 */
	private boolean initCheatSheetView() {
		CheatSheetStopWatch.startStopWatch("CheatSheetViewer.initCheatSheetView()"); //$NON-NLS-1$
		//Re-initialize list to store items collapsed by expand/restore action on c.s. toolbar.
		expandRestoreList = new ArrayList<>();

		// re set that action to turned off.
		if(expandRestoreAction != null)
			expandRestoreAction.setCollapsed(false);

		//reset current item to be null; next item too.
		currentItem = null;
		currentItemNum = 0;
		viewItemList = new ArrayList<>();

		// Reset the page variable
		currentPage = null;

		if(howToBegin != null) {
			howToBegin.dispose();
			howToBegin = null;
		}

		// If a null cheat sheet id was specified, return leaving the cheat sheet empty.
		if(nullCheatSheetId) {
			return false;
		}

		if(invalidCheatSheetId) {
			createErrorPage(Messages.ERROR_CHEATSHEET_DOESNOT_EXIST);
			return false;
		}

		// read our contents, if there are problems reading the file an error page should be created.
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() before readFile() call: "); //$NON-NLS-1$ //$NON-NLS-2$
		IStatus parseStatus = readFile();
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after readFile() call: "); //$NON-NLS-1$ //$NON-NLS-2$
		if (!parseStatus.isOK()) {
			CheatSheetPlugin.getPlugin().getLog().log(parseStatus);
		}
		if(parseStatus.getSeverity() == IStatus.ERROR){

			// Error during parsing.
			// Something is wrong with the Cheat sheet content file at the xml level.

			createErrorPage(parseStatus);
			return false;
		}

		control.setRedraw(false);
		if (model instanceof CheatSheet) {
			CheatSheet cheatSheetModel = (CheatSheet)model;

			if (isRestricted && cheatSheetModel.isContainsCommandOrAction()) {
				boolean isOK = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						Messages.CHEATSHEET_FROM_URL_WITH_EXEC_TITLE,
						Messages.CHEATSHEET_FROM_URL_WITH_EXEC);

				if (!isOK) {
					control.setRedraw(true);
					showStartPage();
					return true;
				}
			}

			currentPage = new CheatSheetPage(cheatSheetModel, viewItemList, this);
			setCollapseExpandButtonEnabled(true);
		} else if (model instanceof CompositeCheatSheetModel) {
			CompositeCheatSheetModel compositeCheatSheetModel = ((CompositeCheatSheetModel)model);
			compositeCheatSheetModel.setId(currentID);
			currentPage = new CompositeCheatSheetPage(compositeCheatSheetModel, stateManager);
			compositeCheatSheetModel.setCheatSheetManager(initManager());
			setCollapseExpandButtonEnabled(false);
		}
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after CheatSheetPage() call: "); //$NON-NLS-1$ //$NON-NLS-2$
		currentPage.createPart(control);
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after CheatSheetPage.createPart() call: "); //$NON-NLS-1$ //$NON-NLS-2$

		if (model instanceof CheatSheet) {
			CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after fireEvent() call: "); //$NON-NLS-1$ //$NON-NLS-2$

			if(!loadState()) {
				// An error occurred when apply the saved state data.
				control.setRedraw(true);
				control.layout();
				return true;
			}

			getManager().fireEvent(ICheatSheetEvent.CHEATSHEET_OPENED);
		}
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after checkSavedState() call: "); //$NON-NLS-1$ //$NON-NLS-2$

		currentPage.initialized();
		control.setRedraw(true);
		control.layout();
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() after layout() call: "); //$NON-NLS-1$ //$NON-NLS-2$

		if (currentItem != null && !currentItem.isCompleted())
			currentItem.setFocus();
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.initCheatSheetView()", "Time in CheatSheetViewer.initCheatSheetView() at end of method: "); //$NON-NLS-1$ //$NON-NLS-2$
		return true;
	}

	private void internalDispose() {
		if(manager != null)
			manager.fireEvent(ICheatSheetEvent.CHEATSHEET_CLOSED);

		saveCurrentSheet();

		for (Iterator<ViewItem> iter = viewItemList.iterator(); iter.hasNext();) {
			ViewItem item = iter.next();
			item.dispose();
		}

		if(currentPage != null) {
			currentPage.dispose();
		}
		manager = null;
	}

	/**
	 * Returns whether or not the cheat sheet viewer is currently active. This
	 * means it is visible to the user and enabled.
	 *
	 * @return whether or not this viewer is active
	 */
	private boolean isActive() {
		Control control = getControl();
		if (control != null && !control.isDisposed()) {
			Control parent = control.getParent();
			return (parent != null && !parent.isDisposed() && parent.isVisible() && parent.isEnabled());
		}
		return false;
	}

	/*
	 * Show the collapse/expand button if we have access to the toolbar
	 */
	private void setCollapseExpandButtonEnabled(boolean enable) {
		if (expandRestoreAction != null) {
			expandRestoreAction.setEnabled(enable);
		}
	}

	/**
	 * Returns whether or not the currently active item requires opening a
	 * modal dialog.
	 *
	 * @return whether the current item opens a modal dialog
	 */
	private boolean isInDialogItem() {
		if (currentItem != null) {
			return currentItem.getItem().isDialog();
		}
		return false;
	}

	/**
	 * Returns whether or not this cheat sheet viewer is inside a modal
	 * dialog.
	 *
	 * @return whether this viewer is inside a modal dialog
	 */
	public boolean isInDialogMode() {
		return inDialog;
	}

	/**
	 * Returns whether the given widget is a TrayDialog.
	 *
	 * @param widget the widget to check
	 * @return whether or not the widget is a TrayDialog
	 */
	private boolean isTrayDialog(Widget widget) {
		return (widget instanceof Shell && ((Shell)widget).getData() instanceof TrayDialog);
	}

	/**
	* Read the contents of the cheat sheet file
	* @return true if the file was read and parsed without error
	*/
	private IStatus readFile() {
		if(parser == null)
			parser = new CheatSheetParser();
		// If the cheat sheet was registered then
		// search for a specific type - composite or simple
		int cheatSheetKind = CheatSheetParser.ANY;
		if (contentElement.isRegistered()) {
			if (contentElement.isComposite()) {
				cheatSheetKind = CheatSheetParser.COMPOSITE_ONLY;
			} else {
				cheatSheetKind = CheatSheetParser.SIMPLE_ONLY;
			}
		}

		model = parser.parse(parserInput, cheatSheetKind);
		return parser.getStatus();
	}

	private void restoreExpandStates() {
		try {
			for (int i = 0; i < expandRestoreList.size(); i++) {
				int index = Integer.parseInt((expandRestoreList.get(i)));
				ViewItem item = getViewItemAtIndex(index);
				if (!item.isExpanded()) {
					item.setExpanded();
				}
			}
			expandRestoreList = null;
		} catch (Exception e) {
		}
	}

	/*package*/ void runPerformExecutable(ImageHyperlink link) {
		link.setCursor(busyCursor);
		currentItem = (ViewItem) link.getData();
		CoreItem coreItem = (CoreItem) currentItem;
		Page page= currentPage;

		if (coreItem != null) {
			try {
				hookDialogListener();
				dialogReturnCode = -1;
				IStatus status = coreItem.runExecutable(getManager());
				if ( status.getSeverity() == IStatus.ERROR) {
					CheatSheetPlugin.getPlugin().getLog().log(status);
					org.eclipse.jface.dialogs.ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), null, null, status);
				}
				if (page != currentPage) {
					// action closed the cheatsheet view or changed the cheatsheet
					return;
				}
				if (status.isOK() && dialogReturnCode != Window.CANCEL) {
					coreItem.setRestartImage();
					if (!coreItem.hasConfirm()) {
						//set that item as complete.
						advanceItem(link, true);
						saveCurrentSheet();
					}
				}
			}
			finally {
				unhookDialogListener();
			}
		}

		link.setCursor(null);
	}

	/*package*/ void runSubItemPerformExecutable(ImageHyperlink link, int subItemIndex) {
		CoreItem coreItem = null;
		link.setCursor(busyCursor);
		currentItem = (ViewItem) link.getData();
		coreItem = (CoreItem) currentItem;

		try {
			if (coreItem != null) {
				hookDialogListener();
				if (coreItem.runSubItemExecutable(getManager(), subItemIndex) == ViewItem.VIEWITEM_ADVANCE && !coreItem.hasConfirm(subItemIndex)) {
					ArrayList<SubItemCompositeHolder> l = coreItem.getListOfSubItemCompositeHolders();
					SubItemCompositeHolder s = l.get(subItemIndex);
					s.getStartButton().setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_RESTART));
					s.getStartButton().setToolTipText(Messages.RESTART_TASK_TOOLTIP);
					advanceSubItem(link, true, subItemIndex);
					saveCurrentSheet();
				}
			}
		} catch (RuntimeException e) {
			IStatus status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, Messages.ERROR_RUNNING_ACTION, e);
			CheatSheetPlugin.getPlugin().getLog().log(status);
			org.eclipse.jface.dialogs.ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), null, null, status);
		} finally {
			unhookDialogListener();
			link.setCursor(null);
		}
	}

	public void saveCurrentSheet() {
		if(currentID != null) {
			if (currentPage instanceof CheatSheetPage) {
				Properties properties = saveHelper.createProperties(currentItemNum, viewItemList, getExpandRestoreActionState(), expandRestoreList, currentID, restorePath);
				IStatus status = stateManager.saveState(properties, getManager());
				if (!status.isOK()) {
					CheatSheetPlugin.getPlugin().getLog().log(status);
				}
			} else if (currentPage instanceof CompositeCheatSheetPage) {
				((CompositeCheatSheetPage)currentPage).saveState();
			}
		}
	}

	private boolean getExpandRestoreActionState() {
		boolean expandRestoreActionState = false;
		if(expandRestoreAction != null)
			expandRestoreActionState = expandRestoreAction.isCollapsed();
		return expandRestoreActionState;
	}

	/*package*/ void setContent(CheatSheetElement element, ICheatSheetStateManager inputStateManager) {
		CheatSheetStopWatch.startStopWatch("CheatSheetViewer.setContent(CheatSheetElement element)"); //$NON-NLS-1$

		// Cleanup previous contents
		internalDispose();

		// Set the current content to new content
		contentElement = element;
		stateManager = inputStateManager;
		stateManager.setElement(element);

		currentID = null;
		parserInput = null;
		if (element != null) {
			initInputFields(element);
		}

		CheatSheetStopWatch.printLapTime("CheatSheetViewer.setContent(CheatSheetElement element)", "Time in CheatSheetViewer.setContent() before initCheatSheetView() call: "); //$NON-NLS-1$ //$NON-NLS-2$
		// Initialize the view with the new contents
		boolean cheatSheetOpened = false;
		if (control != null) {
			cheatSheetOpened = initCheatSheetView();
		}
		if (!cheatSheetOpened) {
			contentElement = null;
			stateManager = null;
		}
		// If the cheat sheet failed to open clear the content element so we don't see an
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.setContent(CheatSheetElement element)", "Time in CheatSheetViewer.setContent() after initCheatSheetView() call: "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void initInputFields(CheatSheetElement element) {
		currentID = element.getID();
		String contentXml = element.getContentXml();
		URL contentURL = null;
		restorePath = element.getRestorePath();
		String errorMessage = null;

		if (contentXml != null) {
			parserInput = new ParserInput(contentXml, element.getHref());
			return;
		}

		// The input was not an XML string, find the content URL
		Bundle bundle = null;
		if(element != null && element.getConfigurationElement() != null)
			try{
				String pluginId = element.getConfigurationElement().getContributor().getName();
				bundle = Platform.getBundle(pluginId);
			} catch (Exception e) {
				// do nothing
			}
		if (bundle != null) {
			contentURL = FileLocator.find(bundle, IPath.fromOSString(element.getContentFile()), null);
			if (contentURL == null && element.getContentFile() != null) {
				errorMessage = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {element.getContentFile()}));
			}
		}

		if (contentURL == null) {
			try {
				contentURL = new URL(element.getHref());
			} catch (MalformedURLException mue) {
			}
			if (contentURL == null && element.getHref() != null) {
				errorMessage = NLS.bind(Messages.ERROR_OPENING_FILE_IN_PARSER, (new Object[] {element.getHref()}));
			}
		}
		String pluginId = bundle != null ? bundle.getSymbolicName() : null;
		parserInput = new ParserInput(contentURL, pluginId, errorMessage);
	}


	/*package*/ void setExpandRestoreAction(CheatSheetExpandRestoreAction action) {
		expandRestoreAction = action;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		//need this to have current item selected. (Assumes that when you reactivate the view you will work with current item.)
		if (currentItem != null) {
			currentItem.setFocus();
		} else {
			getControl().setFocus();
		}
	}


	@Override
	public void setInput(String id) {
		setInput(id, new DefaultStateManager());
	}

	public void setInput(String id, ICheatSheetStateManager inputStateManager) {
		CheatSheetStopWatch.startStopWatch("CheatSheetViewer.setInput(String id)"); //$NON-NLS-1$

		CheatSheetElement element = null;

		if(id == null) {
			nullCheatSheetId = true;
		} else {
			nullCheatSheetId = false;

			element = CheatSheetRegistryReader.getInstance().findCheatSheet(id);
			if(element == null) {
				String message = NLS.bind(Messages.ERROR_INVALID_CHEATSHEET_ID, (new Object[] {id}));
				IStatus status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, null);
				CheatSheetPlugin.getPlugin().getLog().log(status);
				invalidCheatSheetId = true;
			} else {
				invalidCheatSheetId = false;
				this.isRestricted = false;
			}
		}

		CheatSheetStopWatch.printLapTime("CheatSheetViewer.setInput(String id)", "Time in CheatSheetViewer.setInput(String id) before setContent() call: "); //$NON-NLS-1$ //$NON-NLS-2$
		setContent(element, inputStateManager);
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.setInput(String id)", "Time in CheatSheetViewer.setInput(String id) after setContent() call: "); //$NON-NLS-1$ //$NON-NLS-2$

		// Update most recently used cheat sheets list.
		CheatSheetPlugin.getPlugin().getCheatSheetHistory().add(element);
		CheatSheetStopWatch.printLapTime("CheatSheetViewer.setInput(String id)", "Time in CheatSheetViewer.setInput(String id) after getCheatSheetHistory() call: "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void setInput(String id, String name, URL url) {
		setInput(id, name, url, new DefaultStateManager(), false);
	}

	public void setInputFromXml(String id, String name, String xml, String basePath) {
		if (id == null || name == null || xml == null) {
			throw new IllegalArgumentException();
		}
		CheatSheetElement element = new CheatSheetElement(name);
		element.setID(id);
		element.setContentXml(xml);
		element.setHref(basePath);

		nullCheatSheetId = false;
		invalidCheatSheetId = false;
		isRestricted = false;
		setContent(element, new NoSaveStateManager());
	}

	public void setInput(String id, String name, URL url,
			ICheatSheetStateManager inputStateManager, boolean isRestricted) {
		if (id == null || name == null || url == null) {
			throw new IllegalArgumentException();
		}
		CheatSheetElement element = new CheatSheetElement(name);
		element.setID(id);
		element.setHref(url.toString());

		nullCheatSheetId = false;
		invalidCheatSheetId = false;
		this.isRestricted = isRestricted;
		setContent(element, inputStateManager);
	}

	/*package*/ void toggleExpandRestore() {
		if(expandRestoreAction == null)
			return;

		if (expandRestoreAction.isCollapsed()) {
			restoreExpandStates();
			expandRestoreAction.setCollapsed(false);
		} else {
			collapseAllButCurrent(true);
			expandRestoreAction.setCollapsed(true);
		}

	}

	public Action getCopyAction() {
		return copyAction;
	}

	public void setCopyAction(Action copyAction) {
		this.copyAction = copyAction;
	}

	public void copy() {
		for (ViewItem viewItem : viewItemList) {
			if (viewItem.hasFocus()) {
				viewItem.copy();
				return;
			}
		}
	}

	public void addListener(CheatSheetListener listener) {
		if (contentElement != null ) {
			getManager().addListener(listener);
		}
	}

	@Override
	public int contributeToViewMenu(Menu menu, int index) {
		if (currentPage instanceof IMenuContributor) {
			return ((IMenuContributor)currentPage).contributeToViewMenu(menu, index);
		}
		return index;
	}

	public void restart() {
		resetItemState();
		currentItemNum = 0;
		collapseAllButCurrent(false);
		IntroItem introItem = (IntroItem) getViewItemAtIndex(0);
		introItem.setIncomplete();
		showIntroItem();
	}

	public void saveState(IMemento memento) {
		if (currentPage instanceof CheatSheetPage) {
			Properties properties = saveHelper.createProperties(currentItemNum, viewItemList, getExpandRestoreActionState(), expandRestoreList, currentID, restorePath);
			saveHelper.saveToMemento(properties, getManager(), memento);
		}
	}

	@Override
	public void reset(Map<String, String> cheatSheetData) {
		if (currentPage instanceof CheatSheetPage) {
			restart();
			getManager().setData(cheatSheetData);
		} else if (currentPage instanceof CompositeCheatSheetPage) {
			((CompositeCheatSheetPage)currentPage).restart(cheatSheetData);
		}
	}

	public void showError(String message) {
		internalDispose();
		if(howToBegin != null) {
			howToBegin.dispose();
			howToBegin = null;
		}
		createErrorPage(message);
	}

}