package project.richthofen911.rt_droid;

import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private Socket mSocket;{
        try {
            mSocket = IO.socket("http://192.168.128.98:3000");
        } catch (URISyntaxException e) {e.printStackTrace();}
    }

    private EditText mInputMessageView;
    private TextView tv_display;
    private String myConnectionId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInputMessageView = (EditText) findViewById(R.id.et_input);
        tv_display = (TextView) findViewById(R.id.tv_display);
        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptToSend();
            }
        });

        String phoneInfo = Build.BRAND + ": " + Build.MODEL + ": " + Build.VERSION.RELEASE + ": " + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        mSocket.on("news", onNewMessage);
        mSocket.on("yourId", onGetId);
        mSocket.connect().emit("phone info", phoneInfo);

    }

    private void attemptToSend() {
        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }
        mInputMessageView.setText("");
        mSocket.emit("new message", message);
    }

    private Emitter.Listener onGetId = new Emitter.Listener() {
        @Override
        public void call(final Object...args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("get connection id", args[0].toString());
                    JSONObject data = (JSONObject) args[0];
                    try {
                        myConnectionId = data.getString("yourId");
                    } catch (JSONException e) {
                        return;
                    }
                    Log.e("my connection id", myConnectionId);
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object...args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("news received", args[0].toString());
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    try {
                        message = data.getString("cmd");
                    } catch (JSONException e) {
                        return;
                    }
                    // add the message to view
                    showMessage("cmd", message);
                }
            });
        }
    };

    private void showMessage(String name, String msg){
        tv_display.setText(name + ": " + msg);
        mSocket.emit("new message", "phone side success");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.emit("new message", "device " + myConnectionId + " is offline");
        mSocket.disconnect();
        mSocket.off();
    }

}
