package com.pawura.core;

import com.pawura.contract.Predictable;
import com.pawura.model.Location;
import com.pawura.model.Prediction;

import java.util.List;

/**
 * AbstractPredictionModel – ABSTRACTION base for all prediction algorithms.
 * Defines the template method pattern:
 *   1. validate()    – subclass may override
 *   2. preProcess()  – subclass may override
 *   3. compute()     – ABSTRACT, subclass must implement
 *   4. postProcess() – subclass may override
 */
public abstract class AbstractPredictionModel implements Predictable {

    private final String modelName;
    private final String version;

    protected AbstractPredictionModel(String modelName, String version) {
        this.modelName = modelName;
        this.version   = version;
    }

    // ── Template Method ───────────────────────────────────────────────────────
    @Override
    public final Prediction predict(List<Location> history) {
        if (!validate(history)) {
            return buildFallbackPrediction("Insufficient data for prediction.");
        }
        List<Location> processed = preProcess(history);
        Prediction result        = compute(processed);
        return postProcess(result);
    }

    // ── Steps (override as needed) ────────────────────────────────────────────

    /** Returns false if history is too short or null. */
    protected boolean validate(List<Location> history) {
        return history != null && history.size() >= getMinimumHistorySize();
    }

    /** Hook: filter or smooth the location history before computing. */
    protected List<Location> preProcess(List<Location> history) {
        return history; // default: pass through
    }

    /** Core algorithm – MUST be implemented by subclasses. */
    protected abstract Prediction compute(List<Location> history);

    /** Hook: round confidence, add notes, etc. */
    protected Prediction postProcess(Prediction prediction) {
        prediction.setAlgorithm(modelName + " v" + version);
        return prediction;
    }

    /** Subclasses declare how many data points they need. */
    protected abstract int getMinimumHistorySize();

    // ── Helpers ───────────────────────────────────────────────────────────────
    protected Prediction buildFallbackPrediction(String reason) {
        Prediction p = new Prediction();
        p.setConfidenceScore(0.0);
        p.setNotes("⚠ " + reason);
        p.setAlgorithm(modelName + " v" + version);
        return p;
    }

    public String getModelName() { return modelName; }
    public String getVersion()   { return version; }
}
