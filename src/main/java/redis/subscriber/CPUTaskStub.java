package redis.subscriber;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Stub class to emulate some cpu intensive computation during message processing. Calculate
 * Pi using Nilakantha series to n digits accuracy.
 */
public class CPUTaskStub {

    public static String calculatePi(int digits) {
        BigDecimal pi = BigDecimal.valueOf(3);
        BigDecimal one = BigDecimal.valueOf(1);
        BigDecimal two = BigDecimal.valueOf(2);
        BigDecimal four = BigDecimal.valueOf(4);
        BigDecimal firstMult = BigDecimal.valueOf(2);

        boolean positive = true;
        while (true) {
            BigDecimal fract = four.divide(firstMult.multiply(firstMult.add(one))
                    .multiply(firstMult.add(two)), MathContext.DECIMAL128);
            if (positive) {
                pi = pi.add(fract);
            } else {
                pi = pi.subtract(fract);
            }
            if (pi.precision() >= digits) {
                break;
            }
            positive = !positive;
            firstMult = firstMult.add(two);
        }
        return pi.setScale(digits, RoundingMode.DOWN).toString();
    }
}