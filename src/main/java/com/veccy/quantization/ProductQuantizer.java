package com.veccy.quantization;

import com.veccy.exceptions.QuantizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Product Quantization for vector compression.
 * <p>
 * Product Quantization splits vectors into subspaces and quantizes each subspace
 * independently using K-means clustering. This provides excellent compression
 * (typically 32x-64x) while maintaining reasonable accuracy.
 * <p>
 * Algorithm:
 * 1. Split D-dimensional vector into M subspaces of D/M dimensions each
 * 2. Run K-means on each subspace to learn K centroids (codebook)
 * 3. Quantize: replace each subvector with index of nearest centroid
 * 4. Result: M bytes per vector (assuming K=256, i.e., 8-bit codes)
 * <p>
 * Configuration:
 * - num_subspaces: Number of subspaces M (default 8)
 * - num_clusters: Number of clusters K per subspace (default 256)
 * - max_iterations: Maximum K-means iterations (default 100)
 * - convergence_threshold: K-means convergence threshold (default 0.001)
 */
public class ProductQuantizer implements Quantizer {

    private static final Logger logger = LoggerFactory.getLogger(ProductQuantizer.class);

    private static final int DEFAULT_NUM_SUBSPACES = 8;
    private static final int DEFAULT_NUM_CLUSTERS = 256;
    private static final int DEFAULT_MAX_ITERATIONS = 100;
    private static final double DEFAULT_CONVERGENCE_THRESHOLD = 0.001;

    private Map<String, Object> config;
    private int numSubspaces;
    private int numClusters;
    private int maxIterations;
    private double convergenceThreshold;
    private int dimensions;
    private int subspaceDim;

    // Codebooks: codebooks[m][k][d] = centroid k in subspace m
    private double[][][] codebooks;

    private boolean initialized;
    private boolean trained;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ProductQuantizer(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.initialized = false;
        this.trained = false;
    }

    @Override
    public void initialize() {
        if (initialized) {
            logger.warn("ProductQuantizer already initialized");
            return;
        }

        this.numSubspaces = getConfigInt("num_subspaces", DEFAULT_NUM_SUBSPACES);
        this.numClusters = getConfigInt("num_clusters", DEFAULT_NUM_CLUSTERS);
        this.maxIterations = getConfigInt("max_iterations", DEFAULT_MAX_ITERATIONS);
        this.convergenceThreshold = getConfigDouble("convergence_threshold", DEFAULT_CONVERGENCE_THRESHOLD);

        initialized = true;
        logger.info("Product quantizer initialized: subspaces={}, clusters={}", numSubspaces, numClusters);
    }

    @Override
    public void train(double[][] vectors) {
        if (!initialized) {
            throw new QuantizationException("Quantizer not initialized");
        }

        if (vectors == null || vectors.length == 0) {
            throw new QuantizationException("Training vectors cannot be null or empty");
        }

        this.dimensions = vectors[0].length;
        this.subspaceDim = dimensions / numSubspaces;

        if (dimensions % numSubspaces != 0) {
            throw new QuantizationException(
                    "Vector dimension " + dimensions + " must be divisible by num_subspaces " + numSubspaces);
        }

        logger.info("Training product quantizer on {} vectors (dim={})", vectors.length, dimensions);

        // Initialize codebooks
        codebooks = new double[numSubspaces][numClusters][subspaceDim];

        // Train each subspace independently
        for (int m = 0; m < numSubspaces; m++) {
            logger.debug("Training subspace {}/{}", m + 1, numSubspaces);
            trainSubspace(vectors, m);
        }

        trained = true;
        logger.info("Product quantizer training complete");
    }

    @Override
    public byte[][] quantize(double[][] vectors) {
        if (!trained) {
            throw new QuantizationException("Quantizer not trained");
        }

        byte[][] quantized = new byte[vectors.length][numSubspaces];

        for (int i = 0; i < vectors.length; i++) {
            quantized[i] = quantizeSingle(vectors[i]);
        }

        return quantized;
    }

    @Override
    public double[][] dequantize(byte[][] quantizedVectors) {
        if (!trained) {
            throw new QuantizationException("Quantizer not trained");
        }

        double[][] dequantized = new double[quantizedVectors.length][dimensions];

        for (int i = 0; i < quantizedVectors.length; i++) {
            dequantized[i] = dequantizeSingle(quantizedVectors[i]);
        }

        return dequantized;
    }

    @Override
    public double computeDistance(double[] query, byte[] quantizedVector) {
        if (!trained) {
            throw new QuantizationException("Quantizer not trained");
        }

        // Asymmetric distance computation (query unquantized, database quantized)
        double distance = 0.0;

        for (int m = 0; m < numSubspaces; m++) {
            int clusterIdx = Byte.toUnsignedInt(quantizedVector[m]);
            double[] centroid = codebooks[m][clusterIdx];

            // Extract subvector from query
            int offset = m * subspaceDim;
            for (int d = 0; d < subspaceDim; d++) {
                double diff = query[offset + d] - centroid[d];
                distance += diff * diff;
            }
        }

        return Math.sqrt(distance);
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "ProductQuantizer");
        stats.put("initialized", initialized);
        stats.put("trained", trained);
        stats.put("num_subspaces", numSubspaces);
        stats.put("num_clusters", numClusters);

        if (trained) {
            stats.put("dimensions", dimensions);
            stats.put("subspace_dim", subspaceDim);
            stats.put("bytes_per_vector", numSubspaces);
            stats.put("compression_ratio", (double) (dimensions * 8) / numSubspaces);
        }

        return stats;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isTrained() {
        return trained;
    }

    /**
     * Close the quantizer and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                codebooks = null;
                trained = false;
                initialized = false;
                dimensions = 0;
                subspaceDim = 0;
                logger.info("Product quantizer closed");
            } catch (Exception e) {
                logger.error("Error closing product quantizer: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Product quantizer already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Train K-means on a single subspace.
     */
    private void trainSubspace(double[][] vectors, int subspaceIndex) {
        int offset = subspaceIndex * subspaceDim;

        // Extract subvectors
        double[][] subvectors = new double[vectors.length][subspaceDim];
        for (int i = 0; i < vectors.length; i++) {
            System.arraycopy(vectors[i], offset, subvectors[i], 0, subspaceDim);
        }

        // Initialize centroids using k-means++
        double[][] centroids = initializeCentroidsKMeansPlusPlus(subvectors, numClusters);

        // K-means iterations
        int[] assignments = new int[subvectors.length];
        boolean converged = false;

        for (int iter = 0; iter < maxIterations && !converged; iter++) {
            // Assignment step: assign each vector to nearest centroid
            boolean changed = false;
            for (int i = 0; i < subvectors.length; i++) {
                int newAssignment = findNearestCentroid(subvectors[i], centroids);
                if (newAssignment != assignments[i]) {
                    assignments[i] = newAssignment;
                    changed = true;
                }
            }

            if (!changed) {
                converged = true;
                logger.debug("K-means converged at iteration {} for subspace {}", iter, subspaceIndex);
                break;
            }

            // Update step: recompute centroids
            double[][] newCentroids = new double[numClusters][subspaceDim];
            int[] counts = new int[numClusters];

            for (int i = 0; i < subvectors.length; i++) {
                int cluster = assignments[i];
                for (int d = 0; d < subspaceDim; d++) {
                    newCentroids[cluster][d] += subvectors[i][d];
                }
                counts[cluster]++;
            }

            // Compute means and check convergence
            double maxShift = 0.0;
            for (int k = 0; k < numClusters; k++) {
                if (counts[k] > 0) {
                    for (int d = 0; d < subspaceDim; d++) {
                        newCentroids[k][d] /= counts[k];
                        double shift = Math.abs(newCentroids[k][d] - centroids[k][d]);
                        maxShift = Math.max(maxShift, shift);
                    }
                } else {
                    // Handle empty cluster: reinitialize to random point
                    int randomIdx = ThreadLocalRandom.current().nextInt(subvectors.length);
                    System.arraycopy(subvectors[randomIdx], 0, newCentroids[k], 0, subspaceDim);
                }
            }

            centroids = newCentroids;

            if (maxShift < convergenceThreshold) {
                converged = true;
                logger.debug("K-means converged at iteration {} (shift={}) for subspace {}",
                        iter, maxShift, subspaceIndex);
            }
        }

        // Store trained codebook
        codebooks[subspaceIndex] = centroids;
    }

    /**
     * Initialize centroids using k-means++ algorithm for better initial placement.
     */
    private double[][] initializeCentroidsKMeansPlusPlus(double[][] vectors, int k) {
        double[][] centroids = new double[k][subspaceDim];
        Random random = ThreadLocalRandom.current();

        // Choose first centroid randomly
        int firstIdx = random.nextInt(vectors.length);
        System.arraycopy(vectors[firstIdx], 0, centroids[0], 0, subspaceDim);

        // Choose remaining centroids with probability proportional to distance squared
        for (int i = 1; i < k; i++) {
            double[] distances = new double[vectors.length];
            double totalDistance = 0.0;

            for (int j = 0; j < vectors.length; j++) {
                double minDist = Double.MAX_VALUE;
                for (int c = 0; c < i; c++) {
                    double dist = squaredEuclideanDistance(vectors[j], centroids[c]);
                    minDist = Math.min(minDist, dist);
                }
                distances[j] = minDist;
                totalDistance += minDist;
            }

            // Select next centroid
            double threshold = random.nextDouble() * totalDistance;
            double cumulative = 0.0;
            for (int j = 0; j < vectors.length; j++) {
                cumulative += distances[j];
                if (cumulative >= threshold) {
                    System.arraycopy(vectors[j], 0, centroids[i], 0, subspaceDim);
                    break;
                }
            }
        }

        return centroids;
    }

    /**
     * Find the nearest centroid to a vector.
     */
    private int findNearestCentroid(double[] vector, double[][] centroids) {
        int nearest = 0;
        double minDist = squaredEuclideanDistance(vector, centroids[0]);

        for (int k = 1; k < centroids.length; k++) {
            double dist = squaredEuclideanDistance(vector, centroids[k]);
            if (dist < minDist) {
                minDist = dist;
                nearest = k;
            }
        }

        return nearest;
    }

    /**
     * Compute squared Euclidean distance between two vectors.
     */
    private double squaredEuclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * Quantize a single vector.
     */
    private byte[] quantizeSingle(double[] vector) {
        byte[] quantized = new byte[numSubspaces];

        for (int m = 0; m < numSubspaces; m++) {
            int offset = m * subspaceDim;
            double[] subvector = new double[subspaceDim];
            System.arraycopy(vector, offset, subvector, 0, subspaceDim);

            int clusterIdx = findNearestCentroid(subvector, codebooks[m]);
            quantized[m] = (byte) clusterIdx;
        }

        return quantized;
    }

    /**
     * Dequantize a single vector.
     */
    private double[] dequantizeSingle(byte[] quantized) {
        double[] dequantized = new double[dimensions];

        for (int m = 0; m < numSubspaces; m++) {
            int clusterIdx = Byte.toUnsignedInt(quantized[m]);
            int offset = m * subspaceDim;
            System.arraycopy(codebooks[m][clusterIdx], 0, dequantized, offset, subspaceDim);
        }

        return dequantized;
    }

    private int getConfigInt(String key, int defaultValue) {
        if (!config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private double getConfigDouble(String key, double defaultValue) {
        if (!config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}
