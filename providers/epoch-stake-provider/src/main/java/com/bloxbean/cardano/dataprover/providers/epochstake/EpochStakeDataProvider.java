package com.bloxbean.cardano.dataprover.providers.epochstake;

import com.bloxbean.cardano.dataprover.exception.DataProviderException;
import com.bloxbean.cardano.dataprover.exception.SerializationException;
import com.bloxbean.cardano.dataprover.service.provider.AbstractDataProvider;
import com.bloxbean.cardano.dataprover.service.provider.ValidationResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data provider for Cardano epoch stake distribution.
 * Plugin that can be loaded from a JAR file.
 */
public class EpochStakeDataProvider extends AbstractDataProvider<EpochStake> {

    private static final String PROVIDER_NAME = "epoch-stake";

    private static final String SQL_EXISTS_BY_EPOCH =
            "SELECT EXISTS(SELECT 1 FROM epoch_stake WHERE epoch = ?)";

    private static final String SQL_FIND_BY_EPOCH =
            "SELECT active_epoch, address, amount, pool_id FROM epoch_stake WHERE epoch = ? and amount > 0";

    private DataSource dataSource;

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getDescription() {
        return "Cardano epoch stake distribution from Yaci Store";
    }

    @Override
    public void initialize(Map<String, Object> config) {
        log.info("Initializing EpochStakeDataProvider with config");

        String jdbcUrl = (String) config.get("jdbc-url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            log.warn("No JDBC URL provided for EpochStakeDataProvider, provider will not be functional");
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setPoolName("EpochStakePool");

        this.dataSource = new HikariDataSource(hikariConfig);

        log.info("EpochStakeDataProvider initialized with JDBC datasource");
    }

    @Override
    public List<EpochStake> fetchData(Map<String, Object> config) throws DataProviderException {
        if (dataSource == null) {
            throw new DataProviderException(PROVIDER_NAME, "DataSource not initialized");
        }

        try {
            Integer epoch = getRequiredConfig(config, "epoch", Integer.class);

            log.info("Fetching epoch stake data for epoch: {}", epoch);

            if (!existsByEpoch(epoch)) {
                throw new DataProviderException(PROVIDER_NAME,
                        "No stake data found for epoch: " + epoch);
            }

            List<EpochStake> stakes = findByEpoch(epoch);

            log.info("Fetched {} stake entries for epoch {}", stakes.size(), epoch);

            return stakes;

        } catch (DataProviderException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new DataProviderException(PROVIDER_NAME,
                    "Invalid configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataProviderException(PROVIDER_NAME,
                    "Failed to fetch epoch stake data: " + e.getMessage(), e);
        }
    }

    private boolean existsByEpoch(Integer epoch) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS_BY_EPOCH)) {

            stmt.setInt(1, epoch);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
                return false;
            }
        }
    }

    private List<EpochStake> findByEpoch(Integer epoch) throws SQLException {
        List<EpochStake> stakes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_EPOCH)) {

            stmt.setInt(1, epoch - 2);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EpochStake stake = new EpochStake(
                            rs.getInt("active_epoch"),
                            rs.getString("address"),
                            rs.getLong("amount"),
                            rs.getString("pool_id")
                    );
                    stakes.add(stake);
                }
            }
        }

        return stakes;
    }

    @Override
    public byte[] serializeKey(EpochStake data) throws SerializationException {
        try {
            return AddressConverter.stakeAddressToCredentialHash(data.getAddress());
        } catch (Exception e) {
            log.error("Failed to serialize key for address: {}", data.getAddress(), e);
            throw new SerializationException(
                    "Failed to convert stake address to credential hash: " + data.getAddress(), e);
        }
    }

    @Override
    public byte[] serializeValue(EpochStake data) throws SerializationException {
        try {
            return CborSerializer.serializeStakeInfo(data.getAmount(), data.getPoolId());
        } catch (Exception e) {
            log.error("Failed to serialize value for stake: {}", data, e);
            throw new SerializationException(
                    "Failed to serialize stake info to CBOR", e);
        }
    }

    @Override
    public ValidationResult validate(EpochStake data) {
        ValidationResult epochValid = validateNotNull(data.getEpoch(), "epoch");
        if (!epochValid.isValid()) {
            return epochValid;
        }

        ValidationResult addressValid = validateNotBlank(data.getAddress(), "address");
        if (!addressValid.isValid()) {
            return addressValid;
        }

        ValidationResult amountValid = validatePositive(data.getAmount(), "amount");
        if (!amountValid.isValid()) {
            return amountValid;
        }

        ValidationResult poolIdValid = validateNotBlank(data.getPoolId(), "poolId");
        if (!poolIdValid.isValid()) {
            return poolIdValid;
        }

        if (!data.getPoolId().matches("[0-9a-fA-F]{56}")) {
            return ValidationResult.failure("poolId must be 56 hex characters");
        }

        return ValidationResult.success();
    }

    @Override
    public Class<EpochStake> getDataType() {
        return EpochStake.class;
    }
}
