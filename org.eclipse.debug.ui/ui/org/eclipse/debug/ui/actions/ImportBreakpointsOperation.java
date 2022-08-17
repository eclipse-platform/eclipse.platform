/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     Sebastian Schmidt - bug 384460
 *******************************************************************************/

package org.eclipse.debug.ui.actions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IBreakpointImportParticipant;
import org.eclipse.debug.internal.core.BreakpointManager;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.importexport.breakpoints.IImportExportConstants;
import org.eclipse.debug.internal.ui.importexport.breakpoints.ImportExportMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;

/**
 * Imports breakpoints from a file or string buffer into the workspace.
 * <p>
 * This class may be instantiated.
 * </p>
 *
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ImportBreakpointsOperation implements IRunnableWithProgress {

	private boolean fOverwriteAll = false;

	private String fFileName = null;

	private boolean fCreateWorkingSets = false;

	private ArrayList<IBreakpoint> fAdded = new ArrayList<>();

	private String fCurrentWorkingSetProperty = null;

	private BreakpointManager fManager = (BreakpointManager) DebugPlugin.getDefault().getBreakpointManager();

	/**
	 * When a buffer is specified, a file is not used.
	 */
	private StringBuffer fBuffer = null;

	private boolean fImportBreakpoints = true;

	/**
	 * Constructs an operation to import breakpoints.
	 *
	 * @param fileName the file to read breakpoints from - the file should have been
	 *            created from an export operation
	 * @param overwrite whether imported breakpoints will overwrite existing equivalent breakpoints
	 * @param createWorkingSets whether breakpoint working sets should be created. Breakpoints
	 * 	are exported with information about the breakpoint working sets they belong to. Those
	 * 	working sets can be optionally re-created on import if they do not already exist in the
	 *            workspace.
	 */
	public ImportBreakpointsOperation(String fileName, boolean overwrite, boolean createWorkingSets) {
		this(fileName, overwrite, createWorkingSets, true);
	}

	/**
	 * Constructs an operation to import breakpoints.
	 *
	 * @param fileName the file to read breakpoints from - the file should have been created from an
	 *            export operation
	 * @param overwrite whether imported breakpoints will overwrite existing equivalent breakpoints
	 * @param createWorkingSets whether breakpoint working sets should be created. Breakpoints are
	 *            exported with information about the breakpoint working sets they belong to. Those
	 *            working sets can be optionally re-created on import if they do not already exist
	 *            in the workspace.
	 * @param importBreakpoints whether breakpoints should be imported and registered
	 * @since 3.9
	 */
	public ImportBreakpointsOperation(String fileName, boolean overwrite, boolean createWorkingSets, boolean importBreakpoints) {
		fFileName = fileName;
		fOverwriteAll = overwrite;
		fCreateWorkingSets = createWorkingSets;
		fImportBreakpoints = importBreakpoints;
	}

	/**
	 * Constructs an operation to import breakpoints from a string buffer. The buffer
	 * must contain a memento created an {@link ExportBreakpointsOperation}.
	 *
	 * @param buffer the string buffer to read breakpoints from - the file should have been
	 *            created from an export operation
	 * @param overwrite whether imported breakpoints will overwrite existing equivalent breakpoints
	 * @param createWorkingSets whether breakpoint working sets should be created. Breakpoints
	 * 	are exported with information about the breakpoint working sets they belong to. Those
	 * 	working sets can be optionally re-created on import if they do not already exist in the
	 *            workspace.
	 * @since 3.5
	 */
	public ImportBreakpointsOperation(StringBuffer buffer, boolean overwrite, boolean createWorkingSets) {
		this(buffer, overwrite, createWorkingSets, true);
	}

	/**
	 * Constructs an operation to import breakpoints from a string buffer. The buffer must contain a
	 * memento created an {@link ExportBreakpointsOperation}.
	 *
	 * @param buffer the string buffer to read breakpoints from - the file should have been created
	 *            from an export operation
	 * @param overwrite whether imported breakpoints will overwrite existing equivalent breakpoints
	 * @param createWorkingSets whether breakpoint working sets should be created. Breakpoints are
	 *            exported with information about the breakpoint working sets they belong to. Those
	 *            working sets can be optionally re-created on import if they do not already exist
	 *            in the workspace.
	 * @param importBreakpoints whether breakpoints should be imported and registered
	 * @since 3.9
	 */
	public ImportBreakpointsOperation(StringBuffer buffer, boolean overwrite, boolean createWorkingSets, boolean importBreakpoints) {
		fBuffer = buffer;
		fOverwriteAll = overwrite;
		fCreateWorkingSets = createWorkingSets;
		fImportBreakpoints = importBreakpoints;
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, ImportExportMessages.ImportOperation_0, 1);
		try {
			XMLMemento memento = null;
			if (fBuffer == null) {
				try (Reader reader = new InputStreamReader(new FileInputStream(fFileName), StandardCharsets.UTF_8)) {
					memento = XMLMemento.createReadRoot(reader);
				} catch (FileNotFoundException e) {
					throw new InvocationTargetException(e, MessageFormat.format("Breakpoint import file not found: {0}", new Object[] { //$NON-NLS-1$
							fFileName }));
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				}
			} else {
				try (Reader reader = new StringReader(fBuffer.toString())) {
					memento = XMLMemento.createReadRoot(reader);
				} catch (IOException e) {
					throw new InvocationTargetException(e);
				}
			}
			IMemento[] nodes = memento.getChildren(IImportExportConstants.IE_NODE_BREAKPOINT);
			IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
			localmonitor.setWorkRemaining(nodes.length);
			Map<String, Object> attributes = null;
			IBreakpointImportParticipant[] participants = null;
			for (IMemento node : nodes) {
				if(localmonitor.isCanceled()) {
					return;
				}
				attributes = collectBreakpointProperties(node);
				if(attributes == null) {
					continue;
				}
				IResource resource;
				if(fImportBreakpoints) {
					resource = workspace.findMember((String) attributes.get(IImportExportConstants.IE_NODE_PATH));
				} else {
					resource = workspace;
				}
				// filter resource breakpoints that do not exist in this workspace
				if(resource != null) {
					try {
						participants = fManager.getImportParticipants((String) attributes.get(IImportExportConstants.IE_NODE_TYPE));
					}
					catch(CoreException ce) {}
					IMarker marker = findExistingMarker(attributes, participants);
					if(marker == null) {
						marker = resource.createMarker((String) attributes.get(IImportExportConstants.IE_NODE_TYPE));
						restoreBreakpoint(marker, attributes, participants);
					}
					else {
						if(fOverwriteAll) {
							if(!fImportBreakpoints) {
								marker = resource.createMarker((String) attributes.get(IImportExportConstants.IE_NODE_TYPE));
							} else {
								marker.setAttributes(null);
							}
							restoreBreakpoint(marker, attributes, participants);
						}
					}
				}
				fCurrentWorkingSetProperty = null;
				localmonitor.worked(1);
			}
			if(fAdded.size() > 0 && fImportBreakpoints) {
				fManager.addBreakpoints(fAdded.toArray(new IBreakpoint[fAdded.size()]));
			}
		}
		catch(CoreException ce) {
			throw new InvocationTargetException(ce,
 MessageFormat.format("There was a problem importing breakpoints from: {0}", new Object[] { fFileName })); //$NON-NLS-1$
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns a marker backing an existing breakpoint based on the given set of breakpoint attributes
	 * @param attributes the map of attributes to compare for marker equality
	 * @param participants the list of participants to ask if a breakpoint matches the given map of attributes
	 * @return the marker for an existing breakpoint or <code>null</code> if one could not be located
	 * @since 3.5
	 */
	protected IMarker findExistingMarker(Map<String, Object> attributes, IBreakpointImportParticipant[] participants) {
		IBreakpoint[] bps = fManager.getBreakpoints();
		for (IBreakpoint bp : bps) {
			for (IBreakpointImportParticipant participant : participants) {
				try {
					if (participant.matches(attributes, bp)) {
						return bp.getMarker();
					}
				}catch(CoreException ce) {}
			}
		}
		return null;
	}

	/**
	 * Collects all of the properties for a breakpoint from the memento describing it.
	 * The values in the map will be one of:
	 * <ul>
	 * <li>{@link String}</li>
	 * <li>{@link Integer}</li>
	 * <li>{@link Boolean}</li>
	 * </ul>
	 * @param memento the memento to read breakpoint attributes from
	 * @return a new map of all of the breakpoint attributes from the given memento.
	 * @since 3.5
	 */
	protected Map<String, Object> collectBreakpointProperties(IMemento memento) {
		HashMap<String, Object> map = new HashMap<>();

		//collect attributes from the 'breakpoint' node
		map.put(IImportExportConstants.IE_BP_ENABLED, memento.getBoolean(IImportExportConstants.IE_BP_ENABLED));
		map.put(IImportExportConstants.IE_BP_PERSISTANT, memento.getBoolean(IImportExportConstants.IE_BP_PERSISTANT));
		map.put(IImportExportConstants.IE_BP_REGISTERED, memento.getBoolean(IImportExportConstants.IE_BP_REGISTERED));

		//collect attributes from the 'marker' node
		IMemento child = memento.getChild(IImportExportConstants.IE_NODE_MARKER);
		map.put(IImportExportConstants.IE_NODE_TYPE, child.getString(IImportExportConstants.IE_NODE_TYPE));
		map.put(IMarker.LINE_NUMBER, child.getInteger(IMarker.LINE_NUMBER));

		//copy all the marker attributes to the map
		IMemento[] children = child.getChildren(IImportExportConstants.IE_NODE_ATTRIB);
		for (IMemento c : children) {
			readAttribute(c, map);
		}

		//collect attributes from the 'resource' node
		child = memento.getChild(IImportExportConstants.IE_NODE_RESOURCE);
		map.put(IImportExportConstants.IE_NODE_PATH, child.getString(IImportExportConstants.IE_NODE_PATH));
		return map;
	}

	/**
	 * Collects the 'name' and 'value' key / attribute from the given memento and places it in the specified map
	 * @param memento the memento to read a name / value attribute from
	 * @param map the map to add the read attribute to
	 */
	private void readAttribute(IMemento memento, Map<String, Object> map) {
		String name = memento.getString(IImportExportConstants.IE_NODE_NAME),
			   value = memento.getString(IImportExportConstants.IE_NODE_VALUE);
		if (value != null && name != null) {
			if (name.equals(IInternalDebugUIConstants.WORKING_SET_NAME)) {
				fCurrentWorkingSetProperty = value;
			}
			Object val = value;
			try {
				val = Integer.valueOf(value);
			} catch (NumberFormatException e) {
				if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true")) { //$NON-NLS-1$ //$NON-NLS-2$
					val = Boolean.valueOf(value);
				}
			}
			if(val != null) {
				map.put(name, val);
			}
		}
	}

	/**
	 * restores all of the attributes back into the given marker, recreates the breakpoint in the
	 * breakpoint manager, and optionally recreates any working set(s) the breakpoint belongs to.
	 * @param marker the marker to create the new breakpoint on
	 * @param attributes the attributes to set in the new breakpoint
	 * @param participants the list of participants used to verify the restored breakpoint
	 * @since 3.5
	 */
	protected void restoreBreakpoint(IMarker marker, final Map<String, Object> attributes, IBreakpointImportParticipant[] participants) {
		for (Entry<String, Object> entry : attributes.entrySet()) {
			try {
				marker.setAttribute(entry.getKey(), entry.getValue());
			} catch (CoreException ce) {
			}
		}
		IBreakpoint breakpoint = null;
		try {
			// create the breakpoint
			breakpoint = fManager.createBreakpoint(marker);
			breakpoint.setEnabled(((Boolean)attributes.get(IImportExportConstants.IE_BP_ENABLED)).booleanValue());
			breakpoint.setPersisted(((Boolean)attributes.get(IImportExportConstants.IE_BP_PERSISTANT)).booleanValue());
			breakpoint.setRegistered(((Boolean)attributes.get(IImportExportConstants.IE_BP_REGISTERED)).booleanValue());
			fAdded.add(breakpoint);
			if (fImportBreakpoints && fCreateWorkingSets && fCurrentWorkingSetProperty != null) {
				String[] names = fCurrentWorkingSetProperty.split("\\" + IImportExportConstants.DELIMITER); //$NON-NLS-1$
				updateWorkingSets(names, breakpoint);
			}
			if(participants != null) {
				for (IBreakpointImportParticipant participant : participants) {
					participant.verify(breakpoint);
				}
			}
		}
		catch(CoreException ce) {
			//Something bad happened while trying to restore the breakpoint, remove it from the cached list and delete the marker
			//to ensure the manager does not hold bogus breakpoints
			if(breakpoint != null) {
				try {
					fAdded.remove(breakpoint);
					marker.delete();
				} catch (CoreException e) {}
			}
		}
	}

	/**
	 * Updates the working sets the given breakpoint belongs to
	 * @param wsnames the array of working set names
	 * @param breakpoint the breakpoint to add to the working sets
	 * @since 3.5
	 */
	private void updateWorkingSets(String[] wsnames, IBreakpoint breakpoint) {
		IWorkingSetManager mgr = PlatformUI.getWorkbench().getWorkingSetManager();
		ArrayList<IWorkingSet> sets = new ArrayList<>();
		collectContainingWorkingsets(breakpoint, sets);
		for (String wsname : wsnames) {
			if ("".equals(wsname)) { //$NON-NLS-1$
				continue;
			}
			IWorkingSet set = mgr.getWorkingSet(wsname);
			if (set == null) {
				//create working set
				set = mgr.createWorkingSet(wsname, new IAdaptable[] {});
				set.setId(IDebugUIConstants.BREAKPOINT_WORKINGSET_ID);
				mgr.addWorkingSet(set);
			}
			if(!sets.contains(set)) {
				IAdaptable[] elements = set.getElements();
				IAdaptable[] newElements = new IAdaptable[elements.length + 1];
				newElements[newElements.length - 1] = breakpoint;
				System.arraycopy(elements, 0, newElements, 0, elements.length);
				set.setElements(newElements);
			}
			sets.remove(set);
		}
		ArrayList<IAdaptable> items = null;
		for (IWorkingSet set : sets) {
			items = new ArrayList<>(Arrays.asList(set.getElements()));
			if(items.remove(breakpoint)) {
				set.setElements(items.toArray(new IAdaptable[items.size()]));
			}
		}
	}

	/**
	 * Collects all of the breakpoint working sets that contain the given {@link IBreakpoint}
	 * in the given list
	 *
	 * @param breakpoint the breakpoint to collect working set containers from
	 * @param collector the list to collect containing working sets in
	 * @since 3.5
	 */
	private void collectContainingWorkingsets(IBreakpoint breakpoint, List<IWorkingSet> collector) {
		IWorkingSetManager mgr = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet[] sets = mgr.getWorkingSets();
		for (IWorkingSet set : sets) {
			if (IDebugUIConstants.BREAKPOINT_WORKINGSET_ID.equals(set.getId()) && containsBreakpoint(set, breakpoint)) {
				collector.add(set);
			}
		}
	}

	/**
	 * Method to ensure markers and breakpoints are not both added to the working set
	 * @param set the set to check
	 * @param breakpoint the breakpoint to check for existence
	 * @return true if it is present false otherwise
	 */
	private boolean containsBreakpoint(IWorkingSet set, IBreakpoint breakpoint) {
		IAdaptable[] elements = set.getElements();
		for (IAdaptable element : elements) {
			if (element.equals(breakpoint)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the breakpoints that were imported by this operation, possibly
	 * an empty list.
	 *
	 * @return breakpoints imported by this operation
	 * @since 3.5
	 */
	public IBreakpoint[] getImportedBreakpoints() {
		return fAdded.toArray(new IBreakpoint[fAdded.size()]);
	}
}
