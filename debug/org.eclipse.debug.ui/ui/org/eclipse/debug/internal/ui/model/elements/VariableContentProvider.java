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
package org.eclipse.debug.internal.ui.model.elements;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.debug.internal.ui.views.variables.LogicalStructureCache;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * @since 3.3
 */
public class VariableContentProvider extends ElementContentProvider {

	/**
	 * Cache of logical structures to avoid computing structures for different
	 * subranges.
	 */
	private static LogicalStructureCache fgLogicalCache;

	@Override
	protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getAllChildren(element, context).length;
	}

	@Override
	protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return getElements(getAllChildren(parent, context), index, length);
	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
		return ((IVariable)element).getValue().hasVariables();
	}

	@Override
	protected boolean supportsContextId(String id) {
		 return id.equals(IDebugUIConstants.ID_EXPRESSION_VIEW) || id.equals(IDebugUIConstants.ID_VARIABLE_VIEW) || id.equals(IDebugUIConstants.ID_REGISTER_VIEW);
	}

	/**
	 * Gets all the children variables for the parent
	 * @param parent the parent IVariable
	 * @param context the context the children will be presented in
	 * @return an array of all children or an empty array if none
	 * @throws CoreException
	 */
	protected Object[] getAllChildren(Object parent, IPresentationContext context) throws CoreException {
		IVariable variable = (IVariable) parent;
		IValue value = variable.getValue();
		if (value != null) {
			return getValueChildren(variable, value, context);
		}
		return EMPTY;
	}

	/**
	 * Return whether to show compute a logical structure or a raw structure
	 * in the specified context
	 *
	 * @return whether to show compute a logical structure or a raw structure
	 * in the specified context
	 */
	protected boolean isShowLogicalStructure(IPresentationContext context) {
		Boolean show = (Boolean) context.getProperty(VariablesView.PRESENTATION_SHOW_LOGICAL_STRUCTURES);
		return show != null && show.booleanValue();
	}

	/**
	 * Returns the number of entries that should be displayed in each partition
	 * of an indexed collection.
	 *
	 * @return the number of entries that should be displayed in each partition
	 *         of an indexed collection
	 */
	protected int getArrayPartitionSize() {
		// TODO: should fix this with a user preference
		return 100;
	}

	/**
	 * Returns any logical value for the raw value in the specified context
	 *
	 * @param value
	 * @param context
	 * @return logical value for the raw value
	 */
	protected IValue getLogicalValue(IValue value, IPresentationContext context) throws CoreException {
		return getLogicalValue(value, new ArrayList<>(), context);
	}

	/**
	 * Returns children for the given value, creating array partitions if
	 * required
	 *
	 * @param parent expression or variable containing the given value
	 * @param value the value to retrieve children for
	 * @param context the context in which children have been requested
	 * @return children for the given value, creating array partitions if
	 *         required
	 * @throws CoreException
	 */
	protected Object[] getValueChildren(IDebugElement parent, IValue value, IPresentationContext context) throws CoreException {
		if (value == null) {
			return EMPTY;
		}
		IValue logicalValue = getLogicalValue(value, context);
		if (logicalValue instanceof IIndexedValue) {
			IIndexedValue indexedValue = (IIndexedValue) logicalValue;
			int partitionSize = computeParitionSize(indexedValue);
			if (partitionSize > 1) {
				int offset = indexedValue.getInitialOffset();
				int length = indexedValue.getSize();
				int numPartitions = length / partitionSize;
				int remainder = length % partitionSize;
				if (remainder > 0) {
					numPartitions++;
				}
				IVariable[] partitions = new IVariable[numPartitions];
				for (int i = 0; i < (numPartitions - 1); i++) {
					partitions[i] = new IndexedVariablePartition(parent, indexedValue, offset, partitionSize);
					offset = offset + partitionSize;
				}
				if (remainder == 0) {
					remainder = partitionSize;
				}
				partitions[numPartitions - 1] = new IndexedVariablePartition(parent, indexedValue, offset, remainder);
				return partitions;
			}
		}
		if (logicalValue == null) {
			// safeguard against an structure type returning null
			logicalValue = value;
		}
		return logicalValue.getVariables();
	}

	/**
	 * Returns the partition size to use for the given indexed value. The
	 * partition size is computed by determining the number of levels that an
	 * indexed collection must be nested in order to partition the collection
	 * sub-collections of the preferred partition size.
	 *
	 * @param value
	 *            indexed value
	 * @return size of partitions the value should be subdivided into
	 */
	protected int computeParitionSize(IIndexedValue value) {
		int partitionSize = 1;
		try {
			int length = value.getSize();
			int partitionDepth = 0;
			int preferredSize = getArrayPartitionSize();
			int remainder = length % preferredSize;
			length = length / preferredSize;
			while (length > 0) {
				if (remainder == 0 && length == 1) {
					break;
				}
				partitionDepth++;
				remainder = length % preferredSize;
				length = length / preferredSize;
			}
			for (int i = 0; i < partitionDepth; i++) {
				partitionSize = partitionSize * preferredSize;
			}
		} catch (DebugException e) {
		}
		return partitionSize;
	}

	/**
	 * Returns any logical value for the raw value. This method will recurse
	 * over the returned value until the same structure is encountered again (to
	 * avoid infinite recursion).
	 *
	 * @param value raw value to possibly be replaced by a logical value
	 * @param previousStructureIds
	 *            the list of logical structures that have already been applied
	 *            to the returned value during the recursion of this method.
	 *            Callers should always pass in a new, empty list.
	 * @return logical value if one is calculated, otherwise the raw value is returned
	 */
	protected IValue getLogicalValue(IValue value, List<String> previousStructureIds, IPresentationContext context) throws CoreException {
		if (isShowLogicalStructure(context)) {
			ILogicalStructureType[] types = DebugPlugin.getLogicalStructureTypes(value);
			if (types.length > 0) {
				ILogicalStructureType type = DebugPlugin.getDefaultStructureType(types);
				if (type != null && !previousStructureIds.contains(type.getId())) {
					IValue logicalValue = getLogicalStructureCache().getLogicalStructure(type, value);
					previousStructureIds.add(type.getId());
					return getLogicalValue(logicalValue, previousStructureIds, context);
				}
			}
		}
		return value;
	}

	/**
	 * Returns the logical structure cache to use to store calculated structures.  If the cache does not
	 * exist yet, one is created and a debug event listener is added to clear the cache on RESUME and
	 * TERMINATE events.
	 *
	 * @return the logical structure cache to use
	 */
	protected synchronized LogicalStructureCache getLogicalStructureCache(){
		if (fgLogicalCache == null){
			fgLogicalCache = new LogicalStructureCache();
			// Add a listener to clear the cache when resuming, terminating, or suspending
			DebugPlugin.getDefault().addDebugEventListener(events -> {
				for (DebugEvent event : events) {
					if (event.getKind() == DebugEvent.TERMINATE) {
						fgLogicalCache.clear();
						break;
					} else if (event.getKind() == DebugEvent.RESUME && event.getDetail() != DebugEvent.EVALUATION_IMPLICIT) {
						fgLogicalCache.clear();
						break;
					} else if (event.getKind() == DebugEvent.SUSPEND && event.getDetail() != DebugEvent.EVALUATION_IMPLICIT) {
						fgLogicalCache.clear();
						break;
					} else if (event.getKind() == DebugEvent.CHANGE && event.getDetail() == DebugEvent.CONTENT) {
						fgLogicalCache.clear();
						break;
					}
				}
			});
		}
		return fgLogicalCache;
	}

}
