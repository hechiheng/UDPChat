package com.hch.udpchat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hch.udpchat.UDPClient.OnUDPClientListener;

public class ChatActivity extends Activity {
	private String TAG = "ChatActivityLog";
	private EditText chatEdit, sendEdit;
	private Button sendButton;
	private UDPClient client;
	private String localhost;
	private String ip;
	private String msg;
	private SimpleDateFormat format = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			super.handleMessage(message);
			String ip = message.getData().getString("ip");
			String msg = message.getData().getString("msg");
			String time = format.format(new Date());

			chatEdit.append(ip + " " + time + "\n" + msg + "\n");
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		chatEdit = (EditText) findViewById(R.id.chatEdit);
		sendEdit = (EditText) findViewById(R.id.sendEdit);
		sendButton = (Button) findViewById(R.id.sendButton);

		Intent intent = getIntent();
		ip = intent.getExtras().getString("ip");
		localhost = intent.getExtras().getString("localhost");
		setTitle("正在与" + ip + "聊天");
		sendEdit.requestFocus();

		client = UDPClient.getInstance();
		client.setOnUDPClientListener(new OnUDPClientListener() {
			@Override
			public void onReceiveMsg_ONLINE(String ip) {

			}

			@Override
			public void onReceiveMsg_OFFLINE(String ip) {

			}

			@Override
			public void onReceiveMsg(String ip, String msg) {
				Log.d(TAG, "-----msg:" + msg);
				Message message = new Message();
				message.getData().putString("ip", ip);
				message.getData().putString("msg", msg);
				handler.sendMessage(message);
			}

			public void onSelf_OFFLINE() {
			}
		});

		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				msg = sendEdit.getText().toString();
				if (TextUtils.isEmpty(msg)) {
					Toast.makeText(ChatActivity.this, "输入内容不可为空！",
							Toast.LENGTH_SHORT).show();
					return;
				}

				new Thread() {
					public void run() {
						client.sendMsg(ip, msg);
					}
				}.start();

				sendEdit.setText("");

				Message message = new Message();
				message.getData().putString("ip", localhost);
				message.getData().putString("msg", msg);
				handler.sendMessage(message);
			}
		});
	}
}
