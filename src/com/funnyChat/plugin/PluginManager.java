package com.funnyChat.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.*;
import com.funnyChat.event.*;
import com.funnyChat.utils.Log;
import com.funnyChat.utils.Log.LogType;
import com.funnyChat.core.*;

public class PluginManager {
	private HashMap<Integer, Plugin> mPlugins;
	private static PluginManager mInstance;
	private Integer mIdCount;
	private String mDir;
	private Log mLog;
	
	private static class PluginFilter implements FileFilter{
		private static String mSuffix = "class";
		
		public boolean accept(File _path_name){
			return _path_name.getName().endsWith(mSuffix);
		}
		
		public static String getSuffix(){
			return mSuffix;
		}
	}
	
	private PluginManager(String _directory){
		mPlugins = new HashMap<Integer, Plugin>();
		mIdCount = 0;
		mDir = _directory;
		mLog = new Log();
		mLog.setLogFile("plu_log.txt");
	}
	
	private Integer generateId(){
		return mIdCount++;
	}
	
	public void scan(){
		File _plugin_dir = new File(mDir);
		String _plugin_name;
		
		if(!_plugin_dir.exists()) {
			_plugin_dir.mkdir();
		}

		for(File _plugin_sub_dir : _plugin_dir.listFiles()){
			if(_plugin_sub_dir.isDirectory()) {
				_plugin_name = _plugin_sub_dir.getName();
				/*_plugin_name = _plugin_name.substring(0, _plugin_name.length() - 
						PluginFilter.getSuffix().length() - 1);*/
				Plugin _plugin = null;
				try{
					URL[] url = new URL[1];
					url[0] = new URL("file:///" + _plugin_sub_dir.getAbsolutePath() + "/");
					_plugin = (Plugin)Class.forName(_plugin_name, true, new URLClassLoader(url)).newInstance();
				}
				catch(Exception e){
					mLog.addLog("Debug: Failed to instantiate plugin: " + _plugin_name,LogType.DEBUG);
					Core.getLogger().addLog("Failed to instantiate plugin " + _plugin_name + ".", LogType.WARNING);
				}
				if(_plugin != null && !mPlugins.containsValue(_plugin)){
					mPlugins.put(generateId(), _plugin);
				}
			}
		}
	}
	
	public static void initialize(String _directory){
		if(mInstance == null){
			mInstance = new PluginManager(_directory);

			mInstance.scan();
		}
	}
	public static void initialize(){
		initialize("Plugin");
	}
	/*  Aborted
	public int getCount(){
		return mIdCount;
	}
	*/
	public void deinitialize(){
		if(mInstance != null){
			removeAll();
			mPlugins = null;
			mInstance = null;
		}
	}
	public static PluginManager getInstance(){
		return mInstance;
	}
	/*   Aborted
	public Boolean insert(Plugin _plugin){
		if(mPlugins.containsValue(_plugin)){
			return false;
		}
		else{
			mPlugins.put(generateId(), _plugin);
			return true;
		}
	}*/
	public Boolean remove(Integer _id){
		Plugin _plugin = mPlugins.remove(_id);
		if(_plugin == null){
			return false;
		}
		_plugin.destroy();
		
		return true;
	}
	public Boolean remove(Plugin _plugin){
		Integer _id = getId(_plugin);
		return remove(_id);
	}
	public Boolean removeAll(){
		for(Plugin _plugin : mPlugins.values()){
			_plugin.destroy();
		}
		mPlugins.clear();
		return true;
	}
	public Integer getId(Plugin _plugin){
		for(Map.Entry<Integer, Plugin> _item : mPlugins.entrySet()){
			if(_item.getValue().equals(_plugin)){
				return _item.getKey();
			}
		}
		
		return -1;
	}
	public Plugin get(Integer _id){
		return mPlugins.get(_id);
	}
	public Collection<Plugin> getPlugins() {
		return mPlugins.values();
	}
	/*   Aborted
	public Boolean set(Integer _id, Plugin _plugin){
		mPlugins.put(_id, _plugin);
		return true;
	}
	*/
	public void enable(Integer[] _ids){
		Plugin _plugin;
		for(int i=0;i<_ids.length;i++){
			_plugin = mPlugins.get(_ids[i]);
			
			if(_plugin != null){
				_plugin.enable();
			}
		}
	}
	public void enableAll(){
		for(Plugin _plugin : mPlugins.values()){
			_plugin.enable();
		}
	}
	public void disable(Integer[] _ids){
		Plugin _plugin;
		for(int i=0;i<_ids.length;i++){
			_plugin = mPlugins.get(_ids[i]);
			
			if(_plugin != null){
				_plugin.disable();
			}
		}
	}
	public void disableAll(){
		for(Plugin _plugin : mPlugins.values()){
			_plugin.disable();
		}
	}
	public boolean handleEvent(Event _event){
		boolean _result = true;
		for(Plugin _plugin : mPlugins.values()){
			if(_plugin.isEnabled()){
				if(!_plugin.handleEvent(_event)){
					_result = false;
				}
			}
		}
		
		return _result;
	}
	public String getPluginDirectory() {
		return mDir;
	}
	public void setPluginDirectory(String _directory) {
		mDir = _directory;
	}
}
