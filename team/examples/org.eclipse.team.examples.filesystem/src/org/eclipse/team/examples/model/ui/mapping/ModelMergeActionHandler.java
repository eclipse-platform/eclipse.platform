/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.internal.ui.mapping.ResourceModelProviderOperation;
import org.eclipse.team.ui.mapping.MergeActionHandler;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ModelMergeActionHandler extends MergeActionHandler {

	/*
	 * Operation to merge model elements. We're using an internal superclass to save on copying
	 * code.
	 */
	private final class ModelSynchronizeOperation extends ResourceModelProviderOperation {
		public ModelSynchronizeOperation(ISynchronizePageConfiguration configuration, IStructuredSelection selection) {
			super(configuration, selection);
		}

		@Override
		protected void execute(IProgressMonitor monitor) throws InvocationTargetException {
			// We need to perform special handling for any MOE file whose parent MOD is not included in the merge
			try {
				IMergeContext context = (IMergeContext)getContext();
				IDiff[] diffs = getTargetDiffs();
				ModelObjectElementFile[] moeMerges = getMoeOnlyMerges();
				IStatus status = context.merge(diffs, overwrite, monitor);
				if (!status.isOK())
					throw new CoreException(status);
				// For now, just cycle through each lonely MOE and update the parent
				for (ModelObjectElementFile file : moeMerges) {
					ModelObjectDefinitionFile modFile = (ModelObjectDefinitionFile)file.getParent();
					if (file.getResource().exists() && !modFile.hasMoe((IFile)file.getResource()))
						modFile.addMoe((IFile)file.getResource());
					else
						modFile.remove(file);
				}
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		}

		private ModelObjectElementFile[] getMoeOnlyMerges() {
			List<ModelObjectElementFile> result = new ArrayList<>();
			Object[] elements = getElements();
			for (Object object : elements) {
				if (object instanceof ModelObjectElementFile) {
					ModelObjectElementFile moeFile = (ModelObjectElementFile) object;
					result.add(moeFile);
				}
			}
			return result.toArray(new ModelObjectElementFile[result.size()]);
		}

		@Override
		protected FastDiffFilter getDiffFilter() {
			return new FastDiffFilter() {
				@Override
				public boolean select(IDiff node) {
					if (node instanceof IThreeWayDiff) {
						IThreeWayDiff twd = (IThreeWayDiff) node;
						if ((twd.getDirection() == IThreeWayDiff.OUTGOING && overwrite) || twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
							return true;
						}
						return false;
					}
					// Overwrite should always be available for two-way diffs
					return overwrite;
				}
			};
		}
	}

	final boolean overwrite;
	private SynchronizationOperation operation;

	public ModelMergeActionHandler(ISynchronizePageConfiguration configuration, boolean overwrite) {
		super(configuration);
		this.overwrite = overwrite;
	}

	@Override
	protected SynchronizationOperation getOperation() {
		if (operation == null) {
			operation = new ModelSynchronizeOperation(getConfiguration(), getStructuredSelection());
		}
		return operation;
	}

	@Override
	protected void updateEnablement(IStructuredSelection selection) {
		synchronized (this) {
			operation = null;
		}
		super.updateEnablement(selection);
	}

}
