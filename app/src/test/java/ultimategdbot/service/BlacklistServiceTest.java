package ultimategdbot.service;

import org.junit.jupiter.api.Test;
import ultimategdbot.database.BlacklistDao;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlacklistServiceTest {

    /**
     * Constructs a BlacklistService bypassing the factory (which requires a MongoDB-backed BlacklistDao) by accessing
     * the private constructor directly via reflection.
     */
    private BlacklistService createService(Set<Long> initialIds) throws Exception {
        var constructor = BlacklistService.class.getDeclaredConstructor(BlacklistDao.class, Set.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, new HashSet<>(initialIds));
    }

    @Test
    void blacklist_emptyCache_returnsEmptySet() throws Exception {
        var service = createService(Set.of());
        assertTrue(service.blacklist().isEmpty());
    }

    @Test
    void blacklist_withIds_returnsExpectedIds() throws Exception {
        var service = createService(Set.of(111L, 222L, 333L));
        assertEquals(Set.of(111L, 222L, 333L), service.blacklist());
    }

    @Test
    void blacklist_returnedSet_isUnmodifiable() throws Exception {
        var service = createService(Set.of(123L));
        var blacklist = service.blacklist();
        assertThrows(UnsupportedOperationException.class, () -> blacklist.add(999L));
        assertThrows(UnsupportedOperationException.class, () -> blacklist.remove(123L));
    }

    @Test
    void blacklist_withSingleId_containsThatId() throws Exception {
        var service = createService(Set.of(42L));
        assertTrue(service.blacklist().contains(42L));
        assertFalse(service.blacklist().contains(99L));
    }
}
