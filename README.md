# barb

I needed a friendly tool to replace multiline piece of text in many different files organized in a directory tree.  
No complicated command line, the searched pattern and the replacement text are specified as files.  
It is written in Java, of course it could have been written in Perl or Python 🤷.  
The binary is 80Kb large, compiled with Java 8 and is directly executable (no need for `$java -jar ...`) although it requires Java to be installed.  

~~~
Usage: barb [OPTION]... <PATTERN_FILE> <REPLACEMENT_FILE> <TARGET_FILE>...
Search and replace text with support of multi-lines pattern (literal or
PCRE regular expression).

If a regular expression is provided then:
  - the MULTILINE flag is used by default, see
<https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#MU
LTILINE>.
  - the DOTALL flag is used by default, see
<https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#DO
TALL>.

Options:
 -h   Help
 -r   Use PATTERN_FILE as a regular expression instead of a literal.
 -u   Disable default flags (DOTALL and MULTILINE). They can be
      individually enabled in the expression with (?s) for DOTALL, and
      (?m) for MULTILINE.
 -v   Verbose output: print matches and full error traces.
~~~
