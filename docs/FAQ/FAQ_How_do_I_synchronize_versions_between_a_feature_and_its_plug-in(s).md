

FAQ How do I synchronize versions between a feature and its plug-in(s)?
=======================================================================

A version can be forced from a feature into its dependent plug-ins and fragments as follows.

  

*   Open the Manifest Editor on the feature.xml file.

*   Enter a new version, making it a higher number than the old version.

*   Click **Versions...**.

*   Choose **Force feature version into plug-in and fragment manifests**.

*   Save the feature.xml file.

To build the feature, we strongly advise that you create an update site project, add and publish this feature to it, and click the **Build All...** button. (Press F1 in the Manifest Editor for more online help.)

  

  

See Also:
---------

[FAQ How do I create an update site?](./FAQ_How_do_I_create_an_update_site.md "FAQ How do I create an update site?")

