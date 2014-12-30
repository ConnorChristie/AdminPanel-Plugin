package com.mcadminpanel.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.minecraft.util.org.apache.commons.io.FileUtils;
import net.minecraft.util.org.apache.commons.io.IOUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class MethodHandler
{
	private AdminPanel ap;
	
	public MethodHandler(AdminPanel ap)
	{
		this.ap = ap;
	}
	
	public void doInitial(String json)
	{
		try
		{
			JSONObject init = (JSONObject) ap.getJParser().parse(json.replace("~", ","));
			
			ap.whitelist = (Boolean) init.get("whitelist");
			ap.appCount = Integer.parseInt(init.get("appCount").toString());
			ap.hasTinyUrl = (Boolean) init.get("hasTinyUrl");
			ap.tinyUrl = (String) init.get("tinyUrl");
			ap.users = (JSONArray) init.get("users");
		} catch (Error e) { } catch (Exception e) { }
	}
	
	public void doAppNotice(String id, String userName, String desc)
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (p.hasPermission("server.applications"))
			{
				p.sendMessage("");
				p.sendMessage(ChatColor.GOLD + " ------------------- " + ChatColor.AQUA + "New Application " + ChatColor.GOLD + "-------------------");
				p.sendMessage(ChatColor.GOLD + "  Id: " + ChatColor.AQUA + id);
				p.sendMessage(ChatColor.GOLD + "  Name: " + ChatColor.AQUA + userName);
				p.sendMessage(ChatColor.GOLD + "  Description: " + ChatColor.AQUA + desc);
				p.sendMessage("");
				p.sendMessage(ChatColor.GOLD + "          To Accept: " + ChatColor.AQUA + "/ap accept " + id + ChatColor.GOLD + ", To Deny: " + ChatColor.AQUA + "/ap deny " + id);
				p.sendMessage(ChatColor.GOLD + " ----------------------------------------------------");
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public void doPlayerAppNotice(String player, String act)
	{
		//Change to UUIDs
		Player p = Bukkit.getPlayer(player);
		
		if (p != null && p.isOnline())
		{
			if (act.equals("accept"))
			{
				p.sendMessage(ChatColor.GOLD + "[Web] " + ChatColor.AQUA + "Congratulations, your application has been accepted!");
				
				ap.getGhostFactory().setGhost(p, false);
			} else if (act.equals("deny"))
			{
				p.kickPlayer("Sorry but your application has been denied");
			}
		}
	}
	
	public void doMessage(String name, String subject, String message)
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (p.hasPermission("server.messages"))
			{
				p.sendMessage("");
				p.sendMessage(ChatColor.GOLD + " ------------------- " + ChatColor.AQUA + "New Message " + ChatColor.GOLD + "-------------------");
				p.sendMessage(ChatColor.GOLD + "  Name: " + ChatColor.AQUA + name);
				p.sendMessage(ChatColor.GOLD + "  Subject: " + ChatColor.AQUA + subject);
				p.sendMessage(ChatColor.GOLD + "  Message: " + ChatColor.AQUA + message);
				p.sendMessage(ChatColor.GOLD + " ----------------------------------------------------");
			}
		}
	}
	
	public String getOnlinePlayers()
	{
		String plist = "";
		
		Player[] players = Bukkit.getOnlinePlayers();
		
		for (int i = 0; i < players.length; i++)
		{
			Player p = players[i];
			
			plist += (i != 0 ? ";" : "") + p.getName() + "|" + p.getWorld().getName();
		}
		
		return "{\"players\":\"" + plist + "\", \"online\":" + Bukkit.getOnlinePlayers().length + ", \"total\":" + Bukkit.getMaxPlayers() + "}";
	}
	
	public String getChats()
	{
		String chats = "";
		
		for (String[] chat : ap.getChats())
		{
			chats += "<b>" + (2 < chat.length ? chat[2] : "") + " <a href=\"/player/" + chat[0] + "\">" + chat[0] + "</a>:</b> " + chat[1] + "<br />";
		}
		
		if (chats.isEmpty()) chats = "No Chats";
		
		return chats;
	}
	
	public String issueChat(String username, String chat)
	{
		ap.getChats().add(new String[] { username, chat, "[Web]" });
		
		Bukkit.broadcastMessage(ChatColor.GOLD + "[Web] " + ChatColor.WHITE + username + ": " + chat);
		
		return getChats();
	}
	
	@SuppressWarnings("unchecked")
	public String getPListJson(String loggedInStr, String hasEditPermStr)
	{
		boolean loggedIn = Boolean.parseBoolean(loggedInStr);
		boolean hasEditPerm = Boolean.parseBoolean(hasEditPermStr);
		
		JSONArray s = new JSONArray();
		
		for (OfflinePlayer player : Bukkit.getOfflinePlayers())
		{
			if (player.isBanned() && (!loggedIn || (loggedIn && !hasEditPerm)))
				continue;
			
			int health = 0;
			int food = 0;
			
			if (player.isOnline())
			{
				health = (int) (player.getPlayer().getHealth() * 100 / player.getPlayer().getHealthScale());
				food = (int) (player.getPlayer().getFoodLevel() * 100 / 20);
			}
			
			JSONObject obj = new JSONObject();
			
			obj.put("name", player.getName());
			obj.put("world", player.isOnline() ? player.getPlayer().getWorld().getName() : "none");
			
			obj.put("health", health);
			obj.put("food", food);
			
			obj.put("online", player.isOnline());
			obj.put("banned", player.isBanned());
			
			s.add(obj);
		}
		
		return s.toJSONString();
	}
	
	public String doPlayerEvent(String player, String method)
	{
		return doPlayerEvent(player, method, null);
	}
	
	public String doPlayerEvent(String player, String method, String message)
	{
		JSONObject obj = new JSONObject();
		
		List<String> args = new ArrayList<String>(Arrays.asList(new String[] { player, method, message }));
		
		if (method.equalsIgnoreCase("heal"))
			PlayerEvents.heal(obj, args);
		else if (method.equalsIgnoreCase("feed"))
			PlayerEvents.feed(obj, args);
		else if (method.equalsIgnoreCase("kill"))
			PlayerEvents.kill(obj, args);
		else if (method.equalsIgnoreCase("kick"))
			PlayerEvents.kick(obj, args);
		else if (method.equalsIgnoreCase("ban"))
			PlayerEvents.ban(obj, args);
		
		return obj.toJSONString();
	}
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public String getPlayer(String player)
	{
		JSONObject obj = new JSONObject();
		
		OfflinePlayer op = Bukkit.getOfflinePlayer(player);
		
		if (op != null && op.hasPlayedBefore())
		{
			obj.put("name", op.getName());
			
			obj.put("exists", true);
			obj.put("online", op.isOnline());
			
			SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
			
			obj.put("firstPlayed", sdf.format(new Date(op.getFirstPlayed())));
			obj.put("lastPlayed", sdf.format(new Date(op.getLastPlayed())));
			
			if (op.isOnline())
			{
				obj.put("health", op.getPlayer().getHealth() * 100 / op.getPlayer().getHealthScale());
				obj.put("food", op.getPlayer().getFoodLevel() * 100 / 20);
			} else
			{
				obj.put("health", 0);
				obj.put("food", 0);
			}
		} else
			obj.put("exists", false);
		
		return obj.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	public String getPluginsJson()
	{
		JSONArray s = new JSONArray();
		
		for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
		{
			JSONObject obj = new JSONObject();
			
			obj.put("name", plugin.getName());
			obj.put("enabled", plugin.isEnabled());
			obj.put("description", plugin.getDescription().getDescription());
			
			s.add(obj);
		}
		
		return s.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	public String doPluginEvent(String action, String pluginName, String slug, String link)
	{
		JSONObject out = new JSONObject();
		
		PluginManager pm = Bukkit.getPluginManager();
		
		if (action.equalsIgnoreCase("install"))
		{
			if (!pm.isPluginEnabled(pluginName))
			{
				try
				{
					if (link.equals("-"))
						link = "http://api.bukget.org/3/plugins/bukkit/" + slug + "/latest/download";
					
					InputStream in = new URL(link).openStream();
					
					File outFile = new File("plugins", pluginName + ".jar");
					
					FileUtils.copyInputStreamToFile(in, outFile);
					IOUtils.closeQuietly(in);
					
					Plugin p = pm.loadPlugin(outFile);
					pm.enablePlugin(p);
					
					out.put("good", "Successfully installed and enabled the plugin!");
				} catch (MalformedURLException e)
				{
					e.printStackTrace();
				} catch (IOException e)
				{
					e.printStackTrace();
				} catch (UnknownDependencyException e)
				{
					e.printStackTrace();
				} catch (InvalidPluginException e)
				{
					e.printStackTrace();
				} catch (InvalidDescriptionException e)
				{
					e.printStackTrace();
				}
				
				out.put("error", "Plugin could not be installed, check console to see why.");
			} else
				out.put("error", "Plugin is already installed.");
		} else if (!pluginName.equalsIgnoreCase(ap.getName()))
		{
			Plugin p = pm.getPlugin(pluginName);
			
			if (action.equalsIgnoreCase("enable"))
			{
				if (!pm.isPluginEnabled(pluginName))
				{
					pm.enablePlugin(p);
					
					out.put("good", "Successfully enabled the plugin!");
				}
			} else if (action.equalsIgnoreCase("disable"))
			{
				if (pm.isPluginEnabled(pluginName))
				{
					pm.disablePlugin(p);
					
					out.put("good", "Successfully disabled the plugin!");
				}
			} else if (action.equalsIgnoreCase("delete"))
			{
				File file = null;
				
				try
				{
					Field f = JavaPlugin.class.getDeclaredField("file");
					f.setAccessible(true);
					
					file = (File) f.get(p);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				try
				{
					Utils.unloadPlugin(p);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if (file != null && file.exists())
				{
					if (file.delete())
						out.put("good", "Successfully deleted the plugin!");
					else
						out.put("error", "Could not delete the plugins jar.");
				} else
					out.put("error", "Could not find the jar file for the selected plugin.");
			}
		}
		
		return out.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	public String getPluginFiles(String plugin)
	{
		JSONObject out = new JSONObject();
		
		Plugin pl = Bukkit.getPluginManager().getPlugin(plugin);
		
		if (pl != null)
		{
			JSONObject core = new JSONObject();
			
			core.put("data", getAllFiles(pl.getDataFolder()));
			out.put("core", core);
		}
		
		return out.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	private JSONArray getAllFiles(File folder)
	{
		JSONArray data = new JSONArray();
		
		if (folder.exists() && folder.isDirectory())
		{
			for (File f : folder.listFiles())
			{
				JSONObject obj = new JSONObject();
				
				if (f.isDirectory())
				{
					obj.put("text", f.getName());
					obj.put("children", getAllFiles(f));
				} else
				{
					obj.put("text", f.getName());
					obj.put("icon", "/images/file.png");
				}
				
				data.add(obj);
			}
		}
		
		return data;
	}
	
	@SuppressWarnings("unchecked")
	public String getPluginContent(String plugin, String file)
	{
		JSONObject out = new JSONObject();
		
		Plugin pl = Bukkit.getPluginManager().getPlugin(plugin);
		File fl = new File(pl.getDataFolder(), file.replace("~", "/"));
		
		if (pl != null && fl != null)
		{
			try
			{
				out.put("good", IOUtils.toString(new FileInputStream(fl)));
			} catch (FileNotFoundException e) { } catch (IOException e) { }
		}
		
		out.put("error", "Could not find the file specified");
		
		return out.toJSONString();
	}
	
	@SuppressWarnings("unchecked")
	public String savePluginContent(String plugin, String type, String file, String data)
	{
		JSONObject out = new JSONObject();
		
		Plugin pl = Bukkit.getPluginManager().getPlugin(plugin);
		File fl = new File(pl.getDataFolder(), file);
		
		if (pl != null && fl != null)
		{
			data = data.replace("~`~", ",");
			
			if (type.equals("raw"))
			{
				try
				{
					IOUtils.write(data, new FileOutputStream(fl));
					
					out.put("good", "good");
				} catch (FileNotFoundException e) { } catch (IOException e) { }
			} else if (type.equals("perms"))
			{
				try
				{
					DumperOptions options = new DumperOptions();
				    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
				    
				    Yaml yaml = new Yaml(options);
					
					yaml.dump(yaml.load(data), new OutputStreamWriter(new FileOutputStream(fl)));
					
					out.put("good", "good");
				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		out.put("error", "Could not find the file specified");
		
		return out.toJSONString();
	}
}