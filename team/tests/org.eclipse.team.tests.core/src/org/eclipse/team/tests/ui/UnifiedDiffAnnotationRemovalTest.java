/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.team.tests.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.junit.jupiter.api.Test;

public class UnifiedDiffAnnotationRemovalTest {

	private static final class TrackingAnnotation extends Annotation {
		private boolean markDeletedCalled;

		TrackingAnnotation() {
			super("test", false, null); //$NON-NLS-1$
		}

		@Override
		public void markDeleted(boolean deleted) {
			markDeletedCalled = deleted;
			super.markDeleted(deleted);
		}
	}

	private static final class TrackingAnnotationModel extends AnnotationModel {
		private boolean markedDeletedWhenRemoved;

		@Override
		public void removeAnnotation(Annotation annotation) {
			markedDeletedWhenRemoved = annotation.isMarkedDeleted();
			super.removeAnnotation(annotation);
		}
	}

	@Test
	void marksAnnotationsDeletedBeforeRemovingThem() {
		TrackingAnnotation annotation = new TrackingAnnotation();
		TrackingAnnotationModel model = new TrackingAnnotationModel();
		model.addAnnotation(annotation, new Position(0));

		UnifiedDiffManager.removeAnnotations(model, List.of(annotation));

		assertTrue(annotation.markDeletedCalled);
		assertTrue(model.markedDeletedWhenRemoved);
		assertNull(model.getPosition(annotation));
	}
}
