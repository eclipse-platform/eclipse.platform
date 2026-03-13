package org.eclipse.compare.unifieddiff.internal;

import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.disposeUnifiedDiff;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.get;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.getAllAnnotationsForUnifiedDiff;

import java.util.List;

import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewerExtension5;

public class KeepAllRunnable implements Runnable {
	private ITextViewer tv;
	private IAnnotationModel model;

	public KeepAllRunnable(ITextViewer tv, IAnnotationModel model) {
		this.tv = tv;
		this.model = model;
	}

	public String getLabel() {
		return "Keep All";
	}

	@Override
	public void run() {
		List<UnifiedDiff> diffs1 = get(tv);
		for (UnifiedDiff diff : diffs1) {
			List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
			for (var lanno : annos) {
				model.removeAnnotation(lanno);
			}
		}
		diffs1.clear();
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
		disposeUnifiedDiff(tv, model, tv.getTextWidget());
	}
}
