/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.dynamic;

import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;

/*
 * A document processor that traverses every node of a document and allows
 * handlers to perform operations on the nodes.
 */
public class DocumentProcessor {

	private ProcessorHandler[] handlers;

	/*
	 * Creates a processor with no handlers.
	 */
	public DocumentProcessor() {
		handlers = new ProcessorHandler[0];
	}

	/*
	 * Creates a processor with the given handlers.
	 */
	public DocumentProcessor(ProcessorHandler[] handlers) {
		setHandlers(handlers);
	}

	/*
	 * Processes the given node and all its descendants, which exist
	 * inside a document identified by the given id.
	 */
	public void process(UAElement element, String id) {
		for (int i=0;i<handlers.length;++i) {
			short result = handlers[i].handle(element, id);
			if (result == ProcessorHandler.HANDLED_CONTINUE) {
				// handler wants us to keep processing children
				break;
			}
			if (result == ProcessorHandler.HANDLED_SKIP) {
				// handler wants us to skip children
				return;
			}
		}
		// process each child
		IUAElement[] children = element.getChildren();
		for (int i=0;i<children.length;++i) {
			process((UAElement)children[i], id);
		}
	}

	/*
	 * Sets the handlers for this processor.
	 */
	public void setHandlers(ProcessorHandler[] handlers) {
		if (this.handlers != handlers) {
			this.handlers = handlers;
			for (int i=0;i<handlers.length;++i) {
				handlers[i].setProcessor(this);
			}
		}
	}
}
