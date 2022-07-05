package Engine.Weights;

public interface Weight {

    void calculate(WeighingStrategy weigher);

    void read(WeighingStrategy weigher);
}
