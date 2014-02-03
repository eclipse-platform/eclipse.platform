/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.views.actions;

import java.util.Iterator;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.launchConfigurations.AntLaunchShortcut;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action which runs the selected target or the default target of the selected
 * project in the AntView.
 */
public class RunTargetAction extends Action implements IUpdate {
	
	private AntView fView;
	
	/**
	 * Creates a new <code>RunTargetAction</code> which will execute
	 * targets in the given view.
	 * @param view the Ant view whose selection this action will use when
	 * determining which target to run.
	 */
	public RunTargetAction(AntView view) {
		
		setText(AntViewActionMessages.RunTargetAction_Run_1);
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_RUN));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.RUN_TARGET_ACTION);

		setToolTipText(AntViewActionMessages.RunTargetAction_3);
		fView= view;
	}

	/**
	 * Executes the appropriate target based on the selection in the Ant view.
	 */
	public void run() {
		run(getSelectedElement());
	}
	
	/**
     * @param selectedElement The element to use as the context for launching
     */
    public void run(final AntElementNode selectedElement) {
        UIJob job= new UIJob(AntViewActionMessages.RunTargetAction_2) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				launch(selectedElement);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
    }

    /**
	 * Launches the given Ant element node
	 * @param node the node to use to launch
	 * @see AntLaunchShortcut#launch(AntElementNode, String)
	 */
	public void launch(AntElementNode node) {
		AntLaunchShortcut shortcut= new AntLaunchShortcut();
		shortcut.setShowDialog(false);
		shortcut.launch(node, ILaunchManager.RUN_MODE);
	}

	/**
	 * Updates the enablement of this action based on the user's selection
	 */
	public void update() {
		AntElementNode selection= getSelectedElement();
		boolean enabled= false;
		if (selection instanceof AntTargetNode) {
			if (!((AntTargetNode) selection).isErrorNode()) {
				setToolTipText(AntViewActionMessages.RunTargetAction_4);
				enabled= true;
			}
		} else if (selection instanceof AntProjectNode) {
			if (!((AntProjectNode) selection).isErrorNode()) {
				enabled= true;
				setToolTipText(AntViewActionMessages.RunTargetAction_3);
			}
		}  else if (selection instanceof AntTaskNode) {
			if (!((AntTaskNode) selection).isErrorNode()) {
				enabled= true;
				setToolTipText(AntViewActionMessages.RunTargetAction_0);
			}
		}
		setEnabled(enabled);
	}
	
	/**
	 * Returns the selected node or <code>null</code> if more than one element is selected.
	 * 
	 * @return AntElementNode the selected node
	 */
	private AntElementNode getSelectedElement() {
		IStructuredSelection selection= (IStructuredSelection) fView.getViewer().getSelection();
		if (selection.isEmpty()) {
			return null;
		}
		Iterator iter= selection.iterator();
		Object data= iter.next();
		if (iter.hasNext()) {
			return null;
		}
		return (AntElementNode) data;
	}	
}
