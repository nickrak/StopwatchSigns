package nickrak.stopwatchsigns;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class StopwatchSigns extends JavaPlugin implements Runnable
{
	private static final Logger log = Logger.getLogger("StopwatchSigns");
	public static StopwatchSigns instance = null;

	private ConcurrentHashMap<String, Long> times = new ConcurrentHashMap<String, Long>();
	private ConcurrentHashMap<String, Byte> active = new ConcurrentHashMap<String, Byte>();
	private StopwatchBlockListener sbl = null;

	public final static void jout(String msg)
	{
		log.info("[StopwatchSigns] " + msg);
	}

	public final static void jerr(String msg)
	{
		log.severe("[StopwatchSigns] " + msg);
	}

	public final String getState(final String watchName)
	{
		if (this.active.containsKey(watchName))
		{
			switch (this.active.get(watchName))
			{
			case 0:
				return "Click to Start";
			case 1:
				return "Click to Stop";
			case 2:
				return "Click to Reset";
			default:
				return "ERROR";
			}
		}
		return "UNLINKED";
	}

	public final String getTime(final String watchName)
	{
		if (this.times.containsKey(watchName))
		{
		long t = this.times.get(watchName);
		final long maxt = (59 + 59 * 60 + 99 * 60 * 60);
		if (t > maxt)
		{
			jout(watchName + " overflowed max time, it has been reset");
			this.active.put(watchName, (byte) 0);
			this.times.put(watchName, 0l);
			return "00:00:00";
		}

		final int h = (int) t / (60 * 60);
		t %= (60 * 60);
		final int m = (int) t / 60;
		final int s = (int) (t % 60);

		return String.format("%d:%d:%d", h, m, s);
		}
		return "INVALID";
	}

	public StopwatchSigns()
	{
		instance = this;
	}

	@Override
	public final void onDisable()
	{
		this.saveConfig();
		jout("Disabled");
	}

	@Override
	public final void onEnable()
	{
		final PluginManager pm = this.getServer().getPluginManager();
		final StopwatchPlayerListener spl = new StopwatchPlayerListener();
		this.sbl = new StopwatchBlockListener();

		pm.registerEvent(Type.BLOCK_BREAK, this.sbl, Priority.Highest, this);
		pm.registerEvent(Type.SIGN_CHANGE, this.sbl, Priority.Highest, this);
		pm.registerEvent(Type.PLAYER_INTERACT, spl, Priority.Monitor, this);
		
		

		new Thread(this).start();

		jout("Enabled version " + this.getDescription().getVersion());
	}
	
	public final void prepWatch(final String watch)
	{
		synchronized (this.active)
		{
			if (!this.active.containsKey(watch)) this.active.put(watch, (byte) 0);
			if (!this.times.containsKey(watch)) this.times.put(watch, 0l);
		}
	}

	public final void toggleWatch(final String watch)
	{
		synchronized (this.active)
		{
			final byte status = (byte) ((this.active.get(watch) + 1) % 3);

			switch (status)
			{
			case 0: // Reset the timer
				this.active.put(watch, status);
				this.times.put(watch, 0l);
				break;
			case 1: // Start the timer
			case 2: // Stop the timer
				this.active.put(watch, status);
				break;
			}
		}
	}

	@Override
	public final void run()
	{
		int tid = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this.sbl, 20l, 20l);

		if (tid == -1)
		{
			jerr("Stopping primary timing thread due to scheduling failure.");
			return;
		}

		while (this.isEnabled())
		{
			long delayOffset = System.currentTimeMillis();

			for (final String watch : this.active.keySet())
			{
				synchronized (this.active)
				{
					if (this.active.get(watch) == 1)
					{
						final long t = this.times.get(watch) + 1;
						this.times.put(watch, t);
					}
				}
			}
			delayOffset += 1000 - System.currentTimeMillis();

			try
			{
				Thread.sleep(delayOffset);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				jerr("Stopping primary timing thread due to InterruptedException.");
				break;
			}
		}
	}
}
