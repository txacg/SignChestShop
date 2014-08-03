package co.technius.signchestshop.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.evilmidget38.UUIDFetcher;

public class UUIDUtil
{

    private static class OneNameWrapper implements Callable<UUID>
    {

        private final String name;

        OneNameWrapper(final String name)
        {
            this.name = name;
        }

        @Override
        public UUID call() throws Exception
        {
            @SuppressWarnings("deprecation")
            final OfflinePlayer p = Bukkit.getServer().getOfflinePlayer(name);
            if (p != null)
                return p.getUniqueId();

            final ArrayList<String> temp = new ArrayList<String>();
            temp.add(name);
            final Map<String, UUID> m = new UUIDFetcher(temp, false).call();
            return m.get(name);
        }
    }

    /**
     * @param name The name of the player.
     * @return A {@link Future} that returns the player's UUID.
     */
    public static Future<UUID> getUUID(final String name)
    {
        return pool.submit(new OneNameWrapper(name));
    }

    public static Future<Map<String, UUID>> getUUIDs(final ArrayList<String> names)
    {
        return pool.submit(new UUIDFetcher(names, false));
    }

    private static ExecutorService pool = Executors.newCachedThreadPool();

}