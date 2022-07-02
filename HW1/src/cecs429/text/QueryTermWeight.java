package cecs429.text;

public class QueryTermWeight implements Weight {

    private double mValue;
    private WeightCalculator mCalculator;

    public QueryTermWeight(WeightCalculator calculator) {
        mCalculator = calculator;
    }

    @Override
    public void calculate() {
        mValue = mCalculator.calculateWqt();
    }

    @Override
    public void write() {

    }
}
