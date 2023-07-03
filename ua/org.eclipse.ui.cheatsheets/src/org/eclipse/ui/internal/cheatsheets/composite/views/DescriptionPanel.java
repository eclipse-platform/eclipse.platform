/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.views;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.SuccesorTaskFinder;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskStateUtilities;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.composite.parser.MarkupParser;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.IEditableTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;

/**
 * The description panel of a composite cheat sheet panel. This panel shows the introduction
 * message and depending upon the type and state of the task a selection of the following:
 * Completion message
 * A message indicating that the task has been skipped.
 * A message indicating that a parent task has been skipped.
 * A message indicating that the task is blocked.
 * A message indicationg that a parent choice is already satisfied.
 * A link to a successor task.
 * All tasks completed message.
 */

public class DescriptionPanel {

	public static final String REVIEW_IMAGE = "review"; //$NON-NLS-1$
	private static final String GOTO_IMAGE = "goto"; //$NON-NLS-1$
	private static final String SKIP_IMAGE = "skip"; //$NON-NLS-1$
	private static final String START_IMAGE = "start"; //$NON-NLS-1$
	private static final String WARNING_IMAGE = "warning"; //$NON-NLS-1$
	private static final String INFORMATION_IMAGE = "info"; //$NON-NLS-1$
	private Composite panel;
	private Composite control;
	private FormText upperText;
	private FormText lowerText;
	private ScrolledForm form;
	protected DescriptionPanel() {}

	public DescriptionPanel(ManagedForm mform, Composite parent) {

		FormToolkit toolkit = mform.getToolkit();
		control = new Composite(parent, SWT.NULL);
		final GridLayout controlLayout = new GridLayout();
		controlLayout.marginHeight = 0;
		controlLayout.marginWidth = 0;
		control.setLayout(controlLayout);
		form = toolkit.createScrolledForm(control);
		panel = form.getBody();
		form.setLayoutData(new GridData(GridData.FILL_BOTH));
		toolkit.adapt(panel);

		TableWrapLayout layout = new TableWrapLayout();
		panel.setLayout(layout);

		upperText = mform.getToolkit().createFormText(panel, false);
		mform.getToolkit().adapt(upperText, false, false);

		Composite separator = toolkit.createCompositeSeparator(panel);

		TableWrapData data = new TableWrapData();
		data.align = TableWrapData.FILL;
		data.grabHorizontal = true;
		data.maxHeight = 1;
		separator.setLayoutData(data);

		lowerText = mform.getToolkit().createFormText(panel, false);
		mform.getToolkit().adapt(lowerText, false, false);

		upperText.marginWidth = 5;
		upperText.marginHeight = 5;
		upperText.setFont("header", JFaceResources.getHeaderFont()); //$NON-NLS-1$
		upperText.setColor("title", toolkit.getColors().getColor(IFormColors.TITLE)); //$NON-NLS-1$
		lowerText.marginWidth = 5;
		lowerText.marginHeight = 5;
		lowerText.setImage(START_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_START));
		lowerText.setImage(SKIP_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_SKIP));
		lowerText.setImage(GOTO_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_GOTO_TASK));
		lowerText.setImage(REVIEW_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_REVIEW));
		lowerText.setImage(WARNING_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.WARNING));
		lowerText.setImage(INFORMATION_IMAGE, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.INFORMATION));
	}

	public Control getControl() {
		return control;
	}

	public void addHyperlinkListener(IHyperlinkListener listener) {
		lowerText.addHyperlinkListener(listener);
	}

	public void showDescription(final ICompositeCheatSheetTask task) {
		StringBuilder upperMessage = new StringBuilder();
		upperMessage.append("<form>"); //$NON-NLS-1$
		upperMessage.append("<p><span color=\"title\" font=\"header\">"); //$NON-NLS-1$
		upperMessage.append(MarkupParser.escapeText(task.getName()));
		upperMessage.append("</span></p>"); //$NON-NLS-1$
		upperMessage.append(MarkupParser.createParagraph(task.getDescription(), null));
		upperMessage.append("</form>"); //$NON-NLS-1$
		upperText.setText(upperMessage.toString(), true, false);

		StringBuilder buf = new StringBuilder();
		buf.append("<form>"); //$NON-NLS-1$

		boolean startable = false;
		boolean isBlocked = false;
		boolean isSkippable = ((AbstractTask)task).isSkippable();

		if (task.getState() == ICompositeCheatSheetTask.COMPLETED) {
			buf.append(MarkupParser.createParagraph(task.getCompletionMessage(), null));
			isSkippable = false;
		} else if (task.getState() == ICompositeCheatSheetTask.SKIPPED) {
			buf.append(MarkupParser.createParagraph(Messages.THIS_TASK_SKIPPED, INFORMATION_IMAGE));
			isSkippable = false;
		} else if (TaskStateUtilities.findSkippedAncestor(task) != null) {
			ICompositeCheatSheetTask skipped = TaskStateUtilities.findSkippedAncestor(task);
			String skipParentMsg = NLS.bind(Messages.PARENT_SKIPPED,
				(new Object[] {MarkupParser.escapeText((skipped.getName()))}));
			buf.append(MarkupParser.createParagraph(skipParentMsg, WARNING_IMAGE));
			isSkippable = false;
		} else if (TaskStateUtilities.findCompletedAncestor(task) != null) {
			ICompositeCheatSheetTask completed = TaskStateUtilities.findCompletedAncestor(task);
			String completedParentMsg = NLS.bind(Messages.PARENT_COMPLETED,
			   (new Object[] {MarkupParser.escapeText(completed.getName())}));
			buf.append(MarkupParser.createParagraph(completedParentMsg, WARNING_IMAGE));
			isSkippable = false;
		} else if (!task.requiredTasksCompleted()) {
			isBlocked = true;
			showBlockingTasks(Messages.COMPOSITE_PAGE_BLOCKED, task, buf);
		} else if (TaskStateUtilities.findBlockedAncestor(task) != null) {
			isBlocked = true;
			ICompositeCheatSheetTask blockedAncestor = TaskStateUtilities.findBlockedAncestor(task);
			String blockingAncestorMsg = NLS.bind(Messages.PARENT_BLOCKED,
					(new Object[] {MarkupParser.escapeText(blockedAncestor.getName())}));
			showBlockingTasks(blockingAncestorMsg , blockedAncestor, buf);
		} else {
			startable = task instanceof IEditableTask && task.getState() == ICompositeCheatSheetTask.NOT_STARTED;
		}

		if (startable) {
			addHyperlink(buf, CompositeCheatSheetPage.START_HREF, START_IMAGE, Messages.COMPOSITE_PAGE_START_TASK);
		}

		if (task instanceof IEditableTask && task.getState() == ICompositeCheatSheetTask.COMPLETED ) {
			addHyperlink(buf, CompositeCheatSheetPage.REVIEW_TAG, REVIEW_IMAGE, Messages.COMPOSITE_PAGE_REVIEW_TASK);
		}

		if (isSkippable) {
			String skipMessage;
			if (task instanceof ITaskGroup) {
				skipMessage = Messages.COMPOSITE_PAGE_SKIP_TASK_GROUP;
			} else {
				skipMessage = Messages.COMPOSITE_PAGE_SKIP_TASK;
			}
			addHyperlink(buf, CompositeCheatSheetPage.SKIP_HREF, SKIP_IMAGE, skipMessage);
		}

		if (!startable && !isBlocked) {
			showSuccesorTaskLinks(task, buf);
		}

		buf.append("</form>"); //$NON-NLS-1$

		lowerText.setText(buf.toString(), true, false);
		getControl().setData(ICompositeCheatsheetTags.TASK, task);
		form.reflow(true);
	}

	private void showBlockingTasks(String message, final ICompositeCheatSheetTask task, StringBuilder buf) {
		buf.append("<p/>"); //$NON-NLS-1$
		buf.append("<p>"); //$NON-NLS-1$
		buf.append("<img href=\""); //$NON-NLS-1$
		buf.append(WARNING_IMAGE);
		buf.append("\"/> "); //$NON-NLS-1$
		buf.append(message);
		buf.append("</p>");	 //$NON-NLS-1$// Add the list of blocking tasks

		ICompositeCheatSheetTask[] requiredTasks = task.getRequiredTasks();
		for (ICompositeCheatSheetTask requiredTask : requiredTasks) {
			warnOfIncompleteTask(buf, requiredTask);
		}
		buf.append("<p>"); //$NON-NLS-1$
		buf.append("</p>");	 //$NON-NLS-1$
	}

	private void addHyperlink(StringBuilder buf, String href, String imageRef, String message) {
		buf.append("<p><a href=\""); //$NON-NLS-1$
		buf.append(href);
		buf.append("\">"); //$NON-NLS-1$
		buf.append("<img href=\""); //$NON-NLS-1$
		buf.append(imageRef);
		buf.append("\"/> "); //$NON-NLS-1$
		buf.append(message);
		buf.append("</a></p>"); //$NON-NLS-1$
	}

	/*
	 * If this task is incomplete create a message to that effect
	 */
	private void warnOfIncompleteTask(StringBuilder buf, ICompositeCheatSheetTask task) {
		if (task.getState() != ICompositeCheatSheetTask.COMPLETED &&
			task.getState() != ICompositeCheatSheetTask.SKIPPED	) {
			buf.append("<li>"); //$NON-NLS-1$
			buf.append("<a href=\""); //$NON-NLS-1$
			buf.append(CompositeCheatSheetPage.GOTO_TASK_TAG);
			buf.append(task.getId());
			buf.append("\">"); //$NON-NLS-1$
			buf.append(NLS.bind(Messages.COMPOSITE_PAGE_TASK_NOT_COMPLETE, (new Object[]
				{MarkupParser.escapeText(task.getName())})));
			buf.append("</a>"); //$NON-NLS-1$
			buf.append("</li>"); //$NON-NLS-1$
		}
	}

	private void showSuccesorTaskLinks(ICompositeCheatSheetTask task, StringBuilder buf) {
		// Add the links to the next tasks
		ICompositeCheatSheetTask[] successorTasks = new SuccesorTaskFinder(task).getRecommendedSuccessors();
		for (ICompositeCheatSheetTask successor : successorTasks) {
			String message = NLS.bind(Messages.COMPOSITE_PAGE_GOTO_TASK, (new Object[]
				 {MarkupParser.escapeText(successor.getName())}));
			addHyperlink(buf, CompositeCheatSheetPage.GOTO_TASK_TAG + successor.getId(), GOTO_IMAGE, message);
		}
	}

}
