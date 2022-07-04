package Engine.Weights;

public interface Weight {

    double calculate(WeighingStrategy weigher);

    void read(WeighingStrategy weigher);
//
//    void write(WeighingStrategy weigher);


}
