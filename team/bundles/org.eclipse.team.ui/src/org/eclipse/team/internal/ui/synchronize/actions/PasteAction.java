/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchOperation;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionListenerAction;

public class PasteAction extends SelectionListenerAction {

	private final static String ID = TeamUIPlugin.PLUGIN_ID
			+ ".synchronize.action.paste"; //$NON-NLS-1$
	private Shell fShell;
	private Clipboard fClipboard;

	public PasteAction(IWorkbenchPart part) {
		super(TeamUIMessages.PasteAction_1);

		final ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);
		setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));

		fShell = part.getSite().getShell();
		Assert.isNotNull(fShell);
		fClipboard = new Clipboard(fShell.getDisplay());
		setToolTipText(TeamUIMessages.PasteAction_2);
		setId(ID);
	}

	@Override
	public void run() {
		IStorage patchStorage = getPatchStorage();
		if (patchStorage != null) {
			IResource resource = null;
			IResource[] resources = getSelectedResources().toArray(new IResource[0]);
			if (resources.length > 0) {
				resource = resources[0];
			}
			// XXX: This will be fixed in 3.7, see
			// https://bugs.eclipse.org/309803
			ApplyPatchOperation operation = new ApplyPatchOperation(null,
					patchStorage, resource, new CompareConfiguration()) {
				@Override
				protected boolean isApplyPatchInSynchronizeView() {
					// ignore the preference, apply in the sync view
					return true;
				}
			};
			operation.openWizard();
		} else {
			MessageDialog.openError(fShell, TeamUIMessages.PasteAction_3,
					TeamUIMessages.PasteAction_4);
		}
	}

	private IStorage getPatchStorage() {
		final String text = getClipboardText();
		if (text == null)
			return null;

		IStorage storage = new IEncodedStorage() {
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

			@Override
			public boolean isReadOnly() {
				return false;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public IPath getFullPath() {
				return null;
			}

			@Override
			public InputStream getContents() throws CoreException {
				try {
					return new ByteArrayInputStream(text.getBytes(getCharset()));
				} catch (UnsupportedEncodingException e) {
					throw new CoreException(new Status(IStatus.ERROR,
							TeamUIPlugin.ID, IStatus.ERROR, e.getMessage(), e));
				}
			}

			@Override
			public String getCharset() throws CoreException {
				return "UTF-8"; //$NON-NLS-1$
			}
		};

		try {
			if (ApplyPatchOperation.isPatch(storage)) {
				return storage;
			}
		} catch (CoreException e) {
			// ignore
		}
		return null;
	}

	private String getClipboardText() {
		Transfer transfer = TextTransfer.getInstance();
		if (isAvailable(transfer, fClipboard.getAvailableTypes())) {
			return (String) getContents(fClipboard, transfer, fShell);
		}
		return null;
	}

	private static boolean isAvailable(Transfer transfer,
			TransferData[] availableDataTypes) {
		for (TransferData availableDataType : availableDataTypes) {
			if (transfer.isSupportedType(availableDataType)) {
				return true;
			}
		}
		return false;
	}

	private static Object getContents(final Clipboard clipboard,
			final Transfer transfer, Shell shell) {
		// see bug 33028 for explanation why we need this
		final Object[] result = new Object[1];
		shell.getDisplay().syncExec(() -> result[0] = clipboard.getContents(transfer));
		return result[0];
	}

	public void dispose() {
		fClipboard.dispose();
	}

}
