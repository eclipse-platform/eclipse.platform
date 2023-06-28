/*******************************************************************************
 * Copyright (c) 2009, 2012 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.viewer.model;

/**
 * Convenience interface with constants used by the test model update listener.
 *
 * @since 3.6
 */
public interface ITestModelUpdatesListenerConstants {

	int LABEL_SEQUENCE_COMPLETE =      0X00000001;
	int CONTENT_SEQUENCE_COMPLETE =    0X00000002;
	int CONTENT_SEQUENCE_STARTED =     0X00020000;
	int LABEL_UPDATES =                0X00000004;
	int LABEL_SEQUENCE_STARTED =       0X00040000;
	int HAS_CHILDREN_UPDATES =         0X00000008;
	int HAS_CHILDREN_UPDATES_STARTED = 0X00080000;
	int CHILD_COUNT_UPDATES =          0X00000010;
	int CHILD_COUNT_UPDATES_STARTED =  0X00100000;
	int CHILDREN_UPDATES =             0X00000020;
	int CHILDREN_UPDATES_STARTED =     0X00200000;
	int CHILDREN_UPDATES_RUNNING =     0X00400000;
	int MODEL_CHANGED_COMPLETE =       0X00000040;
	int MODEL_PROXIES_INSTALLED =      0X00000080;
	int STATE_SAVE_COMPLETE =          0X00000100;
	int STATE_SAVE_STARTED =           0X01000000;
	int STATE_RESTORE_COMPLETE =       0X00000200;
	int STATE_RESTORE_STARTED =        0X02000000;
	int STATE_UPDATES =                0X00000400;
	int STATE_UPDATES_STARTED =        0X04000000;

	int VIEWER_UPDATES_RUNNING =       0X00001000;
	int LABEL_UPDATES_RUNNING =        0X00002000;

	int ALL_VIEWER_UPDATES_STARTED = HAS_CHILDREN_UPDATES_STARTED | CHILD_COUNT_UPDATES_STARTED | CHILDREN_UPDATES_STARTED;

	int LABEL_COMPLETE = LABEL_SEQUENCE_COMPLETE | LABEL_UPDATES | LABEL_UPDATES_RUNNING;
	int CONTENT_COMPLETE =
		CONTENT_SEQUENCE_COMPLETE | HAS_CHILDREN_UPDATES | CHILD_COUNT_UPDATES | CHILDREN_UPDATES | VIEWER_UPDATES_RUNNING;

	int ALL_UPDATES_COMPLETE = LABEL_COMPLETE | CONTENT_COMPLETE | MODEL_PROXIES_INSTALLED | LABEL_UPDATES_RUNNING | VIEWER_UPDATES_RUNNING;
}
