package io.appform.hope.core.visitors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.VisitorAdapter;
import io.appform.hope.core.combiners.AndCombiner;
import io.appform.hope.core.combiners.OrCombiner;
import io.appform.hope.core.operators.*;
import io.appform.hope.core.utils.Converters;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class Evaluator {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private final Configuration configuration;
    private final ParseContext parseContext;

    public Evaluator() {
        configuration = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
        parseContext = JsonPath.using(configuration);

    }

    public boolean evaluate(Evaluatable evaluatable, JsonNode node) {
        return evaluatable.accept(new LogicEvaluator(new EvaluationContext(parseContext
                                                                                   .parse(node), this)));
    }

    @Data
    @Builder
    public static class EvaluationContext {
        private final DocumentContext jsonContext;
        private final Evaluator evaluator;
        private final Map<String, JsonNode> jsonPathEvalCache = new HashMap<>(128);
    }

    public static class LogicEvaluator extends VisitorAdapter<Boolean> {

        private final EvaluationContext evaluationContext;

        public LogicEvaluator(
                EvaluationContext evaluationContext) {
            super(true);
            this.evaluationContext = evaluationContext;
        }

        public boolean evaluate(Evaluatable evaluatable) {
            return evaluatable.accept(this);
        }

        @Override
        public Boolean visit(AndCombiner andCombiner) {
            return andCombiner.getExpressions()
                    .stream()
                    .allMatch(expression -> expression.accept(new LogicEvaluator(evaluationContext)));
        }

        @Override
        public Boolean visit(OrCombiner orCombiner) {
            return orCombiner.getExpressions()
                    .stream()
                    .anyMatch(expression -> expression.accept(new LogicEvaluator(evaluationContext)));
        }

        @Override
        public Boolean visit(Equals equals) {
            final Object lhs = Converters.objectValue(evaluationContext, equals.getLhs(), null);
            final Object rhs = Converters.objectValue(evaluationContext, equals.getRhs(), null);
            return Objects.equals(lhs, rhs);
        }

        @Override
        public Boolean visit(NotEquals notEquals) {
            final Object lhs = Converters.objectValue(evaluationContext, notEquals.getLhs(), null);
            final Object rhs = Converters.objectValue(evaluationContext, notEquals.getRhs(), null);
            return !Objects.equals(lhs, rhs);
        }

        @Override
        public Boolean visit(Greater greater) {
            final Number lhs = Converters.numericValue(evaluationContext, greater.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, greater.getRhs(), 0);
            return lhs.doubleValue() > rhs.doubleValue();
        }

        @Override
        public Boolean visit(GreaterEquals greaterEquals) {
            final Number lhs = Converters.numericValue(evaluationContext, greaterEquals.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, greaterEquals.getRhs(), 0);
            return lhs.doubleValue() >= rhs.doubleValue();
        }

        @Override
        public Boolean visit(Lesser lesser) {
            final Number lhs = Converters.numericValue(evaluationContext, lesser.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, lesser.getRhs(), 0);
            return lhs.doubleValue() < rhs.doubleValue();
        }

        @Override
        public Boolean visit(LesserEquals lesserEquals) {
            final Number lhs = Converters.numericValue(evaluationContext, lesserEquals.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, lesserEquals.getRhs(), 0);
            return lhs.doubleValue() <= rhs.doubleValue();
        }

        @Override
        public Boolean visit(And and) {
            boolean lhs = Converters.booleanValue(evaluationContext, and.getLhs(), false);
            boolean rhs = Converters.booleanValue(evaluationContext, and.getRhs(), false);

            return lhs && rhs;
        }

        @Override
        public Boolean visit(Or or) {
            boolean lhs = Converters.booleanValue(evaluationContext, or.getLhs(), false);
            boolean rhs = Converters.booleanValue(evaluationContext, or.getRhs(), false);

            return lhs || rhs;
        }

        @Override
        public Boolean visit(Not not) {
            boolean operand = Converters.booleanValue(evaluationContext, not.getOperand(), false);
            return !operand;
        }

    }

}
