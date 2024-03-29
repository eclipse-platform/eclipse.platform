/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Configures visible columns in an asynch tree viewer/
 *
 * @since 3.2
 */
public class ConfigureColumnsAction extends Action implements IUpdate {

	private final TreeModelViewer fViewer;

	static class ColumnContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return ((IColumnPresentation)inputElement).getAvailableColumns();
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	class ColumnLabelProvider extends LabelProvider {

		private final Map<ImageDescriptor, Image> fImages = new HashMap<>();

		@Override
		public Image getImage(Object element) {
			ImageDescriptor imageDescriptor = fViewer.getColumnPresentation().getImageDescriptor((String)element);
			if (imageDescriptor != null) {
				Image image = fImages.get(imageDescriptor);
				if (image == null) {
					image = imageDescriptor.createImage();
					fImages.put(imageDescriptor, image);
				}
				return image;
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			return fViewer.getColumnPresentation().getHeader((String)element);
		}

		@Override
		public void dispose() {
			super.dispose();
			for (Image image : fImages.values()) {
				image.dispose();
			}
			fImages.clear();
		}



	}

	public ConfigureColumnsAction(TreeModelViewer viewer) {
		setText(ActionMessages.ConfigureColumnsAction_0);
		setId(DebugUIPlugin.getUniqueIdentifier() + ".ConfigureColumnsAction"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CONFIGURE_COLUMNS_ACTION);
		fViewer = viewer;
	}

	@Override
	public void update() {
		setEnabled(fViewer.isShowColumns());
	}

	@Override
	public void run() {
		ListSelectionDialog dialog = new ListSelectionDialog(
				fViewer.getControl().getShell(),
				fViewer.getColumnPresentation(),
				new ColumnContentProvider(),
				new ColumnLabelProvider(),
				ActionMessages.ConfigureColumnsAction_1);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.CONFIGURE_COLUMNS_DIALOG);
		String[] visibleColumns = fViewer.getVisibleColumns();
		List<String> initialSelection = new ArrayList<>(visibleColumns.length);
		Collections.addAll(initialSelection, visibleColumns);
		dialog.setTitle(ActionMessages.ConfigureColumnsAction_2);
		dialog.setInitialElementSelections(initialSelection);
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 0) {
				fViewer.setShowColumns(false);
			} else {
				String[] ids = new String[result.length];
				System.arraycopy(result, 0, ids, 0, result.length);
				fViewer.resetColumnSizes(ids);
				fViewer.setVisibleColumns(ids);
			}
		}

	}

}
