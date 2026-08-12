# ADR-014: Dependency-free logging shim in core

**Status:** Accepted   **Date:** 2026-08-12

## Context
Spec §28 requires a logging abstraction and forbids logging sensitive data.
The original plan listed SLF4J-api as a core dependency, but core has zero
compile-scope dependencies so far, which simplifies user classpaths and keeps
dependency analysis clean.

## Decision
Core uses an internal minimal shim (io.sinaq.api.internal.log.SinaqLog) with a
pluggable sink (default: System.err). An SLF4J bridge will be provided as an
adapter/starter concern, not a core dependency. Masking is mandatory before any
value reaches SinaqLog or a ReportEvent payload.

## Consequences
+ core remains dependency-free through Phase 2
+ users without SLF4J get sane default behaviour
- SLF4J users must attach the bridge (starter will do it automatically)
