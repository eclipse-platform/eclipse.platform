/*******************************************************************************
 * Copyright (c) 2026 Sergii Zolotarov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergii Zolotarov - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.TerminateToggleValue;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * The choices of the "Run As" (or "Debug As") menu for a selection, in the
 * same order as in {@link org.eclipse.debug.ui.actions.ContextualLaunchAction}:
 * a selected shared launch configuration, the launch shortcuts that apply, and
 * the launch configurations that these shortcuts give.
 */
public final class LaunchAsChoices {

	/**
	 * One choice.
	 *
	 * @param label  the text in the list
	 * @param image  the icon, or <code>null</code>
	 * @param key    a key that stays the same for the same choice, to find the
	 *               last choice again
	 * @param launch launches the choice
	 */
	public record Choice(String label, ImageDescriptor image, String key, Runnable launch) {
		@Override
		public String toString() {
			return label;
		}
	}

	private LaunchAsChoices() {
	}

	/**
	 * @param selection the selection of the workbench. If an editor is active, its
	 *                  first element is the editor.
	 * @param mode      the launch mode, for example "run" or "debug"
	 * @return the choices, in the order of the "Run As" menu
	 */
	public static List<Choice> of(IStructuredSelection selection, String mode) {
		LaunchConfigurationManager manager = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
		List<Choice> choices = new ArrayList<>();
		Set<ILaunchConfiguration> configurations = new HashSet<>();
		if (!selection.isEmpty()) {
			ILaunchConfiguration shared = manager.isSharedConfig(selection.getFirstElement());
			try {
				if (shared != null && shared.exists() && shared.supportsMode(mode) && configurations.add(shared)) {
					choices.add(configurationChoice(shared, null, mode));
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		List<Object> elements = new ArrayList<>(selection.toList());
		IEditorPart editor = null;
		if (!elements.isEmpty() && elements.get(0) instanceof IEditorPart editorPart) {
			editor = editorPart;
			elements.set(0, editorPart.getEditorInput());
		}
		IEvaluationContext context = DebugUIPlugin.createEvaluationContext(elements);
		context.setAllowPluginActivation(true);
		context.addVariable("selection", elements); //$NON-NLS-1$

		List<Choice> configurationChoices = new ArrayList<>();
		for (LaunchShortcutExtension shortcut : manager.getLaunchShortcuts()) {
			if (!shortcut.getModes().contains(mode) || WorkbenchActivityHelper.filterItem(shortcut)
					|| !isApplicable(shortcut, context) || !isEnabled(shortcut, editor, selection)) {
				continue;
			}
			choices.add(shortcutChoice(shortcut, editor, selection, mode));
			ILaunchConfiguration[] given = editor != null ? shortcut.getLaunchConfigurations(editor)
					: shortcut.getLaunchConfigurations(selection);
			if (given != null) {
				for (ILaunchConfiguration configuration : given) {
					if (configurations.add(configuration)) {
						configurationChoices.add(configurationChoice(configuration, shortcut, mode));
					}
				}
			}
		}
		choices.addAll(configurationChoices);
		return choices;
	}

	private static boolean isApplicable(LaunchShortcutExtension shortcut, IEvaluationContext context) {
		try {
			return shortcut.evalEnablementExpression(context, shortcut.getContextualLaunchEnablementExpression());
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
			return false;
		}
	}

	/**
	 * The same check as the "Run As" menu, which shows a shortcut that fails it as
	 * disabled
	 */
	private static boolean isEnabled(LaunchShortcutExtension shortcut, IEditorPart editor,
			IStructuredSelection selection) {
		if (editor != null) {
			return true;
		}
		try {
			Expression expression = shortcut.getShortcutEnablementExpression();
			if (expression == null) {
				return !selection.isEmpty();
			}
			List<?> list = selection.toList();
			IEvaluationContext context = DebugUIPlugin.createEvaluationContext(list);
			context.addVariable("selection", list); //$NON-NLS-1$
			return shortcut.evalEnablementExpression(context, expression);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
			return false;
		}
	}

	private static Choice shortcutChoice(LaunchShortcutExtension shortcut, IEditorPart editor,
			IStructuredSelection selection, String mode) {
		String label = shortcut.getContextLabel(mode);
		if (label == null) {
			label = shortcut.getLabel();
		}
		return new Choice(LegacyActionTools.removeMnemonics(label), shortcut.getImageDescriptor(),
				"shortcut:" + shortcut.getId(), () -> { //$NON-NLS-1$
					// As the "Run As" menu: no Shift, so the preference decides whether to
					// terminate the last launch of the configuration first
					if (editor != null) {
						DebugUITools.storeLaunchToggleTerminate(editor, Boolean.FALSE);
						try {
							shortcut.launch(editor, mode);
						} finally {
							DebugUITools.removeLaunchToggleTerminate(editor);
						}
					} else {
						DebugUITools.storeLaunchToggleTerminate(selection, new TerminateToggleValue(false, shortcut));
						try {
							shortcut.launch(selection, mode);
						} finally {
							DebugUITools.removeLaunchToggleTerminate(selection);
						}
					}
				});
	}

	private static Choice configurationChoice(ILaunchConfiguration configuration, LaunchShortcutExtension shortcut,
			String mode) {
		String label;
		try {
			label = NLS.bind(DebugUIMessages.LaunchShortcutAction_combineLaunchShortcutName, configuration.getName(),
					configuration.getType().getName());
		} catch (CoreException e) {
			label = configuration.getName();
		}
		ImageDescriptor image = shortcut != null ? shortcut.getImageDescriptor()
				: DebugUITools.getDefaultImageDescriptor(configuration);
		return new Choice(label, image, "configuration:" + configuration.getName(), //$NON-NLS-1$
				() -> DebugUITools.launch(configuration, mode));
	}
}
