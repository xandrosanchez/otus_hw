package org.example.mapper;

import org.example.GenerationType;

/**
 * Инкапсулирует информацию о стратегии генерации ID
 */
public class GenerationStrategy {
    private final GenerationType generationType;
    private final String sequenceName;

    public GenerationStrategy(GenerationType generationType, String sequenceName) {
        this.generationType = generationType;
        this.sequenceName = sequenceName;
    }

    public GenerationStrategy(GenerationType generationType) {
        this(generationType, "");
    }

    public GenerationType getGenerationType() {
        return generationType;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public boolean isIdentity() {
        return generationType == GenerationType.IDENTITY;
    }

    public boolean isSequence() {
        return generationType == GenerationType.SEQUENCE;
    }

    public boolean isUuid() {
        return generationType == GenerationType.UUID;
    }

    public boolean isAuto() {
        return generationType == GenerationType.AUTO;
    }

    @Override
    public String toString() {
        return "GenerationStrategy{" + "generationType="
                + generationType + ", sequenceName='"
                + sequenceName + '\'' + '}';
    }
}
