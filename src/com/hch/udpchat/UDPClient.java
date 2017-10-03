package com.hch.udpchat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class UDPClient implements Runnable {
	private String TAG = "UDPClientLog";
	private DatagramSocket socket;
	private int port = 8888;
	private List<String> ipList = new ArrayList<String>();
	private String localhost;
	private boolean udpLife = false; // udp生命线程
	private byte[] recvBuf = new byte[1024]; // 接收消息
	private final static String MSG_ONLINE = "MSG_ONLINE";// 用户上线
	private final static String MSG_OFFLINE = "MSG_OFFLINE";// 用户离线
	private final static String MSG_SENDMSG = "MSG_SENDMSG";// 发送消息

	private final static UDPClient client = new UDPClient();

	// 静态工厂方法
	public static UDPClient getInstance() {
		return client;
	}

	public UDPClient() {

	}

	public void createSocket(String ip) {
		try {
			if (socket == null || socket.isClosed()) {
				socket = new DatagramSocket(port);
			}
			localhost = ip;
		} catch (Exception e) {
			Log.i(TAG, "创建socket失败");
			e.printStackTrace();
		}
	}

	public interface OnUDPClientListener {
		public void onReceiveMsg_ONLINE(String ip);

		public void onReceiveMsg_OFFLINE(String ip);

		public void onReceiveMsg(String ip, String msg);

		public void onSelf_OFFLINE();
	}

	private OnUDPClientListener listener;

	public void setOnUDPClientListener(OnUDPClientListener l) {
		listener = l;
	}

	public boolean isUdpLife() {
		if (udpLife) {
			return true;
		}

		return false;
	}

	public void setUdpLife(boolean b) {
		udpLife = b;
	}

	// 发送消息
	private void sendMsg(String ip, String command, String msgSend) {
		try {
			String msg = command + ":" + msgSend;
			InetAddress hostAddress = InetAddress.getByName(ip);
			DatagramPacket out = new DatagramPacket(msg.getBytes(),
					msg.getBytes().length, hostAddress, port);
			socket.send(out);
			Log.d(TAG, "发送内容:" + msg);
		} catch (Exception e) {
			Log.i(TAG, "发送失败");
			e.printStackTrace();
		}
	}

	public void sendMsg(String ip, String msgSend) {
		sendMsg(ip, MSG_SENDMSG, msgSend);
	}

	public void sendMsg_ONLINE(String ip) {
		sendMsg(ip, MSG_ONLINE, "");
	}

	public void sendMsg_OFFLINE(String ip) {
		sendMsg(ip, MSG_OFFLINE, "");
		ipList.clear();
		listener.onSelf_OFFLINE();
	}

	@Override
	public void run() {
		try {
			DatagramPacket recvPacket = new DatagramPacket(recvBuf,
					recvBuf.length);
			while (udpLife) {
				Log.i(TAG, "UDP监听");
				socket.receive(recvPacket);
				String recvStr = new String(recvPacket.getData(), 0,
						recvPacket.getLength());
				String ip = recvPacket.getAddress().getHostAddress();
				Log.d(TAG, "接收内容1:" + recvStr);
				String command = recvStr.substring(0, recvStr.indexOf(":"));
				String content = recvStr.substring(recvStr.indexOf(":") + 1);
				if (MSG_ONLINE.equals(command)) {
					if (!ipList.contains(ip) && !localhost.equals(ip)) {
						ipList.add(ip);
						listener.onReceiveMsg_ONLINE(ip);
						sendMsg_ONLINE(ip);
						Log.d(TAG, ip + "上线");
					}
				} else if (MSG_OFFLINE.equals(command)) {
					if (ipList.contains(ip) && !localhost.equals(ip)) {
						ipList.remove(ip);
						listener.onReceiveMsg_OFFLINE(ip);
						Log.d(TAG, ip + "离线");
					}
				} else if (MSG_SENDMSG.equals(command)) {
					Log.d(TAG, "接收内容2:" + content);
					listener.onReceiveMsg(ip, content);
				} else {
					Log.w(TAG, "无法识别命令");
				}

			}
			Log.i("udpClient", "UDP监听关闭");
			socket.close();

		} catch (Exception e) {
			Log.i(TAG, "建立服务端失败");
			e.printStackTrace();
		}

	}

}
