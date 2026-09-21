<!--
Copyright (c) 2026 Rahul Arun Kaushik and others.
SPDX-License-Identifier: EPL-2.0
Contributors: 
    Christopher Gerking - initial API creation
    Rahul Arun Kaushik - initial API and implementation
-->

# QVT Operational Java Blackbox UI

The `org.eclipse.m2m.qvt.oml.bbox.ui` plug-in makes Java blackbox libraries
visible in Eclipse. It provides a project-specific dependency tree in Project
Explorer and a workspace-wide inventory view.

The UI is intended for inspecting which blackboxes QVTo can resolve, which
operations they expose, where they originate, and why a blackbox failed to
load. Discovery does not compile QVTo transformations.

## Motivation

QVTo Java blackboxes can come from project sources, dependencies, installed
plug-ins, and runtime registrations. These sources were not visible together
in the Eclipse UI, making it difficult to determine which libraries a project
could use, what operations they offered, and why loading failed. This plug-in
brings that information into Project Explorer and a global inventory view,
with Java navigation and diagnostics, without compiling transformations just
to discover blackboxes.

## Project Explorer

QVTo projects have a synthetic `QVTo Blackboxes` node:

```text
QVTo project
  QVTo Blackboxes (all visible to project)
    qualified.blackbox.Unit
      Module (operation count) - package URIs
        operation(arg : Type) : ReturnType
      diagnostic
        nested diagnostic
```

Discovery uses the selected project's Java visibility, QVTo resolution
context, and metamodel URI mappings. Unrelated workspace projects are not
included.

### Visibility Scope

The scope is configured independently for each project:

1. Right-click the QVTo project and select **Properties**.
2. Open **QVT Settings > Blackboxes**.
3. Select one of the following scopes.

| Scope | Content |
| --- | --- |
| **All Visible to Project** | Annotated Java blackboxes on the effective classpath plus descriptors enumerated by registered providers for the project context. This is the default. |
| **Project and Dependencies** | Annotated Java blackboxes in project sources, recursively visible referenced projects, and application libraries. |
| **Project Only** | Annotated Java blackboxes physically declared in the selected project. |

The setting is stored in the project's Eclipse preferences and survives a
workspace restart.

## Workspace-Wide View

Open **Window > Show View > Other... > QVT Operational > QVTo Blackboxes** to
inspect all blackboxes found in the current Eclipse instance.

The view groups results by provenance:

| Group | Meaning |
| --- | --- |
| **Workspace Projects** | Annotated blackboxes declared by accessible Java projects in the workspace. |
| **Java Libraries** | Annotated blackboxes found in binary roots and JARs visible to workspace Java projects. |
| **Eclipse Platform > Extension Contributions** | Blackboxes declared through the QVTo Java blackbox extension point, grouped by contributing bundle. |
| **Eclipse Platform > Active Plug-ins** | Annotation-based blackboxes defined by eligible active OSGi bundles. |
| **Runtime and Other Registrations** | Remaining descriptors, including standalone or built-in runtime registrations that were not attributed to an earlier origin. |

Use the view toolbar to refresh the inventory or collapse the tree. Discovery
runs in a background Eclipse Job. Replaced or canceled jobs cannot publish a
stale result.

## Discovery Semantics

Project discovery deliberately combines two registry operations:

1. JDT searches for Java types annotated with
   [`@Module`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/qvt/oml/blackbox/java/Module.java).
2. Each qualified type name is resolved through
   [`BlackboxRegistry#getCompilationUnitDescriptor(...)`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/internal/qvt/oml/blackbox/BlackboxRegistry.java).
3. **All Visible to Project** additionally calls
   [`BlackboxRegistry#getCompilationUnitDescriptors(...)`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/internal/qvt/oml/blackbox/BlackboxRegistry.java) with the project's
   [`ResolutionContext`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/internal/qvt/oml/blackbox/ResolutionContext.java).

The singular lookup resolves known Java candidates according to provider
priority and seeds providers that populate data on demand. Plural enumeration
then adds extension, standalone, and other provider-visible descriptors. The
results are deduplicated by qualified unit name for the project tree.

The workspace-wide view uses dedicated discovery phases for workspace Java
content, extension contributions, active bundles, and runtime registrations.
Descriptors are deduplicated within an origin while the same qualified name is
preserved when it genuinely comes from distinct origins.

Loaded descriptors provide the tree metadata:

- modules from [`BlackboxUnit#getElements()`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/internal/qvt/oml/blackbox/BlackboxUnit.java);
- operations from each generated QVTo module type;
- contextual receiver, parameter, and return types;
- package URIs derived from model types and operation types;
- nested diagnostics and their original severity.

The operation list represents operations exposed by the QVTo blackbox model.
For Java libraries, compatible public Java methods may become blackbox
operations; [`@Operation`](../org.eclipse.m2m.qvt.oml/src/org/eclipse/m2m/qvt/oml/blackbox/java/Operation.java) customizes operation semantics and is not simply an
inclusion filter.

## Diagnostics And Markers

Failed units remain in the tree and expose their diagnostic hierarchy. Eclipse
error overlays decorate failed units, parent groups, and project roots without
adding a redundant text suffix.

[`ProjectBlackboxDiscoveryService#validate(...)`](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/ProjectBlackboxDiscoveryService.java)
synchronizes persistent QVTo problem markers using
[`QVTOProjectPlugin.PROBLEM_MARKER`](../org.eclipse.m2m.qvt.oml.project/src/org/eclipse/m2m/internal/qvt/oml/project/QVTOProjectPlugin.java). Only leaf diagnostics create markers, which
avoids repeating parent and child messages. Feature-owned stale markers are
replaced after successful validation, while canceled discovery leaves the
previous marker set intact.

[`discoverForDisplay(...)`](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/ProjectBlackboxDiscoveryService.java)
discovers the selected scope without writing markers. Marker validation always
uses **All Visible to Project**, independently of the
narrower scope selected for display. The workspace-wide view has no project
marker side effects.

## Open Navigation

Double-click, press Enter, or use **Open** on a unit, module, or operation when
its Java type is resolvable through JDT.

- Workspace source opens in the Java editor.
- Binary dependencies open in Eclipse's class-file editor.
- An operation reveals its Java method when exactly one method has that name.
- If the method name is overloaded, the containing type opens without choosing
  an ambiguous method.
- Registry-only and platform entries without a suitable JDT navigation context
  do not enable Open.

## Architecture

The implementation is split into small internal services. The design uses
[Composite, Adapter, and Strategy concepts](https://www.informit.com/store/design-patterns-elements-of-reusable-object-oriented-software-9780201633610)
where they provide concrete benefits:

- **Composite-style traversal:** [BlackboxTreeNode](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxTreeNode.java)
  provides typed parents, read-only children, and recursive error aggregation.
  Models expose text and semantic roles; they do not create SWT images.
- **Framework adapters:** the content providers bridge Eclipse's
  [Object-based tree API](https://help.eclipse.org/latest/rtopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/jface/viewers/ITreeContentProvider.html)
  to typed nodes. A shared [label provider](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/navigator/BlackboxNavigatorLabelProvider.java)
  owns image selection and generated overlays. Adding a node with an existing
  semantic role does not require new branches in either provider.
- **Optional capabilities:** [BlackboxNavigable](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxNavigable.java)
  identifies nodes that can supply a unit/method target; [BlackboxJavaContext](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxJavaContext.java)
  supplies the project's JDT context. Status and diagnostic nodes need neither.
- **Ordered composition:** [BlackboxDiscoverySource](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/BlackboxDiscoverySource.java)
  lets the discovery facade accept sources with explicit progress allocations.
  The production order remains workspace, extensions, active bundles, then
  runtime fallback. This is a composable sequence, not a mutually exclusive
  choice of Strategies. A source can be substituted in a focused test without
  replacing Eclipse globals.
- **Existing policies:** the visibility enum remains compact; [Comparators](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html)
  supply sorting policies, and the existing settings listeners notify viewers
  when a project's effective scope changes. Extra class hierarchies would not
  improve these already small responsibilities.

| Package or component | Responsibility |
| --- | --- |
| [BlackboxTreeNode](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxTreeNode.java) | Typed traversal and error aggregation, with text/role metadata for presentation. |
| [BlackboxDiscoverySource](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/BlackboxDiscoverySource.java) | Common contract for sequential global discovery sources. |
| [discovery.ProjectBlackboxDiscoveryService](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/ProjectBlackboxDiscoveryService.java) | Orchestrates project-context discovery and optional marker synchronization. |
| [discovery.ProjectBlackboxJavaSearch](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/ProjectBlackboxJavaSearch.java) | Finds annotated Java candidates using the selected visibility policy. |
| [discovery.BlackboxDescriptorLoader](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxDescriptorLoader.java) | Loads descriptors and extracts modules, operations, package URIs, and diagnostics. |
| [discovery.BlackboxProblemMarkerSynchronizer](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/discovery/BlackboxProblemMarkerSynchronizer.java) | Owns leaf-marker creation, replacement, and cleanup. |
| [navigator](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/navigator/) | Contributes the Common Navigator tree, background jobs, labels, decorations, and Open action. |
| [settings](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/settings/) | Stores and edits the project-scoped visibility preference. |
| [global.GlobalBlackboxDiscoveryService](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/GlobalBlackboxDiscoveryService.java) | Coordinates the workspace-wide discovery phases. |
| [global.WorkspaceBlackboxDiscovery](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/WorkspaceBlackboxDiscovery.java) | Discovers workspace source and binary-library blackboxes. |
| [global.ExtensionBlackboxDiscovery](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/ExtensionBlackboxDiscovery.java) | Reads extension-point contributions. |
| [global.ActiveBundleBlackboxDiscovery](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/ActiveBundleBlackboxDiscovery.java) | Discovers blackboxes defined by eligible active OSGi bundles. |
| [global.RuntimeBlackboxDiscovery](src/org/eclipse/m2m/internal/qvt/oml/bbox/ui/global/RuntimeBlackboxDiscovery.java) | Adds remaining registry and standalone descriptors. |

Model construction, registry access, JDT search, descriptor loading, and marker
work run outside the UI thread. SWT/JFace publication is performed on the UI
thread after cancellation, generation, and disposal checks.

## Eclipse Integration

[plugin.xml](plugin.xml) registers the project tree through
[navigator content](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_ui_navigator_navigatorContent.html)
and [viewer binding](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_ui_navigator_viewer.html),
the global view through [views](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_ui_views.html),
and project settings through [property pages](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_ui_propertyPages.html).
The QVTo [Java blackbox extension schema](../org.eclipse.m2m.qvt.oml/schema/javaBlackboxUnits.exsd)
defines the registrations inspected by extension discovery.

The [manifest](META-INF/MANIFEST.MF) declares runtime dependencies and internal
packages. The bundle is included in the [QVTo JDT feature](../../features/org.eclipse.m2m.qvt.oml.editor.jdt-feature/feature.xml);
the [stable Tycho build](../../releng/org.eclipse.qvto.releng.tycho/pom.xml)
packages it into the QVTo SDK repository.

## Refresh Behavior

Project results are invalidated for inputs that can affect blackbox discovery,
including Java source or class changes, classpath changes, workspace JARs,
`MANIFEST.MF`, `plugin.xml`, QVTo sources, metamodels, and metamodel URI
mappings. Dependent QVTo projects are refreshed transitively and cycle-safely.

Changes to external files that Eclipse has not detected may require refreshing
the corresponding workspace resource.

## Performance

The global active-bundle phase uses PDE's resolved bundle state to select the
QVTo core bundle and its direct and indirect dependents. It then scans only
active candidates that can see the QVTo `@Module` annotation through normal
OSGi wiring. If PDE state is unavailable, selection falls back to all installed
bundles so discovery remains complete. A defining-bundle check prevents an
imported class from being attributed to every bundle that can load it.
Buddy-only visibility is excluded because it caused costly scans of unrelated
bundles without producing owned blackboxes.

The core OSGi provider enumerates recursive class resources local to each
selected bundle (including attached fragments), rather than rescanning classes
from required bundles and imported packages. Explicit named imports remain on
the singular `bundle.loadClass(...)` path and do not require `@Module`.

The origin hierarchy is retained because profiling identified provider-driven
bundle scanning, rather than tree rendering or grouping, as the dominant cost.
The implementation therefore avoids a time-only cache, parallel class loading,
and a more complex deferred-tree framework.

## Tests

Focused tests are located under:

```text
tests/org.eclipse.m2m.tests.qvt.oml.ui/src/
  org/eclipse/m2m/internal/tests/qvt/oml/ui/blackbox/
```

[AllBlackboxTests](../../tests/org.eclipse.m2m.tests.qvt.oml.ui/src/org/eclipse/m2m/internal/tests/qvt/oml/ui/blackbox/AllBlackboxTests.java) is the nested JUnit 4 suite for this plug-in. It covers:

- descriptor loading, generated operations, signatures, and package URIs;
- nested and failed diagnostics;
- candidate identity and deterministic deduplication;
- project visibility policies and persisted per-project settings;
- exported and non-exported transitive project and JAR dependencies;
- problem marker ownership, leaf creation, cleanup, and cancellation;
- active-bundle candidate selection, ownership, and extension-name resolution;
- global origin identity, cancellation, and stale-generation prevention;
- resource-change classification and user-facing failure labels;
- typed tree traversal, parent mapping, sorting, and presentation extensibility;
- source composition, per-run state, and descriptor cancellation propagation.

Run `AllBlackboxTests` as a **JUnit Plug-in Test** from the
`org.eclipse.m2m.tests.qvt.oml.ui` test bundle. The suite currently contains 53
test methods.

To compile and package the stable reactor from the repository root:

```shell
mvn -f releng/org.eclipse.qvto.releng.tycho/pom.xml \
  -Pstable -DskipTests package
```

## API Status

The bundle is internal to QVTo and does not define a public API.
