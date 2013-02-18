ParseTreeApplication
====================
Java application to display and compare parse trees.  This doesn't actually run parse text, it only renders the XML or labeled bracketing.

It's intended for natural language parses and may not work well for code parses.  It can export to eps, png, jpg, and svg.

Originally I wrote this because a friend had a set of 100 parses of the same sentence (it was short too).  It happened because the grammar had some duplicate rules and those rules led to 
lots and lots of parses.  This tool helped at least see where in the parse tree those ambiguities were.  Since then, numerous parse tree drawing applications have come about, even in LaTeX!  
So I've predominantly stopped working on this project.