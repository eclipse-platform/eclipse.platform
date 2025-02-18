/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.security.identification;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public interface PublishPasswordUpdateIfc extends Publisher<String>{
	
	public void subscribe(Subscriber subscriber);
	public int getSubscriberCount();
	public void publishMessage(String message);
	public void close();
}
