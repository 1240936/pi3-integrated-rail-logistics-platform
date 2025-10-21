package main.domain;

import java.util.Comparator;

/**
 * Comparator implementing FEFO (First-Expired-First-Out) ordering for {@link Box}.
 *
 * Ordering rules:
 * - expiryDate ascending; null expiry dates (no expiry) are placed last
 * - if expiry ties or both are null, order by receivedAt ascending
 * - if still tied, order by boxId ascending for deterministic ordering
 */
public class BoxFefoComparator implements Comparator<Box> {
    @Override
    public int compare(Box a, Box b) {
        // (a) expiry date asc, nulls last (non-expiring boxes go after expiring ones)
        if (a.getExpiryDate() == null && b.getExpiryDate() != null) return 1;
        if (a.getExpiryDate() != null && b.getExpiryDate() == null) return -1;
        if (a.getExpiryDate() != null && b.getExpiryDate() != null) {
            int c = a.getExpiryDate().compareTo(b.getExpiryDate());
            if (c != 0) return c;
        }
        // (b) receivedAt asc to preserve FIFO among equal expiry dates
        int d = a.getReceivedAt().compareTo(b.getReceivedAt());
        if (d != 0) return d;
        // (c) boxId ASC as final deterministic tie-breaker
        return a.getBoxId().compareTo(b.getBoxId());
    }
}


