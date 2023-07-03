/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.Pair;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Proxy to a launch shortcut extension
 */
public class LaunchShortcutExtension implements ILaunchShortcut2, IPluginContribution {

	private ImageDescriptor fImageDescriptor = null;
	private List<String> fPerspectives = null;
	private ILaunchShortcut fDelegate = null;
	private Set<String> fModes = null;
	private Set<String> fAssociatedTypes = null;
	private Map<String, String> fDescriptions = null;
	private IConfigurationElement fContextualLaunchConfigurationElement = null;
	private Expression fContextualLaunchExpr = null;
	private Expression fStandardLaunchExpr = null;

	/**
	 * Command handler for launch shortcut key binding.
	 */
	private static class LaunchCommandHandler extends AbstractHandler {
		// the shortcut to invoke
		private LaunchShortcutExtension fShortcut;
		private String fMode;

		/**
		 * Constructs a new command handler for the given shortcut
		 *
		 * @param shortcut
		 */
		public LaunchCommandHandler(LaunchShortcutExtension shortcut, String mode) {
			fShortcut = shortcut;
			fMode = mode;
		}

		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			LaunchShortcutAction action = new LaunchShortcutAction(fMode, fShortcut);
			if (action.isEnabled()) {
				action.run();
			} else {
				fShortcut.launch(new StructuredSelection(), fMode);
			}
			return null;
		}
	}

	/**
	 * The configuration element defining this tab.
	 */
	private IConfigurationElement fConfig;
	private/* <Pair> */List<Pair> fContextLabels;

	/**
	 * Constructs a launch configuration tab extension based
	 * on the given configuration element
	 *
	 * @param element the configuration element defining the
	 *  attributes of this launch configuration tab extension
	 * @return a new launch configuration tab extension
	 */
	public LaunchShortcutExtension(IConfigurationElement element) {
		setConfigurationElement(element);
		registerLaunchCommandHandlers();
	}

	/**
	 * Registers command handlers for launch shortcut key bindings
	 */
	private void registerLaunchCommandHandlers() {
		IHandlerService handlerService = PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
		if(handlerService != null) {
			for (String mode : getModes()) {
				String id = getId() + "." + mode; //$NON-NLS-1$
				IHandler handler = new LaunchCommandHandler(this, mode);
				handlerService.activateHandler(id, handler);
			}
		}
	}

	/**
	 * Sets the configuration element that defines the attributes
	 * for this extension.
	 *
	 * @param element configuration element
	 */
	private void setConfigurationElement(IConfigurationElement element) {
		fConfig = element;
	}

	/**
	 * Returns the configuration element that defines the attributes
	 * for this extension.
	 *
	 * @param configuration element that defines the attributes
	 *  for this launch configuration tab extension
	 */
	public IConfigurationElement getConfigurationElement() {
		return fConfig;
	}

	/**
	 * Returns the label of this shortcut
	 *
	 * @return the label of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getLabel() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.LABEL);
	}

	/**
	 * Returns the configuration element for the optional Contextual Launch
	 * element of this Launch Configuration description.
	 * @return contextualLaunch element
	 */
	public IConfigurationElement getContextualLaunchConfigurationElement() {
		if (fContextualLaunchConfigurationElement == null) {
			IConfigurationElement[] elements = getConfigurationElement().getChildren(IConfigurationElementConstants.CONTEXTUAL_LAUNCH);
			if (elements.length > 0) {
				// remember so we don't have to hunt again
				fContextualLaunchConfigurationElement = elements[0];
			}
		}
		return fContextualLaunchConfigurationElement;
	}

	/**
	 * Returns the contextual launch label of this shortcut for the named mode.
	 * <pre>
	 * &lt;launchShortcut...&gt;
	 *   &lt;contextualLaunch&gt;
	 *     &lt;contextLabel mode="run" label="Run Java Application"/&gt;
	 *     &lt;contextLabel mode="debug" label="Debug Java Application"/&gt;
	 *     ...
	 *   &lt;/contextualLaunch&gt;
	 * &lt;/launchShortcut&gt;
	 * </pre>
	 *
	 * @return the contextual label of this shortcut, or <code>null</code> if
	 *         not specified
	 */
	public String getContextLabel(String mode) {
		// remember the list of context labels for this shortcut
		if (fContextLabels == null) {
			IConfigurationElement context = getContextualLaunchConfigurationElement();
			if (context == null) {
				return null;
			}
			IConfigurationElement[] labels = context.getChildren(IConfigurationElementConstants.CONTEXT_LABEL);
			fContextLabels = new ArrayList<>(labels.length);
			for (IConfigurationElement label : labels) {
				fContextLabels.add(new Pair(label.getAttribute(IConfigurationElementConstants.MODE), label.getAttribute(IConfigurationElementConstants.LABEL)));
			}
		}
		// pick out the first occurance of the "name" bound to "mode"
		for (Pair p : fContextLabels) {
			if (p.firstAsString().equals(mode)) {
				return p.secondAsString();
			}
		}
		return getLabel();
	}

	/**
	 * Returns the set of associated launch configuration type ids.
	 *
	 * @return the set of associated launch configuration type ids
	 * @since 3.3
	 */
	public Set<String> getAssociatedConfigurationTypes() {
		if(fAssociatedTypes == null) {
			fAssociatedTypes = new HashSet<>();
			IConfigurationElement[] children = fConfig.getChildren(IConfigurationElementConstants.CONFIGURATION_TYPES);
			String id = null;
			for (IConfigurationElement child : children) {
				id = child.getAttribute(IConfigurationElementConstants.ID);
				if(id != null) {
					fAssociatedTypes.add(id);
				}
			}
		}
		return fAssociatedTypes;
	}

	/**
	 * Returns the contributed description of the launch delegate or <code>null</code>
	 * if one has not been provided
	 * @param mode the mode to get the description for
	 * @return the description of the shortcut for that specific mode or <code>null</code> if one was not provided
	 *
	 * @since 3.3
	 */
	public String getShortcutDescription(String mode) {
		if(mode == null) {
			return null;
		}
		if(fDescriptions == null) {
			fDescriptions = new HashMap<>();
			//get the description for the main element first
			String descr = fConfig.getAttribute(IConfigurationElementConstants.DESCRIPTION);
			if(descr != null) {
				for (String lmode : getModes()) {
					fDescriptions.put(lmode, descr);
				}
			}
			//load descriptions for child description elements
			IConfigurationElement[] children = fConfig.getChildren(IConfigurationElementConstants.DESCRIPTION);
			for (IConfigurationElement child : children) {
				String lmode = child.getAttribute(IConfigurationElementConstants.MODE);
				descr = child.getAttribute(IConfigurationElementConstants.DESCRIPTION);
				fDescriptions.put(lmode, descr);
			}
		}
		return fDescriptions.get(mode);
	}

	/**
	 * Evaluate the given expression within the given context and return
	 * the result. Returns <code>true</code> iff result is either TRUE or NOT_LOADED.
	 * This allows optimistic inclusion of shortcuts before plugins are loaded.
	 * Returns <code>false</code> if exp is <code>null</code>.
	 *
	 * @param exp the enablement expression to evaluate or <code>null</code>
	 * @param context the context of the evaluation. Usually, the
	 *  user's selection.
	 * @return the result of evaluating the expression
	 * @throws CoreException
	 */
	public boolean evalEnablementExpression(IEvaluationContext context, Expression exp) throws CoreException {
		return (exp != null) ? ((exp.evaluate(context)) != EvaluationResult.FALSE) : false;
	}

	/**
	 * Returns an expression that represents the enablement logic for the
	 * contextual launch element of this launch shortcut description or
	 * <code>null</code> if none.
	 * @return an evaluatable expression or <code>null</code>
	 * @throws CoreException if the configuration element can't be
	 *  converted. Reasons include: (a) no handler is available to
	 *  cope with a certain configuration element or (b) the XML
	 *  expression tree is malformed.
	 */
	public Expression getContextualLaunchEnablementExpression() throws CoreException {
		// all of this stuff is optional, so...tedious testing is required
		if (fContextualLaunchExpr == null) {
			IConfigurationElement contextualLaunchElement = getContextualLaunchConfigurationElement();
			if (contextualLaunchElement == null) {
				// not available
				return null;
			}
			IConfigurationElement[] elements = contextualLaunchElement.getChildren(ExpressionTagNames.ENABLEMENT);
			IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;

			if (enablement != null) {
				fContextualLaunchExpr= ExpressionConverter.getDefault().perform(enablement);
			}
		}
		return fContextualLaunchExpr;
	}

	/**
	 * Returns an expression that represents the enablement logic for the
	 * launch shortcut description or <code>null</code> if none.
	 * @return an evaluatable expression or <code>null</code>
	 * @throws CoreException if the configuration element can't be
	 *  converted. Reasons include: (a) no handler is available to
	 *  cope with a certain configuration element or (b) the XML
	 *  expression tree is malformed.
	 */
	public Expression getShortcutEnablementExpression() throws CoreException {
		// all of this stuff is optional, so...tedious testing is required
		if (fStandardLaunchExpr == null) {
			IConfigurationElement[] elements = getConfigurationElement().getChildren(ExpressionTagNames.ENABLEMENT);
			IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;
			if (enablement != null) {
				fStandardLaunchExpr= ExpressionConverter.getDefault().perform(enablement);
			}
		}
		return fStandardLaunchExpr;
	}

	/**
	 * Returns the id of this shortcut
	 *
	 * @return the id of this shortcut, or <code>null</code> if not specified
	 */
	public String getId() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.ID);
	}

	/**
	 * Returns the identifier of the help context associated with this launch
	 * shortcut, or <code>null</code> if one was not specified.
	 *
	 * @return the identifier of this launch shortcut's help context or
	 * <code>null</code>
	 * @since 2.1
	 */
	public String getHelpContextId() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.HELP_CONTEXT_ID);
	}

	/**
	 * Returns the category of this shortcut
	 *
	 * @return the category of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getCategory() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.CATEGORY);
	}

	/**
	 * Returns the image for this shortcut, or <code>null</code> if none
	 *
	 * @return the image for this shortcut, or <code>null</code> if none
	 */
	public ImageDescriptor getImageDescriptor() {
		if (fImageDescriptor == null) {
			fImageDescriptor = DebugUIPlugin.getImageDescriptor(getConfigurationElement(), "icon"); //$NON-NLS-1$
			if (fImageDescriptor == null) {
				fImageDescriptor = ImageDescriptor.getMissingImageDescriptor();
			}
		}
		return fImageDescriptor;
	}

	/**
	 * Returns the perspectives this shortcut is registered for.
	 *
	 * @return list of Strings representing perspective identifiers
	 * @deprecated The use of the perspectives element has been deprecated since 3.1.
	 */
	@Deprecated
	public List<String> getPerspectives() {
		if (fPerspectives == null) {
			IConfigurationElement[] perspectives = getConfigurationElement().getChildren(IConfigurationElementConstants.PERSPECTIVE);
			fPerspectives = new ArrayList<>(perspectives.length);
			for (IConfigurationElement perspective : perspectives) {
				fPerspectives.add(perspective.getAttribute(IConfigurationElementConstants.ID));
			}
		}
		return fPerspectives;
	}

	/**
	 * Returns this shortcut's delegate, or <code>null</code> if none
	 *
	 * @return this shortcut's delegate, or <code>null</code> if none
	 */
	protected ILaunchShortcut getDelegate() {
		if (fDelegate == null) {
			try {
				fDelegate = (ILaunchShortcut)fConfig.createExecutableExtension(IConfigurationElementConstants.CLASS);
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		return fDelegate;
	}

	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	@Override
	public void launch(IEditorPart editor, String mode) {
		ILaunchShortcut shortcut = getDelegate();
		if (shortcut != null) {
			shortcut.launch(editor, mode);
		}
	}

	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	@Override
	public void launch(ISelection selection, String mode) {
		ILaunchShortcut shortcut = getDelegate();
		if (shortcut != null) {
			shortcut.launch(selection, mode);
		}
	}


	/**
	 * Returns the set of modes this shortcut supports.
	 *
	 * @return the set of modes this shortcut supports
	 */
	public Set<String> getModes() {
		if (fModes == null) {
			String modes= getConfigurationElement().getAttribute(IConfigurationElementConstants.MODES);
			if (modes == null) {
				return Collections.EMPTY_SET;
			}
			StringTokenizer tokenizer= new StringTokenizer(modes, ","); //$NON-NLS-1$
			fModes = new HashSet<>(tokenizer.countTokens());
			while (tokenizer.hasMoreTokens()) {
				fModes.add(tokenizer.nextToken().trim());
			}
		}
		return fModes;
	}

	/**
	 * Returns the menu path attribute this shortcut, or <code>null</code> if none
	 *
	 * @return the menu path attribute this shortcut, or <code>null</code> if none
	 * @since 3.0.1
	 */
	public String getMenuPath() {
		return getConfigurationElement().getAttribute(IConfigurationElementConstants.PATH);
	}

	/*
	 * Only for debugging
	 */
	@Override
	public String toString() {
		return getId();
	}

	@Override
	public String getLocalId() {
		return getId();
	}

	@Override
	public String getPluginId() {
		return fConfig.getContributor().getName();
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		ILaunchShortcut delegate = getDelegate();
		if(delegate instanceof ILaunchShortcut2) {
			return ((ILaunchShortcut2)delegate).getLaunchConfigurations(selection);
		}
		return null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		ILaunchShortcut delegate = getDelegate();
		if(delegate instanceof ILaunchShortcut2) {
			return ((ILaunchShortcut2)delegate).getLaunchConfigurations(editorpart);
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		ILaunchShortcut delegate = getDelegate();
		if(delegate instanceof ILaunchShortcut2) {
			return ((ILaunchShortcut2)delegate).getLaunchableResource(selection);
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editorpart) {
		ILaunchShortcut delegate = getDelegate();
		if(delegate instanceof ILaunchShortcut2) {
			return ((ILaunchShortcut2)delegate).getLaunchableResource(editorpart);
		}
		return null;
	}

	/**
	 * Returns if the underlying delegate is a <code>ILaunchShortcut2</code>
	 * @return if the underlying delegate is a <code>ILaunchShortcut2</code>
	 *
	 * @since 3.4
	 */
	public boolean isParticipant() {
		return getDelegate() instanceof ILaunchShortcut2;
	}
}

