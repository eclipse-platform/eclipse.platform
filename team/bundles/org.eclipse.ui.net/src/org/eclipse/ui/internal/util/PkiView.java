package org.eclipse.ui.internal.util;



import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class PkiView extends ViewPart {

	private Composite view;
	private Label alias;
	private Label certLocation;
	private Label expirationDate;
	private Label distinguishedName;
	private Label seperator;
	public PkiView() {
		super();
	}
	public void init(ArrayList expiredCerts) {
		ExpiredX509CertificateData data=null;
		for(Object d : expiredCerts.toArray()) {

			data = (ExpiredX509CertificateData) d;
			certLocation.setText("Certificate File Location:" + data.getCertLocation()); //$NON-NLS-1$
			//seperator.setText(null);
			alias.setText("Alias:" + data.getAlias()); //$NON-NLS-1$
			expirationDate.setText("EXPIRATION DATE:" + data.getExpirationDate()); //$NON-NLS-1$
			distinguishedName.setText("DN:" + data.getDistinguishedName()); //$NON-NLS-1$
			alias.setFocus();
			view.pack();

		}
		//System.out.println("ExpiredCertCheck PKIVIEW:"+data.getExpirationDate());

	}
	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub

		view = parent;
		RowLayout layout = new RowLayout();
		//Label separator = new Label(parent, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		layout.type = SWT.VERTICAL;
	    view.setLayout( layout);

		certLocation = new Label(view, 0);
		// Create a Label is Horizontal Separator
	    //Label hSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);

		//seperator = new Label(parent, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		alias = new Label(view, 0);

		expirationDate = new Label(view, 0);
		distinguishedName  = new Label(view, 0);


		view.layout(true);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		certLocation.setFocus();
		alias.setFocus();
	}

}
