

FAQ When does my language need its own perspective?
===================================================

Your language needs its own perspective only when you have a certain collection of views and editors that work together and would be cumbersome to open individually. Adding a new perspective should be done with reservation, though. If you add a new perspective and bring users there to do programming in your language, you have made many hidden assumptions about how programs are developed and in what sequence. Ask yourself, What if people want to develop programs written in my language and at the same time in Java and/or C? Will my perspective be a help or hindrance?

Think of perspectives as one of the windows in a virtual desktop manager. When you start using a virtual desktop, you like the separation of function. You use one of the windows for editing, one for reading mail, one for browsing, one for programming. This is great. You can switch between tasks with a clever keyboard shortcut or a simple mouse click. Inevitably though, you get sloppy, separation rules are broken, and all applications end up in one window. The virtual desktop ends up as a useful tool for demos, but that's about it. The same issues apply to perspectives. People like them for their separation of context and their memory of what views are chosen with their layout. However, people can manage only a limited number of perspectives and are particularly annoyed when an application rudely jumps to another perspective. Be sure to ask permission from the user, as the Debug perspective does.

Perspectives are created by using the org.eclipse.ui.perspectives extension point:

    <extension
            point="org.eclipse.ui.perspectives">
        <perspective
                name="Cool Perspective"
                icon="icons/cool.gif"
                class="org.eclipse.faq.sample.CoolPerspective"
                id="org.eclipse.faq.sample.coolPerspective">
        </perspective>
    </extension>

  

See Also:
---------

[FAQ How do I add documentation and help for my own language?](./FAQ_How_do_I_add_documentation_and_help_for_my_own_language.md "FAQ How do I add documentation and help for my own language?")

