/*
 * This file is part of rivet-extras - https://github.com/florianreuth/rivet-extras
 * Copyright (C) 2026 Florian Reuth <git@florianreuth.de>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianreuth.rivetextras.graph;

import net.lenni0451.commons.color.Color;

/**
 * Named line of a {@link Graph}, points are connected in the order they were added and can be added from any thread.
 */
public final class GraphSeries {

    private final String name;
    private volatile Color color;
    // Ring buffer, the oldest point is at start, so dropping old points of live data does not shift the arrays
    private double[] xs = new double[16];
    private double[] ys = new double[16];
    private int start;
    private int size;
    private int maxPoints;
    // Lets the graph reuse its copy of the points while nothing changed
    private volatile int version;

    public GraphSeries(final String name) {
        this(name, null);
    }

    /**
     * @param color the line color, or null to pick one from the default palette by the position in the graph
     */
    public GraphSeries(final String name, final Color color) {
        this.name = name;
        this.color = color;
    }

    public String name() {
        return this.name;
    }

    public Color color() {
        return this.color;
    }

    /**
     * @param color the line color, or null to pick one from the default palette by the position in the graph
     */
    public GraphSeries color(final Color color) {
        this.color = color;
        return this;
    }

    /**
     * @return the maximum number of points, 0 if unlimited
     */
    public synchronized int maxPoints() {
        return this.maxPoints;
    }

    /**
     * Limits the number of points, the oldest points are dropped when more are added, e.g. to show the latest values of live data.
     *
     * @param maxPoints the maximum number of points, 0 for unlimited
     * @throws IllegalArgumentException if the limit is negative
     */
    public synchronized GraphSeries maxPoints(final int maxPoints) {
        if (maxPoints < 0) {
            throw new IllegalArgumentException("Max points must not be negative, got " + maxPoints);
        }
        this.maxPoints = maxPoints;
        if (maxPoints > 0 && this.size > maxPoints) {
            this.dropOldest(this.size - maxPoints);
        }
        return this;
    }

    /**
     * @throws IllegalArgumentException if a coordinate is NaN or infinite
     */
    public synchronized GraphSeries add(final double x, final double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Point coordinates must be finite, got " + x + ", " + y);
        }
        if (this.maxPoints > 0 && this.size >= this.maxPoints) {
            this.dropOldest(this.size - this.maxPoints + 1);
        }
        if (this.size == this.xs.length) {
            this.grow();
        }

        final int index = (this.start + this.size) % this.xs.length;
        this.xs[index] = x;
        this.ys[index] = y;
        this.size++;
        this.version++;
        return this;
    }

    public synchronized GraphSeries clear() {
        this.start = 0;
        this.size = 0;
        this.version++;
        return this;
    }

    public synchronized int size() {
        return this.size;
    }

    /**
     * @param index 0 is the oldest point
     * @throws IndexOutOfBoundsException if the index is outside the points
     */
    public synchronized double x(final int index) {
        return this.xs[this.ringIndex(index)];
    }

    /**
     * @param index 0 is the oldest point
     * @throws IndexOutOfBoundsException if the index is outside the points
     */
    public synchronized double y(final int index) {
        return this.ys[this.ringIndex(index)];
    }

    int version() {
        return this.version;
    }

    synchronized Snapshot snapshot() {
        final double[] xs = new double[this.size];
        final double[] ys = new double[this.size];
        for (int i = 0; i < this.size; i++) {
            final int index = (this.start + i) % this.xs.length;
            xs[i] = this.xs[index];
            ys[i] = this.ys[index];
        }
        return new Snapshot(this.version, xs, ys);
    }

    private int ringIndex(final int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside the " + this.size + " points");
        }
        return (this.start + index) % this.xs.length;
    }

    private void dropOldest(final int count) {
        this.start = (this.start + count) % this.xs.length;
        this.size -= count;
        this.version++;
    }

    private void grow() {
        final double[] xs = new double[this.xs.length * 2];
        final double[] ys = new double[this.ys.length * 2];
        for (int i = 0; i < this.size; i++) {
            final int index = (this.start + i) % this.xs.length;
            xs[i] = this.xs[index];
            ys[i] = this.ys[index];
        }
        this.xs = xs;
        this.ys = ys;
        this.start = 0;
    }


    /**
     * @param version the version of the series when the copy was made
     */
    record Snapshot(int version, double[] xs, double[] ys) {
    }

}
