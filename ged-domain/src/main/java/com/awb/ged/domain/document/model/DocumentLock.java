package com.awb.ged.domain.document.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentLock</h1>
 * <p>
 * Value Object representing a check-out lock on a document.
 * Prevents concurrent modifications while a user has locked/checked out the document.
 * </p>
 */
@Value
@Builder
public class DocumentLock {

    /** Identifier of the user who locked the document */
    UUID lockedBy;

    /** Timestamp when the lock was acquired in UTC */
    Instant lockedAt;

    /** Expiration timestamp of the lock in UTC */
    Instant expiration;

    /**
     * Checks if the lock is expired.
     *
     * @param now the current timestamp to check against
     * @return true if the lock has expired, false otherwise
     */
    public boolean isExpired(Instant now) {
        return expiration != null && now.isAfter(expiration);
    }
}
