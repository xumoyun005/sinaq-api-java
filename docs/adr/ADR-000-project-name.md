# ADR-000: Project name "Sinaq"

**Status:** Accepted   **Date:** 2026-08-12

## Context
Original working name "Xumo" collides with Xumo, LLC — a Comcast/Charter streaming
brand (xumo.com) — creating trademark risk for a global OSS project, and the
io.xumo groupId would require owning xumo.io.

## Decision
Rename to **Sinaq** (Uzbek: "test"). Coordinates: groupId `uz.sinaq`
(domain `sinaq.uz`), packages `io.sinaq.api.*`, artifacts `sinaq-api-*`,
exceptions `Sinaq*Exception`.

## Evidence at decision time
PyPI `sinaq` and `sinaq-api` free; npm free; no software company/framework found
under this name (only an abandoned empty Launchpad test entry). Registry-level
check, not formal trademark clearance.

## Action items
1. [ ] Register sinaq.io (fallback: publish under io.github.<username>)
2. [ ] Create GitHub org "sinaq"
3. [ ] Reserve PyPI/NuGet names with 0.0.1 placeholders before V1
