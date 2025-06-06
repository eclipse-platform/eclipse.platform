/*******************************************************************************
 * Copyright (c) Dec 28, 2012 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Default handler for the Open External Documentation command for the Ant editor
 *
 * @since 3.5.400
 */
public class OpenExternalAntDocHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof AntEditor editor) {
			ISelection s = HandlerUtil.getCurrentSelection(event);
			if (s instanceof ITextSelection ts) {
				AntModel model = editor.getAntModel();
				AntElementNode node = null;
				if (model != null) {
					node = model.getNode(ts.getOffset(), false);
				}
				if (node != null) {
					try {
						URL url = getExternalLocation(node, editor);
						if (url != null) {
							AntUtil.openBrowser(url.toString(), editor.getSite().getShell(), AntEditorActionMessages.getString("OpenExternalAntDocHandler_open_external_ant_doc")); //$NON-NLS-1$
						}
					}
					catch (MalformedURLException e) {
						AntUIPlugin.log(e);
					}
				}
			}
		}
		return null;
	}

	public URL getExternalLocation(AntElementNode node, AntEditor editor) throws MalformedURLException {
		URL baseLocation = getBaseLocation();
		if (baseLocation == null) {
			return null;
		}

		String urlString = baseLocation.toExternalForm();

		StringBuffer pathBuffer = new StringBuffer(urlString);
		if (!urlString.endsWith("/")) { //$NON-NLS-1$
			pathBuffer.append('/');
		}

		if (node instanceof AntProjectNode) {
			pathBuffer.append("using.html#projects"); //$NON-NLS-1$
		} else if (node instanceof AntTargetNode) {
			if (((AntTargetNode) node).isExtensionPoint()) {
				pathBuffer.append("targets.html#extension-points"); //$NON-NLS-1$
			} else {
				pathBuffer.append("using.html#targets"); //$NON-NLS-1$
			}
		} else if (node instanceof AntTaskNode taskNode) {
			if (editor.getAntModel().getDefininingTaskNode(taskNode.getTask().getTaskName()) == null) {
				// not a user defined task
				appendTaskPath(taskNode, pathBuffer);
			}
		}

		try {
			return new URL(pathBuffer.toString());
		}
		catch (MalformedURLException e) {
			AntUIPlugin.log(e);
		}
		return null;
	}

	private void appendTaskPath(AntTaskNode node, StringBuffer buffer) {
		String taskName = node.getTask().getTaskName();
		String taskPart = null;
		if (taskName.equalsIgnoreCase("path")) { //$NON-NLS-1$
			buffer.append("using.html#path"); //$NON-NLS-1$
			return;
		}
		taskPart = getTaskTypePart(node);
		if (taskPart == null) {
			return;
		}
		buffer.append(taskPart);
		buffer.append('/');
		buffer.append(taskName);
		buffer.append(".html"); //$NON-NLS-1$
	}

	private URL getBaseLocation() throws MalformedURLException {
		IPreferenceStore prefs = AntUIPlugin.getDefault().getPreferenceStore();
		String base = prefs.getString(IAntUIPreferenceConstants.DOCUMENTATION_URL);
		return new URL(base);
	}

	private String getTaskTypePart(AntTaskNode node) {
		AntProjectNode projectNode = node.getProjectNode();
		if (projectNode != null) {
			Project antProject = projectNode.getProject();
			AntTypeDefinition definition = ComponentHelper.getComponentHelper(antProject).getDefinition(node.getTask().getTaskName());
			if (definition == null) {
				return null;
			}
			String className = definition.getClassName();
			if (className.contains("taskdef")) { //$NON-NLS-1$
				return "Tasks"; //$NON-NLS-1$
			}
			return "Types"; //$NON-NLS-1$
		}

		return null;
	}
}
