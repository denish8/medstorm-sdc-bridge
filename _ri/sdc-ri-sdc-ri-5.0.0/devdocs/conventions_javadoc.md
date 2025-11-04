# SDCri Javadoc conventions

This document describes conventions when writing Javadoc comments for the SDCri software.

Most of the rules defined herein are based on [this blog post](https://blog.joda.org/2012/11/javadoc-coding-standards.html), which itself is based on the [Oracle Javadoc guide](http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html). The Oracle Javadoc guide can be used as a source of information in case the reader is missing something from this document.

Each of the guidelines below consists of a short description of the rule and an explanation plus - optionally - an example.

## Write Javadoc to be read as source code

Always write Javadoc to be best read on source code level. Only a minority of developers will ever pay attention to the Javadoc HTML pages. Nevertheless, put HTML tags to the comments to facilitate well-formatted HTML output.

## Public and protected

 - All public and protected methods **shall** be fully defined with Javadoc.
 - Package and private methods **may** be defined with Javadoc.
 - If a method is overridden in a subclass, Javadoc **shall** only be present if it says something distinct from the original definition of the method.

## Use simple HTML tags, not valid XHTML

Javadoc uses HTML tags to identify paragraphs and other elements. Many developers get drawn to the thought that XHTML is necessarily best, ensuring that all tags open and close correctly. This is a mistake. XHTML adds many extra tags that make the Javadoc harder to read as source code. The Javadoc parser will interpret the incomplete HTML tag soup just fine.

## Use a single `<p>` tag between paragraphs

Longer Javadoc always needs multiple paragraphs. This naturally results in a question of how and where to add the paragraph tags. A single `<p>` tag **shall** be used on the blank line between paragraphs:

```
  /**
   * First paragraph.
   * <p>
   * Second paragraph.
   * May be on multiple lines.
   * <p>
   * Third paragraph.
   */
  public ...
```

## Use a single `<li>` tag for items in a list

Lists are useful in Javadoc when explaining a set of options, choices or issues. These standards place a single `<li>` tag at the start of the line and no closing tag:

```
  /**
   * First paragraph.
   *
   * <ul>
   * <li>the first item
   * <li>the second item
   * <li>the third item
   * </ul>
   *
   * Second paragraph.
   */
  public ...
```

## Define a punchy first sentence

The first sentence, typically ended by a dot, is used in the next-level higher Javadoc. As such, it has the responsibility of summing up the method or class to readers scanning the class or package. To achieve this, the first sentence **should** be clear and punchy, and generally short.

It is **recommended** that the first sentence is a paragraph to itself. This helps retain the punchiness for readers of the source code.

The third person form **shall** be used at the start. For example, "Gets the foo", "Sets the "bar" or "Consumes the baz". Avoid the second person form, such as "Get the foo".

## Use *this* to refer to an instance of the class

When referring to an instance of the class being documented, the term *this* **shall** be used to reference it. For example, "Returns a copy of this foo with the bar value updated".

## Aim for short single line sentences

Wherever possible, make Javadoc sentences fit on a single line. Allow flexibility in the line length, favoring 120 characters to make this work.

In most cases, each new sentence **should** start on a new line. This aids readability as source code, and simplifies refactoring re-writes of complex Javadoc.

```
  /**
   * This is the first paragraph, on one line.
   * <p>
   * This is the first sentence of the second paragraph, on one line.
   * This is the second sentence of the second paragraph, on one line.
   * This is the third sentence of the second paragraph which is a bit longer so has been
   * split onto a second line, as that makes sense.
   * This is the fourth sentence, which starts a new line, even though there is space above.
   */
  public ...
```

## Use @link and @code wisely

Many Javadoc descriptions reference other methods and classes. This can be achieved most effectively using the @link and @code features.

The @link feature creates a visible hyperlink in generated Javadoc to the target. The @link target is one of the following forms:

```
  /**
   * First paragraph.
   * <p>
   * Link to a class named 'Foo': {@link Foo}.
   * Link to a method 'bar' on a class named 'Foo': {@link Foo#bar}.
   * Link to a method 'baz' on this class: {@link #baz}.
   * Link specifying text of the hyperlink after a space: {@link Foo the Foo class}.
   * Link to a method handling method overload {@link Foo#bar(String,int)}.
   */
  public ...
```

The @code feature provides a section of fixed-width font, ideal for references to methods and class names. While @link references are checked by the Javadoc compiler, @code references are not.

@link **shall** only be used on the first reference to a specific class or method. The @code keyword **should** be used for subsequent references. This avoids excessive hyperlinks cluttering up the Javadoc.

## Never use @link in the first sentence

The first sentence is used in the higher level Javadoc. Adding a hyperlink in that first sentence makes the higher level documentation more confusing. The keyword @link **shall not** be used in the first sentence. Use @code if necessary. @link can be used from the second sentence/paragraph onwards.

## Do not use @code for null, true or false

The concepts of null, true and false are very common in Javadoc. Adding @code for every occurrence is a burden to both the reader and writer of the Javadoc and adds no real value, hence @code **shall not** be used for null, true and false.

## Use @param, @return and @throws

Almost all methods take in a parameter, return a result or both. The @param and @return features specify those inputs and outputs. The @throws feature specifies the thrown exceptions.

The @param entries **shall** be specified in the same order as the parameters. The @return **shall** be after the @param entries. The @throws **shall** be after the @return entry.

## Use one blank line before @param

There **shall** be one blank line between the Javadoc text and the first @param, @return or @throws. This aids readability in source code.

## Treat @param and @return as a phrase

The @param and @return **should** be treated as phrases rather than complete sentences. They **shall** start with a lower case letter, typically using the word *the*. They **shall not** end with a dot if be written as a single sentence. This aids readability in source code and when generated.

## Treat @throws as an if clause

The @throws feature **should** normally be followed by *if* and the rest of the phrase describing the condition. For example, `@throws IllegalArgumentException if the file could not be found`. This aids readability in source code and when generated.

## Avoid @author

The @author feature can be used to record the authors of the class. This **shall** be avoided, as it is usually out of date, and it can promote code ownership by an individual. The source control system is in a much better position to record authors.