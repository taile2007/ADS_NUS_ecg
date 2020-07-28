package com.nordicsemi.nrfUARTv2;

public class IIR_Filter {
    private double[] B_coeff = {0.145869755195673, -0.925285841831314, 2.69694235352519, -4.79411043461389, 5.75316833546117, -4.79411043461389, 2.69694235352519,
            -0.925285841831312, 0.145869755195673};
    private double[] A_coeff = {-6.62815676962616, 19.2980903872498, -32.2699397581598, 33.9302017305931, -22.9970371843728, 9.82924121450740, -2.42992391554021, 0.267524295418928};

    public double[] update_input_filter_array(double[] array, double newNumber) {
        for(int i = 8; i >= 1; i--) {
            array[i] = array[i-1];
        }

        array[0] = newNumber;
        return array;
    }

    public double[] update_output_filter_array(double[] array, double newNumber) {
        for(int i = 7; i >= 1; i--) {
            array[i] = array[i-1];
        }

        array[0] = newNumber;
        return array;
    }

    public double filter(double[] input, double[] priorOutputs) {
        double output = 0;

        for(int i = 0; i < 9; i++) {
            output += B_coeff[i]*input[i];
        }

        for(int j = 0; j < 8; j++) {
            output -= A_coeff[j]*priorOutputs[j];
        }

        return output;
    }
}
