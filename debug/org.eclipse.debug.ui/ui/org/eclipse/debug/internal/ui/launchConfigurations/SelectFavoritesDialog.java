/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.AbstractDebugCheckboxSelectionDialog;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

/**
 * This dialog is used to select one or more launch configurations to add to
 * your favorites
 *
 * @since 3.3.0
 */
public class SelectFavoritesDialog extends AbstractDebugCheckboxSelectionDialog {

	/**
	 * Content provider for table
	 */
	protected class LaunchConfigurationContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			ILaunchConfiguration[] all = null;
			try {
				all = LaunchConfigurationManager
						.filterConfigs(DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations());
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
				return new ILaunchConfiguration[0];
			}
			List<ILaunchConfiguration> list = new ArrayList<>(all.length);
			ViewerFilter filter = new LaunchGroupFilter(fHistory.getLaunchGroup());
			for (ILaunchConfiguration config : all) {
				if (filter.select(null, null, config)) {
					list.add(config);
				}
			}
			list.removeAll(fCurrentFavoriteSet);
			Object[] objs = list.toArray();
			Arrays.sort(objs, (o1, o2) -> {
				if (o1 instanceof ILaunchConfiguration launch1 && o2 instanceof ILaunchConfiguration launch2) {
					try {
						String type1 = launch1.getType().getName();
						String type2 = launch2.getType().getName();
						int cmp = type1.compareToIgnoreCase(type2);
						if (cmp != 0) {
							return cmp;
						}
						return launch1.getName().compareToIgnoreCase(launch2.getName());
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
				}
				return 0;
			});
			return objs;
		}

		@Override
		public void dispose() {
		}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	private final LaunchHistory fHistory;
	private final List<ILaunchConfiguration> fCurrentFavoriteSet;

	/**
	 * Constructor
	 */
	public SelectFavoritesDialog(Shell parentShell, LaunchHistory history, List<ILaunchConfiguration> favorites) {
		super(parentShell);
		fHistory = history;
		fCurrentFavoriteSet = favorites;
		setTitle(MessageFormat.format(LaunchConfigurationsMessages.FavoritesDialog_0, getModeLabel()));
		setShowSelectAllButtons(true);
	}

	/**
	 * Returns a label to use for launch mode with accelerators removed.
	 *
	 * @return label to use for launch mode with accelerators removed
	 */
	private String getModeLabel() {
		return DebugUIPlugin.removeAccelerators(fHistory.getLaunchGroup().getLabel());
	}

	@Override
	protected String getDialogSettingsId() {
		return IDebugUIConstants.PLUGIN_ID + ".SELECT_FAVORITESS_DIALOG"; //$NON-NLS-1$
	}

	@Override
	protected Object getViewerInput() {
		return fHistory.getLaunchGroup().getMode();
	}

	@Override
	protected IContentProvider getContentProvider() {
		return new LaunchConfigurationContentProvider();
	}

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.SELECT_FAVORITES_DIALOG;
	}

	@Override
	protected String getViewerLabel() {
		return LaunchConfigurationsMessages.FavoritesDialog_7;
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Text filterText = new Text(container, SWT.SEARCH | SWT.CANCEL);
		filterText.setMessage(LaunchConfigurationsMessages.SelectFavTypeToFilter);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Table table = new Table(container, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		gd.widthHint = 250;
		table.setLayoutData(gd);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		ViewerFilter filter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer2, Object parentElement, Object element) {
				String search = filterText.getText().toLowerCase();
				if (search.isEmpty()) {
					return true;
				}
				return element.toString().toLowerCase().contains(search);
			}
		};
		viewer.addFilter(filter);
		filterText.addModifyListener(e -> viewer.refresh());
		return viewer;
	}
}
