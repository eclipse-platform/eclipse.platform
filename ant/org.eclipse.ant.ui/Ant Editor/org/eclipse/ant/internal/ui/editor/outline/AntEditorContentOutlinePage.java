/*******************************************************************************
 * Copyright (c) 2002, 2005 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49380, bug 34548, bug 53547
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.outline;

import java.util.List;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.actions.TogglePresentationAction;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntImportNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntModelChangeEvent;
import org.eclipse.ant.internal.ui.model.AntModelContentProvider;
import org.eclipse.ant.internal.ui.model.AntModelCore;
import org.eclipse.ant.internal.ui.model.AntModelLabelProvider;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntPropertyNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.ant.internal.ui.model.IAntModelListener;
import org.eclipse.ant.internal.ui.views.actions.AntOpenWithMenu;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * Content outline page for the Ant Editor.
 */
public class AntEditorContentOutlinePage extends ContentOutlinePage implements IShowInSource, IAdaptable {
	
	private static final int EXPAND_TO_LEVEL= 2;

	private Menu fMenu;
	private AntOpenWithMenu fOpenWithMenu;
	
	private IAntModelListener fListener;
	private IAntModel fModel;
	private AntModelCore fCore;
	private ListenerList fPostSelectionChangedListeners= new ListenerList();
	private boolean fIsModelEmpty= true;
	private boolean fFilterInternalTargets;
    private boolean fFilterImportedElements;   
	private boolean fFilterProperties;
	private boolean fFilterTopLevel;
	private boolean fSort;

	private ViewerComparator fComparator;
	
	private AntEditor fEditor;
	
	private TogglePresentationAction fTogglePresentation;
	
	/**
	 * A viewer filter for the Ant Content Outline
	 */
	private class AntOutlineFilter extends ViewerFilter {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof AntElementNode) {
                AntElementNode node = (AntElementNode) element;
                if (fFilterTopLevel && (node instanceof AntTaskNode && parentElement instanceof AntProjectNode)) {
					return false;
				}
			    if (fFilterImportedElements && (node.getImportNode() !=  null || node.isExternal())) {
					if (node instanceof AntTargetNode && ((AntTargetNode)node).isDefaultTarget()) {
						return true;
					}
					return false;
				}
			    if (fFilterInternalTargets && node instanceof AntTargetNode) {
					return !((AntTargetNode)node).isInternal();
				} 
			    if (fFilterProperties && node instanceof AntPropertyNode) {
					return false;
				} 
			    if (!node.isStructuralNode()) {
			        return false;
				} 
			} 
			return true;
		}
	}
	
	private class AntOutlineComparator extends ViewerComparator {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof AntElementNode && e2 instanceof AntElementNode)) {
				return super.compare(viewer, e1, e2);
			}
			String name1= ((AntElementNode) e1).getLabel();
			String name2= ((AntElementNode) e2).getLabel();
			return getComparator().compare(name1, name2);
		}
	}

	/**
	 * Sets whether internal targets should be filtered out of the outline.
	 * 
	 * @param filter whether or not internal targets should be filtered out
	 */
	protected void setFilterInternalTargets(boolean filter) {
		fFilterInternalTargets= filter;
		setFilter(filter, IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS);   
	}
	
	/**
	 * Sets whether imported elements should be filtered out of the outline.
	 * 
	 * @param filter whether or not imported elements should be filtered out
	 */
    protected void setFilterImportedElements(boolean filter) {
		fFilterImportedElements= filter;
		setFilter(filter, IAntUIPreferenceConstants.ANTEDITOR_FILTER_IMPORTED_ELEMENTS);        
    }

	private void setFilter(boolean filter, String name) {
		if (name != null) {
			AntUIPlugin.getDefault().getPreferenceStore().setValue(name, filter);
		}
		//filter has been changed
		getTreeViewer().refresh();
	}

	/**
	 * Sets whether properties should be filtered out of the outline.
	 * 
	 * @param filter whether or not properties should be filtered out
	 */
	protected void setFilterProperties(boolean filter) {
		fFilterProperties= filter;
		setFilter(filter, IAntUIPreferenceConstants.ANTEDITOR_FILTER_PROPERTIES);     
	}
	
	/**
	 * Sets whether internal targets should be filtered out of the outline.
	 * 
	 * @param filter whether or not internal targets should be filtered out
	 */
	protected void setFilterTopLevel(boolean filter) {
		fFilterTopLevel= filter;
		setFilter(filter, IAntUIPreferenceConstants.ANTEDITOR_FILTER_TOP_LEVEL);     
	}
	
	/**
	 * Returns whether internal targets are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not internal targets are being filtered out
	 */
	protected boolean filterInternalTargets() {
		return fFilterInternalTargets;
	}
	
	/**
	 * Returns whether imported elements are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not imported elements are being filtered out
	 */
	protected boolean filterImportedElements() {
		return fFilterImportedElements;
	}

	/**
	 * Returns whether properties are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not properties are being filtered out
	 */
	protected boolean filterProperties() {
		return fFilterProperties;
	}
	
	/**
	 * Returns whether top level tasks/types are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not top level tasks/types are being filtered out
	 */
	protected boolean filterTopLevel() {
		return fFilterTopLevel;
	}
	
	/**
	 * Sets whether elements should be sorted in the outline.
	 *  
	 * @param sort whether or not elements should be sorted
	 */
	protected void setSort(boolean sort) {
		fSort= sort;
		if (sort) {
			if (fComparator == null) {
				fComparator= new AntOutlineComparator();
			}
			getTreeViewer().setComparator(fComparator);
		} else {
			getTreeViewer().setComparator(null);
		}
		AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.ANTEDITOR_SORT, sort);
	}
	
	/**
	 * Returns whether elements are currently being sorted.
	 * 
	 * @return whether elements are currently being sorted
	 */
	protected boolean isSort() {
		return fSort;
	}
	
	/**
	 * Creates a new AntEditorContentOutlinePage.
	 */
	public AntEditorContentOutlinePage(AntModelCore core, AntEditor editor) {
		super();
		fCore= core;
		fFilterInternalTargets= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS);
		fFilterImportedElements= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_IMPORTED_ELEMENTS);
		fFilterProperties= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_PROPERTIES);
		fFilterTopLevel= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_TOP_LEVEL);
		fSort= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_SORT);
		fEditor= editor;
		
		fTogglePresentation= new TogglePresentationAction();
		fTogglePresentation.setEditor(editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#dispose()
	 */
	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
		}
		if (fOpenWithMenu != null) {
			fOpenWithMenu.dispose();
		}
		if (fListener != null) {
			fCore.removeAntModelListener(fListener);
			fListener= null;
		}
		fTogglePresentation.setEditor(null);
		
		super.dispose();
	}
	
	/**  
	 * Creates the control (outline view) for this page
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
    
		TreeViewer viewer = getTreeViewer();
        
		viewer.setContentProvider(new AntModelContentProvider());
		setSort(fSort);

		viewer.setLabelProvider(new AntModelLabelProvider());
		viewer.addFilter(new AntOutlineFilter());
		if (fModel != null) {
			setViewerInput(fModel);
		}
		
		MenuManager manager= new MenuManager("#PopUp"); //$NON-NLS-1$
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuManager) {
				contextMenuAboutToShow(menuManager);
			}
		});
		fMenu= manager.createContextMenu(viewer.getTree());
		viewer.getTree().setMenu(fMenu);

		IPageSite site= getSite();
		site.registerContextMenu(IAntUIConstants.PLUGIN_ID + ".antEditorOutline", manager, viewer); //$NON-NLS-1$
		
		IToolBarManager tbm= site.getActionBars().getToolBarManager();
		tbm.add(new ToggleSortAntOutlineAction(this));
		tbm.add(new FilterInternalTargetsAction(this));
		tbm.add(new FilterPropertiesAction(this));
		tbm.add(new FilterImportedElementsAction(this));
		tbm.add(new FilterTopLevelAction(this));
		
		IMenuManager viewMenu= site.getActionBars().getMenuManager();
		viewMenu.add(new ToggleLinkWithEditorAction(fEditor));
		
		fOpenWithMenu= new AntOpenWithMenu(this.getSite().getPage());
		
		viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				firePostSelectionChanged(event.getSelection());
			}
		});
		
		site.getActionBars().setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY, fTogglePresentation);
	}
	
	private void setViewerInput(Object newInput) {
		TreeViewer tree= getTreeViewer();
		Object oldInput= tree.getInput();
		
		boolean isAntModel= (newInput instanceof AntModel);
		boolean wasAntModel= (oldInput instanceof AntModel);
		
		if (isAntModel && !wasAntModel) {
			if (fListener == null) {
				fListener= createAntModelChangeListener();
			}
			fCore.addAntModelListener(fListener);
		} else if (!isAntModel && wasAntModel && fListener != null) {
			fCore.removeAntModelListener(fListener);
			fListener= null;
		}

		
		tree.setInput(newInput);
		
		if (isAntModel) {
			updateTreeExpansion();
		}
	}
	
	public void setPageInput(AntModel xmlModel) {
		fModel= xmlModel;
		if (getTreeViewer() != null) {
			setViewerInput(fModel);
		}
	}
		
	private IAntModelListener createAntModelChangeListener() {
		return new IAntModelListener() {
			public void antModelChanged(final AntModelChangeEvent event) {
				if (event.getModel() == fModel && !getControl().isDisposed()) {
					getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							Control ctrl= getControl();
							if (ctrl != null && !ctrl.isDisposed()) {
								getTreeViewer().refresh();
								updateTreeExpansion();
							}
						}
					});
				}
			}
		};
	}
	
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.add(listener);
	}
	
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.remove(listener);
	}
	
	private void updateTreeExpansion() {
		boolean wasModelEmpty= fIsModelEmpty;
		fIsModelEmpty= fModel == null || fModel.getProjectNode() == null;
		if (wasModelEmpty && !fIsModelEmpty) {
			getTreeViewer().expandToLevel(EXPAND_TO_LEVEL);
		}
	}
	
	private void firePostSelectionChanged(ISelection selection) {
		// create an event
		SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
 
		// fire the event
		Object[] listeners= fPostSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; ++i) {
			((ISelectionChangedListener) listeners[i]).selectionChanged(event);
		}
	}
	
	private void contextMenuAboutToShow(IMenuManager menuManager) {	
		if (shouldAddOpenWithMenu()) {
			addOpenWithMenu(menuManager);
		}
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void addOpenWithMenu(IMenuManager menuManager) {
		AntElementNode element= getSelectedNode();
		IFile file = null;
		if (element != null) {
			file = element.getIFile();
		}
		if (file != null) {
			menuManager.add(new Separator("group.open")); //$NON-NLS-1$
			IMenuManager submenu= new MenuManager(AntOutlineMessages.AntEditorContentOutlinePage_Open_With_1);
			fOpenWithMenu.setNode(element);
			submenu.add(fOpenWithMenu);
			menuManager.appendToGroup("group.open", submenu); //$NON-NLS-1$
		}
	}
	
	private boolean shouldAddOpenWithMenu() {
		AntElementNode node= getSelectedNode();
		if (node instanceof AntImportNode) {
			return true;
		}
		if (node != null && node.isExternal()) {
			String path = node.getFilePath();
			if (path != null && path.length() > 0) {
				return true;
			}
		}
		return false;
	}

	private AntElementNode getSelectedNode() {
		ISelection iselection= getSelection();
		if (iselection instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)iselection;
			if (selection.size() == 1) {
				Object selected= selection.getFirstElement();
				if (selected instanceof AntElementNode) {
					return (AntElementNode)selected;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInSource.class) {
			return this;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IShowInSource#getShowInContext()
	 */
	public ShowInContext getShowInContext() {
		IFile file= null;
		if (fModel != null) {
			AntElementNode node= getSelectedNode();
			if (node != null) {
				file= node.getIFile();
			}
		}
		if (file != null) {
			ISelection selection= new StructuredSelection(file);
			return new ShowInContext(null, selection);
		} 
		return null;
	}
	
	public void select(AntElementNode node) {
		if (getTreeViewer() != null) {
			ISelection s= getTreeViewer().getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) s;
				List nodes= ss.toList();
				if (!nodes.contains(node)) {
					s= (node == null ? StructuredSelection.EMPTY : new StructuredSelection(node));
					getTreeViewer().setSelection(s, true);
				}
			}
		}
	}
}
