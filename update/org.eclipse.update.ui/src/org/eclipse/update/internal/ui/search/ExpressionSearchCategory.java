package org.eclipse.update.internal.ui.search;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.model.ISiteAdapter;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class ExpressionSearchCategory extends SearchCategory {
	private static final String KEY_EXPRESSION = "ExpressionSearchCategory.expression";
	private static final String KEY_CASE = "ExpressionSearchCategory.case";
	private static final String KEY_LOOK = "ExpressionSearchCategory.look";
	private static final String KEY_NAME = "ExpressionSearchCategory.name";
	private static final String KEY_PROVIDER = "ExpressionSearchCategory.provider";
	private static final String KEY_DESCRIPTION = "ExpressionSearchCategory.description";

	private Text expressionText;
	private Button caseCheck;
	private Button nameCheck;
	private Button providerCheck;
	private Button descriptionCheck;
	private boolean caseSensitive = false;
	private boolean searchName = true;
	private boolean searchProvider = false;
	private boolean searchDesc = false;
	private String expression="";
	private String noCaseExpression;
	
	public ExpressionSearchCategory() {
	}
	
	public void createControl(Composite parent, FormWidgetFactory factory) {		
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.numColumns = 2;
		container.setLayout(layout);
		factory.createLabel(container, UpdateUIPlugin.getResourceString(KEY_EXPRESSION));
		expressionText = factory.createText(container, "");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		expressionText.setLayoutData(gd);
		caseCheck = factory.createButton(container, UpdateUIPlugin.getResourceString(KEY_CASE), SWT.CHECK);
		fillHorizontal(caseCheck, 0);
		Label label = factory.createLabel(container, UpdateUIPlugin.getResourceString(KEY_LOOK));
		fillHorizontal(label, 0);
		nameCheck = factory.createButton(container, UpdateUIPlugin.getResourceString(KEY_NAME), SWT.CHECK);
		fillHorizontal(nameCheck, 10);
		providerCheck = factory.createButton(container, UpdateUIPlugin.getResourceString(KEY_PROVIDER), SWT.CHECK);
		fillHorizontal(providerCheck, 10);
		descriptionCheck = factory.createButton(container, UpdateUIPlugin.getResourceString(KEY_DESCRIPTION), SWT.CHECK);
		fillHorizontal(descriptionCheck, 10);
		factory.paintBordersFor(container);
		initializeWidgets(false);
		setControl(container);
	}
	private void fillHorizontal(Control control, int indent) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = indent;
		control.setLayoutData(gd);
	}

	/**
	 * @see ISearchCategory#matches(IFeature)
	 */
	public ISearchQuery [] getQueries() {
		storeSettingsFromWidgets();
		ISearchQuery query = new ISearchQuery() {
			public ISiteAdapter getSearchSite() {
				return null;
			}
			public IFeature [] getMatchingFeatures(ISite site, IProgressMonitor monitor) {
				ArrayList result = new ArrayList();
				ArrayList candidates = getCandidates(site, monitor);
				for (int i=0; i<candidates.size(); i++) {
					if (internalMatches((IFeature)candidates.get(i)))
						result.add(candidates.get(i));
				}
				return (IFeature[])result.toArray(new IFeature[result.size()]);
			}
		};
		return new ISearchQuery [] { query };
	}
	
	private ArrayList getCandidates(ISite site, IProgressMonitor monitor) {
		IFeatureReference [] references = site.getFeatureReferences();
		ArrayList result = new ArrayList();
		monitor.beginTask("", references.length);
		for (int i=0; i<references.length; i++) {
			try {
				IFeature feature = references[i].getFeature();
				result.add(feature);
			}
			catch (CoreException e) {
			}
			monitor.worked(1);
		}
		monitor.done();
		return result;
	}
	
	private void storeSettingsFromWidgets() {
		caseSensitive = caseCheck.getSelection();
		searchName = nameCheck.getSelection();
		searchProvider = providerCheck.getSelection();
		searchDesc = descriptionCheck.getSelection();
		expression = expressionText.getText().trim();
		noCaseExpression = expression.toLowerCase();
	}
	private void initializeWidgets(boolean editable) {
		caseCheck.setSelection(caseSensitive);
		nameCheck.setSelection(searchName);
		providerCheck.setSelection(searchProvider);
		descriptionCheck.setSelection(searchDesc);
		expressionText.setText(expression);

		caseCheck.setEnabled(editable);
		nameCheck.setEnabled(editable);
		providerCheck.setEnabled(editable);
		descriptionCheck.setEnabled(editable);
		expressionText.setEnabled(editable);
	}
	
	public String getCurrentSearch() {
		return "\""+expressionText.getText()+"\"";
	}
	private boolean internalMatches(IFeature feature) {
		if (searchName) {
			if (matches(feature.getLabel())) return true;
		}
		if (searchProvider) {
			if (matches(feature.getProvider())) return true;
		}
		if (searchDesc) {
			String annotation = null;
			if (feature.getDescription()!=null)
				annotation = feature.getDescription().getAnnotation();
			if (annotation!=null) {
				if (matches(annotation)) return true;
			}
		}
		return false;
	}
	private boolean matches(String text) {
		if (!caseSensitive) {
			String noCaseText = text.toLowerCase();
			return noCaseText.indexOf(noCaseExpression)!= -1;
		}
		return text.indexOf(expression)!= -1;
	}
	public void load(Map map, boolean editable) {
		caseSensitive = getBoolean("case", map);
		searchName = getBoolean("name", map);
		searchProvider = getBoolean("provider", map);
		searchDesc = getBoolean("desc", map);
		expression = getString("expression", map);
		if (caseCheck!=null)
			initializeWidgets(editable);
	}

	public void store(Map map) {
		storeSettingsFromWidgets();
		map.put("case", caseSensitive?"true":"false");
		map.put("name", searchName?"true":"false");
		map.put("provider", searchProvider?"true":"false");
		map.put("desc", searchDesc?"true":"false");
		map.put("expression", expression);
	}
}