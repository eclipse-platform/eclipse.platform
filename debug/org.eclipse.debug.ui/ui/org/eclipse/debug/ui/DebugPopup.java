/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import java.text.MessageFormat;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.debug.internal.ui.views.DebugUIViewsMessages;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.osgi.framework.FrameworkUtil;

/**
 * A <code>PopupDialog</code> that is automatically positioned relative
 * to a specified anchor point. The popup can be dismissed in the same
 * manor as all popup dialogs, but additionally allows clients the option
 * of specifying a command id that can be used to persist the contents of
 * the dialog.
 * <p>
 * Clients may subclass this.
 * @since 3.2
 */
public abstract class DebugPopup extends PopupDialog {

	private Point fAnchor;

	private IHandlerActivation fActivation;

	private IHandlerService fHandlerService;

	private String fCommandId;

	private boolean fPersisted = false;

	/**
	 * Constructs a new popup dialog of type <code>PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE</code>
	 * @param parent The parent shell
	 * @param anchor point at which to anchor the popup dialog in Display coordinate space.
	 * 	Since 3.3, <code>null</code> can be specified to use a default anchor point
	 * @param commandId The command id to be used for persistence of
	 *  the dialog, or <code>null</code>
	 */
	public DebugPopup(Shell parent, Point anchor, String commandId) {
		super(parent, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, true, true, false, true, false, null, null);
		fAnchor = anchor;
		fCommandId = commandId;
	}

	/**
	 * Returns the text to be shown in the popups's information area.
	 * May return <code>null</code>.
	 * <p>
	 * By default, if this dialog has a persistence command associated with it,
	 * the text displayed is of the form "Press {key-sequence} to {action}". The
	 * action text is specified by the method <code>getActionText()</code>.
	 * </p>
	 * @return The text to be shown in the popup's information area or <code>null</code>
	 */
	protected String getInfoText() {
		if (getCommandId() != null && getActionText() != null) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IBindingService bindingService = workbench.getAdapter(IBindingService.class);
			String formattedBinding = bindingService.getBestActiveBindingFormattedFor(getCommandId());

			String infoText = null;
			if (formattedBinding != null) {
				infoText = MessageFormat.format(DebugUIViewsMessages.InspectPopupDialog_1, new Object[] {
						formattedBinding, getActionText() });
			}
			return infoText;
		}
		return null;
	}

	/**
	 * Returns the text to be shown as the action performed when this dialog's
	 * persist command is invoked, or <code>null</code>.
	 * <p>
	 * Subclasses should override as necessary.
	 * </p>
	 * @return the text to be shown as the action performed when this dialog's
	 *  persist command is invoked
	 */
	protected String getActionText() {
		return null;
	}

	/**
	 * Returns the command id to be used for persisting the contents of the
	 * dialog. If the contents should not be persisted, this method should
	 * return null.
	 *
	 * @return The command id to be used for persisting the contents of the
	 * dialog or <code>null</code>
	 */
	protected String getCommandId() {
		return fCommandId;
	}

	/**
	 * Persists the contents of the dialog. Subclasses should override as required,
	 * but also call super.persist().
	 */
	protected void persist() {
		fPersisted = true;
	}

	/**
	 * Returns whether the command handler was invoked to persist this popup's result.
	 *
	 * @return whether the command handler was invoked to persist this popup's result
	 */
	protected boolean wasPersisted() {
		return fPersisted;
	}

	@Override
	protected abstract Control createDialogArea(Composite parent);


	/**
	 * Returns the initial location to use for the shell based upon the
	 * current selection in the viewer. Bottom is preferred to top, and
	 * right is preferred to left, therefore if possible the popup will
	 * be located below and to the right of the selection.
	 *
	 * @param initialSize
	 *            the initial size of the shell, as returned by
	 *            <code>getInitialSize</code>.
	 * @return the initial location of the shell
	 */
	@Override
	protected Point getInitialLocation(Point initialSize) {
		if (fAnchor == null) {
			return super.getInitialLocation(initialSize);
		}
		Point point = fAnchor;
		Rectangle monitor = getShell().getMonitor().getClientArea();
		if (monitor.width < point.x + initialSize.x) {
			point.x = Math.max(0, point.x - initialSize.x);
		}
		if (monitor.height < point.y + initialSize.y) {
			point.y = Math.max(0, point.y - initialSize.y);
		}
		return point;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		return PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(DebugPopup.class)).getDialogSettings();
	}

	@Override
	public int open() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		String commandId = getCommandId();
		if (commandId != null) {
			IHandler fCloseHandler = new AbstractHandler() {
				@Override
				public Object execute(ExecutionEvent event) throws ExecutionException {
					persist();
					close();
					return null;
				}
			};

			fHandlerService = workbench.getAdapter(IHandlerService.class);
			fActivation = fHandlerService.activateHandler(commandId, fCloseHandler);
		}

		String infoText = getInfoText();
		if (infoText != null) {
			setInfoText(infoText);
		}

		return super.open();
	}

	@Override
	public boolean close() {
		if (fActivation != null) {
			fHandlerService.deactivateHandler(fActivation);
		}

		return super.close();
	}
}
