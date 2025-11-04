package org.ulpgc.bd;

public class BenchmarkUtils {

    public static double calcEfficiency(double baseThroughput, double maxThroughput, int threads) {
        return (maxThroughput / (baseThroughput * threads)) * 100;
    }
}
