package cecs429.text;

public class DocTermWeight implements Weight {
    private double mValue;
    private WeightCalculator mCalculator;

    public DocTermWeight(WeightCalculator calculator) {
        mCalculator = calculator;
    }

    @Override
    public void calculate() {
        mValue = mCalculator.calculateWdt();
    }

    @Override
    public void write() {

    }
}
