# cpi-script-collection-framework

## Overview

The script collection provides a standardized approach to logging, error handling, notifications, and reusable components for SAP CPI iFlows. It enables developers to associate Value Maps with iFlows for consistent logging and metadata enrichment, supports multiple design patterns (batch, async, sync), and offers utility functions for enhanced logging and customization.

### Key Features

- **Groovy Script Collections:** Easy to import and use, encapsulating common scenarios and utilities for lower-code builds.
- **Centralized Core Components:** Framework-driven error handling, notifications, and integration with observability tools (cALM, Dynatrace).
- **Design Patterns & Templates:** Ready-to-import iFlow and Value Map templates.
- **Comprehensive Documentation:** API conventions, FAQs, design guidelines, SDLC checklists, and error handling best practices.
- **CI/CD Integration:** Templates and pipelines for artifact backup, config automation, and queue/topic creation.
- **Enhanced Logs:** Business-relevant metadata alongside technical logs via external Value Maps.

---

## Changelog

### v1.0.28 – SCR_Framework_V2 (21 Apr 2025)

#### Features & Improvements

- **Validation Error Fix:** Prevents repeated validation error messages; improves `error_message` header clarity.
- **Error Classification:** New `errorType` property (defaults to `TECHNICAL`, can be set to `FUNCTIONAL` before `LOG_ERROR`). Propagated to custom headers.
- **Functional Email Routing:** `functionalEmailRecipients` on value maps directs emails for `FUNCTIONAL` errors, with fallback to `emailRecipients`.
- **Validation Error Type:** Mandatory field validation errors (`LOG_AFTER_VALIDATION`, `VALIDATE_PAYLOAD`) default to `FUNCTIONAL`.
- **Soft Error Handling:** Soft errors (see below) are classified as `FUNCTIONAL`.
- **Soft Error Detection:** Auto-detects soft errors in payloads using predicates (see below).
- **Improved Attachment Logic:** More accurate log attachment eligibility using SAP runtime state.
- **Framework_API:** New class for SDK/non-standard API abstraction (e.g., DataStore, SecureStore).
- **Expanded Constants:** Greater use of constants throughout the collection.
- **Static Value Map Caching:** 5-minute TTL, drastically reducing Value Map API calls per iFlow transaction.

#### Soft Error Detection

- **Purpose:** Detects empty payloads or empty XML root nodes as "soft errors" using reusable predicates.
- **Usage:**
  - **Strict:** Use `logStrict` in your iFlow Groovy script to throw exception on soft error (to be caught by `LOG_ERROR`).
  - **Non-Strict:** Default `processData` sets properties/headers for developer handling.
- **Properties/Headers Set:**
  - `isSoftError` (boolean)
  - `softErrorReason` (string, e.g. `EMPTY_XML_ROOT`)
  - `softErrorMessage` (string)
  - `CamelHttpResponseCode` (default: 400)
  - `errorType` (`FUNCTIONAL`)
- **Extensible:** Add new soft error types by editing the ILCD code.
- **Example Predicate:**

  ```groovy
  [reason: Constants.SoftError.EMPTY_XML_ROOT, predicate: { node ->
      if (node.children().isEmpty() && node.text().trim().isEmpty()) {
          return [matched: true, message: "XML root node is empty"]
      }
      return [matched: false]
  }]
  ```

#### Static, In-Memory Cache for Value Map Metadata

- **Problem:** Excessive API calls for value map metadata (one per entry per log step).
- **Solution:** Static in-memory cache keyed by a composite of source/target/key.
- **Configurable Options:**
  - `cache_disabled`: If true, disables caching (default: false)
  - `cache_ttl_seconds`: Time-to-live for cache entries (default: 300 seconds)
  - `cache_stats_datastore_enabled`: Enables saving cache hit/miss statistics to DataStore
- **How to Set:** As message properties at runtime or as secure parameters globally (message properties take precedence).

---

### v1.0.27 – SCR_Framework_V2 (19 Mar 2025)

#### Enhancement – Configurable Payload Logging

- `loggerPayloadLogPoints`: For Interface VMs. Accepts comma-separated log points (e.g., `START,END`). If `START`, logs payload at `LOG_START` script execution. Controlled by settings below.
- `setting_allowedPayloadLogPoints`: For Framework/Global VM. Restricts payload logging to specified log points. Empty disables logging.
- `setting_payloadLogMaxLines`: Controls max lines to attach before truncation.