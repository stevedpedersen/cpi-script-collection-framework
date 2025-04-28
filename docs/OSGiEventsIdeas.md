# OSGi Events in SCR Framework: Practical Use Cases & Examples

This document collects advanced, real-world ways to use OSGi events in your CPI SCR Framework, with code examples using the new `Framework_API.fireOSGiEvent` and `registerOSGiEventHandler` methods.

---

## 1. Centralized Audit/Event Trail
**Pattern:** Decouple audit logging from your main flows. Capture all errors, warnings, or business events in one place—even if the iFlow terminates early.

**Integration Point:**
```groovy
// In Framework_Logger.groovy or Framework_ExceptionHandler.groovy
Framework_API.fireOSGiEvent("SCR.Audit", [
    messageId: message.getProperty("SAP_MessageProcessingLogID"),
    eventType: "ERROR",
    errorMessage: logEntry.text,
    timestamp: System.currentTimeMillis()
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.Audit") { event ->
    saveAuditEvent(event.properties)
}
```

---

## 2. Dynamic Feature Toggles / Runtime Configuration
**Pattern:** Change logging level, enable/disable features, or update value maps at runtime—across all iFlows—without redeployment.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.ConfigUpdate", [
    configKey: "logLevel",
    newValue: "DEBUG"
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.ConfigUpdate") { event ->
    if (event.properties.configKey == "logLevel") {
        GLOBAL_LOG_LEVEL.value = event.properties.newValue
    }
}
```

---

## 3. Cross-iFlow or Cross-Process Correlation
**Pattern:** Coordinate compensation, rollback, or notification logic across multiple iFlows (e.g., after a soft error in one, trigger a cleanup in another).

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.Correlation", [
    correlationId: message.getProperty("OrderID"),
    eventType: "SoftError",
    details: "Order validation failed"
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.Correlation") { event ->
    if (event.properties.eventType == "SoftError") {
        triggerCompensation(event.properties.correlationId)
    }
}
```

---

## 4. Asynchronous Notification/Alerting
**Pattern:** Send notifications (email, SMS, webhook) on critical events without blocking your main process.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.Alert", [
    alertType: "ValidationFailure",
    payload: message.getBody(String),
    timestamp: System.currentTimeMillis()
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.Alert") { event ->
    sendAlert(event.properties.alertType, event.properties.payload)
}
```

---

## 5. Custom Metrics and Health Monitoring
**Pattern:** Emit metrics for monitoring (e.g., error rates, processing time, validation pass/fail) to a dashboard.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.Metric", [
    metric: "SoftErrorCount",
    value: 1,
    timestamp: System.currentTimeMillis()
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.Metric") { event ->
    incrementMetric(event.properties.metric, event.properties.value)
}
```

---

## 6. Distributed Cache Invalidation
**Pattern:** When a value map or config changes in one iFlow, invalidate cached values in all others.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.CacheInvalidation", [
    cacheKey: "ValueMap:MyMap"
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.CacheInvalidation") { event ->
    CACHE.invalidate(event.properties.cacheKey)
}
```

---

## 7. Distributed Transaction/Workflow Coordination
**Pattern:** Orchestrate multi-step, multi-iFlow processes (e.g., saga pattern) by emitting and listening for workflow events.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.WorkflowStep", [
    sagaId: "SAGA-123",
    step: "PaymentCompleted"
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.WorkflowStep") { event ->
    if (event.properties.step == "PaymentCompleted") {
        continueSaga(event.properties.sagaId)
    }
}
```

---

## 8. Real-time Debug/Trace Hook
**Pattern:** Enable deep, on-demand tracing in production by firing trace events only when a debug flag is set.

**Integration Point:**
```groovy
if (DEBUG_ENABLED) {
    Framework_API.fireOSGiEvent("SCR.Trace", [
        tracePoint: "beforeValidation",
        payload: message.getBody(String)
    ])
}
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.Trace") { event ->
    logTrace(event.properties.tracePoint, event.properties.payload)
}
```

---

## 9. Automated Test/Replay Hooks
**Pattern:** Capture events during production runs for later automated replay/testing.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.TestReplay", [
    step: "validation",
    input: message.getBody(String),
    timestamp: System.currentTimeMillis()
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.TestReplay") { event ->
    saveForReplay(event.properties)
}
```

---

## 10. Self-Healing/Auto-Remediation
**Pattern:** When a recurring error is detected, fire an event that triggers a self-healing script or iFlow.

**Integration Point:**
```groovy
Framework_API.fireOSGiEvent("SCR.SelfHeal", [
    errorType: "DBConnectionLost",
    context: ...
])
```
**Handler Example:**
```groovy
Framework_API.registerOSGiEventHandler("SCR.SelfHeal") { event ->
    attemptRemediation(event.properties.errorType, event.properties.context)
}
```

---

## Summary Table

| Use Case                       | Event Topic             | Example Handler Action                 |
|--------------------------------|------------------------|----------------------------------------|
| Centralized Audit Trail         | SCR.Audit              | Write to DB/file/API                   |
| Dynamic Config/Feature Toggle   | SCR.ConfigUpdate       | Update in-memory config                |
| Cross-iFlow Correlation         | SCR.Correlation        | Trigger compensation/cleanup           |
| Async Notification/Alerting     | SCR.Alert              | Send email/SMS/webhook                 |
| Custom Metrics/Health           | SCR.Metric             | Push to dashboard                      |
| Distributed Cache Invalidation  | SCR.CacheInvalidation  | Invalidate local cache                 |
| Workflow Coordination (Saga)    | SCR.WorkflowStep       | Continue/rollback workflow             |
| Real-time Debug/Trace           | SCR.Trace              | Log or stream trace info               |
| Automated Test/Replay           | SCR.TestReplay         | Save for replay/testing                |
| Self-Healing/Auto-Remediation   | SCR.SelfHeal           | Run remediation logic                  |

---

## Advanced Tips
- You can register for multiple topics at once by passing a list to `registerOSGiEventHandler`.
- Use event properties to pass context, correlation IDs, or payloads.
- Event handlers can be registered/unregistered dynamically at runtime.
- Combine with DataStore or external APIs for persistent, distributed state.
- Use OSGi events for "fire-and-forget" side effects, not for synchronous control flow in CPI scripts.

---

**With these patterns, your SCR Framework is ready for robust, decoupled, event-driven extensions in CPI and beyond!**
