package com.mcadminpanel.plugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPluginLoader;

public class Utils
{
	private static Field loadersF;
	private static SimpleCommandMap scm;
	private static Map<String, Command> kc;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void unloadPlugin(Plugin plugin) throws Exception
	{
		PluginManager bpm = Bukkit.getServer().getPluginManager();
		
		if (!(bpm instanceof SimplePluginManager))
		{
			throw new Exception("Unknown Bukkit plugin system detected: " + bpm.getClass().getName());
		}
		
		SimplePluginManager spm = (SimplePluginManager) bpm;
		
		if (scm == null)
		{
			Field scmF = SimplePluginManager.class.getDeclaredField("commandMap");
			scmF.setAccessible(true);
			
			scm = ((SimpleCommandMap) scmF.get(spm));
			
			if (!(scm instanceof SimpleCommandMap))
			{
				throw new Exception("Unsupported Bukkit command system detected: " + scm.getClass().getName());
			}
		}
		
		if (kc == null)
		{
			Field kcF = scm.getClass().getDeclaredField("knownCommands");
			kcF.setAccessible(true);
			
			kc = ((Map) kcF.get(scm));
		}
		
		try
		{
			plugin.getClass().getClassLoader().getResources("*");
		} catch (IOException e1)
		{
			throw e1;
		}
		
		Map<String, Plugin> ln;
		List<Plugin> pl;
		
		try
		{
			Field lnF = spm.getClass().getDeclaredField("lookupNames");
			lnF.setAccessible(true);
			ln = (Map) lnF.get(spm);
			
			Field plF = spm.getClass().getDeclaredField("plugins");
			plF.setAccessible(true);
			pl = (List) plF.get(spm);
		} catch (Exception e)
		{
			throw e;
		}
		
		synchronized (scm)
		{
			Iterator<Map.Entry<String, Command>> it = kc.entrySet().iterator();
			
			while (it.hasNext())
			{
				Map.Entry<String, Command> entry = (Map.Entry) it.next();
				
				if ((entry.getValue() instanceof PluginCommand))
				{
					PluginCommand c = (PluginCommand) entry.getValue();
					
					if (c.getPlugin().getName().equalsIgnoreCase(plugin.getName()))
					{
						c.unregister(scm);
						it.remove();
					}
				}
			}
		}
		
		spm.disablePlugin(plugin);
		
		synchronized (spm)
		{
			ln.remove(plugin.getName());
			pl.remove(plugin);
		}
		
		JavaPluginLoader jpl = (JavaPluginLoader) plugin.getPluginLoader();
		
		if (loadersF == null)
		{
			try
			{
				loadersF = jpl.getClass().getDeclaredField("loaders");
				loadersF.setAccessible(true);
			} catch (Exception e)
			{
				throw e;
			}
		}
		
		try
		{
			Map<String, ?> loaderMap = (Map) loadersF.get(jpl);
			
			loaderMap.remove(plugin.getDescription().getName());
		} catch (Exception e)
		{
			throw e;
		}
		
		closeClassLoader(plugin);
		
		System.gc();
		System.gc();
	}
	
	private static void closeClassLoader(Plugin plugin)
	{
		try
		{
			((URLClassLoader) plugin.getClass().getClassLoader()).close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void hasTinyUrl(AdminPanel ap, AsyncPlayerPreLoginEvent event)
	{
		if (ap.hasTinyUrl() && Bukkit.getOnlinePlayers().length >= 8)
		{
			event.disallow(Result.KICK_FULL, "The server is full!");
		}
	}
	
	public static void hasTinyUrl(AdminPanel ap, ServerListPingEvent event)
	{
		if (ap.hasTinyUrl())
		{
			event.setMaxPlayers(8);
		}
	}
}