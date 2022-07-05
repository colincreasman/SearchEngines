package Engine.Weights;

import App.Driver;
import App.Driver.WeighingScheme;

public interface Weight {

    void calculate(WeighingScheme scheme);

    void read(WeighingScheme scheme);
}
