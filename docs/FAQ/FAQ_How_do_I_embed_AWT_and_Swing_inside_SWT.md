

FAQ How do I embed AWT and Swing inside SWT?
============================================

In Eclipse 3.0, APIs have been introduced for integrating AWT and Swing with SWT. This support is product-quality on Windows and has only early access support on Linux under JDK 1.5. The main entry point for AWT integration is the class SWT_AWT. It provides a factory method, new_Frame, that creates an AWT Frame that is parented within an SWT Composite. From there, you can create whatever AWT components you want within that frame. The bridging layer created by SWT_AWT handles forwarding of SWT events to the corresponding AWT events within the frame.

Articles
--------

*   [Swing/SWT Integration](https://www.eclipse.org/articles/article.php?file=Article-Swing-SWT-Integration/index.html) Article on eclipse.org

See Also:
---------

*   [FAQ Is SWT better than Swing?](./FAQ_Is_SWT_better_than_Swing.md "FAQ Is SWT better than Swing?")
*   [Albireo](https://www.eclipse.org/albireo/) Albireo is an Eclipse Technology project (in incubation phase) that simplifies the task of combining user interface components from the Swing and SWT toolkits. It builds on SWT's standard SWT_AWT bridge, implementing much of the tricky code that is currently left to the developer.
*   [DJ project](http://djproject.sourceforge.net/) The other way around. The DJ Project lets you embed SWT components in Swing.

