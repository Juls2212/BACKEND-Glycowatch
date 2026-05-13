package com.glycowatch.intelligence.model;

public class GlucoseAnalysisMetrics {

    private Double averageLast24h;
    private Double averageLast7d;
    private Double minLast7d;
    private Double maxLast7d;
    private Integer countLast24h;
    private Integer countLast7d;
    private Integer highReadingsCount;
    private Integer lowReadingsCount;
    private Double variability;
    private Double latestValue;

    public Double getAverageLast24h() {
        return averageLast24h;
    }

    public void setAverageLast24h(Double averageLast24h) {
        this.averageLast24h = averageLast24h;
    }

    public Double getAverageLast7d() {
        return averageLast7d;
    }

    public void setAverageLast7d(Double averageLast7d) {
        this.averageLast7d = averageLast7d;
    }

    public Double getMinLast7d() {
        return minLast7d;
    }

    public void setMinLast7d(Double minLast7d) {
        this.minLast7d = minLast7d;
    }

    public Double getMaxLast7d() {
        return maxLast7d;
    }

    public void setMaxLast7d(Double maxLast7d) {
        this.maxLast7d = maxLast7d;
    }

    public Integer getCountLast24h() {
        return countLast24h;
    }

    public void setCountLast24h(Integer countLast24h) {
        this.countLast24h = countLast24h;
    }

    public Integer getCountLast7d() {
        return countLast7d;
    }

    public void setCountLast7d(Integer countLast7d) {
        this.countLast7d = countLast7d;
    }

    public Integer getHighReadingsCount() {
        return highReadingsCount;
    }

    public void setHighReadingsCount(Integer highReadingsCount) {
        this.highReadingsCount = highReadingsCount;
    }

    public Integer getLowReadingsCount() {
        return lowReadingsCount;
    }

    public void setLowReadingsCount(Integer lowReadingsCount) {
        this.lowReadingsCount = lowReadingsCount;
    }

    public Double getVariability() {
        return variability;
    }

    public void setVariability(Double variability) {
        this.variability = variability;
    }

    public Double getLatestValue() {
        return latestValue;
    }

    public void setLatestValue(Double latestValue) {
        this.latestValue = latestValue;
    }
}
