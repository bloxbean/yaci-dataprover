package com.bloxbean.cardano.dataprover.providers.epochstake;

/**
 * DTO representing epoch stake distribution data.
 */
public class EpochStake {
    private Integer epoch;
    private String address;
    private Long amount;
    private String poolId;

    public EpochStake() {
    }

    public EpochStake(Integer epoch, String address, Long amount, String poolId) {
        this.epoch = epoch;
        this.address = address;
        this.amount = amount;
        this.poolId = poolId;
    }

    public Integer getEpoch() {
        return epoch;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    @Override
    public String toString() {
        return "EpochStake{" +
                ", epoch=" + epoch +
                ", address='" + address + '\'' +
                ", amount=" + amount +
                ", poolId='" + poolId + '\'' +
                '}';
    }
}
