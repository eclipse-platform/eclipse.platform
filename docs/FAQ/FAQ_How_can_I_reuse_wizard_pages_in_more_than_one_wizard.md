

FAQ How can I reuse wizard pages in more than one wizard?
=========================================================

The wizard framework is designed to allow for very loose coupling between a wizard page and its containing wizard. Just as a wizard interacts with an abstraction of its container via IWizardContainer, a wizard page can interact with the wizard it is part of, using the generic IWizard interface. This allows the page to act as a reusable piece that can be incorporated into different concrete wizards.

Of course, it is up to the page implementer to ensure that tighter coupling does not creep in. Some wizard page implementations pass a reference to a concrete wizard in the page constructor, thus making it difficult or impossible to reuse the page in a different wizard. To avoid this, try to keep each wizard page as generic as possible. A page is really only a mechanism for gathering information from the user. The page should simply expose accessors for the information it collects, allowing the concrete wizard to pull together all the information and use it for that wizard's ultimate goal.

See Also:
---------

*   [FAQ Can I reuse wizards from other plug-ins?](./FAQ_Can_I_reuse_wizards_from_other_plug-ins.md "FAQ Can I reuse wizards from other plug-ins?")

