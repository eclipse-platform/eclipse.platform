/****************************************************************************
* Copyright (c) 2017, 2018 Red Hat Inc. and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Mickael Istria (Red Hat Inc.) - [521958] initial implementation
*******************************************************************************/
package org.eclipse.debug.internal.ui.hover;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.details.DefaultDetailPane;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Creates an information control to display an expression in a hover control.
 * This was mostly copied for JDT
 * org.eclipse.jdt.internal.debug.ui.ExpressionInformationControlCreator
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 3.13
 */
public class ExpressionInformationControlCreator implements IInformationControlCreator {

	class ExpressionInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

		/**
		 * Dialog setting key for height
		 */
		private static final String HEIGHT = "HEIGHT"; //$NON-NLS-1$

		/**
		 * Dialog setting key for width.
		 */
		private static final String WIDTH = "WIDTH"; //$NON-NLS-1$

		/**
		 * Dialog setting key for tree sash weight
		 */
		private static final String SASH_WEIGHT_TREE = "SashWeightTree"; //$NON-NLS-1$

		/**
		 * Dialog setting key for details sash weight
		 */
		private static final String SASH_WEIGHT_DETAILS = "SashWeightDetails"; //$NON-NLS-1$

		/**
		 * Variable to display.
		 */
		private IVariable fVariable;

		private IPresentationContext fContext;
		private TreeModelViewer fViewer;
		private SashForm fSashForm;
		private Composite fDetailPaneComposite;
		private DetailPaneProxy fDetailPane;
		private Tree fTree;

		/**
		 * Creates the content for the root element of the tree viewer in the hover
		 */
		private class TreeRoot extends ElementContentProvider {
			@Override
			protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
				return 1;
			}

			@Override
			protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
				return new Object[] { fVariable };
			}

			@Override
			protected boolean supportsContextId(String id) {
				return true;
			}
		}

		/**
		 * Inner class implementing IDetailPaneContainer methods.  Handles changes to detail
		 * pane and provides limited access to the detail pane proxy.
		 */
		private class DetailPaneContainer implements IDetailPaneContainer{

			@Override
			public String getCurrentPaneID() {
				return fDetailPane.getCurrentPaneID();
			}

			@Override
			public IStructuredSelection getCurrentSelection() {
				return fViewer.getStructuredSelection();
			}

			@Override
			public void refreshDetailPaneContents() {
				fDetailPane.display(getCurrentSelection());
			}

			@Override
			public Composite getParentComposite() {
				return fDetailPaneComposite;
			}

			@Override
			public IWorkbenchPartSite getWorkbenchPartSite() {
				return null;
			}

			@Override
			public void paneChanged(String newPaneID) {
				if (newPaneID.equals(DefaultDetailPane.ID)){
					fDetailPane.getCurrentControl().setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					fDetailPane.getCurrentControl().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
				}
			}

		}

		/**
		 * Constructs a new control in the given shell.
		 *
		 * @param parentShell shell
		 * @param resize whether resize is supported
		 */
		ExpressionInformationControl(Shell parentShell, boolean resize) {
			super(parentShell, resize);
			create();
		}

		@Override
		public Point computeSizeHint() {
			IDialogSettings settings = getDialogSettings(false);
			if (settings != null) {
				int x = getIntSetting(settings, WIDTH);
				if (x > 0) {
					int y = getIntSetting(settings, HEIGHT);
					if (y > 0) {
						return new Point(x,y);
					}
				}
			}
			return super.computeSizeHint();
		}

		/**
		 * Returns the dialog settings for this hover or <code>null</code> if none
		 *
		 * @param create whether to create the settings
		 */
		private IDialogSettings getDialogSettings(boolean create) {
			IDialogSettings settings = PlatformUI
					.getDialogSettingsProvider(FrameworkUtil.getBundle(ExpressionInformationControlCreator.class))
					.getDialogSettings();
			IDialogSettings section = settings.getSection(this.getClass().getName());
			if (section == null && create) {
				section = settings.addNewSection(this.getClass().getName());
			}
			return section;
		}

		/**
		 * Returns an integer value in the given dialog settings or -1 if none.
		 *
		 * @param settings dialog settings
		 * @param key key
		 * @return value or -1 if not present
		 */
		private int getIntSetting(IDialogSettings settings, String key) {
			try {
				return settings.getInt(key);
			} catch (NumberFormatException e) {
				return -1;
			}
		}

		@Override
		public void dispose() {
			persistSettings(getShell());
			fContext.dispose();
			super.dispose();
		}

		/**
		 * Persists dialog settings.
		 *
		 * @param shell
		 */
		private void persistSettings(Shell shell) {
			if (shell != null && !shell.isDisposed()) {
				if (isResizable()) {
					IDialogSettings settings = getDialogSettings(true);
					Point size = shell.getSize();
					settings.put(WIDTH, size.x);
					settings.put(HEIGHT, size.y);
					int[] weights = fSashForm.getWeights();
					settings.put(SASH_WEIGHT_TREE, weights[0]);
					settings.put(SASH_WEIGHT_DETAILS, weights[1]);
				}
			}
		}

		@Override
		public void setVisible(boolean visible) {
			if (!visible) {
				persistSettings(getShell());
			}
			super.setVisible(visible);
		}

		@Override
		protected void createContent(Composite parent) {

			fSashForm = new SashForm(parent, parent.getStyle());
			fSashForm.setOrientation(SWT.VERTICAL);

			// update presentation context
			AbstractDebugView view = getViewToEmulate();
			fContext = new PresentationContext(IDebugUIConstants.ID_VARIABLE_VIEW);
			if (view != null) {
				// copy over properties
				IPresentationContext copy = ((TreeModelViewer)view.getViewer()).getPresentationContext();
				for (String key : copy.getProperties()) {
					fContext.setProperty(key, copy.getProperty(key));
				}
			}

			fViewer = new TreeModelViewer(fSashForm, SWT.NO_TRIM | SWT.MULTI | SWT.VIRTUAL, fContext);
			fViewer.setAutoExpandLevel(1);

			if (view != null) {
				// copy over filters
				StructuredViewer structuredViewer = (StructuredViewer) view.getViewer();
				if (structuredViewer != null) {
					for (ViewerFilter filter : structuredViewer.getFilters()) {
						fViewer.addFilter(filter);
					}
				}
			}

			fDetailPaneComposite = SWTFactory.createComposite(fSashForm, 1, 1, GridData.FILL_BOTH);
			Layout layout = fDetailPaneComposite.getLayout();
			if (layout instanceof GridLayout) {
				GridLayout gl = (GridLayout) layout;
				gl.marginHeight = 0;
				gl.marginWidth = 0;
			}

			fDetailPane = new DetailPaneProxy(new DetailPaneContainer());
			fDetailPane.display(null); // Bring up the default pane so the user doesn't see an empty composite

			fTree = fViewer.getTree();
			fTree.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fDetailPane.display(fViewer.getStructuredSelection());
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});

			initSashWeights();

			// add update listener to auto-select and display details of root expression
			fViewer.addViewerUpdateListener(new IViewerUpdateListener() {
				@Override
				public void viewerUpdatesComplete() {
				}
				@Override
				public void viewerUpdatesBegin() {
				}
				@Override
				public void updateStarted(IViewerUpdate update) {
				}
				@Override
				public void updateComplete(IViewerUpdate update) {
					if (update instanceof IChildrenUpdate) {
						TreeSelection selection = new TreeSelection(new TreePath(new Object[]{fVariable}));
						fViewer.setSelection(selection);
						fDetailPane.display(selection);
						fViewer.removeViewerUpdateListener(this);
					}
				}
			});

			setForegroundColor(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			setBackgroundColor(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		}


		/**
		 * Attempts to find an appropriate view to emulate, this will either be the
		 * variables view or the expressions view.
		 * @return a view to emulate or <code>null</code>
		 */
		private AbstractDebugView getViewToEmulate() {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			AbstractDebugView expressionsView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
			if (expressionsView != null && expressionsView.isVisible()) {
				return expressionsView;
			}
			AbstractDebugView variablesView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
			if (variablesView != null && variablesView.isVisible()) {
				return variablesView;
			}
			if (expressionsView != null) {
				return expressionsView;
			}
			return variablesView;
		}

		/**
		 * Initializes the sash form weights from the preference store (using default values if
		 * no sash weights were stored previously).
		 */
		protected void initSashWeights(){
			IDialogSettings settings = getDialogSettings(false);
			if (settings != null) {
				int tree = getIntSetting(settings, SASH_WEIGHT_TREE);
				if (tree > 0) {
					int details = getIntSetting(settings, SASH_WEIGHT_DETAILS);
					if (details > 0) {
						fSashForm.setWeights(new int[]{tree, details});
					}
				}
			}
		}

		@Override
		public void setForegroundColor(Color foreground) {
			super.setForegroundColor(foreground);
			fDetailPaneComposite.setForeground(foreground);
			fTree.setForeground(foreground);
		}

		@Override
		public void setBackgroundColor(Color background) {
			super.setBackgroundColor(background);
			fDetailPaneComposite.setBackground(background);
			fTree.setBackground(background);
		}

		@Override
		public void setFocus() {
			super.setFocus();
			fTree.setFocus();
		}

		@Override
		public boolean hasContents() {
			return fVariable != null;
		}

		@Override
		public void setInput(Object input) {
			if (input instanceof IVariable) {
				fVariable = (IVariable) input;
				fViewer.setInput(new TreeRoot());
			}
		}

		@Override
		public IInformationControlCreator getInformationPresenterControlCreator() {
			return new ExpressionInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell shell) {
					return new ExpressionInformationControl(shell, true);
				}
			};
		}
	}

	@Override
	public IInformationControl createInformationControl(Shell parent) {
		return new ExpressionInformationControl(parent, false);
	}


}
