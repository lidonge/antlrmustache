这里是更新后的README文件：

README
--------

# Mustache-Java-Tool

A Java implementation tool that uses ANTLR to parse and generate Mustache templates.

## Features:

1. **ANTLR-based Mustache parsing**: We use ANTLR to correctly and efficiently parse and generate Mustache templates.
2. **Code simplicity**: Our codebase is designed to be simple and easy to understand, making maintenance and extension easier.
3. **Recursive section support**: Our tool supports recursive Mustache sections, allowing you to easily handle complex data structures.
4. **Map-to-List conversion**: We provide a special feature that allows Map objects to be treated as List objects in Mustache templates. This is achieved by using the `{{@sectionName}}` syntax, where `sectionName` returns a Map object.
5. **Embedded Expression tool**: Provied expression tool is a powerful tool for parsing and evaluating mathematical expressions. It leverages ANTLR4 to provide robust support for arithmetic operations, logical expressions, conditional expressions, function calls, and more. 

## Features of embedded Expression tool

- **Arithmetic Operations**: Supports addition, subtraction, multiplication, and division.
- **Logical Expressions**: Handles logical operations like AND, OR, and NOT.
- **Conditional Expressions**: Allows conditional expressions with '?' clauses.
- **Function Calls**: Supports simple function calls with arguments.
- **Qualified Names**: Handles variable names in the form of `qualifiedName`.
- **Multiline Support**: Parses and evaluates multiple expressions separated by semicolons.

## Grammar of Expression

The `MultiExpr.g4` file defines the syntax and rules for parsing expressions. It includes:

- Arithmetic operators: `+`, `-`, `*`, `/`
- Logical operators: `&&`, `||`, `!`
- Compare operations: '==' | '!=' | '<' | '>' | '<=' | '>=';
- Conditional expressions: `? :`
- Function calls: `func(arg1, arg2, ...)`
- Variable names with support for qualified names.

## Basic Usage:

To get started with Mustache-Java-Tool, follow these steps:

1. Create an instance of `MustacheCompiler` and pass your Mustache template file to it.
2. Call the `compile()` method on the compiler instance to generate Java code from the Mustache template.
3. Use the generated Java code to write output to a StringBuffer.

Here's an example:
```java
// Create MustacheCompiler instance
MustacheCompiler mustacheCompiler = new MustacheCompiler(mustacheFile);

// Compile Mustache template
MustacheListenerImpl impl = mustacheCompiler.compile();

// Create StringBuffer for output
StringBuffer sb = new StringBuffer();

//Create MustacheWriter
MustacheWriter writer = new MustacheWriter();

// Write output to StringBuffer
writer.write(sb, new ArrayList<>(), root, impl.getTemplate(), BaseSection.SectionType.Normal);

// Print output
System.out.println(sb);
```

## Recursive Example:

Suppose we have a complex data structure represented by the following Java class:
```java
class Component{
    int id;
    List<Component> children = new ArrayList<>();

    public Component(int id) {
        this.id = id;
    }

    public Component add(Component child){
        children.add(child);
        return this;
    }

    @Override
    public String toString() {
        return "Component{" +
                "id=" + id +
                ", children=" + children +
                '}';
    }
}
```
We can use the following Mustache template to recursively render the tree structure:
```mustache
{{id}}{{# children}}
  {{id}}({{-index}}){{^-last}},{{/-last}} {{*children}}{{/children}}
{{/children}}
```
This template uses the `{{#children}}` section to iterate over the list of child components, and the `{{*children}}` syntax to include a sub-template for each child. The `{{-index}}` and `{{^-last}},{{/-last}}` syntax are used to display the index and separator for each child.

## Explanation:

The `{{*children}}` syntax represents a recursive call to render the tree structure. This means that when Mustache encounters `{{*children}}`, it will start rendering the component's children, and recursively repeat this process for each child until all children have been rendered.

For example, if we have the following data:
```java
Component root = new Component(1);
root.add(new Component(2).add(new Component(3).add(new Component(6))).add(new Component(5)));
root.add(new Component(4));
```
The Mustache template will output:
```
1
    2(0), 
	    3(0), 
		    6(0) 
	    5(1) 
    4(1)
```
This shows how the recursive call to `{{*children}}` allows us to render the entire tree structure in a single pass.

## Expression Example
```javascript
val=obj_getVar("id");
text="id is " + val;
val == 1 ? text : ""
```
1. `val = obj_getVar("id");`

   This line of code retrieves the value of the variable named `"id"` and assigns it to the variable `val`.

2. `text = "id is " + val;`

   This line of code creates a new string that contains the text `"id is "` followed by the value of `val`. The result is a string that includes the current value of `"id"`.

3. `val == 1 ? text : ""`

   This line of code checks whether the condition `val == 1` is true. If it is, then the value of `text` (which is `"id is 1"`) is returned. If not, then nothing is returned.

In this case, when evaluating the condition `val == 1`, if `val` does not equal `1`, then the expression after it (i.e., `text`) will not be executed, and instead, undefined or nothing at all will be returned.
And you can use {{%}} to include it in the template as:
```mustache
{{% val=obj_getVar("id");text="id is " + val;val == 1 ? text : ""}}
```
Here's an example to extend fuctions:
```java
// Create MustacheCompiler instance
MustacheCompiler mustacheCompiler = new MustacheCompiler(mustacheFile);

// Compile Mustache template
MustacheListenerImpl impl = mustacheCompiler.compile();

// Create StringBuffer for output
StringBuffer sb = new StringBuffer();

//Create MustacheWriter
MustacheWriter writer = new MustacheWriter();
writer.getExprEvaluator().setEnvironment(new DefaultEnvironment(){
@Override
public void addDefault() {
     super.addDefault();
     addFunction("str_replace", args -> ((String) args[0]).replace((String) args[1], (String)args[2]));
     }
});
// Write output to StringBuffer
writer.write(sb, new ArrayList<>(), root, impl.getTemplate(), BaseSection.SectionType.Normal);

// Print output
System.out.println(sb);
```