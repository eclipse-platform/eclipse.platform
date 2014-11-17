/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;

/**
 * Handler that serializes the updating of a synchronize model provider.
 * All modifications to the synchronize model are performed in this
 * handler's thread.
 */
public class SynchronizeModelUpdateHandler extends BackgroundEventHandler implements IResourceChangeListener, ISyncInfoSetChangeListener {
    private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();
    
    // Event that indicates that the markers for a set of elements has changed
	private static final int MARKERS_CHANGED = 1;
	private static final int BUSY_STATE_CHANGED = 2;
	private static final int RESET = 3;
	private static final int SYNC_INFO_SET_CHANGED = 4;
	
	private AbstractSynchronizeModelProvider provider;
	
	private Set pendingLabelUpdates = Collections.synchronizedSet(new HashSet());
	
	// Flag to indicate the need for an early dispath in order to show
	// busy for elements involved in an operation
	private boolean dispatchEarly = false;
	
	private static final int EARLY_DISPATCH_INCREMENT = 100;
	
	/**
	 * Custom event for posting marker changes
	 */
	class MarkerChangeEvent extends Event {
        private final ISynchronizeModelElement[] elements;
        public MarkerChangeEvent(ISynchronizeModelElement[] elements) {
            super(MARKERS_CHANGED);
            this.elements = elements;
        }
        public ISynchronizeModelElement[] getElements() {
            return elements;
        }
	}
	
	/**
	 * Custom event for posting busy state changes
	 */
	class BusyStateChangeEvent extends Event {
        
        private final ISynchronizeModelElement element;
        private final boolean isBusy;
        public BusyStateChangeEvent(ISynchronizeModelElement element, boolean isBusy) {
            super(BUSY_STATE_CHANGED);
            this.element = element;
            this.isBusy = isBusy;
        }
        public ISynchronizeModelElement getElement() {
            return element;
        }
        public boolean isBusy() {
            return isBusy;
        }
	}
	
	/**
	 * Custom event for posting sync info set changes
	 */
	class SyncInfoSetChangeEvent extends Event {
        private final ISyncInfoSetChangeEvent event;
        public SyncInfoSetChangeEvent(ISyncInfoSetChangeEvent event) {
            super(SYNC_INFO_SET_CHANGED);
            this.event = event;
        }
        public ISyncInfoSetChangeEvent getEvent() {
            return event;
        }
	}
	
	private IPropertyChangeListener listener = new IPropertyChangeListener() {
		public void propertyChange(final PropertyChangeEvent event) {
			if (event.getProperty() == ISynchronizeModelElement.BUSY_PROPERTY) {
				Object source = event.getSource();
				if (source instanceof ISynchronizeModelElement)
				    updateBusyState((ISynchronizeModelElement)source, ((Boolean)event.getNewValue()).booleanValue());
			}
		}
	};

    private boolean performingBackgroundUpdate;
    
    /*
     * Map used to keep track of additions so they can be added in batch at the end of the update
     */
    private Map additionsMap;
    
	/**
     * Create the marker update handler.
     */
    public SynchronizeModelUpdateHandler(AbstractSynchronizeModelProvider provider) {
        super(TeamUIMessages.SynchronizeModelProvider_0, TeamUIMessages.SynchronizeModelUpdateHandler_0); // 
        this.provider = provider;
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        provider.getSyncInfoSet().addSyncSetChangedListener(this);
    }
	
    /**
     * Return the marker types that are of interest to this handler.
     * @return the marker types that are of interest to this handler
     */
    protected String[] getMarkerTypes() {
		return new String[] {IMarker.PROBLEM};
	}
    
	/**
	 * Return the <code>AbstractTreeViewer</code> associated with this
	 * provider or <code>null</code> if the viewer is not of the proper type.
	 * @return the structured viewer that is displaying the model managed by this provider
	 */
	public StructuredViewer getViewer() {
		return provider.getViewer();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
			String[] markerTypes = getMarkerTypes();
			Set handledResources = new HashSet();
			Set changes = new HashSet();
			
			// Accumulate all distinct resources that have had problem marker
			// changes
			for (int idx = 0; idx < markerTypes.length; idx++) {
				IMarkerDelta[] markerDeltas = event.findMarkerDeltas(markerTypes[idx], true);
					for (int i = 0; i < markerDeltas.length; i++) {
						IMarkerDelta delta = markerDeltas[i];
						IResource resource = delta.getResource();
						if (!handledResources.contains(resource)) {
						    handledResources.add(resource);
						    ISynchronizeModelElement[] elements = provider.getClosestExistingParents(delta.getResource());
							if(elements != null && elements.length > 0) {
							    for (int j = 0; j < elements.length; j++) {
                                    ISynchronizeModelElement element = elements[j];
                                    changes.add(element);
                                }
							}
						}
					}
				}
			
			if (!changes.isEmpty()) {
			    updateMarkersFor((ISynchronizeModelElement[]) changes.toArray(new ISynchronizeModelElement[changes.size()]));
		}
	}
	
    private void updateMarkersFor(ISynchronizeModelElement[] elements) {
        queueEvent(new MarkerChangeEvent(elements), false /* not on front of queue */);
    }
    
    protected void updateBusyState(ISynchronizeModelElement element, boolean isBusy) {
        queueEvent(new BusyStateChangeEvent(element, isBusy), false /* not on front of queue */);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#processEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
        switch (event.getType()) {
		case BackgroundEventHandler.RUNNABLE_EVENT :
			executeRunnable(event, monitor);
			break;
        case MARKERS_CHANGED:
			// Changes contains all elements that need their labels updated
			long start = System.currentTimeMillis();
			ISynchronizeModelElement[] elements = getChangedElements(event);
			for (int i = 0; i < elements.length; i++) {
				ISynchronizeModelElement element = elements[i];
				propagateProblemMarkers(element);
				updateParentLabels(element);
			}
			if (Policy.DEBUG_SYNC_MODELS) {
				long time = System.currentTimeMillis() - start;
				DateFormat TIME_FORMAT = new SimpleDateFormat("m:ss.SSS"); //$NON-NLS-1$
				String took = TIME_FORMAT.format(new Date(time));
				System.out.println(took + " for " + elements.length + " files"); //$NON-NLS-1$//$NON-NLS-2$
			}
            break;
        case BUSY_STATE_CHANGED:
            BusyStateChangeEvent e = (BusyStateChangeEvent)event;
            queueForLabelUpdate(e.getElement());
            if (e.isBusy()) {
                // indicate that we want an early dispatch to show busy elements
                dispatchEarly = true;
            }
            break;
        case RESET:
            // Perform the reset immediately
            pendingLabelUpdates.clear();
            provider.reset();
            break;
        case SYNC_INFO_SET_CHANGED:
            // Handle the sync change immediately
            handleChanges(((SyncInfoSetChangeEvent)event).getEvent(), monitor);
        default:
            break;
        }
    }

    private ISynchronizeModelElement[] getChangedElements(Event event) {
        if (event.getType() == MARKERS_CHANGED) {
            return ((MarkerChangeEvent)event).getElements();
        }
        return new ISynchronizeModelElement[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#doDispatchEvents(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
		// Fire label changed
        dispatchEarly = false;
        if (pendingLabelUpdates.isEmpty()) {
            return false;
        } else {
			Utils.asyncExec(new Runnable() {
				public void run() {
					firePendingLabelUpdates();
				}
			}, getViewer());
			return true;
        }
    }
    
	/**
	 * Forces the viewer to update the labels for queued elemens
	 * whose label has changed during this round of changes. This method
	 * should only be invoked in the UI thread.
	 */
	protected void firePendingLabelUpdates() {
		if (!Utils.canUpdateViewer(getViewer())) return;
		try {
			Object[] updates = pendingLabelUpdates.toArray(new Object[pendingLabelUpdates.size()]);
			updateLabels(updates);
		} finally {
			pendingLabelUpdates.clear();
		}
	}
	
	/*
	 * Forces the viewer to update the labels for the given elements
	 */
	private void updateLabels(Object[] elements) {
	    StructuredViewer tree = getViewer();
		if (Utils.canUpdateViewer(tree)) {	
			tree.update(elements, null);
		}
	}
	
	/**
	 * Queue all the parent elements for a label update.
	 * @param element the element whose label and parent labels need to be updated
	 */
	public void updateParentLabels(ISynchronizeModelElement element) {
		queueForLabelUpdate(element);
		while (element.getParent() != null) {
			element = (ISynchronizeModelElement)element.getParent();
			queueForLabelUpdate(element);
		}
	}
	
	/**
	 * Update the label of the given diff node. Diff nodes
	 * are accumulated and updated in a single call.
	 * @param diffNode the diff node to be updated
	 */
	protected void queueForLabelUpdate(ISynchronizeModelElement diffNode) {
		pendingLabelUpdates.add(diffNode);
	}
	
	/**
	 * Calculate and propagate problem markers in the element model
	 * @param element the ssynchronize element
	 */
	private void propagateProblemMarkers(ISynchronizeModelElement element) {
		IResource resource = element.getResource();
		if (resource != null) {
			String property = provider.calculateProblemMarker(element);
			// If it doesn't have a direct change, a parent might
			boolean recalculateParentDecorations = hadProblemProperty(element, property);
			if (recalculateParentDecorations) {
				ISynchronizeModelElement parent = (ISynchronizeModelElement) element.getParent();
				if (parent != null) {
					propagateProblemMarkers(parent);
				}
			}
		}
	}
	
	// none -> error
	// error -> none
	// none -> warning
	// warning -> none
	// warning -> error
	// error -> warning
	private boolean hadProblemProperty(ISynchronizeModelElement element, String property) {
		boolean hadError = element.getProperty(ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY);
		boolean hadWarning = element.getProperty(ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY);
		
		// Force recalculation of parents of phantom resources
		IResource resource = element.getResource();
		if(resource != null && resource.isPhantom()) {
			return true;
		}
		
		if(hadError) {
			if(! (property == ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY)) {
				element.setPropertyToRoot(ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY, false);
				if(property != null) {
					// error -> warning
					element.setPropertyToRoot(property, true);
				}
				// error -> none
				// recalculate parents
				return true;
			}	
			return false;
		} else if(hadWarning) {
			if(! (property == ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY)) {
				element.setPropertyToRoot(ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY, false);
				if(property != null) {
					// warning -> error
					element.setPropertyToRoot(property, true);
					return false;
				}
				// warning ->  none
				return true;
			}	
			return false;		
		} else {
			if(property == ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY) {
				// none -> error
				element.setPropertyToRoot(property, true);
				return false;
			} else if(property == ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY) {
				// none -> warning
				element.setPropertyToRoot(property, true);
				return true;
			}	
			return false;
		}
	}

	/*
	 * Queue an event that will reset the provider
	 */
    private void reset() {
        queueEvent(new ResourceEvent(ROOT, RESET, IResource.DEPTH_INFINITE), false);
    }
    
    public void dispose() {
        shutdown();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        provider.getSyncInfoSet().removeSyncSetChangedListener(this);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.BackgroundEventHandler#getShortDispatchDelay()
     */
    protected long getShortDispatchDelay() {
        if (dispatchEarly) {
            dispatchEarly = false;
            return EARLY_DISPATCH_INCREMENT;
        }
        return super.getShortDispatchDelay();
    }

    /**
     * This method is invoked whenever a node is added to the viewer
     * by the provider or a sub-provider. The handler adds an update
     * listener to the node and notifies the root provider that 
     * a node was added.
     * @param element the added element
     * @param provider the provider that added the element
     */
    public void nodeAdded(ISynchronizeModelElement element, AbstractSynchronizeModelProvider provider) {
        element.addPropertyChangeListener(listener);
        this.provider.nodeAdded(element, provider);
        if (Policy.DEBUG_SYNC_MODELS) {
            System.out.println("Node added: " + getDebugDisplayLabel(element) + " -> " + getDebugDisplayLabel((ISynchronizeModelElement)element.getParent()) + " : " + getDebugDisplayLabel(provider)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    /**
     * This method is invoked whenever a node is removed the viewer
     * by the provider or a sub-provider. The handler removes any
     * listener and notifies the root provider that 
     * a node was removed. The node removed may have children for which 
     * a nodeRemoved callback was not invoked (see modelObjectCleared).
     * @param element the removed element
     * @param provider the provider that added the element
     */
    public void nodeRemoved(ISynchronizeModelElement element, AbstractSynchronizeModelProvider provider) {
        element.removePropertyChangeListener(listener);
        this.provider.nodeRemoved(element, provider);
        if (Policy.DEBUG_SYNC_MODELS) {
            System.out.println("Node removed: " + getDebugDisplayLabel(element) + " -> " + getDebugDisplayLabel((ISynchronizeModelElement)element.getParent()) + " : " + getDebugDisplayLabel(provider)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    /**
     * This method is invoked whenever a model object (i.e. node)
     * is cleared from the model. This is similar to node removal but
     * is deep.
     * @param node the node that was cleared
     */
    public void modelObjectCleared(ISynchronizeModelElement node) {
        node.removePropertyChangeListener(listener);
        this.provider.modelObjectCleared(node);
        if (Policy.DEBUG_SYNC_MODELS) {
            System.out.println("Node cleared: " + getDebugDisplayLabel(node)); //$NON-NLS-1$
        }
    }
    
    private String getDebugDisplayLabel(ISynchronizeModelElement node) {
        if (node == null) {
            return "ROOT"; //$NON-NLS-1$
        }
        if (node.getResource() != null) {
            return node.getResource().getFullPath().toString();
        }
        return node.getName();
    }

    private String getDebugDisplayLabel(AbstractSynchronizeModelProvider provider2) {
        return provider2.toString();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		if(provider.isDisposed()) {
			set.removeSyncSetChangedListener(this);
		} else {
		    reset();
		}
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoChanged(final ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
		if (! (event instanceof ISyncInfoTreeChangeEvent)) {
			reset();
		} else {
			queueEvent(new SyncInfoSetChangeEvent(event), false);
		}
    }

    /*
     * Handle the sync info set change event in the UI thread.
     */
    private void handleChanges(final ISyncInfoSetChangeEvent event, final IProgressMonitor monitor) {
        runViewUpdate(new Runnable() {
            public void run() {
				provider.handleChanges((ISyncInfoTreeChangeEvent)event, monitor);
				firePendingLabelUpdates();
            }
        }, true /* preserve expansion */);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		// When errors occur we currently don't process them. It may be possible to decorate
		// elements in the model with errors, but currently we prefer to let ignore and except
		// another listener to display them. 
    }
    
    public ISynchronizeModelProvider getProvider() {
        return provider;
    }

    public void connect(IProgressMonitor monitor) {
        getProvider().getSyncInfoSet().connect(this, monitor);
    }
    
    public void runViewUpdate(final Runnable runnable, final boolean preserveExpansion) {
        if (Utils.canUpdateViewer(getViewer()) || isPerformingBackgroundUpdate()) {
            internalRunViewUpdate(runnable, preserveExpansion);
        } else {
            if (Thread.currentThread() != getEventHandlerJob().getThread()) {
                // Run view update should only be called from the UI thread or
                // the update handler thread. 
                // We will log the problem for now and make it an assert later
                TeamUIPlugin.log(IStatus.WARNING, "View update invoked from invalid thread", new TeamException("View update invoked from invalid thread")); //$NON-NLS-1$ //$NON-NLS-2$
            }
	        final Control ctrl = getViewer().getControl();
	        if (ctrl != null && !ctrl.isDisposed()) {
	        	ctrl.getDisplay().syncExec(new Runnable() {
	        		public void run() {
	        			if (!ctrl.isDisposed()) {
	        				BusyIndicator.showWhile(ctrl.getDisplay(), new Runnable() {
	        					public void run() {
	    						    internalRunViewUpdate(runnable, preserveExpansion);
	        					}
	        				});
	        			}
	        		}
	        	});
	        }
        }
    }
    
    /*
     * Return whether the event handler is performing a background view update.
     * In other words, a client has invoked <code>performUpdate</code>.
     */
    public boolean isPerformingBackgroundUpdate() {
        return Thread.currentThread() == getEventHandlerJob().getThread() && performingBackgroundUpdate;
    }

    /*
     * Method that can be called from the UI thread to update the view model.
     */
    private void internalRunViewUpdate(final Runnable runnable, boolean preserveExpansion) {
        StructuredViewer viewer = getViewer();
        IResource[] expanded = null;
        IResource[] selected = null;
		try {
		    if (Utils.canUpdateViewer(viewer)) {
		        viewer.getControl().setRedraw(false);
		        if (preserveExpansion) {
		            expanded = provider.getExpandedResources();
		            selected = provider.getSelectedResources();
		        }
                if (viewer instanceof AbstractTreeViewer && additionsMap == null)
                    additionsMap = new HashMap();
		    }
			runnable.run();
		} finally {
		    if (Utils.canUpdateViewer(viewer)) {
                try {
                    if (additionsMap != null && !additionsMap.isEmpty() && Utils.canUpdateViewer(viewer)) {
                        for (Iterator iter = additionsMap.keySet().iterator(); iter.hasNext();) {
                            ISynchronizeModelElement parent = (ISynchronizeModelElement) iter.next();
                            if (Policy.DEBUG_SYNC_MODELS) {
                                System.out.println("Adding child view items of " + parent.getName()); //$NON-NLS-1$
                            }
                            Set toAdd = (Set)additionsMap.get(parent);
                            ((AbstractTreeViewer)viewer).add(parent, toAdd.toArray(new Object[toAdd.size()]));
                        }
                        additionsMap = null;
                    }
    		        if (expanded != null) {
    		            provider.expandResources(expanded);
    		        }
    		        if (selected != null) {
    		            provider.selectResources(selected);
    		        }
                } finally {
                    viewer.getControl().setRedraw(true);
                }
		    }
		}

		ISynchronizeModelElement root = provider.getModelRoot();
		if(root instanceof SynchronizeModelElement)
			((SynchronizeModelElement)root).fireChanges();
    }

    /**
     * Execute a runnable which performs an update of the model being displayed
     * by the handler's provider. The runnable should be executed in a thread-safe manner
     * which esults in the view being updated.
     * @param runnable the runnable which updates the model.
     * @param preserveExpansion whether the expansion of the view should be preserver
     * @param updateInUIThread if <code>true</code>, the model will be updated in the
     * UI thread. Otherwise, the model will be updated in the handler thread and the view
     * updated in the UI thread at the end.
     */
    public void performUpdate(final IWorkspaceRunnable runnable, boolean preserveExpansion, boolean updateInUIThread) {
        if (updateInUIThread) {
            queueEvent(new BackgroundEventHandler.RunnableEvent(getUIUpdateRunnable(runnable, preserveExpansion), true), true);
        } else {
	        queueEvent(new BackgroundEventHandler.RunnableEvent(getBackgroundUpdateRunnable(runnable, preserveExpansion), true), true);
        }
    }

    /**
     * Wrap the runnable in an outer runnable that preserves expansion.
     */
    private IWorkspaceRunnable getUIUpdateRunnable(final IWorkspaceRunnable runnable, final boolean preserveExpansion) {
        return new IWorkspaceRunnable() {
            public void run(final IProgressMonitor monitor) throws CoreException {
                final CoreException[] exception = new CoreException[] { null };
                runViewUpdate(new Runnable() {
                    public void run() {
    	                try {
    		                runnable.run(monitor);
    	                } catch (CoreException e) {
                            exception[0] = e;
                        }
                    }
                }, true /* preserve expansion */);
                if (exception[0] != null)
                    throw exception[0];
            }
        };
    }

    /*
     * Wrap the runnable in an outer runnable that preserves expansion if requested
     * and refreshes the view when the update is completed.
     */
    private IWorkspaceRunnable getBackgroundUpdateRunnable(final IWorkspaceRunnable runnable, final boolean preserveExpansion) {
        return new IWorkspaceRunnable() {
            IResource[] expanded;
            IResource[] selected;
            public void run(IProgressMonitor monitor) throws CoreException {
                if (preserveExpansion)
                    recordExpandedResources();
                try {
                    performingBackgroundUpdate = true;
	                runnable.run(monitor);
                } finally {
                    performingBackgroundUpdate = false;
                }
                updateView();
                
            }
            private void recordExpandedResources() {
        	    final StructuredViewer viewer = getViewer();
        		if (viewer != null && !viewer.getControl().isDisposed() && viewer instanceof AbstractTreeViewer) {
        			viewer.getControl().getDisplay().syncExec(new Runnable() {
        				public void run() {
        					if (viewer != null && !viewer.getControl().isDisposed()) {
        					    expanded = provider.getExpandedResources();
        					    selected = provider.getSelectedResources();
        					}
        				}
        			});
        		}
            }
            private void updateView() {
                // Refresh the view and then set the expansion
                runViewUpdate(new Runnable() {
                    public void run() {
                        provider.getViewer().refresh();
                        if (expanded != null)
                            provider.expandResources(expanded);
                        if (selected != null)
                            provider.selectResources(selected);
                    }
                }, false /* do not preserve expansion (since it is done above) */);
            }
        };
    }
    
	/*
	 * Execute the RunnableEvent
	 */
	private void executeRunnable(Event event, IProgressMonitor monitor) {
		try {
			// Dispatch any queued results to clear pending output events
			dispatchEvents(Policy.subMonitorFor(monitor, 1));
		} catch (TeamException e) {
			handleException(e);
		}
		try {
			((RunnableEvent)event).run(Policy.subMonitorFor(monitor, 1));
		} catch (CoreException e) {
			handleException(e);
		}
	}
    
    /**
     * Add the element to the viewer.
     * @param parent the parent of the element which is already added to the viewer
     * @param element the element to be added to the viewer
     */
    protected void doAdd(ISynchronizeModelElement parent, ISynchronizeModelElement element) {
        if (additionsMap == null) {
            if (Policy.DEBUG_SYNC_MODELS) {
                System.out.println("Added view item " + element.getName()); //$NON-NLS-1$
            }
            AbstractTreeViewer viewer = (AbstractTreeViewer)getViewer();
            viewer.add(parent, element);
        } else {
            // Accumulate the additions
            if (Policy.DEBUG_SYNC_MODELS) {
                System.out.println("Queueing view item for addition " + element.getName()); //$NON-NLS-1$
            }
            Set toAdd = (Set)additionsMap.get(parent);
            if (toAdd == null) {
                toAdd = new HashSet();
                additionsMap.put(parent, toAdd);
            }
            toAdd.add(element);
        }
    }
}
