/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Dialog for organizing favorite launch configurations
 */
public class FavoritesDialog extends TrayDialog {

	/**
	 * Table of favorite launch configurations
	 */
	private TableViewer fFavoritesTable;

	// history being organized
	private LaunchHistory fHistory;

	// favorites collection under edit
	private List<ILaunchConfiguration> fFavorites;

	// buttons
	protected Button fAddFavoriteButton;
	protected Button fRemoveFavoritesButton;
	protected Button fMoveUpButton;
	protected Button fMoveDownButton;

	// button action handler
	/**
	 * Listener that delegates when a button is pressed
	 */
	private SelectionAdapter fButtonListener= new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button button = (Button) e.widget;
			if (button == fAddFavoriteButton) {
				handleAddConfigButtonSelected();
			} else if (button == fRemoveFavoritesButton) {
				removeSelectedFavorites();
			} else if (button == fMoveUpButton) {
				handleMoveUpButtonSelected();
			} else if (button == fMoveDownButton) {
				handleMoveDownButtonSelected();
			}
		}
	};

	/**
	 * Listener that delegates when the selection changes in a table
	 */
	private ISelectionChangedListener fSelectionChangedListener= event -> handleFavoriteSelectionChanged();

	/**
	 * Listener that delegates when a key is pressed in a table
	 */
	private KeyListener fKeyListener= new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if (event.character == SWT.DEL && event.stateMask == 0) {
				removeSelectedFavorites();
			}
		}
	};

	/**
	 * Content provider for favorites table
	 */
	protected class FavoritesContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			ILaunchConfiguration[] favorites= getFavorites().toArray(new ILaunchConfiguration[0]);
			return LaunchConfigurationManager.filterConfigs(favorites);
		}
		@Override
		public void dispose() {}
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}

	/**
	 * Constructs a favorites dialog.
	 *
	 * @param parentShell shell to open the dialog on
	 * @param history launch history to edit
	 */
	public FavoritesDialog(Shell parentShell, LaunchHistory history) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fHistory = history;
	}

	/**
	 * The 'add config' button has been pressed
	 */
	protected void handleAddConfigButtonSelected() {
		SelectFavoritesDialog sfd = new SelectFavoritesDialog(fFavoritesTable.getControl().getShell(), getLaunchHistory(), getFavorites());
		sfd.open();
		Object[] selection = sfd.getResult();
		if (selection != null) {
			for (Object s : selection) {
				getFavorites().add((ILaunchConfiguration) s);
			}
			updateStatus();
		}
	}

	/**
	 * The 'remove favorites' button has been pressed
	 */
	protected void removeSelectedFavorites() {
		IStructuredSelection sel = getFavoritesTable().getStructuredSelection();
		Iterator<?> iter = sel.iterator();
		while (iter.hasNext()) {
			Object config = iter.next();
			getFavorites().remove(config);
		}
		getFavoritesTable().refresh();
	}

	/**
	 * The 'move up' button has been pressed
	 */
	protected void handleMoveUpButtonSelected() {
		handleMove(-1);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 500);
	}

	/**
	 * The 'move down' button has been pressed
	 */
	protected void handleMoveDownButtonSelected() {
		handleMove(1);
	}

	/**
	 * Handles moving a favorite up or down the listing
	 * @param direction the direction to make the move (up or down)
	 */
	protected void handleMove(int direction) {
		IStructuredSelection sel = getFavoritesTable().getStructuredSelection();
		List<?> selList = sel.toList();
		Object[] movedFavs= new Object[getFavorites().size()];
		int i;
		for (Object config : selList) {
			i= getFavorites().indexOf(config);
			movedFavs[i + direction]= config;
		}

		getFavorites().removeAll(selList);

		for (int j = 0; j < movedFavs.length; j++) {
			Object config = movedFavs[j];
			if (config != null) {
				getFavorites().add(j, (ILaunchConfiguration) config);
			}
		}
		getFavoritesTable().refresh();
		handleFavoriteSelectionChanged();
	}

	/**
	 * Returns the table of favorite launch configurations.
	 *
	 * @return table viewer
	 */
	protected TableViewer getFavoritesTable() {
		return fFavoritesTable;
	}


	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		getShell().setText(MessageFormat.format(LaunchConfigurationsMessages.FavoritesDialog_1, new Object[] { getModeLabel() }));
		createFavoritesArea(composite);
		handleFavoriteSelectionChanged();
		return composite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getDialogArea(), IDebugHelpContextIds.ORGANIZE_FAVORITES_DIALOG);
		return contents;
	}

	/**
	 * Returns a label to use for launch mode with accelerators removed.
	 *
	 * @return label to use for launch mode with accelerators removed
	 */
	private String getModeLabel() {
		return DebugUIPlugin.removeAccelerators(fHistory.getLaunchGroup().getLabel());
	}

	/**
	 * Creates the main area of the dialog
	 * @param parent the parent to add this content to
	 */
	protected void createFavoritesArea(Composite parent) {
		Composite topComp = SWTFactory.createComposite(parent, parent.getFont(), 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createLabel(topComp, LaunchConfigurationsMessages.FavoritesDialog_2, 2);
		fFavoritesTable = createTable(topComp, new FavoritesContentProvider());
		Composite buttonComp = SWTFactory.createComposite(topComp, topComp.getFont(), 1, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		fAddFavoriteButton = SWTFactory.createPushButton(buttonComp, LaunchConfigurationsMessages.FavoritesDialog_3, null);
		fAddFavoriteButton.addSelectionListener(fButtonListener);
		fAddFavoriteButton.setEnabled(true);
		fRemoveFavoritesButton = SWTFactory.createPushButton(buttonComp, LaunchConfigurationsMessages.FavoritesDialog_4, null);
		fRemoveFavoritesButton.addSelectionListener(fButtonListener);
		fMoveUpButton = SWTFactory.createPushButton(buttonComp, LaunchConfigurationsMessages.FavoritesDialog_5, null);
		fMoveUpButton.addSelectionListener(fButtonListener);
		fMoveDownButton = SWTFactory.createPushButton(buttonComp, LaunchConfigurationsMessages.FavoritesDialog_6, null);
		fMoveDownButton.addSelectionListener(fButtonListener);
	}

	/**
	 * Creates a fully configured table with the given content provider
	 */
	private TableViewer createTable(Composite parent, IContentProvider contentProvider) {
		TableViewer tableViewer= new TableViewer(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setInput(DebugUIPlugin.getDefault());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		gd.heightHint = 100;
		tableViewer.getTable().setLayoutData(gd);
		tableViewer.getTable().setFont(parent.getFont());
		tableViewer.addSelectionChangedListener(fSelectionChangedListener);
		tableViewer.getControl().addKeyListener(fKeyListener);
		return tableViewer;
	}

	/**
	 * Returns the current list of favorites.
	 */
	protected List<ILaunchConfiguration> getFavorites() {
		if (fFavorites == null) {
			ILaunchConfiguration[] favs = getInitialFavorites();
			fFavorites = new ArrayList<>(favs.length);
			addAll(favs, fFavorites);
		}
		return fFavorites;
	}

	protected LaunchHistory getLaunchHistory() {
		return fHistory;
	}

	/**
	 * Returns the initial content for the favorites list
	 */
	protected ILaunchConfiguration[] getInitialFavorites() {
		return getLaunchHistory().getFavorites();
	}

	/**
	 * Returns the mode of this page - run or debug.
	 */
	protected String getMode() {
		return getLaunchHistory().getLaunchGroup().getMode();
	}

	/**
	 * Copies the array into the list
	 */
	protected void addAll(ILaunchConfiguration[] array, List<ILaunchConfiguration> list) {
		Collections.addAll(list, array);
	}

	/**
	 * Refresh all tables and buttons
	 */
	protected void updateStatus() {
		getFavoritesTable().refresh();
		handleFavoriteSelectionChanged();
	}

	/**
	 * The selection in the favorites list has changed
	 */
	protected void handleFavoriteSelectionChanged() {
		IStructuredSelection selection = getFavoritesTable().getStructuredSelection();
		List<ILaunchConfiguration> favs = getFavorites();
		boolean notEmpty = !selection.isEmpty();
		Iterator<?> elements = selection.iterator();
		boolean first= false;
		boolean last= false;
		int lastFav= favs.size() - 1;
		while (elements.hasNext()) {
			Object element = elements.next();
			if(!first && favs.indexOf(element) == 0) {
				first= true;
			}
			if (!last && favs.indexOf(element) == lastFav) {
				last= true;
			}
		}

		fRemoveFavoritesButton.setEnabled(notEmpty);
		fMoveUpButton.setEnabled(notEmpty && !first);
		fMoveDownButton.setEnabled(notEmpty && !last);
	}

	/**
	 * Method performOK. Uses scheduled Job format.
	 * @since 3.2
	 */
	public void saveFavorites() {

		final Job job = new Job(LaunchConfigurationsMessages.FavoritesDialog_8) {
			@SuppressWarnings("deprecation")
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				ILaunchConfiguration[] initial = getInitialFavorites();
				List<ILaunchConfiguration> current = getFavorites();
				String groupId = getLaunchHistory().getLaunchGroup().getIdentifier();

				int taskSize = Math.abs(initial.length-current.size());//get task size
				monitor.beginTask(LaunchConfigurationsMessages.FavoritesDialog_8, taskSize);//and set it
				// removed favorites
				for (ILaunchConfiguration configuration : initial) {
					if (!current.contains(configuration)) {
						// remove fav attributes
						try {
							ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
							workingCopy.setAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, (String)null);
							workingCopy.setAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, (String)null);
							List<String> groups = workingCopy.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List<String>) null);
							if (groups != null) {
								groups.remove(groupId);
								if (groups.isEmpty()) {
									groups = null;
								}
								workingCopy.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, groups);
							}
							workingCopy.doSave();
						} catch (CoreException e) {
							DebugUIPlugin.log(e);
							return Status.CANCEL_STATUS;
						}
					}
					monitor.worked(1);
				}

				// update added favorites
				Iterator<ILaunchConfiguration> favs = current.iterator();
				while (favs.hasNext()) {
					ILaunchConfiguration configuration = favs.next();
					try {
						List<String> groups = configuration.getAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, (List<String>) null);
						if (groups == null) {
							groups = new ArrayList<>();
						}
						if (!groups.contains(groupId)) {
							groups.add(groupId);
							ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
							workingCopy.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, groups);
							workingCopy.doSave();
						}
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
						return Status.CANCEL_STATUS;
					}
					monitor.worked(1);
				}

				fHistory.setFavorites(getArray(current));
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		PlatformUI.getWorkbench().getProgressService().showInDialog(getParentShell(), job);
		job.schedule();

	}

	protected ILaunchConfiguration[] getArray(List<ILaunchConfiguration> list) {
		return list.toArray(new ILaunchConfiguration[list.size()]);
	}

	@Override
	protected void okPressed() {
		saveFavorites();
		super.okPressed();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(FavoritesDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsSectionName());
		}
		return section;
	}

	/**
	 * Returns the name of the section that this dialog stores its settings in
	 *
	 * @return String
	 */
	private String getDialogSettingsSectionName() {
		return "FAVORITES_DIALOG_SECTION"; //$NON-NLS-1$
	}
}
