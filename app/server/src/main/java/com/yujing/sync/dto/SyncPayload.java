package com.yujing.sync.dto;

import java.util.List;

/**
 * Delta payload returned when a client asks for {@code ?since=}: rows changed
 * after that instant plus tombstones so offline clients can drop deleted rows.
 */
public record SyncPayload<T>(List<T> items, List<TombstoneView> tombstones, String serverTime) {
}
