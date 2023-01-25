---
tsn: 2
version: 2.0.0
date: 2023-01-25
authors: Moritz Moxter (moritz@clockworks.io)
status: Draft
namespace: io.clockworks
document-type: tsn@1.0.0
---

# Any Decision Record - Rev 2

## Abstract

An Any Decision Record (ADR) is a document that captures an important decision made along the process of designing and implementing with its context and consequences.

## Conformance Requirements

All diagrams, examples, and notes in this specification are non-normative, as are all sections explicitly marked non-normative. Everything else in this specification is normative.

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC2119](https://tools.ietf.org/html/rfc2119).

Requirements phrased in the imperative as part of algorithms (such as "strip any leading space characters" or "return false and abort these steps") are to be interpreted with the meaning of the key word ("MUST", "SHOULD", "MAY", etc.) used in introducing the algorithm.

### Terminology and Other Conventions

n.n.


## Introduction

An Any Decision Record (ADR) is a document that captures an important decision made along the process of designing and implementing with its context and consequences. The original idea stems from the Architectural Decision Record[^1] and was extended to be used not only for architectural decisions in a software development context. Instead it can be used to capture arbitrary decisions.

The ADR can optionally link related resources. Those could be i.e. a User Stories or an Issues in the context of software engineering.

## Specification

The ADR is written as a plain text file with markdown as markup language. This allows easy reading and writing ADRs including the use of common version control systems. An ADR MUST start with a metadata section and the chapters described in the next subsection.

Updates to an ADR MUST be limited once it left the _draft_ status. Only patch level updates are allowed. Those are only modifications that do not change the documents context and implications. Valid changes are for example fixing spelling mistakes or updating the status when an ADR was updated or deprecated. For a change that modifies the documents context and implications a new ADR MUST be created which updates the original ADR.

### ADR Metadata

The task of the metadata section is to provide a common set of attributes describing the document that follow a machine readable format. The metadata section is MANDATORY and MUST be placed at the beginning of the document in the form of _YAML Front Matter_[^2].

The following paragraphs define and explain the various attributes an ADR can have:


`adr`

:   The _adr_ attribute is an integer that identifies a ADR. It MUST be unique within a _namespace_ and is incremented for each new ADR.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<Integer\>_

    Example: &nbsp;&nbsp; `adr: 123`

`version`

:   The Version of the ADR. Used to identify various versions of the document and track changes. MUST follow the [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html) specification. It SHOULD use the `draft` keyword for prerelease versions followed by a build identifier. The build identifier is an integer that gets incremented for each new build/pre-release draft version.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<major\>.<minor\>.<patch\>[-draft.<integer\>]?_

    Example: &nbsp;&nbsp; `version: 1.0.0-draft.3`

`date`

:   The date when the ADR was originally created.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _yyyy-mm-dd_

    Example: &nbsp;&nbsp; `date: 2022-05-11`

`authors`

:   A list with all authors who worked on the ADR. This does NOT include bare commentors. An ADR MUST have at least one author but it CAN have more than one. If there is more than one author, the authors are separated by a comma (`,`).

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<first-name\>_ _<middle-names\>*_ _<last-name\>_ (_<email\>_)

    Example: &nbsp;&nbsp; `authors: Moritz Moxter (moritz@clockworks.io)`

`status`

:   The status of the ADR.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<string\>_(_ by ADR<integer\>_)?

    Example: &nbsp;&nbsp; `status: Draft` or `status: Updated by ADR456`

`namespace`

:   The namespace for which the ADR is valid. MUST follow the namespace structure used in Java. It SHOULD use the domain name of the organization or individual who created the ADR as prefix.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<string\>(_.<string\>_)*

    Example: &nbsp;&nbsp; `namespace: io.clockworks.comp`

`document-type`

:   The type of the document. SHOULD specify the version. If no version is given, the latest version is implicitly assumed.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _<string\>@_<version\>_

    Example: &nbsp;&nbsp; `document-type: adr@1.0.0`

### ADR Structure and Content

The ADR MUST use the following sections. To enrich the content it can use simple graphical elements (embedded graphics like [mermaid](https://mermaid-js.github.io/mermaid/), images, ...) which MUST be bundled with the ADR.

An ADR MUST have the following sections (if a section is optional and has no content the section itself MUST still exist!):

`Title`

:   The first level 0 headline of the document directly after the metadata section MUST be the name of the ADR.

    Format: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _# <text\>_

    Example: &nbsp;&nbsp; `# Important Decision about Cats`


`Summary`

:   (optional) High level summary of the ADR. While this section is optional it SHOULD be present to help a reader quickly get an overview of the decision.

`Context`

:   What is the issue that we're seeing that is motivating this decision or change

`Decision`

:   Clearly state the architecture’s direction—that is, the position you’ve selected.

`Explanation`

:   Explain why the decision was made. Do not duplicate on constraints, assumptions and consequences. This section can be used to fill in gaps not fully explained by those.

`Assumptions`

:   Clearly describe the underlying assumptions in the environment in which you’re making the decision—cost, schedule, technology, and so on. Note that environmental constraints (such as accepted technology standards, enterprise architecture, commonly employed patterns, and so on) might limit the alternatives you consider.

`Constraints`

:   Capture any additional constraints to the environment that the chosen alternative (the decision) might pose.

`Consequences`

:   A decision always has consequences. For example, a decision might introduce a need to make other decisions, create new requirements, or modify existing requirements; pose additional constraints to the environment; require renegotiating scope or schedule with customers; or require additional staff training. Clearly understanding and stating your decision’s implications can be very effective in gaining buy-in and creating a roadmap for architecture execution.

`Alternatives`

:   (optional) Describe potential alternatives for the decision made. This should include a hint why the alternative descision was dropped.

`Comments`

:   (optional) Section for comments of other stakeholders on the decision. Useful during the decision making process and to better understand how the decision was reached. Could be some external discussion tool like a forum instead of direct text comments.


### Status - Allowed Values and Workflow

The following values are valid for the status attribute of the ADR:

`Draft`

:   The ADR is still in the draft phase. This is the status of every newly created ADR.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _-_

    Valid Successors: &nbsp;&nbsp; _Discarded_, _Accepted_

`Discarded`

:   The ADR did not make it past draft status and is now discarded. That is, it will no longer be discussed or refined and will not get applied at any time in the future.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _Draft_

    Valid Successors: &nbsp;&nbsp; -

`Accepted`

:   The ADR was accepted.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _Draft_

    Valid Successors: &nbsp;&nbsp; _Updated by_, _Superseded by_, _Deprecated_

`Updated by`

:   The ADR is still valid but updates are available that extend or refine this ADR. The updates MUST not make the ADR obsolete or state contradicting claims. The full status including the reference follows the form `Updated by ADR<xxx>` where `<xxx>` is the number of the ADR with updates. It is possible to have more than one updating ADR. I.e. `Updated by ADR123, ADR456`. Changing the status to _Updated by_ MUST increment the patch part of the version attribute.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _Accepted_

    Valid Successors: &nbsp;&nbsp;  _Superseded by_, _Deprecated_

`Superseded by`

:   The ADR is no longer valid and another ADR makes new claims about the same facts with different implications. The status MUST reference a new ADR. The full status including the reference follows the form `Superseded by ADR<xxx>` where `<xxx>` is the number of the superseding ADR. It is possible to have more than one superseding ADR. This COULD be the case if the complexity of claims with the changes to the original ADR got to big to be represented in a single document. I.e. `Updated by ADR123, ADR456`. Changing the status to _Superseded by_ MUST increment the patch part of the version attribute.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _Accepted_, _Updated by_

    Valid Successors: &nbsp;&nbsp;  -

`Deprecated`

:   The ADR is no longer valid and no other ADR has updated or superseded this ADR. Changing the status to _Deprecated_ MUST increment the patch part of the version attribute.

    Valid Ancestors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; _Accepted_, _Updated by_

    Valid Successors: &nbsp;&nbsp;  -

The following graphic illustrates the workflow of the different status values:

``` mermaid
graph LR
  A[Draft] --> C[Accepted];
  A --> B[Discarded];
  C --> D[Updated by];
  C --> E[Superseded by];
  C --> F[Deprecated];
  D --> F;
  D --> E;
```

### Best practices

When ADRs are used in an environment with a version control system the documents SHOULD be put in the same repository as the content about which it reasons.

## Comments

## Acknowledgements

## References


[^1]: https://adr.github.io/
