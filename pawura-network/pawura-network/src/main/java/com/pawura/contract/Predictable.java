package com.pawura.contract;

import com.pawura.model.Location;
import com.pawura.model.Prediction;

import java.util.List;

/**
 * Predictable – implemented by any service that can generate movement predictions
 * based on a list of historical locations.
 */
public interface Predictable {
    /**
     * Generate a movement prediction given a sequence of past locations.
     *
     * @param history  ordered list of past locations (oldest → newest)
     * @return         a Prediction object with confidence score and notes
     */
    Prediction predict(List<Location> history);
}
