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
 *     Ian Pun (Red Hat Inc.) - Bug 518652
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * A tree view of launch configurations
 */
public class LaunchConfigurationView extends AbstractDebugView implements ILaunchConfigurationListener {

	/**
	 * the filtering tree viewer
	 *
	 * @since 3.2
	 */
	private LaunchConfigurationFilteredTree fTree;

	/**
	 * a handle to the launch manager
	 *
	 * @since 3.2
	 */
	private ILaunchManager fLaunchManager = DebugPlugin.getDefault().getLaunchManager();

	/**
	 * The launch group to display
	 */
	private LaunchGroupExtension fLaunchGroup;

	/**
	 * Actions
	 */
	private CreateLaunchConfigurationAction fCreateAction;
	private CreateLaunchConfigurationPrototypeAction fCreatePrototypeAction;
	private DeleteLaunchConfigurationAction fDeleteAction;
	private DuplicateLaunchConfigurationAction fDuplicateAction;
	private ExportLaunchConfigurationAction fExportAction;
	private CollapseAllLaunchConfigurationAction fCollapseAllAction;
	private LinkPrototypeAction fLinkPrototypeAction;
	private UnlinkPrototypeAction fUnlinkPrototypeAction;
	private ResetWithPrototypeValuesAction fResetWithPrototypeValuesAction;

	/**
	 * Action for providing filtering to the Launch Configuration Dialog
	 * @since 3.2
	 */
	private FilterLaunchConfigurationAction fFilterAction;

	/**
	 * This label is used to notify users that items (possibly) have been filtered from the
	 * launch configuration view
	 * @since 3.3
	 */
	private Label fFilteredNotice = null;

	/**
	 * Whether to automatically select configs that are added
	 */
	private boolean fAutoSelect = true;

	/**
	 * the group of additional filters to be added to the viewer
	 * @since 3.2
	 */
	private ViewerFilter[] fFilters = null;

	/**
	 * Constructs a launch configuration view for the given launch group
	 */
	public LaunchConfigurationView(LaunchGroupExtension launchGroup) {
		super();
		fLaunchGroup = launchGroup;
	}

	/**
	 * Constructor
	 * @param launchGroup
	 * @param filters
	 */
	public LaunchConfigurationView(LaunchGroupExtension launchGroup, ViewerFilter[] filters) {
		super();
		fLaunchGroup = launchGroup;
		fFilters = filters;
	}

	/**
	 * Returns the launch group this view is displaying.
	 *
	 * @return the launch group this view is displaying
	 */
	protected LaunchGroupExtension getLaunchGroup() {
		return fLaunchGroup;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Viewer createViewer(Composite parent) {
		fTree = new LaunchConfigurationFilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, new PatternFilter(), fLaunchGroup, fFilters);
		fTree.createViewControl();
		getLaunchManager().addLaunchConfigurationListener(this);
		LaunchConfigurationViewer viewer = fTree.getLaunchConfigurationViewer();
		viewer.setLaunchConfigurationView(this);
		return viewer;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> key) {
		if (key == IContextProvider.class) {
			return (T) new IContextProvider() {
				@Override
				public int getContextChangeMask() {
					return SELECTION;
				}

				@Override
				public IContext getContext(Object target) {
					String id = fTree.computeContextId();
					if (id!=null) {
						return HelpSystem.getContext(id);
					}
					return null;
				}

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}
			};
		}
		return super.getAdapter(key);
	}

	/**
	 * Returns the filtering text control from the viewer or <code>null</code>
	 * if the text control was not created.
	 *
	 * @return the filtering text control or <code>null</code>
	 * @since 3.2
	 */
	public Text getFilteringTextControl() {
		return fTree.getFilterControl();
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#createActions()
	 */
	@Override
	protected void createActions() {
		fCreateAction = new CreateLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(CreateLaunchConfigurationAction.ID_CREATE_ACTION, fCreateAction);

		fCreatePrototypeAction = new CreateLaunchConfigurationPrototypeAction(getViewer(), getLaunchGroup().getMode());
		setAction(CreateLaunchConfigurationPrototypeAction.ID_CREATE_PROTOTYPE_ACTION, fCreatePrototypeAction);

		fDeleteAction = new DeleteLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(DeleteLaunchConfigurationAction.ID_DELETE_ACTION, fDeleteAction);
		setAction(IDebugView.REMOVE_ACTION, fDeleteAction);

		fDuplicateAction = new DuplicateLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(DuplicateLaunchConfigurationAction.ID_DUPLICATE_ACTION, fDuplicateAction);

		fExportAction = new ExportLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(ExportLaunchConfigurationAction.ID_EXPORT_ACTION, fExportAction);

		fCollapseAllAction = new CollapseAllLaunchConfigurationAction((TreeViewer)getViewer());
		setAction(CollapseAllLaunchConfigurationAction.ID_COLLAPSEALL_ACTION, fCollapseAllAction);

		fFilterAction = new FilterLaunchConfigurationAction();
		setAction(FilterLaunchConfigurationAction.ID_FILTER_ACTION, fFilterAction);

		fLinkPrototypeAction = new LinkPrototypeAction(getViewer(), getLaunchGroup().getMode());
		setAction(LinkPrototypeAction.ID_LINK_PROTOTYPE_ACTION, fLinkPrototypeAction);

		fUnlinkPrototypeAction = new UnlinkPrototypeAction(getViewer(), getLaunchGroup().getMode());
		setAction(UnlinkPrototypeAction.ID_UNLINK_PROTOTYPE_ACTION, fUnlinkPrototypeAction);

		fResetWithPrototypeValuesAction = new ResetWithPrototypeValuesAction(getViewer(), getLaunchGroup().getMode());
		setAction(ResetWithPrototypeValuesAction.ID_RESET_WITH_PROTOTYPE_VALUES_ACTION, fResetWithPrototypeValuesAction);
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#getHelpContextId()
	 */
	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.LAUNCH_CONFIGURATION_VIEW;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void fillContextMenu(IMenuManager menu) {
		menu.add(fCreateAction);
		menu.add(fCreatePrototypeAction);
		menu.add(fExportAction);
		menu.add(fDuplicateAction);
		menu.add(fDeleteAction);
		menu.add(new Separator());
		menu.add(fLinkPrototypeAction);
		menu.add(fUnlinkPrototypeAction);
		menu.add(fResetWithPrototypeValuesAction);
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#configureToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	protected void configureToolBar(IToolBarManager tbm) {}

	/**
	 * Returns this view's tree viewer
	 *
	 * @return this view's tree viewer
	 */
	protected TreeViewer getTreeViewer() {
		return fTree.getLaunchConfigurationViewer();
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		fCreateAction.dispose();
		fCreatePrototypeAction.dispose();
		fDeleteAction.dispose();
		fDuplicateAction.dispose();
		fExportAction.dispose();
		fFilterAction = null;
		fCollapseAllAction = null;
		fLinkPrototypeAction.dispose();
		fUnlinkPrototypeAction.dispose();
		fResetWithPrototypeValuesAction.dispose();
		getLaunchManager().removeLaunchConfigurationListener(this);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationAdded(final ILaunchConfiguration configuration) {
		if(isSupportedConfiguration(configuration)) {
			//due to notification and async messages we need to collect the moved from config
			//now, else it is null'd out before the following async job runs
			//@see bug 211235 - making local config shared creates "non-existant dup" in LCD
			final ILaunchConfiguration from  = getLaunchManager().getMovedFrom(configuration);
			// handle asynchronously: @see bug 198428 - Deadlock deleting launch configuration
			Display display = DebugUIPlugin.getStandardDisplay();
			display.asyncExec(() -> {
				if (!fTree.isDisposed()) {
					handleConfigurationAdded(configuration, from);
				}
			});
		}
	}

	/**
	 * The given launch configuration has been added. Add it to the tree.
	 * @param configuration the added configuration
	 */
	private void handleConfigurationAdded(ILaunchConfiguration configuration, ILaunchConfiguration from) {
		TreeViewer viewer = getTreeViewer();
		if (viewer != null) {
			try {
				viewer.getControl().setRedraw(false);
				Object parentElement = configuration.getPrototype();
				if (parentElement == null) {
					parentElement = configuration.getType();
				}
				viewer.add(parentElement, configuration);
				// if moved, remove original now
				if (from != null) {
					viewer.remove(from);
				}
				if (isAutoSelect()) {
					viewer.setSelection(new StructuredSelection(configuration), true);
				}
				updateFilterLabel();
				// Need to refresh here, bug 559758
				if (!configuration.isLocal()) {
					viewer.refresh(parentElement);
				}
			}
			catch (CoreException e) {}
			finally {
				viewer.getControl().setRedraw(true);
			}
		}
	}

	/**
	 * Returns if the specified configuration is supported by this instance of the view.
	 * Supported means that:
	 * <ul>
	 * <li>The configuration is not private</li>
	 * <li>AND that the configurations' type supports the mode of the current launch group</li>
	 * <li>AND that the category of the configurations' type matches that of the current launch group</li>
	 * </ul>
	 * @param configuration the configuration
	 * @return true if the configuration is supported by this instance of the view, false otherwise
	 *
	 * @since 3.4
	 */
	protected boolean isSupportedConfiguration(ILaunchConfiguration configuration) {
		try {
			ILaunchConfigurationType type = configuration.getType();
			return !configuration.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false) &&
					type.supportsMode(getLaunchGroup().getMode()) &&
					equalCategories(type.getCategory(), getLaunchGroup().getCategory());
		}
		catch(CoreException ce) {
			DebugUIPlugin.log(ce);
		}
		return false;
	}

	/**
	 * Returns whether the given categories are equal.
	 *
	 * @param c1 category identifier or <code>null</code>
	 * @param c2 category identifier or <code>null</code>
	 * @return boolean
	 *
	 * @since 3.4
	 */
	private boolean equalCategories(String c1, String c2) {
		if (c1 == null || c2 == null) {
			return c1 == c2;
		}
		return c1.equals(c2);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void launchConfigurationRemoved(final ILaunchConfiguration configuration) {
		// if moved, ignore
		ILaunchConfiguration to = getLaunchManager().getMovedTo(configuration);
		if (to != null) {
			return;
		}
		// handle asynchronously: @see bug 198428 - Deadlock deleting launch configuration
		Display display = DebugUIPlugin.getStandardDisplay();
		display.asyncExec(() -> {
			if (!fTree.isDisposed()) {
				handleConfigurationRemoved(configuration);
			}
		});
	}

	/**
	 * The given launch configuration has been removed. Remove it from the tree.
	 * @param configuration the deleted configuration
	 */
	private void handleConfigurationRemoved(ILaunchConfiguration configuration) {
		getTreeViewer().remove(configuration);
		updateFilterLabel();
	}

	/**
	 * This is similar to IWorkbenchPart#createPartControl(Composite), but it is
	 * called by the launch dialog when creating the launch config tree view.
	 * Since this view is not contained in the workbench, we cannot do all the
	 * usual initialization (toolbars, etc).
	 */
	public void createLaunchDialogControl(Composite parent) {
		createViewer(parent);
		createActions();
		createContextMenu(getViewer().getControl());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, getHelpContextId());
		getViewer().getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
			}
		});
		if (getViewer() instanceof StructuredViewer) {
			((StructuredViewer)getViewer()).addDoubleClickListener(this);
		}
		fFilteredNotice = SWTFactory.createLabel(parent, IInternalDebugCoreConstants.EMPTY_STRING, 1, true);
		fFilteredNotice.setBackground(parent.getBackground());
	}

	/**
	 * @see org.eclipse.debug.ui.IDebugView#getViewer()
	 */
	@Override
	public Viewer getViewer() {
		return fTree.getLaunchConfigurationViewer();
	}

	/**
	 * Updates the filter notification label
	 * @since 3.3
	 */
	public void updateFilterLabel() {
		LaunchConfigurationViewer viewer = (LaunchConfigurationViewer) getViewer();
		fFilteredNotice.setText(MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationView_0, new Object[] {
				Integer.toString(viewer.getNonFilteredChildCount()),
				Integer.toString(viewer.getTotalChildCount()) }));
	}

	/**
	 * returns the launch manager
	 * @return
	 */
	protected ILaunchManager getLaunchManager() {
		return fLaunchManager;
	}

	/**
	 * Sets whether to automatically select configs that are
	 * added into the view (newly created).
	 *
	 * @param select whether to automatically select configs that are
	 * added into the view (newly created)
	 */
	public void setAutoSelect(boolean select) {
		fAutoSelect = select;
	}

	/**
	 * Returns whether this view is currently configured to
	 * automatically select newly created configs that are
	 * added into the view.
	 *
	 * @return whether this view is currently configured to
	 * automatically select newly created configs
	 */
	protected boolean isAutoSelect() {
		return fAutoSelect;
	}
}
