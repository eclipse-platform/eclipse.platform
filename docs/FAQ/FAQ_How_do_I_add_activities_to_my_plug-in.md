

FAQ How do I add activities to my plug-in?
==========================================

You don't. An important feature of activities is that they are not defined in the plug-ins alongside the functionality they describe. Because activities are designed to solve a scaling problem when large numbers of plug-ins are used together, attempting to define them at the plug-in level would be futile. For example, let's say that you create a small handful of plug-ins that implement development tools for the PHP programming language. If you are providing only basic editing and debugging functionality, chances are that your plug-ins alone do not present a UI scalability problem. However, if someone then takes your small handful of plug-ins and combines them with 500 more plug-ins from other sources, the UI starts to become cluttered and difficult to use. If you introduced a couple of activities in your plug-in to separate different types of PHP development and if the other 500 plug-ins also introduced activities, the activities themselves would become cluttered and unusable. The moral of the story is that activities need to be defined at the level where the scalability problem exists.

  
Typically activities will be defined by an administrator or product manager who is assembling an end-user application from a large pool of plug-ins. A power user who wants to coordinate a large set of plug-ins downloaded from the Web also may establish some activities. The administrator or power user defines activities entirely declaratively in a plugin.xml file in a plug-in. No Java code is involved! Let's dig into the actual mechanics of defining activities. Before you create any activities, you need to establish one or more activity _categories_. A category is simply a container for one or more activities. Here is a simple category defined in the FAQ Examples plug-in:

        <category
            name="FAQ Category"
            id="org.eclipse.faq.faqCategory">
        </category>

_Note:_ All configuration elements in this FAQ must be contained in an extension to the org.eclipse.ui.activities extension point. The ID and name of the extension itself are not used.

  
Next, you need to define one or more activities and connect them to a category.

        <activity
        name="FAQ Activity"
        id="org.eclipse.faq.faqActivity">
        </activity>
        <categoryActivityBinding
        activityId="org.eclipse.faq.faqActivity"
        categoryId="org.eclipse.faq.faqCategory">
        </categoryActivityBinding>

Note that activities, categories, and bindings are all separate top-level elements. This allows you to define each of them in different plug-ins if desired.

After creating appropriate activities and categories, we get to the interesting part, connecting functionality to activities. An activity-pattern binding associates an activity with a regular expression that is used to match identifiers. The syntax of the regular expressions conforms to the usage defined in java.util.regex.Pattern. The identifiers describing functionality that can be filtered from the UI are of the form `<plugin-id>`/`<local-id>` or simply `<local-id>`.

When an activity is enabled, all functionality that matches the identifier patterns associated with that activity will be visible. If an identifier matches a disabled activity and does not match any enabled activity, the functionality associated with that ID will be invisible. Interpretation of exactly what `<local-id>` means is left up to the UI component that is doing the filtering, but typically it is the ID of an extension.

This all sounds very confusing, so let's look at a concrete example from the FAQ Examples plug-in:

        <activityPatternBinding
            activityId="org.eclipse.faq.faqActivity"
            pattern="org.eclipse.faq*">
        </activityPatternBinding>

This pattern binding says, when faqActivity is disabled, hide all functionality associated with any plug-in whose ID starts with org.eclipse.faq.

Let's try a slightly more complicated pattern:

    <activityPatternBinding
          activityId="org.eclipse.faq.faqWizard"
          pattern="org.eclipse.faq.examples/[a-z[.]]*addingWizard">
    </activityPatternBinding>

This pattern is much more selective. It will disable functionality only in the org.eclipse.faq.examples plug-in whose ID ends with the string addingWizard. This will disable only the addingWizard extension from the org.eclipse.faq.examples plug-in. By selecting your patterns carefully, you can define precisely which parts of the UI are filtered by a given activity.

  
To learn more about regular expressions in Java, see Jeffrey E. F. Friedl, _Mastering Regular Expressions_ (O'Reilly, 1997).

