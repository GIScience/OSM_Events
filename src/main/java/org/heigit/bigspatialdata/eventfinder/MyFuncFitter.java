package org.heigit.bigspatialdata.eventfinder;

import java.util.Collection;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;

class MyFunc implements ParametricUnivariateFunction {
	public double value(double t, double ... parameters) {
		return parameters[0]/(1.0 + parameters[1] * Math.exp(-parameters[2] * (t - parameters[3]))); // logistic function to be estimated
	}

	public double[] gradient(double t, double... parameters) { // partial derivatives of the function
		final double a = parameters[0];
		final double b = parameters[1];
		final double k = parameters[2];
		final double u = parameters[3];
		
		return new double[] {
				1 / (b * Math.exp(-k * (t - u)) + 1),
				-a * Math.exp(k*(t-u)) / Math.pow(Math.exp(k*(t-u)) + b, 2),
				a*b*(t-u)*Math.exp(k*(t-u)) / Math.pow(b + Math.exp(k*(t-u)), 2),
				-a*b*k*Math.exp(k*(t-u)) / Math.pow(b + Math.exp(k*(t-u)), 2)
		};		
	}
}

public class MyFuncFitter extends AbstractCurveFitter {
	protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
		final int len = points.size();
		final double[] target = new double[len];
		final double[] weights = new double[len];
		final double[] initialGuess = {1.0, 1.0, 1.0, 1.0};
		
		int i = 0;
		for (WeightedObservedPoint point: points) {
			target[i] = point.getY();
			weights[i] = point.getWeight();
			i += 1;
		}
		
		final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction(new MyFunc(), points);
		
		return new LeastSquaresBuilder()
				.maxEvaluations(Integer.MAX_VALUE)
				.maxIterations(Integer.MAX_VALUE)
				.start(initialGuess)
				.target(target)
				.weight(new DiagonalMatrix(weights))
				.model(model.getModelFunction(), model.getModelFunctionJacobian())
				.build();
	}
	
	protected RealMatrix getCovariances(LeastSquaresProblem prob) {
		final Optimum topt = getOptimizer().optimize(prob);
		return topt.getCovariances(1.5e-14);
	}
}
