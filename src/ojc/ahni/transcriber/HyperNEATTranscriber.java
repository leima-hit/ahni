package ojc.ahni.transcriber;

import java.util.Arrays;

import ojc.ahni.evaluation.AHNIFitnessFunction;
import ojc.ahni.hyperneat.Configurable;
import ojc.ahni.hyperneat.HyperNEATEvolver;
import ojc.ahni.hyperneat.Properties;
import ojc.ahni.util.Range;
import ojc.ahni.util.Point;

import org.apache.log4j.Logger;
import org.jgapcustomised.BulkFitnessFunction;
import org.jgapcustomised.Chromosome;

import com.anji.integration.Activator;
import com.anji.integration.AnjiNetTranscriber;
import com.anji.integration.Transcriber;
import com.anji.integration.TranscriberException;

/**
 * To "transcribe" is to construct a phenotype from a genotype. This is a base class for Transcribers implementing the
 * HyperNEAT encoding scheme or extensions thereof. A non-abstract sub-class will typically specify a particular
 * Activator implementation that it creates. See {@link HyperNEATTranscriberBain} for an example of a sub-class.
 * 
 * @author Oliver Coleman
 */
public abstract class HyperNEATTranscriber<T extends Activator> implements Transcriber<T>, Configurable {
	private final static Logger logger = Logger.getLogger(HyperNEATTranscriber.class);

	/**
	 * Set to true to restrict the substrate network to a strictly feed-forward topology.
	 */
	public static final String HYPERNEAT_FEED_FORWARD = "ann.hyperneat.feedforward";
	/**
	 * Enable bias connections in the substrate network.
	 */
	public static final String HYPERNEAT_ENABLE_BIAS = "ann.hyperneat.enablebias";
	/**
	 * If true indicates that the CPPN should receive the delta value for each axis between the source and target neuron
	 * coordinates.
	 */
	public static final String HYPERNEAT_INCLUDE_DELTA = "ann.hyperneat.includedelta";
	/**
	 * If true indicates that the CPPN should receive the angle in the XY plane between the source and target neuron
	 * coordinates (relative to the line X axis).
	 */
	public static final String HYPERNEAT_INCLUDE_ANGLE = "ann.hyperneat.includeangle";
	/**
	 * If true indicates that instead of using a separate output from the CPPN to specify weight values for each weight
	 * layer in a feed-forward network, the layer coordinate is input to the CPPN and only a single output from CPPN is
	 * used to specify weight values for all weight layers.
	 */
	public static final String HYPERNEAT_LAYER_ENCODING = "ann.hyperneat.useinputlayerencoding";
	/**
	 * The minimum CPPN output required to produce a non-zero weight in the substrate network.
	 */
	public static final String HYPERNEAT_CONNECTION_EXPRESSION_THRESHOLD = "ann.hyperneat.connection.expression.threshold";
	/**
	 * The minimum weight values in the substrate network. If this is not specified then the negated value of HYPERNEAT_CONNECTION_WEIGHT_MAX will be used. 
	 */
	public static final String HYPERNEAT_CONNECTION_WEIGHT_MIN = "ann.hyperneat.connection.weight.min";
	/**
	 * The maximum weight values in the substrate network.
	 */
	public static final String HYPERNEAT_CONNECTION_WEIGHT_MAX = "ann.hyperneat.connection.weight.max";
	/**
	 * Limits the incoming connections to a target neuron to include those from source neurons within the specified
	 * range of the target neuron. Set this to -1 to disable it.
	 */
	public static final String HYPERNEAT_CONNECTION_RANGE = "ann.hyperneat.connection.range";
	/**
	 * The number of layers in the substrate network.
	 */
	public static final String SUBSTRATE_DEPTH = "ann.hyperneat.depth";
	/**
	 * Comma separated list of the height of each layer in the network, including input and output layers and starting
	 * with the input layer.
	 */
	public static final String SUBSTRATE_HEIGHT = "ann.hyperneat.height";
	/**
	 * Comma separated list of the width of each layer in the network, including input and output layers and starting
	 * with the input layer.
	 */
	public static final String SUBSTRATE_WIDTH = "ann.hyperneat.width";
	/**
	 * The coordinate range of neurons in the substrate in the X dimension, corresponding to the width (this is used to
	 * determine the input to the CPPN for a given neuron location). Defaults to [0, 1].
	 */
	public static final String RANGE_X = "ann.hyperneat.range.x";
	/**
	 * The coordinate range of neurons in the substrate in the Y dimension, corresponding to the height (this is used to
	 * determine the input to the CPPN for a given neuron location). Defaults to [0, 1].
	 */
	public static final String RANGE_Y = "ann.hyperneat.range.y";
	/**
	 * The coordinate range of neurons in the substrate in the Z dimension, corresponding to the depth (this is used to
	 * determine the input to the CPPN for a given neuron location). Defaults to [0, 1].
	 */
	public static final String RANGE_Z = "ann.hyperneat.range.z";

	/**
	 * <p>
	 * The coordinates of neurons in a specified layer. The layer must be specified as part of the property key/name,
	 * e.g. to specify the coordinates of two neurons in the layer with index 2 (counting from 0):<br />
	 * <code>ann.hyperneat.layer.positions.2=(-0.5, 0, 0), (0.5, 0, 0)</code>
	 * </p>
	 * <p>
	 * For 2D layers the coordinates should be specified in row-packed order (i.e. one row after another, where a single
	 * row has the same y index/coordinate). Coordinates must be specified for all neurons in the given layer. If the z
	 * coordinate is not given it will be set to the default for the layer.
	 * </p>
	 */
	public static final String NEURON_POSITIONS_FOR_LAYER = "ann.hyperneat.layer.positions";

	/**
	 * For recurrent networks, the number of activation cycles to perform each time the substrate network is presented
	 * with new input and queried for its output.
	 */
	public static final String SUBSTRATE_CYCLES_PER_STEP = "ann.hyperneat.cyclesperstep";
	/**
	 * Enable or disable Link Expression Output (LEO). See P. Verbancsics and K. O. Stanley (2011) Constraining
	 * Connectivity to Encourage Modularity in HyperNEAT. In Proceedings of the Genetic and Evolutionary Computation
	 * Conference (GECCO 2011). Default is "false".
	 * 
	 * @see #HYPERNEAT_LEO_LOCALITY
	 */
	public static final String HYPERNEAT_LEO = "ann.hyperneat.leo";
	/**
	 * Enable or disable seeding of the initial population of {@link org.jgapcustomised.Chromosome}s to incorporate a
	 * bias towards local connections via the Link Expression Output (LEO), see {@link #HYPERNEAT_LEO} and the article
	 * reference within. Default is "false".
	 */
	public static final String HYPERNEAT_LEO_LOCALITY = "ann.hyperneat.leo.localityseeding";

	/**
	 * The width of each layer in the substrate.
	 */
	protected int width[];
	/**
	 * The height of each layer in the substrate.
	 */
	protected int height[];
	/**
	 * The number of layers in the substrate, including input and output layers.
	 */
	protected int depth;
	/**
	 * Used by {@link HyperNEATTranscriber.CPPN} to translate from the default range [0, 1] to the range specified by
	 * {@link #RANGE_X}.
	 */
	protected Range rangeX = new Range();
	/**
	 * Used by {@link HyperNEATTranscriber.CPPN} to translate from the default range [0, 1] to the range specified by
	 * {@link #RANGE_Y}.
	 */
	protected Range rangeY = new Range();
	/**
	 * Used by {@link HyperNEATTranscriber.CPPN} to translate from the default range [0, 1] to the range specified by
	 * {@link #RANGE_Z}.
	 */
	protected Range rangeZ = new Range();

	/**
	 * Custom positions for all neurons in the network. This is used when either the {@link #NEURON_POSITIONS_FOR_LAYER}
	 * property is used and/or the fitness function defines some/all neuron layers and positions. The coordinates are in
	 * the ranges {@link #rangeX}, {@link #rangeY} and {@link #rangeZ} (rather than unit ranges).
	 */
	protected Point[][] neuronPositionsForLayer;

	/**
	 * If true indicates whether the substrate should have a feed-forward topology (no recurrent connections).
	 */
	protected boolean feedForward;
	/**
	 * If true indicates that each neuron in the substrate should receive a bias signal.
	 */
	protected boolean enableBias;
	/**
	 * If true indicates that the CPPN should receive the delta value for each axis between the source and target neuron
	 * coordinates.
	 */
	protected boolean includeDelta;
	/**
	 * If true indicates that the CPPN should receive the angle in the XY plane between the source and target neuron
	 * coordinates.
	 */
	protected boolean includeAngle;
	/**
	 * The minimum output from the CPPN required to create a non-zero weight value in the substrate.
	 */
	protected double connectionExprThresh = 0.2;
	/**
	 * The minimum connection weight in the substrate.
	 */
	protected double connectionWeightMin;
	/**
	 * The maximum connection weight in the substrate.
	 */
	protected double connectionWeightMax;
	/**
	 * Limits the incoming connections to a target neuron to include those from source neurons within the specified
	 * range of the target neuron. A value of -1 indicates that this is disabled.
	 */
	protected int connectionRange;
	/**
	 * If true indicates that instead of using a separate output from the CPPN to specify weight values for each weight
	 * layer in a feed-forward network, the layer coordinate is input to the CPPN and only a single output from CPPN is
	 * used to specify weight values for all weight layers.
	 */
	protected boolean layerEncodingIsInput = false;
	/**
	 * For substrate networks with recurrent connections, the number of activation cycles to perform each time the
	 * substrate network is presented with new input and queried for its output.
	 */
	protected int cyclesPerStep;
	/**
	 * If true indicates that Link Expression Output is enabled.
	 */
	protected boolean enableLEO = false;
	/**
	 * The number of inputs to the CPPN.
	 * 
	 * @see #getCPPNInputCount()
	 */
	protected short cppnInputCount;
	/**
	 * The number of outputs from the CPPN.
	 * 
	 * @see #getCPPNOutputCount()
	 */
	protected short cppnOutputCount;

	/**
	 * Transcriber for network for use as a CPPN. Not usually accessed directly, see {@link HyperNEATTranscriber.CPPN}.
	 */
	protected Transcriber cppnTranscriber;

	// Index of target and source coordinate inputs in CPPN input vector.
	int cppnIdxTX = -1, cppnIdxTY = -1, cppnIdxTZ = -1, cppnIdxSX = -1, cppnIdxSY = -1, cppnIdxSZ = -1;
	// Index of delta and angle inputs in CPPN input vector.
	int cppnIdxDX = -1, cppnIdxDY = -1, cppnIdxDZ = -1, cppnIdxAn = -1;
	// Index of output signals in CPPN output vector.
	int[] cppnIdxW = new int[0]; // weights (either a single output for all layers or one output per layer)
	int[] cppnIdxB = new int[0]; // bias (either a single output for all layers or one output per layer)
	int[] cppnIdxL = new int[0]; // link expression (either a single output for all layers or one output per layer)

	/**
	 * Subclasses may set this to "force" or "prevent" before calling super.init(Properties) to either force or prevent
	 * the use of Z coordinate inputs for the CPPN (both source and target neuron Z coordinates will be affected).
	 */
	protected String zCoordsForCPPN = "";

	/**
	 * This method should be called from overriding methods.
	 * 
	 * @see Configurable#init(Properties)
	 */
	public void init(Properties props) {
		feedForward = props.getBooleanProperty(HYPERNEAT_FEED_FORWARD);
		enableBias = props.getBooleanProperty(HYPERNEAT_ENABLE_BIAS);
		includeDelta = props.getBooleanProperty(HYPERNEAT_INCLUDE_DELTA);
		includeAngle = props.getBooleanProperty(HYPERNEAT_INCLUDE_ANGLE);
		layerEncodingIsInput = props.getBooleanProperty(HYPERNEAT_LAYER_ENCODING, layerEncodingIsInput);
		// layerEncodingIsInput must be used for recurrent networks.
		layerEncodingIsInput = !feedForward ? true : layerEncodingIsInput;
		connectionExprThresh = props.getDoubleProperty(HYPERNEAT_CONNECTION_EXPRESSION_THRESHOLD, connectionExprThresh);
		connectionWeightMax = props.getDoubleProperty(HYPERNEAT_CONNECTION_WEIGHT_MAX);
		connectionWeightMin = props.getDoubleProperty(HYPERNEAT_CONNECTION_WEIGHT_MIN, -connectionWeightMax);
		connectionRange = props.getIntProperty(HYPERNEAT_CONNECTION_RANGE, -1);
		depth = props.getIntProperty(SUBSTRATE_DEPTH);
		cyclesPerStep = feedForward ? depth - 1 : props.getIntProperty(SUBSTRATE_CYCLES_PER_STEP, 1);
		enableLEO = props.getBooleanProperty(HYPERNEAT_LEO, enableLEO);
		
		if (enableLEO && connectionExprThresh != 0) {
			logger.warn("LEO is enabled but the connection expression threshold is not 0. It is recommended to set the connection expression threshold to 0 when LEO is enabled.");
		}
		
		width = getProvisionalLayerSize(props, SUBSTRATE_WIDTH);
		height = getProvisionalLayerSize(props, SUBSTRATE_HEIGHT);
		
		rangeX = props.getObjectFromArgsProperty(RANGE_X, Range.class, rangeX, null);
		rangeY = props.getObjectFromArgsProperty(RANGE_Y, Range.class, rangeY, null);
		rangeZ = props.getObjectFromArgsProperty(RANGE_Z, Range.class, rangeZ, null);

		// Determine width and height of each layer.
		BulkFitnessFunction ff = (BulkFitnessFunction) props.singletonObjectProperty(HyperNEATEvolver.FITNESS_FUNCTION_CLASS_KEY);
		AHNIFitnessFunction aff = (ff instanceof AHNIFitnessFunction) ? (AHNIFitnessFunction) ff : null;
		for (int layer = 0; layer < depth; layer++) {
			// If the fitness function is to define this layer.
			if (width[layer] == -1 || height[layer] == -1) {
				int[] layerDims = aff != null ? aff.getLayerDimensions(layer, depth) : null;
				if (layerDims != null) {
					if (width[layer] == -1) {
						width[layer] = layerDims[0];
					}
					if (height[layer] == -1) {
						height[layer] = layerDims[1];
					}
					logger.info("Fitness function defines dimensions for layer " + layer + ": " + width[layer] + "x" + height[layer]);
				} else {
					throw new IllegalArgumentException("Properties specify that fitness function should specify layer dimensions for layer " + layer + " but fitness function does not specify this.");
				}
			}
		}

		// Determine custom neuron positions (if any) for each layer.
		neuronPositionsForLayer = new Point[depth][];
		for (int layer = 0; layer < depth; layer++) {
			// If the user has specified positions for neurons for this layer.
			if (props.containsKey(NEURON_POSITIONS_FOR_LAYER + "." + layer)) {
				// Provide default z coordinate if not specified in properties.
				Point defaultCoords = new Point(0, 0, depth == 1 ? 0 : (double) layer / (depth - 1));
				defaultCoords.translateFromUnit(rangeX, rangeY, rangeZ);
				double[] defaultArgs = new double[] { defaultCoords.x, defaultCoords.y, defaultCoords.z };
				neuronPositionsForLayer[layer] = props.getObjectArrayProperty(NEURON_POSITIONS_FOR_LAYER + "." + layer, Point.class, defaultArgs);
				if (neuronPositionsForLayer[layer].length != width[layer] * height[layer]) {
					throw new IllegalArgumentException("The number of neuron positions specified for " + NEURON_POSITIONS_FOR_LAYER + "." + layer + " does not match the number of neurons for the layer.");
				}
			} else if (aff != null) {
				neuronPositionsForLayer[layer] = aff.getNeuronPositions(layer, depth);
				if (neuronPositionsForLayer[layer] != null) {
					if (neuronPositionsForLayer[layer].length != width[layer] * height[layer]) {
						throw new IllegalArgumentException("The number of neuron positions specified by the fitness function for " + layer + " does not match the number of neurons for the layer.");
					}
					StringBuilder logStr = new StringBuilder("Fitness function defines neuron positions for layer " + layer + ": ");
					for (Point p : neuronPositionsForLayer[layer]) {
						p.translateFromUnit(rangeX, rangeY, rangeZ);
						logStr.append(p + ", ");
					}
					logger.info(logStr);
				}
			}
		}

		// Determine CPPN input size and mapping.
		cppnInputCount = 1; // Bias always has index 0.
		cppnIdxSX = cppnInputCount++;
		cppnIdxSY = cppnInputCount++;
		cppnIdxTX = cppnInputCount++;
		cppnIdxTY = cppnInputCount++;
		logger.info("CPPN: Added bias, sx, sy, tx, ty inputs.");
		if (zCoordsForCPPN.equals("force") || (!zCoordsForCPPN.equals("prevent") && (feedForward && layerEncodingIsInput && depth > 2 || !feedForward && depth > 1))) {
			cppnIdxTZ = cppnInputCount++;
			logger.info("CPPN: Added tz input.");

			if (zCoordsForCPPN.equals("force") || !feedForward) {
				cppnIdxSZ = cppnInputCount++;
				logger.info("CPPN: Added sz input.");
				if (includeDelta) {
					cppnIdxDZ = cppnInputCount++; // z delta
					logger.info("CPPN: Added delta z input.");
				}
			}
		}
		if (includeDelta) {
			cppnIdxDY = cppnInputCount++; // y delta
			cppnIdxDX = cppnInputCount++; // x delta
			logger.info("CPPN: Added delta x and y inputs.");
		}
		if (includeAngle) {
			cppnIdxAn = cppnInputCount++; // angle
			logger.info("CPPN: Added angle input.");
		}

		// Determine CPPN output size and mapping.
		cppnOutputCount = 0;
		if (layerEncodingIsInput) { // same output for all layers
			cppnIdxW = new int[1];
			cppnIdxW[0] = cppnOutputCount++; // weight value
			logger.info("CPPN: Added single weight output.");

			if (enableBias) {
				cppnIdxB = new int[1];
				cppnIdxB[0] = cppnOutputCount++; // bias value
				logger.info("CPPN: Added single bias output.");
			}

			if (enableLEO) {
				cppnIdxL = new int[1];
				cppnIdxL[0] = cppnOutputCount++; // bias value
				logger.info("CPPN: Added single link expression output.");
			}
		} else { // one output per layer
			int layerOutputCount = Math.max(1, depth - 1); // Allow for depth of 1 (2D horizontal substrate)
			cppnIdxW = new int[layerOutputCount];
			for (int w = 0; w < layerOutputCount; w++)
				cppnIdxW[w] = cppnOutputCount++; // weight value
			logger.info("CPPN: Added " + layerOutputCount + " weight outputs.");

			if (enableBias) {
				cppnIdxB = new int[layerOutputCount];
				for (int w = 0; w < layerOutputCount; w++)
					cppnIdxB[w] = cppnOutputCount++; // bias value
				logger.info("CPPN: Added " + layerOutputCount + " bias outputs.");
			}

			if (enableLEO) {
				cppnIdxL = new int[layerOutputCount];
				for (int w = 0; w < layerOutputCount; w++)
					cppnIdxL[w] = cppnOutputCount++; // leo value
				logger.info("CPPN: Added " + layerOutputCount + " link expression outputs.");
			}
		}

		logger.info("CPPN input/output size: " + cppnInputCount + "/" + cppnOutputCount);

		cppnTranscriber = (Transcriber) props.singletonObjectProperty(AnjiNetTranscriber.class);
	}
	
	/**
	 * Returns provisional width or height values for each layer of the substrate. 
	 * Any values that are to be determined by the fitness function have value -1.
	 * @param props The properties to read the size from.
	 * @param sizeKey Whether to retrieve width or height values, allowed values are {@link #SUBSTRATE_WIDTH} or  {@link #SUBSTRATE_HEIGHT}.
	 * @return An array containing values for the width or height of each layer of the substrate.
	 */
	public static int[] getProvisionalLayerSize(Properties props, String sizeKey) {
		int depth = props.getIntProperty(SUBSTRATE_DEPTH);
		int[] sizes;
		if (props.containsKey(sizeKey)) {
			String valString = props.getProperty(sizeKey).replaceAll("f", "-1");
			sizes = props.getIntArrayFromString(valString);
			if (sizes.length != depth) {
				throw new IllegalArgumentException("Number of comma-separated layer dimensions in " + sizeKey + " does not match " + SUBSTRATE_DEPTH + ".");
			}
		}
		else {
			sizes = new int[depth];
			Arrays.fill(sizes, -1);
		}
		return sizes;
	}

	/**
	 * Get the number of layers in the substrate, including input and output layers.
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Get the width of each layer in the substrate.
	 */
	public int[] getWidth() {
		return width;
	}

	/**
	 * Get the height of each layer in the substrate.
	 */
	public int[] getHeight() {
		return height;
	}

	/**
	 * Get the limit of the range of connections, this limits the incoming connections to a target neuron to include
	 * those from source neurons within the specified range of the target neuron. This is not implemented by all
	 * transcribers. A value of -1 indicates that this is disabled.
	 */
	public int getConnectionRange() {
		return connectionRange;
	}

	/**
	 * Specify new dimensions for each layer. If connection range is not used it can be set to -1.
	 */
	public void resize(int[] width, int[] height, int connectionRange) {
		this.width = width;
		this.height = height;
		this.connectionRange = connectionRange;
	}

	/**
	 * The number of inputs to the CPPN.
	 */
	public short getCPPNInputCount() {
		return cppnInputCount;
	}

	public short getCPPNOutputCount() {
		return cppnOutputCount;
	}

	/**
	 * Provides a wrapper for an {@link com.anji.integration.Activator} that represents a CPPN.
	 */
	protected class CPPN {
		protected Activator cppnActivator;
		protected double[] cppnInput = new double[cppnInputCount];
		protected double[] cppnOutput;
		protected double cppnMin, cppnMax, cppnRange;
		protected boolean cppnOutputUnitBounded;
		protected boolean cppnOutputPlusMinusUnitBounded;

		public CPPN(Chromosome genotype) throws TranscriberException {
			cppnActivator = cppnTranscriber.transcribe(genotype);
			cppnInput[0] = 1; // Bias.
			cppnMin = cppnActivator.getMinResponse();
			cppnMax = cppnActivator.getMaxResponse();
			cppnRange = cppnMax - cppnMin;
			cppnOutputUnitBounded = cppnMin == 0 && cppnMax == 1;
			cppnOutputPlusMinusUnitBounded = cppnMin == -1 && cppnMax == 1;
		}

		/**
		 * Set the coordinates of the source neuron in a two-dimensional substrate with dimensions with range [0, 1]
		 * (coordinates are translated to a user-specified range if necessary, e.g. see {@link #rangeX}).
		 * 
		 * @param x The x coordinate, range should be [0, 1].
		 * @param y The y coordinate, range should be [0, 1].
		 */
		public void setSourceCoordinates(double x, double y) {
			cppnInput[cppnIdxSX] = rangeX.translateFromUnit(x);
			cppnInput[cppnIdxSY] = rangeY.translateFromUnit(y);
		}

		/**
		 * Set the coordinates of the source neuron in a three-dimensional substrate with dimensions with range [0, 1]
		 * (coordinates are translated to a user-specified range if necessary, e.g. see {@link #rangeX}). If the z
		 * coordinate is not required it will be ignored.
		 * 
		 * @param x The x coordinate, range should be [0, 1].
		 * @param y The y coordinate, range should be [0, 1].
		 * @param z The z coordinate, range should be [0, 1].
		 */
		public void setSourceCoordinates(double x, double y, double z) {
			cppnInput[cppnIdxSX] = rangeX.translateFromUnit(x);
			cppnInput[cppnIdxSY] = rangeY.translateFromUnit(y);
			if (cppnIdxSZ != -1) {
				cppnInput[cppnIdxSZ] = rangeZ.translateFromUnit(z);
			}
		}

		/**
		 * Set the coordinates of the source neuron in a substrate with dimensions with range [0, 1] (coordinates are
		 * translated to a user-specified range if necessary, e.g. see {@link #rangeX}). If the z coordinate is not
		 * required it will be ignored.
		 * 
		 * @param p A Point containing the coordinates of the source neuron. Coordinates should be in range [0, 1].
		 */
		public void setSourceCoordinates(Point p) {
			setSourceCoordinates(p.x, p.y, p.z);
		}

		/**
		 * Set the coordinates of the source neuron specified by the given indices into a grid-based substrate. If the z
		 * coordinate is not required it will be ignored. Subclasses of {@link HyperNEATTranscriber} should generally
		 * use this method instead of directly specifying coordinates as this method supports specifying arbitrary
		 * coordinates for neurons via {@link HyperNEATTranscriber#NEURON_POSITIONS_FOR_LAYER} or coordinates specified
		 * by the fitness function. Note that if custom coordinates have been specified for a layer then the values of
		 * the x and y parameters may not correlate linearly to the actual coordinates if the layer does not use a
		 * regular grid-based layout, however this detail can generally be ignored by subclasses, they can just blindly
		 * rely on the {@link HyperNEATTranscriber#width} and {@link HyperNEATTranscriber#height} values (see
		 * {@link HyperNEATTranscriberBain} for an example).
		 * 
		 * @param x The index of the source neuron in the X dimension.
		 * @param y The index of the source neuron in the Y dimension.
		 * @param z The index of the source neuron in the Z dimension (the layer index).
		 */
		public void setSourceCoordinatesFromGridIndices(int x, int y, int z) {
			if (depth == 1)
				z = 0;
			if (neuronPositionsForLayer[z] == null) {
				setSourceCoordinates(width[z] > 1 ? (double) x / (width[z] - 1) : 0.5, height[z] > 1 ? (double) y / (height[z] - 1) : 0.5, depth > 1 ? (double) z / (depth - 1) : 0);
			} else {
				Point p = neuronPositionsForLayer[z][y * width[z] + x];
				// Don't use setSourceCoordinates(Point) as the Points are already translated from unit.
				cppnInput[cppnIdxSX] = p.x;
				cppnInput[cppnIdxSY] = p.y;
				if (cppnIdxSZ != -1) {
					cppnInput[cppnIdxSZ] = p.y;
				}
			}
		}

		/**
		 * Set the coordinates of the source neuron in a two-dimensional substrate with dimensions with range [0, 1]
		 * (coordinates are translated to a user-specified range if necessary, e.g. see {@link #rangeX}).
		 * 
		 * @param x The x coordinate, range should be [0, 1].
		 * @param y The y coordinate, range should be [0, 1].
		 */
		public void setTargetCoordinates(double x, double y) {
			cppnInput[cppnIdxTX] = rangeX.translateFromUnit(x);
			cppnInput[cppnIdxTY] = rangeY.translateFromUnit(y);
		}

		/**
		 * Set the coordinates of the source neuron in a three-dimensional substrate with dimensions with range [0, 1]
		 * (coordinates are translated to a user-specified range if necessary, e.g. see {@link #rangeX}). If the z
		 * coordinate is not required it will be ignored.
		 * 
		 * @param x The x coordinate, range should be [0, 1].
		 * @param y The y coordinate, range should be [0, 1].
		 * @param z The z coordinate, range should be [0, 1].
		 */
		public void setTargetCoordinates(double x, double y, double z) {
			cppnInput[cppnIdxTX] = rangeX.translateFromUnit(x);
			cppnInput[cppnIdxTY] = rangeY.translateFromUnit(y);
			if (cppnIdxTZ != -1) {
				cppnInput[cppnIdxTZ] = rangeZ.translateFromUnit(z);
			}
		}

		/**
		 * Set the coordinates of the target neuron in a substrate with dimensions with range [0, 1] (coordinates are
		 * translated to a user-specified range if necessary, e.g. see {@link #rangeX}). If the z coordinate is not
		 * required it will be ignored.
		 * 
		 * @param p A Point containing the coordinates of the target neuron. Coordinates should be in range [0, 1].
		 */
		public void setTargetCoordinates(Point p) {
			setTargetCoordinates(p.x, p.y, p.z);
		}

		/**
		 * Set the coordinates of the target neuron specified by the given indices into a grid-based substrate. If the z
		 * coordinate is not required it will be ignored. Subclasses of {@link HyperNEATTranscriber} should generally
		 * use this method instead of directly specifying coordinates as this method supports specifying arbitrary
		 * coordinates for neurons via {@link HyperNEATTranscriber#NEURON_POSITIONS_FOR_LAYER} or coordinates specified
		 * by the fitness function. Note that if custom coordinates have been specified for a layer then the values of
		 * the x and y parameters may not correlate linearly to the actual coordinates if the layer does not use a
		 * regular grid-based layout, however this detail can generally be ignored by subclasses, they can just blindly
		 * rely on the {@link HyperNEATTranscriber#width} and {@link HyperNEATTranscriber#height} values (see
		 * {@link HyperNEATTranscriberBain} for an example).
		 * 
		 * @param x The index of the target neuron in the X dimension.
		 * @param y The index of the target neuron in the Y dimension.
		 * @param z The index of the target neuron in the Z dimension (the layer index).
		 */
		public void setTargetCoordinatesFromGridIndices(int x, int y, int z) {
			if (depth == 1)
				z = 0;
			if (neuronPositionsForLayer[z] == null) {
				setTargetCoordinates(width[z] > 1 ? (double) x / (width[z] - 1) : 0.5, height[z] > 1 ? (double) y / (height[z] - 1) : 0.5, depth > 1 ? (double) z / (depth - 1) : 0);
			} else {
				Point p = neuronPositionsForLayer[z][y * width[z] + x];
				// Don't use setTargetCoordinates(Point) as the Points are already translated from unit.
				cppnInput[cppnIdxTX] = p.x;
				cppnInput[cppnIdxTY] = p.y;
				if (cppnIdxTZ != -1) {
					cppnInput[cppnIdxTZ] = p.y;
				}
			}
		}

		/**
		 * Query this CPPN.
		 * 
		 * @return The value of the (first) weight output. Other outputs can be retrieved with the various get methods.
		 */
		public double query() {
			if (includeDelta) {
				cppnInput[cppnIdxDX] = cppnInput[cppnIdxSX] - cppnInput[cppnIdxTX];
				cppnInput[cppnIdxDY] = cppnInput[cppnIdxSY] - cppnInput[cppnIdxTY];
				if (cppnIdxDZ != -1) {
					cppnInput[cppnIdxDZ] = cppnInput[cppnIdxSZ] - cppnInput[cppnIdxTZ];
				}
			}
			if (includeAngle) {
				double angle = (double) Math.atan2(cppnInput[cppnIdxSY] - cppnInput[cppnIdxTY], cppnInput[cppnIdxSX] - cppnInput[cppnIdxTX]);
				angle /= 2 * (double) Math.PI;
				if (angle < 0)
					angle += 1;
				cppnInput[cppnIdxAn] = angle;
			}

			cppnActivator.reset();
			cppnOutput = cppnActivator.next(cppnInput);
			return getWeight();
		}

		/**
		 * Query this CPPN with the specified coordinates.
		 * 
		 * @return The value of the (first) weight output. Other outputs can be retrieved with the various get methods.
		 */
		public double query(double sx, double sy, double tx, double ty) {
			setSourceCoordinates(sx, sy);
			setTargetCoordinates(tx, ty);
			return query();
		}

		/**
		 * Query this CPPN with the specified coordinates.
		 * 
		 * @return The value of the (first) weight output. Other outputs can be retrieved with the various get methods.
		 */
		public double query(double sx, double sy, double sz, double tx, double ty, double tz) {
			setSourceCoordinates(sx, sy, sz);
			setTargetCoordinates(tx, ty, tz);
			return query();
		}

		/**
		 * Query this CPPN with the specified coordinates.
		 * 
		 * @return The value of the (first) weight output. Other outputs can be retrieved with the various get methods.
		 */
		public double query(Point source, Point target) {
			setSourceCoordinates(source);
			setTargetCoordinates(target);
			return query();
		}

		/**
		 * Query this CPPN with the specified grid indices, see
		 * {@link CPPN#setSourceCoordinatesFromGridIndices(int, int, int)} and
		 * {@link CPPN#setTargetCoordinatesFromGridIndices(int, int, int)}. Subclasses of {@link HyperNEATTranscriber}
		 * should generally use this method instead of directly specifying coordinates as this method supports
		 * specifying arbitrary coordinates for neurons via {@link HyperNEATTranscriber#NEURON_POSITIONS_FOR_LAYER}.
		 * 
		 * @return The value of the (first) weight output. Other outputs can be retrieved with the various get methods.
		 */
		public double queryWithGridIndices(int sx, int sy, int sz, int tx, int ty, int tz) {
			setSourceCoordinatesFromGridIndices(sx, sy, sz);
			setTargetCoordinatesFromGridIndices(tx, ty, tz);
			return query();
		}

		/**
		 * Get the value of the weight. Should be called after calling {@link #query()}.
		 */
		public double getWeight() {
			return cppnOutput[cppnIdxW[0]];
		}
		
		/**
		 * Get the value of the weight. Should be called after calling {@link #query()}.
		 * The output will be transformed to be within the specified range and may optionally
		 * have a threshold applied.
		 */
		public double getRangedWeight(double minValue, double maxValue, double valueRange, double threshold) {
			return getRangedOutput(cppnIdxW[0], minValue, maxValue, valueRange, threshold);
		}

		/**
		 * Get the value of the weight for the specified layer in a layered feed-forward network encoded with
		 * {@link #HYPERNEAT_LAYER_ENCODING} set to false. Should be called after calling {@link #query()}.
		 */
		public double getWeight(int sourceLayerIndex) {
			return cppnOutput[cppnIdxW[sourceLayerIndex]];
		}
		
		/**
		 * Get the value of the weight for the specified layer in a layered feed-forward network encoded with
		 * {@link #HYPERNEAT_LAYER_ENCODING} set to false. Should be called after calling {@link #query()}.
		 * The output will be transformed to be within the specified range and may optionally
		 * have a threshold applied.
		 */
		public double getRangedWeight(int sourceLayerIndex, double minValue, double maxValue, double valueRange, double threshold) {
			return getRangedOutput(cppnIdxW[sourceLayerIndex], minValue, maxValue, valueRange, threshold);
		}

		/**
		 * Get the value of the weight for a bias connection. Should be called after calling {@link #query()}. The bias
		 * is usually queried for a target neuron after setting the source coordinate values to 0.
		 */
		public double getBiasWeight() {
			return cppnOutput[cppnIdxB[0]];
		}
		
		/**
		 * Get the value of the weight for a bias connection. Should be called after calling {@link #query()}. The bias
		 * is usually queried for a target neuron after setting the source coordinate values to 0.
		 * The output will be transformed to be within the specified range and may optionally
		 * have a threshold applied.
		 */
		public double getRangedBiasWeight(double minValue, double maxValue, double valueRange, double threshold) {
			return getRangedOutput(cppnIdxB[0], minValue, maxValue, valueRange, threshold);
		}

		/**
		 * Get the value of the weight for a bias connection for the specified layer in a layered feed-forward network
		 * encoded with {@link #HYPERNEAT_LAYER_ENCODING} set to false. Should be called after calling {@link #query()}.
		 * The bias is usually queried for a target neuron after setting the source coordinate values to 0.
		 */
		public double getBiasWeight(int sourceLayerIndex) {
			return cppnOutput[cppnIdxB[sourceLayerIndex]];
		}

		/**
		 * Get the value of the weight for a bias connection for the specified layer in a layered feed-forward network
		 * encoded with {@link #HYPERNEAT_LAYER_ENCODING} set to false. Should be called after calling {@link #query()}.
		 * The bias is usually queried for a target neuron after setting the source coordinate values to 0.
		 * The output will be transformed to be within the specified range and may optionally
		 * have a threshold applied.
		 */
		public double getRangedBiasWeight(int sourceLayerIndex, double minValue, double maxValue, double valueRange, double threshold) {
			return getRangedOutput(cppnIdxB[sourceLayerIndex], minValue, maxValue, valueRange, threshold);
		}

		/**
		 * Get the value of the link expression output (see {@link #HYPERNEAT_LEO}). Should be called after calling
		 * {@link #query()}.
		 */
		public double getLEO() {
			return cppnOutput[cppnIdxL[0]];
		}

		/**
		 * Get the value of the link expression output (see {@link #HYPERNEAT_LEO}) for the specified layer in a layered
		 * feed-forward network encoded with {@link #HYPERNEAT_LAYER_ENCODING} set to false. Should be called after
		 * calling {@link #query()}.
		 */
		public double getLEO(int sourceLayerIndex) {
			return cppnOutput[cppnIdxL[sourceLayerIndex]];
		}

		/**
		 * Get the value of the output with the given index. This is useful for sub-classes of HyperNEATTranscriber that
		 * add additional outputs to the CPPN.
		 */
		public double getOutput(int index) {
			return cppnOutput[index];
		}
		
		/**
		 * Get the value of the output with the given index. This is useful for sub-classes of HyperNEATTranscriber that
		 * add additional outputs to the CPPN. The output will be transformed to be within the specified range and may optionally
		 * have a threshold applied.
		 */
		public double getRangedOutput(int index, double minValue, double maxValue, double valueRange, double threshold) {
			double output = cppnOutput[index];
			if (cppnOutputUnitBounded) {
				// Scale to range [minValue, maxValue].
				output = output * valueRange + minValue;
			}
			else if (cppnOutputPlusMinusUnitBounded) {
				// Scale to range [minValue, maxValue].
				output = ((output + 1) * 0.5) * valueRange + minValue;
			}
			else {
				// Truncate to range [minValue, maxValue].
				output = Math.min(maxValue, Math.max(minValue, output));
			}
			
			// If thresholding is to be applied.
			if (threshold > 0) { 
				if (Math.abs(output) > threshold) {
					if (output > 0)
						output = (output - threshold) * (maxValue / (maxValue - threshold));
					else
						output = (output + threshold) * (minValue / (minValue + threshold));
				}
				else {
					output = 0;
				}
			}
			return output;
		}
	}

	/**
	 * Returns true iff the Link Expression Output (LEO) is enabled.
	 * 
	 * @see #HYPERNEAT_LEO
	 */
	public boolean leoEnabled() {
		return enableLEO;
	}

	/**
	 * @return the index in the CPPN inputs for the bias.
	 */
	public int getCPPNIndexBiasInput() {
		return 0;
	}

	/**
	 * @return the index in the CPPN inputs for the x coordinate for the target neuron.
	 */
	public int getCPPNIndexTargetX() {
		return cppnIdxTX;
	}

	/**
	 * @return the index in the CPPN inputs for the y coordinate for the target neuron.
	 */
	public int getCPPNIndexTargetY() {
		return cppnIdxTY;
	}

	/**
	 * @return the index in the CPPN inputs for the z coordinate for the target neuron. Returns -1 if this input is not
	 *         enabled.
	 */
	public int getCPPNIndexTargetZ() {
		return cppnIdxTZ;
	}

	/**
	 * @return the index in the CPPN inputs for the x coordinate for the source neuron.
	 */
	public int getCPPNIndexSourceX() {
		return cppnIdxSX;
	}

	/**
	 * @return the index in the CPPN inputs for the y coordinate for the source neuron.
	 */
	public int getCPPNIndexSourceY() {
		return cppnIdxSY;
	}

	/**
	 * @return the index in the CPPN inputs for the z coordinate for the source neuron. Returns -1 if this input is not
	 *         enabled.
	 */
	public int getCPPNIndexSourceZ() {
		return cppnIdxSZ;
	}

	/**
	 * @return the index in the CPPN inputs for the delta (difference) for the x coordinates (the difference between the
	 *         x coordinates of the source and target neurons). Returns -1 if this input is not enabled.
	 */
	public int getCPPNIndexDeltaX() {
		return cppnIdxDX;
	}

	/**
	 * @return the index in the CPPN inputs for the delta (difference) for the y coordinates (the difference between the
	 *         y coordinates of the source and target neurons). Returns -1 if this input is not enabled.
	 */
	public int getCPPNIndexDeltaY() {
		return cppnIdxDY;
	}

	/**
	 * @return the index in the CPPN inputs for the delta (difference) for the z coordinates (the difference between the
	 *         z coordinates of the source and target neurons). Returns -1 if this input is not enabled.
	 */
	public int getCPPNIndexDeltaZ() {
		return cppnIdxDZ;
	}

	/**
	 * @return the index in the CPPN inputs for the angle in the XY plane between the source and target neuron
	 *         coordinates (relative to the line X axis). Returns -1 if this input is not enabled.
	 */
	public int getCPPNIndexAngle() {
		return cppnIdxAn;
	}

	/**
	 * @return an array containing the indexes in the CPPN outputs for the weight value(s).
	 */
	public int[] getCPPNIndexWeight() {
		return cppnIdxW;
	}

	/**
	 * @return an array containing the indexes in the CPPN outputs for the bias value(s). Returns [-1] if this output is
	 *         not enabled.
	 */
	public int[] getCPPNIndexBiasOutput() {
		return cppnIdxB;
	}

	/**
	 * @return an array containing the indexes in the CPPN outputs for the Link Expression Output (LEO) value(s).
	 *         Returns [-1] if this output is not enabled.
	 */
	public int[] getCPPNIndexLEO() {
		return cppnIdxL;
	}
}