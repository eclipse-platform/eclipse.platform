package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceRanking;

@Component(property = { "filtervalue=Test" })
@ServiceRanking(1)
public class FilterServiceA implements TestService {

}
