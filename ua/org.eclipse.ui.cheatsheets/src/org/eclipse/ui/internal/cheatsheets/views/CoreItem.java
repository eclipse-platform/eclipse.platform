/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.data.AbstractExecutable;
import org.eclipse.ui.internal.cheatsheets.data.AbstractSubItem;
import org.eclipse.ui.internal.cheatsheets.data.ConditionalSubItem;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.data.Item;
import org.eclipse.ui.internal.cheatsheets.data.RepeatedSubItem;
import org.eclipse.ui.internal.cheatsheets.data.SubItem;

public class CoreItem extends ViewItem {

	protected boolean buttonsHandled = false;

	private static final int SUBITEM_COLUMNS = 6;

	/**
	 * Constructor for CoreItem.
	 * @param parent
	 * @param contentItem
	 */
	public CoreItem(CheatSheetPage page, Item item, Color itemColor, CheatSheetViewer viewer) {
		super(page, item, itemColor, viewer);
	}

	private void createButtonComposite() {
		buttonComposite = page.getToolkit().createComposite(bodyWrapperComposite);
		GridLayout buttonlayout = new GridLayout(1, false);
		buttonlayout.marginHeight = 2;
		buttonlayout.marginWidth = 2;
		buttonlayout.verticalSpacing = 2;

		TableWrapData buttonData = new TableWrapData(TableWrapData.FILL);

		buttonComposite.setLayout(buttonlayout);
		buttonComposite.setLayoutData(buttonData);
		buttonComposite.setBackground(itemColor);

	}

	private void createButtons(AbstractExecutable executable) {
		/*
		 * Actions are disabled while inside dialogs.
		 */
		boolean isActionShown = false;
		if (executable != null && !isInDialogMode()) {
			isActionShown = true;
			final ImageHyperlink startButton = createButtonWithText(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_START), this, itemColor, Messages.PERFORM_TASK_TOOLTIP);
			startButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			startButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.runPerformExecutable(startButton);
				}
			});
		}
		if (!isActionShown || executable.isConfirm() || !executable.isRequired()) {
			final ImageHyperlink completeButton = createButtonWithText(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_COMPLETE), this, itemColor, Messages.COMPLETE_TASK_TOOLTIP);
			completeButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.advanceItem(completeButton, true);
				}
			});
		}
		if (item.isSkip()) {
			final ImageHyperlink skipButton = createButtonWithText(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_SKIP), this, itemColor, Messages.SKIP_TASK_TOOLTIP);
			skipButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.advanceItem(skipButton, false);
				}
			});
		}
	}

	private void createSubItemButtonComposite() {
		buttonComposite = page.getToolkit().createComposite(bodyWrapperComposite);

		TableWrapLayout xbuttonlayout = new TableWrapLayout();
		xbuttonlayout.numColumns = SUBITEM_COLUMNS;
		xbuttonlayout.leftMargin = 0;
		xbuttonlayout.rightMargin = 0;
		xbuttonlayout.horizontalSpacing = 0;

		TableWrapData xbuttonData = new TableWrapData(TableWrapData.FILL);

		buttonComposite.setLayout(xbuttonlayout);
		buttonComposite.setLayoutData(xbuttonData);
		buttonComposite.setBackground(itemColor);
	}

	private void createSubItemButtons(SubItem sub, String thisValue, int index) {
		int added = 0;
		if (index != 0) {
			addSeparator();
		}
		final int LABEL_MARGIN = 5; // space to the left and right of the label
		SubItemCompositeHolder holder = new SubItemCompositeHolder(sub);

		//Spacer label added.
		Label checkDoneLabel = page.getToolkit().createLabel(buttonComposite, null);
		checkDoneLabel.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_COMPLETE));
		checkDoneLabel.setVisible(false);
		checkDoneLabel.setBackground(itemColor);
		holder.setCheckDoneLabel(checkDoneLabel);
		added++;

		//Now add the label.
		String labelText = null;
		if( thisValue != null ) {
			labelText = performLineSubstitution(sub.getLabel(), "${this}", thisValue); //$NON-NLS-1$
		} else {
			labelText = sub.getLabel();
		}
		Control subItemLabel;
		if (sub.isFormatted()) {
			FormText formText = page.getToolkit().createFormText(buttonComposite, false);
			formText.setText(labelText, labelText.startsWith(IParserTags.FORM_START_TAG), false);
			formText.setBackground(itemColor);
			subItemLabel = formText;
		} else {
			Text text = new Text(buttonComposite, SWT.READ_ONLY + SWT.WRAP);
			text.setText(labelText);
			text.setBackground(itemColor);
			subItemLabel = text;
		}
		TableWrapData labelData = new TableWrapData();
		labelData.indent = LABEL_MARGIN;
		subItemLabel.setLayoutData(labelData);
		holder.setSubitemLabel(subItemLabel);
		added++;

		// Add some space to the right of the label

		Label spacer = page.getToolkit().createLabel(buttonComposite, null);
		TableWrapData spacerData = new TableWrapData();
		spacerData.maxWidth = 0;
		spacerData.indent = LABEL_MARGIN;
		spacer.setLayoutData(spacerData);
		added++;

		AbstractExecutable subExecutable = null;
		if(sub.getPerformWhen() != null) {
			sub.getPerformWhen().setSelectedExecutable(viewer.getManager());
			subExecutable = sub.getPerformWhen().getSelectedExecutable();
		} else {
			subExecutable = sub.getExecutable();
		}

		/*
		 * Actions are disabled while inside dialogs.
		 */
		final int fi = index;
		ImageHyperlink startButton = null;
		boolean isActionShown = false;
		if (subExecutable != null && !isInDialogMode()) {
			added++;
			isActionShown = true;
			startButton = createButton(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_START), this, itemColor, Messages.PERFORM_TASK_TOOLTIP);
			final ImageHyperlink finalStartButton = startButton;
			startButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.runSubItemPerformExecutable(finalStartButton, fi);
				}
			});
			holder.setStartButton(startButton);
		}
		if (!isActionShown || subExecutable.isConfirm() || !subExecutable.isRequired()) {
			added++;
			final ImageHyperlink completeButton = createButton(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_COMPLETE), this, itemColor, Messages.COMPLETE_TASK_TOOLTIP);
			completeButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.advanceSubItem(completeButton, true, fi);
				}
			});
			holder.setCompleteButton(completeButton);
		}
		if (sub.isSkip()) {
			added++;
			final ImageHyperlink skipButton = createButton(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_SKIP), this, itemColor, Messages.SKIP_TASK_TOOLTIP);
			skipButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.advanceSubItem(skipButton, false, fi);
				}
			});
			holder.setSkipButton(skipButton);
		}

		while (added < SUBITEM_COLUMNS) {
			// Add filler labels as needed to complete the row
			Label filler = page.getToolkit().createLabel(buttonComposite, null);
			TableWrapData fillerData = new TableWrapData();
			fillerData.maxWidth = 0;
			filler.setLayoutData(fillerData);
			added++;
		}
		holder.setThisValue(thisValue);
		listOfSubItemCompositeHolders.add(holder);
	}

	private void addSeparator() {
		Label pad = page.getToolkit().createLabel(buttonComposite, null);
		TableWrapData padData = new TableWrapData();
		padData.maxWidth = 0;
		pad.setLayoutData(padData);
		Label separator = new Label(buttonComposite, SWT.SEPARATOR + SWT.HORIZONTAL);
		TableWrapData separatorData = new TableWrapData();
		separatorData.align = TableWrapData.FILL;
		separatorData.grabHorizontal = true;
		separatorData.maxHeight = 1;
		separatorData.valign = TableWrapData.MIDDLE;
		separator.setLayoutData(separatorData);
		for (int i = 3; i <= SUBITEM_COLUMNS; i++) {
			Label filler = page.getToolkit().createLabel(buttonComposite, null);
			TableWrapData fillerData = new TableWrapData();
			fillerData.maxWidth = 0;
			filler.setLayoutData(fillerData);
		}
	}

	private AbstractExecutable getExecutable() {
		AbstractExecutable executable = item.getExecutable();
		if(executable == null) {
			if(item.getPerformWhen() != null){
				executable = item.getPerformWhen().getSelectedExecutable();
			}
		}
		return executable;
	}

	private AbstractExecutable getExecutable(int index) {
		if (item.getSubItems() != null && item.getSubItems().size()>0 && listOfSubItemCompositeHolders != null) {
			SubItemCompositeHolder s = listOfSubItemCompositeHolders.get(index);
			if(s != null) {
				SubItem subItem = s.getSubItem();
				AbstractExecutable executable = subItem.getExecutable();
				if(executable == null) {
					if(subItem.getPerformWhen() != null){
						executable = subItem.getPerformWhen().getSelectedExecutable();
					}
				}
				return executable;
			}
		}
		return null;
	}

	public ArrayList<SubItemCompositeHolder> getListOfSubItemCompositeHolders() {
		return listOfSubItemCompositeHolders;
	}

	private ImageHyperlink getStartButton() {
		if (item.getSubItems() != null && item.getSubItems().size() > 0) {
			// Bug 137332 - don't look in items with subitems
			return null;
		}
		if(buttonComposite != null) {
			Control[] controls = buttonComposite.getChildren();
			for (Control control : controls) {
				if(control instanceof ImageHyperlink) {
					String toolTipText = control.getToolTipText();
					if( toolTipText != null &&
						(toolTipText.equals(Messages.PERFORM_TASK_TOOLTIP) ||
						 toolTipText.equals(Messages.RESTART_TASK_TOOLTIP))) {
						return (ImageHyperlink)control;
					}
				}
			}
		}
		return null;
	}

	@Override
	/*package*/ void handleButtons() {
		if(item.isDynamic()) {
			handleDynamicButtons();
			return;
		} else if( item.getSubItems() != null && item.getSubItems().size() > 0) {
			handleSubButtons();
		}

		if (buttonsHandled)
			return;

		createButtonComposite();
		createButtons(item.getExecutable());
		buttonsHandled = true;
	}

	private void handleDynamicButtons() {
		if( item.getSubItems() != null && item.getSubItems().size() > 0 ) {
			handleDynamicSubItemButtons();
		} else if( item.getPerformWhen() != null ) {
			handlePerformWhenButtons();
		}
	}

	private void handleDynamicSubItemButtons() {
		boolean refreshRequired = false;
		if(buttonComposite != null) {
			Control[] children = buttonComposite.getChildren();
			for (Control control : children) {
				control.dispose();
			}

			refreshRequired = true;
		} else {
			createSubItemButtonComposite();
		}

		//Instantiate the list to store the sub item composites.
		listOfSubItemCompositeHolders = new ArrayList<>(20);

		//loop throught the number of sub items, make a new composite for each sub item.
		//Add the spacer, the label, then the buttons that are applicable for each sub item.
		int i=0;
		for (Iterator<AbstractSubItem> iter = item.getSubItems().iterator(); iter.hasNext(); i++) {
			AbstractSubItem subItem = iter.next();
			if( subItem instanceof RepeatedSubItem ) {

				//Get the sub item to add.
				RepeatedSubItem repeatedSubItem = (RepeatedSubItem)subItem;
				String values = repeatedSubItem.getValues();
				values = viewer.getManager().getVariableData(values);
				if(values == null || values.length() <= 0 || (values.startsWith("${") && values.endsWith("}"))) { //$NON-NLS-1$ //$NON-NLS-2$
					String message = NLS.bind(Messages.ERROR_DATA_MISSING_LOG, (new Object[] {repeatedSubItem.getValues()}));
					IStatus status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, null);
					CheatSheetPlugin.getPlugin().getLog().log(status);

					status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, Messages.ERROR_DATA_MISSING, null);
					CheatSheetPlugin.getPlugin().getLog().log(status);
					org.eclipse.jface.dialogs.ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), null, null, status);
					break;
				}

				SubItem sub = (SubItem)repeatedSubItem.getSubItems().get(0);

				StringTokenizer tokenizer = new StringTokenizer(values, ","); //$NON-NLS-1$
				while(tokenizer.hasMoreTokens()) {
					String value = tokenizer.nextToken();
					createSubItemButtons(sub, value, i++);
				}

				// Decrement the counter by because the outer loop increments it prior to the next iteration
				i--;
			} else if( subItem instanceof ConditionalSubItem ) {
				//Get the sub item to add.
				ConditionalSubItem sub = (ConditionalSubItem)subItem;

				sub.setSelectedSubItem(viewer.getManager());
				SubItem selectedSubItem = sub.getSelectedSubItem();

				if(selectedSubItem == null) {
					String message = NLS.bind(Messages.ERROR_CONDITIONAL_DATA_MISSING_LOG, (new Object[] {sub.getCondition(), getItem().getTitle()}));
					IStatus status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, null);
					CheatSheetPlugin.getPlugin().getLog().log(status);

					status = new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, Messages.ERROR_DATA_MISSING, null);
					CheatSheetPlugin.getPlugin().getLog().log(status);
					org.eclipse.jface.dialogs.ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), null, null, status);
					break;
				}

				createSubItemButtons(selectedSubItem, null, i);
			} else if( subItem instanceof SubItem ) {
				createSubItemButtons((SubItem)subItem, null, i);
			}
		}

		if(refreshRequired) {
			refresh(buttonComposite);
		}
	}

	private void handlePerformWhenButtons() {
		boolean refreshRequired = false;

		if(buttonComposite != null) {
			Control[] controls = buttonComposite.getChildren();
			for (int i = 0; i < controls.length; i++) {
				Control control = controls[i];
				if(control instanceof ImageHyperlink) {
					control.dispose();
				}
			}

			refreshRequired = true;
		} else {
			createButtonComposite();
		}

		item.getPerformWhen().setSelectedExecutable(viewer.getManager());
		AbstractExecutable performExecutable = item.getPerformWhen().getSelectedExecutable();

		createButtons(performExecutable);

		if(refreshRequired) {
			refresh(buttonComposite);
		}
	}

	private void handleSubButtons() {
		if (buttonsHandled)
			return;
		//Instantiate the list to store the sub item composites.
		listOfSubItemCompositeHolders = new ArrayList<>(20);

		ArrayList<AbstractSubItem> sublist = item.getSubItems();

		createSubItemButtonComposite();

		//loop throught the number of sub items, make a new composite for each sub item.
		//Add the spacer, the label, then the buttons that are applicable for each sub item.
		for (int i = 0; i < sublist.size(); i++) {
			createSubItemButtons((SubItem)sublist.get(i), null, i);
		}
		buttonsHandled = true;
	}

	/*package*/
	boolean hasConfirm() {
		AbstractExecutable executable = getExecutable();

		if (executable == null || executable.isConfirm()) {
			return true;
		}
		return false;
	}

	/*package*/
	boolean hasConfirm(int index) {
		AbstractExecutable executable = getExecutable(index);

		if (executable == null || executable.isConfirm()) {
			return true;
		}
		return false;
	}

	public String performLineSubstitution(String line, String variable, String value) {
		StringBuilder buffer = new StringBuilder(line.length());

		StringDelimitedTokenizer tokenizer = new StringDelimitedTokenizer(line, variable);
		boolean addValue = false;

		while (tokenizer.hasMoreTokens()) {
			if (addValue) {
				buffer.append(value);
			}
			buffer.append(tokenizer.nextToken());
			addValue = true;
		}
		if (tokenizer.endsWithDelimiter()) {
			buffer.append(value);
		}

		return buffer.toString();
	}

	/*package*/
	IStatus runExecutable(CheatSheetManager csm) {
		return runExecutable(getExecutable(), csm);
	}

	IStatus runExecutable(AbstractExecutable executable, CheatSheetManager csm) {
		if(executable != null) {
			return executable.execute(csm);
		}
		return Status.OK_STATUS;
	}

	/*package*/
	byte runSubItemExecutable(CheatSheetManager csm, int index) {
		if (item.getSubItems() != null && item.getSubItems().size()>0 && listOfSubItemCompositeHolders != null) {
			SubItemCompositeHolder s = listOfSubItemCompositeHolders.get(index);
			if(s != null) {
				AbstractExecutable executable = getExecutable(index);

				if(executable != null) {
					try {
						if(s.getThisValue() != null) {
							csm.setData("this", s.getThisValue()); //$NON-NLS-1$
						}
						IStatus status = runExecutable(executable, csm);
						if ( status.isOK()) {
							return VIEWITEM_ADVANCE;
						}
						if ( status.getSeverity() == IStatus.ERROR) {
							CheatSheetPlugin.getPlugin().getLog().log(status);
							org.eclipse.jface.dialogs.ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), null, null, status);
						}
						return VIEWITEM_DONOT_ADVANCE;
					} finally {
						if(s.getThisValue() != null) {
							csm.setData("this", null); //$NON-NLS-1$
						}
					}
				}
			}
		}
		return VIEWITEM_ADVANCE;
	}

	/*package*/void setButtonsHandled(boolean handled){
		buttonsHandled = handled;
	}

	/*package*/ @Override
	void setIncomplete() {
		super.setIncomplete();

		//check for sub items and reset their icons.
		ArrayList<SubItemCompositeHolder> l = getListOfSubItemCompositeHolders();
		if(l != null){
			for(int j=0; j<l.size(); j++){
				SubItemCompositeHolder s = l.get(j);
				if(s.isCompleted() || s.isSkipped())
					s.getCheckDoneLabel().setVisible(false); //setImage(null);
				if(s.getStartButton() != null) {
					s.getStartButton().setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_START));
					s.getStartButton().setToolTipText(Messages.PERFORM_TASK_TOOLTIP);
				}
			}
		}
	}

	/*package*/ @Override
	void setRestartImage() {
		ImageHyperlink startButton = getStartButton();
		if (startButton != null) {
			startButton.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_RESTART));
			startButton.setText(Messages.RESTART_TASK_TOOLTIP);
			startButton.setToolTipText(Messages.RESTART_TASK_TOOLTIP);
		}
	}

	/*package*/ @Override
	void setStartImage() {
		ImageHyperlink startButton = getStartButton();
		if (startButton != null) {
			startButton.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_START));
			if (startButton.getText() != null) {
				startButton.setText(Messages.PERFORM_TASK_TOOLTIP);
			}
			startButton.setToolTipText(Messages.PERFORM_TASK_TOOLTIP);
		}
	}

	@Override
	boolean hasCompletionMessage() {
		return item.getCompletionMessage() != null;
	}


	@Override
	void createCompletionComposite(boolean isFinalItem) {
		String completionMessage = viewer.getManager().performVariableSubstitution
			(item.getCompletionMessage());
		if (completionMessage != null) {
			Color backgroundColor = bodyWrapperComposite.getBackground();
			completionComposite = page.getToolkit().createComposite(
					bodyWrapperComposite);
			TableWrapLayout completionlayout = new TableWrapLayout();
			completionlayout.numColumns = 1;

			TableWrapData completionData = new TableWrapData(TableWrapData.FILL);

			completionComposite.setLayout(completionlayout);
			completionComposite.setLayoutData(completionData);
			completionComposite.setBackground(backgroundColor);

			FormText completionText = page.getToolkit().createFormText(completionComposite, false);
			completionText.setText(completionMessage, completionMessage.startsWith(IParserTags.FORM_START_TAG), false);
			completionText.setBackground(backgroundColor);
			final ImageHyperlink completeButton = createButtonWithText(
					completionComposite,
					getCompletionButtonIcon(isFinalItem),
					this,
					backgroundColor,
					getCompletionButtonTooltip(isFinalItem));
			completeButton.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					viewer.advanceItem(completeButton, true);
				}
			});
			completionComposite.setVisible(false);
			// The line below is necessary because without it the color of
			// the composite containing the button does not get set
			setBackgroundColor(completionComposite, backgroundColor);
			refresh(completionComposite);
		}
	}

	private Image getCompletionButtonIcon(boolean isFinalItem) {
		if (isFinalItem) {
			return CheatSheetPlugin
			.getPlugin()
			.getImage(
					ICheatSheetResource.CHEATSHEET_RETURN);
		}
		return CheatSheetPlugin
				.getPlugin()
				.getImage(
						ICheatSheetResource.CHEATSHEET_ITEM_BUTTON_COMPLETE);
	}

	private String getCompletionButtonTooltip(boolean isFinalItem) {
		if (isFinalItem) {
			return Messages.RETURN_TO_INTRO_TOOLTIP;
		}
		return Messages.ADVANCE_TASK_TOOLTIP;
	}

	private void refresh(Composite composite) {
		composite.layout();
		getMainItemComposite().layout();
		page.getForm().reflow(true);
	}

	public void refreshItem() {
		if (buttonComposite != null) {
			refresh(buttonComposite);
		}
	}

	@Override
	protected void setFocus() {
		ArrayList<SubItemCompositeHolder> list = getListOfSubItemCompositeHolders();
		Control subitemLabel = null;
		SubItemCompositeHolder holder = null;
		if (list != null) {
			for (Iterator<SubItemCompositeHolder> iter = list.iterator(); iter.hasNext() && subitemLabel == null;) {
				holder = iter.next();
				if (!holder.isCompleted() && !holder.isSkipped()) {
					subitemLabel = holder.getSubitemLabel();
				}
			}
		}
		if (subitemLabel != null) {
			FormToolkit.ensureVisible(subitemLabel);
			if (holder.getStartButton() != null) {
				holder.getStartButton().setFocus();
			} else if (holder.getCompleteButton() != null) {
				holder.getCompleteButton().setFocus();
			}
		} else {
			FormToolkit.ensureVisible(getMainItemComposite());
			super.setFocus();
		}
	}

}
