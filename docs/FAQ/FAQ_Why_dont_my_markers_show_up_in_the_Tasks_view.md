

FAQ Why don't my markers show up in the Tasks view?
===================================================

The Tasks view, also called the task list, shows all markers of type org.eclipse.core.resources.taskmarker, as well as any marker type that declares taskmarker as a supertype. Prior to Eclipse 3.0, the task list also showed markers of type problemmarker. In 3.0, problem markers appear in the Problems view instead. If you define a custom marker with one of these types, it will appear in either the Tasks or Problems view automatically. Note that if you don't see your marker there, it might have been filtered out. Check the filter dialog to ensure that your marker type is selected.

