# Products

## Product Assistant

To add a new product run the product assistant with `bb clci product add` and follow the steps. Each step is explained in detail in the following:

### General Steps

These steps apply for all products independend of their type or the chosen product template.

#### Type

The Product Type influences the behaviour of some automations like release creation.

- **Application**: Intended for a Product that builds as runnable artifact.
- **Library**: A product that is intended to be used by other Products (internal or external) as library.
- **Other**: Everything else that does not fit to the two types above.

#### Template

A Product template defines how the base of the new product will look like. When choosing the _Clojure_ the assistant will for example create a basic directory structure and configuration like a `deps.edn` file.

#### Name

The name of the product.

#### Root

The root directory relative to the repository where the product will be located. Default is `./<product-name>/`.
A special case of the product root is a repository with a single product and a product root `.`. In this case the repository root and the product root are equivalent. This should only be used when the repository implements a single product per repo approach.

#### Key

A key to uniquely identify the product. Defaults to the name of the product as keyword.

#### Release Creation

Determines if the product can be included in the automatic release creation using the release tools of clci.

### Template Specific Steps

Depending on the choosen template, the next steps are specific to the choosen template.