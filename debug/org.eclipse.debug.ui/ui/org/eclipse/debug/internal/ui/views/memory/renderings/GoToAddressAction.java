/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.debug.ui.memory.IRepositionableMemoryRendering;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;


/**
 * Go To Address Action for table rendering
 *
 * @since 3.0
 */
public class GoToAddressAction extends Action
{
	private IMemoryRenderingContainer fContainer;
	private IRepositionableMemoryRendering fRendering;

	public GoToAddressAction(IMemoryRenderingContainer container, IRepositionableMemoryRendering rendering)
	{
		super(DebugUIMessages.GoToAddressAction_title);
		fContainer = container;
		setToolTipText(DebugUIMessages.GoToAddressAction_title);

		fRendering = rendering;

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugUIConstants.PLUGIN_ID + ".GoToAddressAction_context"); //$NON-NLS-1$
	}
	@Override
	public void run()
	{
		try
		{
			Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			// create dialog to ask for expression/address to block
			GoToAddressDialog dialog = new GoToAddressDialog(shell);
			dialog.open();

			int returnCode = dialog.getReturnCode();

			if (returnCode == Window.CANCEL)
			{
				return;
			}

			// get expression from dialog
			String expression = dialog.getExpression();

			expression = parseExpression(expression);

			doGoToAddress(expression);
		}
		// open error in case of any error
		catch (DebugException e)
		{
			MemoryViewUtil.openError(DebugUIMessages.GoToAddressAction_Go_to_address_failed,
				DebugUIMessages.GoToAddressAction_Go_to_address_failed, e);
		}
		catch (NumberFormatException e1)
		{
			MemoryViewUtil.openError(DebugUIMessages.GoToAddressAction_Go_to_address_failed,
				DebugUIMessages.GoToAddressAction_Address_is_invalid, null);
		}
	}
	/**
	 * @param expression
	 * @return
	 */
	public String parseExpression(String expression) {
		expression = expression.toUpperCase();
		expression = expression.trim();

		if (expression.startsWith("0X")) //$NON-NLS-1$
		{
			expression = expression.substring(2);
		}
		return expression;
	}
	/**
	 * @param expression
	 * @throws DebugException
	 */
	public void doGoToAddress(String expression) throws DebugException, NumberFormatException {
		// convert expression to address
		BigInteger address = new BigInteger(expression, 16);

		// look at this address and figure out if a new memory block should
		// be opened.
		IMemoryBlock mb = fRendering.getMemoryBlock();
		if (mb instanceof IMemoryBlockExtension)
		{
			IMemoryBlockExtension mbExt = (IMemoryBlockExtension)mb;
			BigInteger mbStart = mbExt.getMemoryBlockStartAddress();
			BigInteger mbEnd = mbExt.getMemoryBlockEndAddress();

			if (mbStart != null)
			{
				// if trying to go beyond the start address
				// of the memory block
				if (address.compareTo(mbStart) < 0)
				{
					IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(mbExt);

					// add a new memory block and then the same rendering as fRendering
					// in the same container.
					if (retrieval != null && retrieval instanceof IMemoryBlockRetrievalExtension)
					{
						addNewMemoryBlock(expression, (IMemoryBlockRetrievalExtension)retrieval);
						return;
					}
				}
			}
			if (mbEnd != null)
			{
				// if trying to go beyond the end address
				// of the memory block
				if (address.compareTo(mbEnd) > 0)
				{
					IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(mbExt);

					// add a new memory block and then the same rendering as fRendering
					// in the same container.
					if (retrieval != null && retrieval instanceof IMemoryBlockRetrievalExtension)
					{
						addNewMemoryBlock(expression, (IMemoryBlockRetrievalExtension)retrieval);
						return;
					}
				}
			}
		}

		// go to specified address
		fRendering.goToAddress(address);
	}

	private void addNewMemoryBlock(String expression, IMemoryBlockRetrievalExtension retrieval)
	{
		Object elem = DebugUITools.getPartDebugContext(fContainer.getMemoryRenderingSite().getSite());

		if (!(elem instanceof IDebugElement)) {
			return;
		}

		try {
			if (retrieval != null)
			{
				IMemoryBlockExtension mbext = retrieval.getExtendedMemoryBlock(expression, elem);
				if (mbext != null)
				{
					IMemoryBlock[] memArray = new IMemoryBlock[]{mbext};
					DebugPlugin.getDefault().getMemoryBlockManager().addMemoryBlocks(memArray);
				}

				IMemoryRenderingType renderingType = DebugUITools.getMemoryRenderingManager().getRenderingType(fRendering.getRenderingId());

				if (renderingType != null)
				{
					IMemoryRendering rendering = renderingType.createRendering();

					if (rendering != null && fRendering instanceof AbstractMemoryRendering)
					{
						rendering.init(((AbstractMemoryRendering)fRendering).getMemoryRenderingContainer(), mbext);
						((AbstractMemoryRendering)fRendering).getMemoryRenderingContainer().addMemoryRendering(rendering);
					}
				}
			}
		} catch (DebugException e) {
			MemoryViewUtil.openError(DebugUIMessages.GoToAddressAction_Go_to_address_failed,
			DebugUIMessages.GoToAddressAction_Go_to_address_failed, e);
		} catch (CoreException e)
		{
			MemoryViewUtil.openError(DebugUIMessages.GoToAddressAction_Go_to_address_failed,
			DebugUIMessages.GoToAddressAction_Go_to_address_failed, e);
		}
	}
}
