package com.hch.udpchat;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.hch.udpchat.UDPClient.OnUDPClientListener;

public class MainActivity extends Activity {
	private String TAG = "MainActivityLog";
	private ListView listView;
	private Button button;
	private List<String> ipList = new ArrayList<String>();
	private ArrayAdapter<String> adapter;

	private UDPClient client;
	private boolean isConnect;
	private String localhost;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			adapter.notifyDataSetChanged();
		}

	};

	private String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		localhost = intToIp(ipAddress);
		Log.d(TAG, "localhost:" + localhost);

		button = (Button) findViewById(R.id.button);
		listView = (ListView) findViewById(R.id.listView);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, ipList);
		listView.setAdapter(adapter);

		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				client = UDPClient.getInstance();
				client.setOnUDPClientListener(new OnUDPClientListener() {
					@Override
					public void onReceiveMsg_ONLINE(String ip) {
						ipList.add(ip);
						Message message = new Message();
						handler.sendMessage(message);
					}

					@Override
					public void onReceiveMsg_OFFLINE(String ip) {
						ipList.remove(ip);
						Message message = new Message();
						handler.sendMessage(message);
					}

					@Override
					public void onReceiveMsg(String ip, String msg) {

					}

					public void onSelf_OFFLINE() {
						ipList.clear();
						Message message = new Message();
						handler.sendMessage(message);
					}
				});
				if (!isConnect) {
					isConnect = true;
					client.createSocket(localhost);
					client.setUdpLife(true);
					button.setText("离线");
					Thread t = new Thread(client);
					t.start();
					new Thread() {
						public void run() {
							client.sendMsg_ONLINE("255.255.255.255");
						}
					}.start();

				} else {
					isConnect = false;
					button.setText("上线");
					new Thread() {
						public void run() {
							client.sendMsg_OFFLINE("255.255.255.255");
							client.setUdpLife(false);
						}
					}.start();
				}

			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String ip = ipList.get(position);
				Intent intent = new Intent(MainActivity.this,
						ChatActivity.class);
				intent.putExtra("ip", ip);
				intent.putExtra("localhost", localhost);
				startActivity(intent);
			}

		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "------onDestroy----");
		if (isConnect) {
			new Thread() {
				public void run() {
					client.sendMsg_OFFLINE("255.255.255.255");
				}
			}.start();
		}
		System.exit(0);
	}

}
