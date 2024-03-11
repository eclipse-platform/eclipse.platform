

FAQ How can Content Assist make me the fastest coder ever?
==========================================================

When extending Eclipse, plug-in authors are confronted with an overwhelming selection of API to choose from. In the good old days, books could be published with API references, and programmers could study the material and recite the proper incantations to drive the relatively simple APIs. With modern APIs, this is no longer possible. There simply is too much to read and remember. Content Assist to the rescue!

  

Content Assist can take the guesswork out of coding in a number of ways:

  

*   _Finding a given type_.

Assume that you are writing some code and want to use a button in your UI. You start typing the letter B and don't remember the rest of the word. Simply press Ctrl+Space, and the Java tooling will present you with a list of all types that start with the letter B. The list starts with Boolean. Keep typing, and the list narrows down. After typing But, you get to choose between java.awt.Button and org.eclipse.swt.widgets.Button. Choose the one you like, and the editor inserts the class _and_ inserts an import statement for the class at the same time.

  

  

*   _Finding a given field or method_.

After typing a dot after a certain expression, Content Assist will suggest all possible fields and methods applicable to the expression's result type. This functionality is very useful for discovering what operations can be applied to a given object. Combined with pervasive use of getters and setters, browsing an API is really simple. For the Button example, continuations for get show all attributes that can be obtained from a button. The ones starting with set show the attributes that can be modified. Another frequent prefix used while writing plug-ins is add to add event listeners. Having Content Assist at your fingertips definitely improves coding speed by combining intuition with content-assisted browsing.

  

*   _Entering method parameter values_.

When entering Ctrl+Space after the ( for a method call, Content Assist will provide the expected type name for each parameter. When you advance to the next parameter-by pressing a comma-the Content Assist hints move along with you. This is especially useful for overloaded methods with ambiguous signatures and for methods with many parameters.

  

*   _Overriding inherited methods_.

Invoke Content Assist when the cursor is between method declarations. Proposals will be shown for all possible methods that can be overridden from superclasses.

  

*   _Generating getters and setters_.

Between two method declarations, type get, and invoke Content Assist. Proposals will be shown for creating accessor methods for any fields in the class that do not yet have an accessor. The same applies for generating setter methods by invoking Content Assist on the prefix set.

  

*   _Creating anonymous inner classes_.

Eclipse likes loose coupling and hence works a lot with listeners that are registered on demand. The listeners implement a given interface with methods that are called when the event of interest happens. Typical usage is to declare an anonymous inner class, as in this example:

    button.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
            // do something here
        }
    });

Here is how an experienced Eclipse user might enter that code using Content Assist.

        but**<Ctrl+Space>** select 'button'
        .add**<Ctrl+Space>** select 'addSelectionListener'
        new Sel**<Ctrl+Space>** select 'SelectionAdapter'
        () { **<Ctrl+Space>** select 'widgetSelected'

  

Note that Content Assist is also available inside javadoc comments and can help when declaring fields and can assist with named local variables and method arguments. In [FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?") we explain how Content Assist is implemented in Eclipse.

  

Also note that Content Assist can be fully customized from the **Java > Editor** preference page.

  

  

See Also:
---------

[FAQ How can templates make me the fastest coder ever?](./FAQ_How_can_templates_make_me_the_fastest_coder_ever.md "FAQ How can templates make me the fastest coder ever?")

[FAQ How do I add Content Assist to my language editor?](./FAQ_How_do_I_add_Content_Assist_to_my_language_editor.md "FAQ How do I add Content Assist to my language editor?")

