package io.appform.hope.core.functions.impl;

import io.appform.hope.core.Value;
import io.appform.hope.core.functions.FunctionImplementation;
import io.appform.hope.core.functions.HopeFunction;
import io.appform.hope.core.utils.Converters;
import io.appform.hope.core.values.NumericValue;
import io.appform.hope.core.visitors.Evaluator;

/**
 *
 */
@FunctionImplementation(
        value = "abs",
        paramTypes = {
                Value.class
        })
public class Abs extends HopeFunction<NumericValue> {

    private final Value param;

    public Abs(Value param) {
        this.param = param;
    }

    @Override
    public NumericValue apply(Evaluator.EvaluationContext evaluationContext) {
        double value = Converters.numericValue(evaluationContext, param, 0)
                .doubleValue();
        return new NumericValue(Math.abs(value));
    }
}