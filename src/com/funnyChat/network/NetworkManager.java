package com.funnyChat.network;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import com.funnyChat.event.*;
import com.funnyChat.memory.*;

public class NetworkManager {
	private int mIdCount;
	private static NetworkManager mInstance;
	private HashMap<Integer, Connection> mConnections;
	private int mTimeout;
	private ServerSocketChannel mServerSocketChannel;
	private Selector mSelector;
	private int mPingInterval;
	private int mMaxCount;
	
	private NetworkManager(int _max_count, int _port){
		try{
			mIdCount = 0;
			mConnections = new HashMap<Integer, Connection>();
			mSelector = Selector.open();
			mServerSocketChannel = ServerSocketChannel.open();
			mServerSocketChannel.configureBlocking(false);
			mServerSocketChannel.socket().bind(new InetSocketAddress(_port));
			mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
			mTimeout = 5000;
			mPingInterval = 5000;
			mMaxCount = _max_count;
		}
		catch(IOException e){
			//Wait for logger...
		}
	}
	
	public static boolean initialize(int _max_count, int _port){
		if(mInstance == null){
			mInstance = new NetworkManager(_max_count, _port);
			return true;
		}
		return false;
	}
	
	public static boolean initialize(){
		return initialize(50, 55555);
	}
	
	public boolean deinitialize(){
		try{
			if(mInstance != null){
				removeAll();
				mConnections = null;
				mServerSocketChannel.socket().close();
				mServerSocketChannel.close();
				mInstance = null;
				return true;
			}
			return false;
		}
		catch(IOException e){
			//Wait for logger...
			return false;
		}
	}
	
	public static NetworkManager getInstance(){
		return mInstance;
	}
	
	public int getTimeout(){
		return mTimeout;
	}
	
	public void setTimeout(int _timeout){
		mTimeout = _timeout;
	}
	
	public int getPingInterval(){
		return mPingInterval;
	}
	
	public void setPingInterval(int _interval){
		mPingInterval = _interval;
	}
	
	public int getMaxCount(){
		return mMaxCount;
	}
	
	public void setMaxCount(int _max_count){
		mMaxCount = _max_count;
	}
	
	protected int generateId(){
		return mIdCount++;
	}
	
	public void removeAll(){
		mConnections.clear();
	}
	
	public boolean disconnect(int _id){
		try{
			Connection _connection = mConnections.get(_id);
			if(_connection != null){
				mConnections.remove(_id);
				_connection.getSocketChannel().close();
				
				return true;
			}
			else{
				return false;
			}
		}
		catch(IOException e){
			//Wait for logger...
			return false;
		}
	}
	
	public int connect(InetAddress ip, int port){
		try{
			SocketAddress _address = new InetSocketAddress(ip, port);
			SocketChannel _socket_channel = SocketChannel.open();
			_socket_channel.configureBlocking(false);
			_socket_channel.connect(_address);
			_socket_channel.register(mSelector, SelectionKey.OP_READ);
			Connection _connection = new Connection(_socket_channel);
			//
			//Send hello message.
			//
			int _id = generateId();
			mConnections.put(_id, _connection);
			_socket_channel.finishConnect();
			return _id;
		}
		catch(IOException e){
			//Wait for logger...
			return -1;
		}
	}
	
	public void send(Event _event){
		try{
			Connection _connection = mConnections.get(_event.getTarget());
			MemoryManager _mm = MemoryManager.getInstance();
			int _length = 0;
			byte[] _data = _event.serialize();

			ByteBuffer _buffer = ByteBuffer.allocate(Integer.SIZE / 8 + _data.length * Byte.SIZE / 8);

			_buffer.putInt(_data.length);
			_buffer.put(_data);
			_buffer.clear();

			_connection.getSocketChannel().write(_buffer);
		}
		catch(IOException e){
			//Logger...
		}
	}
	
	public void run(){
		try{
			mSelector.select();
			Set<SelectionKey> _selectedKeys = mSelector.selectedKeys();
			Iterator<SelectionKey> _iter = _selectedKeys.iterator();
			SocketChannel _sc;
			EventManager _eventManager = EventManager.getInstance();
			
			while(_iter.hasNext()){
				SelectionKey _key = _iter.next();
				ByteBuffer _buffer = ByteBuffer.allocate(Integer.SIZE / 8);
				
				if((_key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT){
					ServerSocketChannel _ssc = (ServerSocketChannel)_key.channel();
					_sc = _ssc.accept();
					
					_sc.configureBlocking(false);
					_sc.register(mSelector, SelectionKey.OP_READ);
					
					_iter.remove();
				}
				else if((_key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ){
					_sc = (SocketChannel)_key.channel();
					
					_sc.read(_buffer);
					_buffer.clear();
					
					int _size = _buffer.getInt();
					_buffer = ByteBuffer.allocate(_size);
					
					_sc.read(_buffer);
					_buffer.clear();
					
					_eventManager.enqueue(_buffer.array());
				}
			}
		}
		catch(IOException e){
			//Logger...
		}
	}
}
