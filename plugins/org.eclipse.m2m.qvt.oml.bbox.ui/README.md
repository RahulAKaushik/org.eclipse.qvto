# QVT Operational Java Blackbox UI

The `org.eclipse.m2m.qvt.oml.bbox.ui` plug-in makes Java blackbox libraries
visible in Eclipse. It provides a project-specific dependency tree in Project
Explorer and a workspace-wide inventory view.

The UI is intended for inspecting which blackboxes QVTo can resolve, which
operations they expose, where they originate, and why a blackbox failed to
load. Discovery does not compile QVTo transformations.

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
   `org.eclipse.m2m.qvt.oml.blackbox.java.Module`.
2. Each qualified type name is resolved through
   `BlackboxRegistry#getCompilationUnitDescriptor(...)`.
3. **All Visible to Project** additionally calls
   `BlackboxRegistry#getCompilationUnitDescriptors(...)` with the project's
   `ResolutionContext`.

The singular lookup resolves known Java candidates according to provider
priority and seeds providers that populate data on demand. Plural enumeration
then adds extension, standalone, and other provider-visible descriptors. The
results are deduplicated by qualified unit name for the project tree.

The workspace-wide view uses dedicated discovery phases for workspace Java
content, extension contributions, active bundles, and runtime registrations.
Descriptors are deduplicated within an origin while the same qualified name is
preserved when it genuinely comes from distinct origins.

Loaded descriptors provide the tree metadata:

- modules from `BlackboxUnit#getElements()`;
- operations from each generated QVTo module type;
- contextual receiver, parameter, and return types;
- package URIs derived from model types and operation types;
- nested diagnostics and their original severity.

The operation list represents operations exposed by the QVTo blackbox model.
For Java libraries, compatible public Java methods may become blackbox
operations; `@Operation` customizes operation semantics and is not simply an
inclusion filter.

## Diagnostics And Markers

Failed units remain in the tree and expose their diagnostic hierarchy. Eclipse
error overlays decorate failed units, parent groups, and project roots without
adding a redundant text suffix.

Project discovery synchronizes persistent QVTo problem markers using
`QVTOProjectPlugin.PROBLEM_MARKER`. Only leaf diagnostics create markers, which
avoids repeating parent and child messages. Feature-owned stale markers are
replaced after successful validation, while canceled discovery leaves the
previous marker set intact.

Marker validation always uses **All Visible to Project**, independently of the
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

The implementation is split into small internal services:

| Package or component | Responsibility |
| --- | --- |
| `discovery.ProjectBlackboxDiscoveryService` | Orchestrates project-context discovery and optional marker synchronization. |
| `discovery.ProjectBlackboxJavaSearch` | Finds annotated Java candidates using the selected visibility policy. |
| `discovery.BlackboxDescriptorLoader` | Loads descriptors and extracts modules, operations, package URIs, and diagnostics. |
| `discovery.BlackboxProblemMarkerSynchronizer` | Owns leaf-marker creation, replacement, and cleanup. |
| `navigator` | Contributes the Common Navigator tree, background jobs, labels, decorations, and Open action. |
| `settings` | Stores and edits the project-scoped visibility preference. |
| `global.GlobalBlackboxDiscoveryService` | Coordinates the workspace-wide discovery phases. |
| `global.WorkspaceBlackboxDiscovery` | Discovers workspace source and binary-library blackboxes. |
| `global.ExtensionBlackboxDiscovery` | Reads extension-point contributions. |
| `global.ActiveBundleBlackboxDiscovery` | Discovers blackboxes defined by eligible active OSGi bundles. |
| `global.RuntimeBlackboxDiscovery` | Adds remaining registry and standalone descriptors. |

Model construction, registry access, JDT search, descriptor loading, and marker
work run outside the UI thread. SWT/JFace publication is performed on the UI
thread after cancellation, generation, and disposal checks.

## Refresh Behavior

Project results are invalidated for inputs that can affect blackbox discovery,
including Java source or class changes, classpath changes, workspace JARs,
`MANIFEST.MF`, `plugin.xml`, QVTo sources, metamodels, and metamodel URI
mappings. Dependent QVTo projects are refreshed transitively and cycle-safely.

Changes to external files that Eclipse has not detected may require refreshing
the corresponding workspace resource.

## Performance

The global active-bundle phase scans only active bundles that can see the QVTo
`@Module` annotation through normal OSGi wiring. A defining-bundle check prevents
an imported class from being attributed to every bundle that can load it.
Buddy-only visibility is excluded because it caused costly scans of unrelated
bundles without producing owned blackboxes.

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

`AllBlackboxTests` is the nested JUnit 4 suite for this plug-in. It covers:

- descriptor loading, generated operations, signatures, and package URIs;
- nested and failed diagnostics;
- candidate identity and deterministic deduplication;
- project visibility policies and persisted per-project settings;
- exported and non-exported transitive project and JAR dependencies;
- problem marker ownership, leaf creation, cleanup, and cancellation;
- active-bundle ownership and extension-name resolution;
- global origin identity, cancellation, and stale-generation prevention;
- resource-change classification and user-facing failure labels.

Run `AllBlackboxTests` as a **JUnit Plug-in Test** from the
`org.eclipse.m2m.tests.qvt.oml.ui` test bundle. The suite currently contains 41
test methods.

To compile and package the stable reactor from the repository root:

```shell
mvn -f releng/org.eclipse.qvto.releng.tycho/pom.xml \
  -Pstable -DskipTests package
```

## API Status

The bundle is internal to QVTo and does not define a public API.
