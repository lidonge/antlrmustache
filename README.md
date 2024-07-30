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