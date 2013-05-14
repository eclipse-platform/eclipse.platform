/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.win32;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class WordMergeViewer extends AbstractMergeViewer implements IFlushable, IPropertyChangeNotifier  {
	
	private static final String RESOURCE_BUNDLE_NAME = "org.eclipse.compare.internal.win32.WordMergeViewer"; //$NON-NLS-1$
	
	private FormToolkit formToolkit;
	private PageBook composite;
	private Composite docArea;
	private Composite textArea;
	private Label description;
	private WordComparison wordArea;
	private boolean isDirty;
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
	private Action saveAction;
	private ResourceBundle resourceBundle;
	private Action inplaceAction;
	private long resultFileTimestamp = -1;

	public WordMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(configuration);
		createContentArea(parent);
		getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				handleDispose();
			}
		});
		getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, CompareWin32Messages.WordMergeViewer_1);
		IToolBarManager toolBarManager = CompareViewerPane.getToolBarManager(parent);
		if (toolBarManager != null) {
			toolBarManager.removeAll();
			initializeToolbar(toolBarManager);
		}
		updateEnablements();
	}

	private void createContentArea(Composite parent) {
		formToolkit = new FormToolkit(parent.getDisplay());
		composite = new PageBook(parent, SWT.NONE);
		createDocArea(composite);
		createTextArea(composite);
	}

	private void initializeToolbar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Separator("modes"));	//$NON-NLS-1$
		toolBarManager.add(new Separator("file"));	//$NON-NLS-1$
		CompareConfiguration configuration = getConfiguration();
		// For now, only support saving if one side is editable
		if (configuration.isRightEditable() || configuration.isLeftEditable() 
				&& (configuration.isRightEditable() != configuration.isLeftEditable())) {
			saveAction = new Action() {
				public void run() {
					saveDocument();
				}
			};
			initAction(saveAction, getResourceBundle(), "action.save."); //$NON-NLS-1$
			toolBarManager.appendToGroup("file", saveAction); //$NON-NLS-1$
		}
		
		inplaceAction = new Action(CompareWin32Messages.WordMergeViewer_2, Action.AS_CHECK_BOX) {
			public void run() {
				toggleInplaceExternalState();
			}
		};
		initAction(inplaceAction, getResourceBundle(), "action.inplace."); //$NON-NLS-1$
		toolBarManager.appendToGroup("modes", inplaceAction); //$NON-NLS-1$
		
		toolBarManager.update(true);
	}
	
	/*
	 * Initialize the given Action from a ResourceBundle.
	 */
	private static void initAction(IAction a, ResourceBundle bundle, String prefix) {
		
		String labelKey= "label"; //$NON-NLS-1$
		String tooltipKey= "tooltip"; //$NON-NLS-1$
		String imageKey= "image"; //$NON-NLS-1$
		String descriptionKey= "description"; //$NON-NLS-1$
		
		if (prefix != null && prefix.length() > 0) {
			labelKey= prefix + labelKey;
			tooltipKey= prefix + tooltipKey;
			imageKey= prefix + imageKey;
			descriptionKey= prefix + descriptionKey;
		}
		
		a.setText(getString(bundle, labelKey, labelKey));
		a.setToolTipText(getString(bundle, tooltipKey, null));
		a.setDescription(getString(bundle, descriptionKey, null));
		
		String relPath= getString(bundle, imageKey, null);
		if (relPath != null && relPath.trim().length() > 0) {
			
			String dPath;
			String ePath;
			
			if (relPath.indexOf("/") >= 0) { //$NON-NLS-1$
				String path= relPath.substring(1);
				dPath= 'd' + path;
				ePath= 'e' + path;
			} else {
				dPath= "dlcl16/" + relPath; //$NON-NLS-1$
				ePath= "elcl16/" + relPath; //$NON-NLS-1$
			}
			
			ImageDescriptor id= getImageDescriptor(dPath);	// we set the disabled image first (see PR 1GDDE87)
			if (id != null)
				a.setDisabledImageDescriptor(id);
			id= getImageDescriptor(ePath);
			if (id != null) {
				a.setImageDescriptor(id);
				a.setHoverImageDescriptor(id);
			}
		}
	}
	
	private static ImageDescriptor getImageDescriptor(String relativePath) {
		IPath path= new Path("$nl$/icons/full/").append(relativePath);		
		URL url= FileLocator.find(Activator.getDefault().getBundle(), path, null);
		if (url == null)
			return null;
		return ImageDescriptor.createFromURL(url);
	}
	
	private static String getString(ResourceBundle bundle, String key, String dfltValue) {
		
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// fall through
			}
		}
		return dfltValue;
	}

	private ResourceBundle getResourceBundle() {
		if (resourceBundle == null) {
			resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
		}
		return resourceBundle;
	}

	private Composite createComposite(Composite parent) {
		return formToolkit.createComposite(parent);
	}

	private void updateEnablements() {
		if (saveAction != null)
			saveAction.setEnabled(isDirty());
		inplaceAction.setChecked(wordArea.isInplace());
		inplaceAction.setEnabled(wordArea.isOpen());
	}


	private void createDocArea(PageBook book) {
		docArea = createComposite(book);
		docArea.setLayout(GridLayoutFactory.fillDefaults().create());
		wordArea = new WordComparison(docArea);
		IWorkbenchPart workbenchPart = getConfiguration().getContainer().getWorkbenchPart();
		if (workbenchPart != null) {
			wordArea.initializeWorkbenchMenus(workbenchPart.getSite().getWorkbenchWindow());
		}
		wordArea.getFrame().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		wordArea.getFrame().setBackground(formToolkit.getColors().getBackground());
		updateDirtyFlag();
	}
	
	private void createTextArea(PageBook book) {
		textArea = createComposite(book);
		textArea.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(10, 10, 10, 10).create());
		description = formToolkit.createLabel(textArea, getTextDescription(), SWT.WRAP);
		description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	protected void saveDocument() {
		try {
			File result = getResultFile();
			wordArea.saveAsDocument(result.getAbsolutePath());
			// Forward the saved content to the save target
			IEditableContent saveTarget = getSaveTarget();
			if (saveTarget == null) {
				if (MessageDialog.openQuestion(WordMergeViewer.this.composite.getShell(), "Save to File?", "The compare editor is not saveable. Would you like to save your changes to another file?")) {
					FileDialog dialog = new FileDialog(WordMergeViewer.this.composite.getShell(), SWT.SAVE);
					String filename = dialog.open();
					if (filename != null) {
						wordArea.saveAsDocument(filename);
					}
				}
			} else {
				synchronized (result) {
					if (result.exists()) {
						saveTarget.setContent(asBytes(result));
						resultFileTimestamp = result.lastModified();
					}
				}
			}
			updateEnablements();
		} catch (IOException e) {
			ErrorDialog.openError(WordMergeViewer.this.composite.getShell(), null, null, new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
		} catch (SWTException e) {
			ErrorDialog.openError(WordMergeViewer.this.composite.getShell(), null, null, new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
		}
	}
	
	public void flush(IProgressMonitor monitor) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if (isReallyDirty())
					saveDocument();
			}
		});
	}


	protected void toggleInplaceExternalState() {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				try {
					if (isReallyDirty()) {
						// If the file is dirty, save the result file before switching so our changes are not lost
						try {
							File result = getResultFile();
							wordArea.saveAsDocument(result.getAbsolutePath());
						} catch (IOException e) {
							throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
						}
					}
					openComparison(!wordArea.isInplace());
				} catch (CoreException e) {
					ErrorDialog.openError(WordMergeViewer.this.composite.getShell(), null, null, e.getStatus());
					Activator.log(e);
				}
			}
		});
	}

	private boolean isReallyDirty() {
		return isDirty() || wordArea.isDirty();
	}
	
	private void openComparison(boolean inplace) throws CoreException {
		try {
			if (isOneSided()) {
				File file = getFileForSingleSide();
				if (file != null) {
					try {
						wordArea.openDocument(file.getAbsolutePath(), inplace);
					} catch (SWTException e) {
						throw new CoreException(new Status(IStatus.ERROR,
								Activator.PLUGIN_ID, NLS.bind(
										CompareWin32Messages.WordComparison_16,
										file.getAbsolutePath()), e));
					}
				}
			} else {
				File left = getFileForLeft();
				File right = getFileForRight();
				if (left != null && right != null) {
					File result = getResultFile();
					int direction = getCompareInput().getKind() & Differencer.DIRECTION_MASK;
					File base, revised;
					if (direction == Differencer.RIGHT) {
						base = left;
						revised = right;
					} else {
						base = right;
						revised = left;
					}
					synchronized (result) {	
						if (!result.exists()) {
							wordArea.createWorkingCopy(base.getAbsolutePath(), revised.getAbsolutePath(), result.getAbsolutePath());
							resultFileTimestamp = result.lastModified();
							description.setText(getTextDescription());
						}
						try {
							wordArea.openDocument(result.getAbsolutePath(), inplace);
						} catch (SWTException e) {
							throw new CoreException(new Status(IStatus.ERROR,
									Activator.PLUGIN_ID, NLS.bind(
											CompareWin32Messages.WordComparison_16,
											result.getAbsolutePath()), e));
						}
					}
				}
			}
			if (wordArea.isInplace()) {
				composite.showPage(docArea);
			} else {
				composite.showPage(textArea);
			}
		} catch (SWTException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, e.getMessage(), e));
		}
		updateEnablements();
	}

	private String getTextDescription() {
		if (saveAction != null &&hasResultFile()) {
			IEditableContent saveTarget = getSaveTarget();
			String name = CompareWin32Messages.WordMergeViewer_3;
			if (saveTarget instanceof ITypedElement) {
				ITypedElement te = (ITypedElement) saveTarget;
				name = te.getName();
			}
			try {
				return NLS.bind(CompareWin32Messages.WordMergeViewer_4, getResultFile().getName(), name);
			} catch (IOException e) {
				// This shouldn't happen since the result file has already been created
			}
		}
		return CompareWin32Messages.WordMergeViewer_5;
	}

	public Control getControl() {
		return composite;
	}
	
	public void setInput(Object input) {
		super.setInput(input);
		try {
			openComparison(true);
		} catch (CoreException e) {
			ErrorDialog.openError(WordMergeViewer.this.composite.getShell(), null, null, e.getStatus());
			Activator.log(e);
		}
	}
	
	private void updateDirtyFlag() {
		final Runnable dirtyFlagUpdater = new Runnable() {
			public void run() {
				if (wordArea.getFrame().isDisposed())
					return;
				boolean dirty = wordArea.isDirty();
				if (hasResultFile()) {
					try {
						File resultFile = getResultFile();
						synchronized (resultFile) {
							if (resultFile.exists()) {
								long lastModified = resultFile.lastModified();
								if (lastModified != resultFileTimestamp) {
									dirty = true;
								}
							}
						}
					} catch (IOException e) {
						// Shouldn't happen since we only get the result file if it has already been created
					}
				}
				if (isDirty() != dirty) {
					setDirty(dirty);
				}
				composite.getDisplay().timerExec(1000, this);
			}
		};
		dirtyFlagUpdater.run();
	}

	protected boolean isDirty() {
		return isDirty;
	}

	protected void setDirty(boolean dirty) {
		if (isDirty != dirty) {
			isDirty = dirty;
			updateEnablements();
			firePropertyChange(CompareEditorInput.DIRTY_STATE, Boolean.valueOf(!isDirty), Boolean.valueOf(isDirty));
		}
	}

	private void firePropertyChange(String property, Object oldValue, Object newValue) {
		Object[] allListeners = listeners.getListeners();
		final PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
		for (int i = 0; i < allListeners.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener)allListeners[i];
			SafeRunner.run(new SafeRunnable() {
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			});
		}
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	private void handleDispose() {
		BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
			public void run() {
				wordArea.dispose();
				formToolkit.dispose();
				reset();
			}
		});
	}
	
	protected void reset() {
		if (wordArea.isOpen()) {
			wordArea.close();
		}
		super.reset();
	}
}
