/**
 * @author <a href="mailto:cmoon@expedia.com">cmoon</a>
 */
public class P2Engine {

    private double[] q;
    private double[] dn;
    private double[] np;
    private int[] n;
    private int count;
    private int marker_count;

    public P2Engine() {
        count = 0;
        addEndMarkers();
    }

    public P2Engine(double quant) {
        count = 0;
        addEndMarkers();
        addQuantile(quant);
    }

    private void addEndMarkers() {
        marker_count = 2;
        q = new double[marker_count];
        dn = new double[marker_count];
        np = new double[marker_count];
        n = new int[marker_count];
        dn[0] = 0.0;
        dn[1] = 1.0;

        updateMarkers();
    }

    /**
     * Return marker pointer index value.
     */
    private int allocateMarkers(int count) {
        double[] newq = new double[marker_count + count];
        double[] newdn = new double[marker_count + count];
        double[] newnp = new double[marker_count + count];
        int[] newn = new int[marker_count + count];

        System.arraycopy(q, 0, newq, 0, 2);
        System.arraycopy(dn, 0, newdn, 0, 2);
        System.arraycopy(np, 0, newnp, 0, 2);
        System.arraycopy(n, 0, newn, 0, 2);

        q = newq;
        dn = newdn;
        np = newnp;
        n = newn;

        marker_count += count;

        return marker_count - count;
    }

    private void updateMarkers() {
        bubbleSort(dn, marker_count);

	/* Then entirely reset np markers, since the marker count changed */
        for (int i = 0; i < marker_count; i++) {
            np[i] = (marker_count - 1) * dn[i] + 1;
        }
    }

    private void addQuantile(double quant) {
        int pointer = allocateMarkers(3);

	/* Add in appropriate dn markers */
        dn[pointer] = quant;
        dn[pointer + 1] = quant / 2.0;
        dn[pointer + 2] = (1.0 + quant) / 2.0;

        updateMarkers();
    }

    private void addEqualSpacing(int count) {
        int pointer = allocateMarkers(count - 1);

	/* Add in appropriate dn markers */
        for (int i = 1; i < count; i++) {
            dn[pointer + i - 1] = 1.0 * i / count;
        }

        updateMarkers();
    }

    private int sign(double d) {
        if (d >= 0.0) {
            return 1;
        } else {
            return -1;
        }
    }

    //

    /**
     * Simple bubblesort, because bubblesort is efficient for small count, and the count is likely
     * to be small.
     */
    private void bubbleSort(double q[], int count) {
        double k;
        int i, j;
        for (j = 1; j < count; j++) {
            k = q[j];
            i = j - 1;

            while (i >= 0 && q[i] > k) {
                q[i + 1] = q[i];
                i--;
            }
            q[i + 1] = k;
        }
    }

    /**
     * Parabolic algorithm.
     */
    private double parabolic(int i, int d) {
        return q[i] + d / (double) (n[i + 1] - n[i - 1]) * (
            (n[i] - n[i - 1] + d) * (q[i + 1] - q[i]) / (n[i + 1] - n[i])
            + (n[i + 1] - n[i] - d) * (q[i] - q[i - 1]) / (n[i] - n[i - 1]));
    }

    /**
     * Linear algorithm.
     */
    private double linear(int i, int d) {
        return q[i] + d * (q[i + d] - q[i]) / (n[i + d] - n[i]);
    }

    /**
     * Add a new observation.
     */
    public void add(double data) {
        int i;
        int k = 0;
        double d;
        double newq;

        if (count >= marker_count) {
            count++;

            // B1
            if (data < q[0]) {
                q[0] = data;
                k = 1;
            } else if (data >= q[marker_count - 1]) {
                q[marker_count - 1] = data;
                k = marker_count - 1;
            } else {
                for (i = 1; i < marker_count; i++) {
                    if (data < q[i]) {
                        k = i;
                        break;
                    }
                }
            }

            // B2
            for (i = k; i < marker_count; i++) {
                n[i]++;
                np[i] = np[i] + dn[i];
            }
            for (i = 0; i < k; i++) {
                np[i] = np[i] + dn[i];
            }

            // B3
            for (i = 1; i < marker_count - 1; i++) {
                d = np[i] - n[i];
                if ((d >= 1.0 && n[i + 1] - n[i] > 1)
                    || (d <= -1.0 && n[i - 1] - n[i] < -1.0)) {
                    newq = parabolic(i, sign(d));
                    if (q[i - 1] < newq && newq < q[i + 1]) {
                        q[i] = newq;
                    } else {
                        q[i] = linear(i, sign(d));
                    }
                    n[i] += sign(d);
                }
            }
        } else {
            q[count] = data;
            count++;

            if (count == marker_count) {
                // We have enough to start the algorithm, initialize
                bubbleSort(q, marker_count);

                for (i = 0; i < marker_count; i++) {
                    n[i] = i + 1;
                }
            }
        }
    }

    /**
     * Return the quantile value.
     * @return
     */
    public double result() {
        if (marker_count != 5) {
            throw new RuntimeException("Multiple quantiles in use");
        }
        return result(dn[(marker_count - 1) / 2]);
    }

    /**
     * Return nearest quantile passed as a parameter. <p>In most cases, the return value would not
     * be accurate because it picks up the nearest quantile among post calculated quantiles. </p>
     */
    public double result(double quantile) {
        if (count < marker_count) {
            int closest = 1;
            bubbleSort(q, count);
            for (int i = 2; i < count; i++) {
                if (Math.abs(((double) i) / count - quantile) < Math.abs(
                    ((double) closest) / marker_count - quantile)) {
                    closest = i;
                }
            }
            return q[closest];
        } else {
            // Figure out which quantile is the one we're looking for by nearest dn
            int closest = 1;
            for (int i = 2; i < marker_count - 1; i++) {
                if (Math.abs(dn[i] - quantile) < Math.abs(dn[closest] - quantile)) {
                    closest = i;
                }
            }
            return q[closest];
        }
    }
}
