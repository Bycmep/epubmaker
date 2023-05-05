# epubmaker

A utility to create file structure and boilerplate files for EPUB format electronic books.

The source book is a single XHTML file, main.xhtml, contained in the source directory
along with any media files used in the project.
It is broken down to chapters/parts according to inner markup which uses HTML comments
(<!---->) in order to stay hidden while editing and previewing the main XHTML file.

There are added features like ability to:

- add footnotes which are placed in a separate file and opened with hyperlink, and have a
link back to their call location (not all readers can go back). Footnotes content goes to
a special notes.xhtml file,

- add pictures which are presented by an icon and fully opened in a new page
(thought it may be a good idea),

- automatically scale pictures' width relative to screen width (must specify image width
that corresponds to 100%),

- do kepub formatting (kobo addition to the epub format)

Look at the test book in "test" folder for details on the markup.

__Usage__: epubmaker input_folder [-nocr -kepub]

Output folder name will be created automatically by adding "_out" to the name of the input one.

-nocr: do not output line breaks (ebook readers don't need them, but don't expect
big space savings)

-kepub: turns on kepub formatting (takes space and is useless anywhere but Kobo reader)
