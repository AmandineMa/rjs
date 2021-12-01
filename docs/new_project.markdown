---
layout: default
title: Create a new project using RJS
permalink: /create/
nav_order: 2
---

# Create a new project using RJS and Eclipse

To create a new project using RJS and Eclipse, **File > New  > Jason Project**.

Then, two possibilities: 
- to work with the rjs jar, or 
- to work with the Eclipse project (to be able to use the debug tool for example, or when frequently bringing modifications to RJS). 

### To work with the rjs jar

**Right click on the project > Build Path > Configure Build Path** and then go on the **Libraries tab > Add Jars > Select the rjs jar**

### To work with the Eclipse project

<div class="code-example" markdown="1">
**Prerequisites**

Having RJS added as project in Eclipse → see [ the "how to"](/howto/)
</div>

**Right click on the project > Build Path > Configure Build Path** and then go on the **Project tab > Add > rjs**