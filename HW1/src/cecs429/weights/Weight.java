package cecs429.weights;

import edu.csulb.Driver;
import static edu.csulb.Driver.WeighingScheme;


public interface Weight {

    void calculate(WeighingScheme scheme);

    void read(WeighingScheme scheme);

}