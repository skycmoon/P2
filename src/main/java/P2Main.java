import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Random;

/**
 * @author <a href="mailto:cmoon@expedia.com">cmoon</a>
 */
public class P2Main {

    private static final int SAMPLE_SIZE = 1000000;

    public static void main(String... args) {

        double[] moreValues = new double[SAMPLE_SIZE];

        Random rand = new Random();
        for (int i = 0; i < moreValues.length; i++) {
            moreValues[i] = rand.nextDouble() * 100;
        }

        // Feed samples to DescriptiveStatistics class.
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        for (double i : moreValues) {
            descriptiveStatistics.addValue(i);
        }

        long start = System.nanoTime();

        // Feed samples to P2 implementation.
        P2Engine p2Engine = new P2Engine(0.99);
        for (double value : moreValues) {
            p2Engine.add(value);
        }
        double estimate = p2Engine.result();

        long end = System.nanoTime();

        long takenTime = end - start;


        double actual = descriptiveStatistics.getPercentile(99);

        System.out.println("Sample size: " + SAMPLE_SIZE);
        System.out.println("Taken time: " + takenTime);
        System.out.println("Actual value: " + actual);
        System.out.println("Estimated value: " + estimate);
        System.out.println("Difference: " + Math.abs(estimate - actual));

    }

}
