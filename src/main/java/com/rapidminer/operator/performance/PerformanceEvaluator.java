/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.operator.performance;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.*;
import com.rapidminer.tools.LogService;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * <p>
 * A performance evaluator is an operator that expects a test {@link ExampleSet} as input, whose
 * elements have both true and predicted labels, and delivers as output a list of performance values
 * according to a list of performance criteria that it calculates. If an input performance vector
 * was already given, this is used for keeping the performance values.
 * </p>
 *
 * <p>
 * All of the performance criteria can be switched on using boolean parameters. Their values can be
 * queried by a ProcessLogOperator using the same names. The main criterion is used for comparisons
 * and need to be specified only for processes where performance vectors are compared, e.g. feature
 * selection processes. If no other main criterion was selected the first criterion in the resulting
 * performance vector will be assumed to be the main criterion.
 * </p>
 *
 * <p>
 * The resulting performance vectors are usually compared with a standard performance comparator
 * which only compares the fitness values of the main criterion. Other implementations than this
 * simple comparator can be specified using the parameter <var>comparator_class</var>. This may for
 * instance be useful if you want to compare performance vectors according to the weighted sum of
 * the individual criteria. In order to implement your own comparator, simply subclass
 * {@link PerformanceComparator}. Please note that for true multi-objective optimization usually
 * another selection scheme is used instead of simply replacing the performance comparator.
 * </p>
 *
 * <p>
 * Additional user-defined implementations of {@link PerformanceCriterion} can be specified by using
 * the parameter list <var>additional_performance_criteria</var>. Each key/value pair in this list
 * must specify a fully qualified classname (as the key), and a string parameter (as value) that is
 * passed to the constructor. Please make sure that the class files are in the classpath (this is
 * the case if the implementations are supplied by a plugin) and that they implement a one-argument
 * constructor taking a string parameter. It must also be ensured that these classes extend
 * {@link MeasuredPerformance} since the PerformanceEvaluator operator will only support these
 * criteria. Please note that only the first three user defined criteria can be used as logging
 * value with names &quot;user1&quot;, ... , &quot;user3&quot;.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class PerformanceEvaluator extends AbstractPerformanceEvaluator {

	/**
	 * The parameter name for &quot;The weights for all classes (first column: class name, second
	 * column: weight), empty: using 1 for all classes.&quot;
	 */
	public static final String PARAMETER_CLASS_WEIGHTS = "class_weights";

	/** The proper criteria to the names. */
	private static final Class<?>[] SIMPLE_CRITERIA_CLASSES = {
		com.rapidminer.operator.performance.RootMeanSquaredError.class,
		com.rapidminer.operator.performance.AbsoluteError.class,
		com.rapidminer.operator.performance.RelativeError.class,
		com.rapidminer.operator.performance.LenientRelativeError.class,
		com.rapidminer.operator.performance.StrictRelativeError.class,
		com.rapidminer.operator.performance.NormalizedAbsoluteError.class,
		com.rapidminer.operator.performance.RootRelativeSquaredError.class,
		com.rapidminer.operator.performance.SquaredError.class,
		com.rapidminer.operator.performance.CorrelationCriterion.class,
		com.rapidminer.operator.performance.SquaredCorrelationCriterion.class,
		com.rapidminer.operator.performance.PredictionAverage.class,
		com.rapidminer.operator.performance.AreaUnderCurve.class,
		com.rapidminer.operator.performance.CrossEntropy.class, com.rapidminer.operator.performance.Margin.class,
		com.rapidminer.operator.performance.SoftMarginLoss.class, com.rapidminer.operator.performance.LogisticLoss.class };

	public PerformanceEvaluator(OperatorDescription description) {
		super(description);
	}

	/** Does nothing. */
	@Override
	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {}

	@Override
	protected double[] getClassWeights(Attribute label) throws UndefinedParameterError {
		double[] weights = null;
		if (isParameterSet(PARAMETER_CLASS_WEIGHTS)) {
			weights = new double[label.getMapping().size()];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1.0d;
			}
			List<String[]> classWeights = getParameterList(PARAMETER_CLASS_WEIGHTS);
			Iterator<String[]> i = classWeights.iterator();
			while (i.hasNext()) {
				String[] classWeightArray = i.next();
				String className = classWeightArray[0];
				double classWeight = Double.valueOf(classWeightArray[1]);
				int index = label.getMapping().mapString(className);
				weights[index] = classWeight;
			}

			// logging
			List<Double> weightList = new LinkedList<Double>();
			for (double d : weights) {
				weightList.add(d);
			}
			log(getName() + ": used class weights --> " + weightList);
		}
		return weights;
	}

	@Override
	public List<PerformanceCriterion> getCriteria() {
		List<PerformanceCriterion> performanceCriteria = new LinkedList<PerformanceCriterion>();
		// simple criteria
		for (int i = 0; i < SIMPLE_CRITERIA_CLASSES.length; i++) {
			try {
				performanceCriteria.add((PerformanceCriterion) SIMPLE_CRITERIA_CLASSES[i].newInstance());
			} catch (InstantiationException e) {
				// LogService.getGlobal().logError("Cannot instantiate " +
				// SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
				LogService
				.getRoot()
				.log(Level.SEVERE,
						"com.rapidminer.operator.performance.PerformanceEvaluator.instantiating_simple_criteria_classes_error",
						SIMPLE_CRITERIA_CLASSES[i]);
			} catch (IllegalAccessException e) {
				// LogService.getGlobal().logError("Cannot instantiate " +
				// SIMPLE_CRITERIA_CLASSES[i] + ". Skipping...");
				LogService
				.getRoot()
				.log(Level.SEVERE,
						"com.rapidminer.operator.performance.PerformanceEvaluator.instantiating_simple_criteria_classes_error",
						SIMPLE_CRITERIA_CLASSES[i]);
			}
		}

		// multi class classification criteria
		for (int i = 0; i < MultiClassificationPerformance.NAMES.length; i++) {
			performanceCriteria.add(new MultiClassificationPerformance(i));
		}

		// multi class classification criteria
		for (int i = 0; i < WeightedMultiClassPerformance.NAMES.length; i++) {
			performanceCriteria.add(new WeightedMultiClassPerformance(i));
		}

		// rank correlation criteria
		for (int i = 0; i < RankCorrelation.NAMES.length; i++) {
			performanceCriteria.add(new RankCorrelation(i));
		}
		return performanceCriteria;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeList(PARAMETER_CLASS_WEIGHTS,
				"The weights for all classes, empty: using 1 for all classes.", new ParameterTypeString("class_name",
						"The name of the class."), new ParameterTypeDouble("weight", "The weight for this class.", 0.0d,
								Double.POSITIVE_INFINITY, 1.0d)));
		return types;
	}

	@Override
	protected boolean canEvaluate(int valueType) {
		return true;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case BINOMINAL_LABEL:
			case POLYNOMINAL_LABEL:
			case ONE_CLASS_LABEL:
				return true;
			case POLYNOMINAL_ATTRIBUTES:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
			default:
				return false;
		}
	}
}
