package fr.pchab.androidrtc;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.LinkedList;
import java.util.List;

import fr.pchab.webrtcclient.PeerConnectionParameters;
import fr.pchab.webrtcclient.WebRtcClient;

public class RtcActivity extends ListActivity implements WebRtcClient.RtcListener{
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private VideoRendererGui.ScalingType scalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;
    private EditText mChatEditText;
    private String username;
    private ListView mChatList;
    private ChatAdapter mChatAdapter;
    private String myId;
    private String number;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.main);
        this.mChatEditText = (EditText) findViewById(R.id.chat_input);
        this.mChatList     = getListView();
        this.mChatEditText = (EditText) findViewById(R.id.chat_input);

        //Set list chat adapter for the list activity
        List<ChatMessage> ll = new LinkedList<ChatMessage>();
        mChatAdapter = new ChatAdapter(this, ll);
        mChatList.setAdapter(mChatAdapter);

        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");

        //Set sources for video renderer in view
        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            myId = extras.getString("id");
            number = extras.getString("number");
        }
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        //if this is to answer the phone from other user, get the link which contain caller id
        //final Intent intent = getIntent();



            Log.d("minh final","function end here");

//        final String action = intent.getAction();
//
//        if (Intent.ACTION_VIEW.equals(action)) {
//            final List<String> segments = intent.getData().getPathSegments();
//            callerId = segments.get(0);
//        }
    }

    /**
     * Initialize webrtc client
     *
     * Set up the peer connection parameters get some video information and then pass these information to Webrtcclient class.
     *
     */
    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_VP9, true, 1, AUDIO_CODEC_OPUS, true);
        client = new WebRtcClient(this, mSocketAddress, params, VideoRendererGui.getEGLContext(),this.myId);
    }

    /**
     * Handle chat messages when people click the send button
     *
     * Get the message from input then add it to chat adapter
     * Transmit the message to other users except the one who call this function
     *
     * @param view the view that contain the button
     */
    public void sendMessage(View view) {
        String message = mChatEditText.getText().toString();
        if (message.equals("")) return; // Return if empty
        ChatMessage chatMsg = new ChatMessage(this.username, message, System.currentTimeMillis());
        mChatAdapter.addMessage(chatMsg);

        //Data is being sent under JSON object
        JSONObject messageJSON = new JSONObject();
        try {
            messageJSON.put("user_id", chatMsg.getSender());
            messageJSON.put("msg", chatMsg.getMessage());
            messageJSON.put("time", chatMsg.getTimeStamp());
            client.transmitChat(messageJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Hide keyboard when you send a message.
        View focusView = this.getCurrentFocus();
        if (focusView != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        mChatEditText.setText("");
    }

    /**
     * Handle when people click hangup button
     *
     * Destroy all video resources and connection
     *
     * @param view the view that contain the button
     */
    public void hangup(View view) {
        if(client != null) {
            onDestroy();
        }
    }

    /**
     * Handle when people click stopvideo button
     *
     * Stop all video resources and connection
     *
     * @param view the view that contain the button
     */
    public void stopvideo(View view) {
        if(client != null) {
            client.stopVideo();
        }
    }

    /**
     * Handle onPause event which is implement by RtcListener class
     *
     * Pause the video source
     *
     */
    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    /**
     * Handle onResume event which is implement by RtcListener class
     *
     * Resume the video source
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if(client != null) {
            client.onResume();
        }
    }

    /**
     * Handle onDestroy event which is implement by RtcListener class
     *
     * Destroy the video source
     *
     */
    @Override
    public void onDestroy() {
//        if(client != null) {
//            client.onDestroy();
//        }
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * This function is being call when user have got an id from nodejs server
     *
     * check if caller id is not null then answer the call
     * if not then start the camera and send id to other user
     *
     * @param callId the id of the user
     */
    @Override
    public void onCallReady(String callId) {
        this.username      = client.client_id();
        if (number != null) {
            try {
                answer(number);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            call(callId);
        }
    }

    /**
     * This function is being when the chat event is being triggered
     *
     * Add the chat message to the chat adapter
     *
     * @param id the id of the user sent the chat
     * @param msg the message
     */
    @Override
    public void receiveMessage(final String id,final String msg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatMessage chatMsg = new ChatMessage(id, msg, System.currentTimeMillis());
                mChatAdapter.addMessage(chatMsg);
            }
        });

    }

    /**
     * This function is being call to answer call from other user
     *
     * send init message to the caller and connect
     * start the camera
     *
     * @param callerId the id of the caler
     */
    public void answer(String callerId) throws JSONException {
        //client.sendMessage(callerId, "init", null);
        client.sendMessage(callerId, "init", null);
        client.sendConnected();
        startCam();
    }

    /**
     * This function is to send message contain id to the other user in order to start a call
     *
     * Start intent then start the message intent contain url and user id
     *
     * @param callId the id of the user
     */
    public void call(String callId) {
//        Intent msg = new Intent(Intent.ACTION_SEND);
//        msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + callId);
//        msg.setType("text/plain");
//        startActivityForResult(Intent.createChooser(msg, "Call someone :"), VIDEO_CALL_SENT);
        startCam();
    }

    /**
     * The function being triggered when the send intent is being called
     *
     * Start the camera
     *
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == VIDEO_CALL_SENT) {
//            startCam();
//        }
//    }

    /**
     * Start camera function
     *
     * call the webrtc start camera function
     *
     */
    public void startCam() {
        // Camera settings
        client.start("android_test");
    }

    /**
     * Being called when call status change
     *
     * Log message when webrtc status change
     *
     */
    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Being called when local stream is added
     *
     * Update render view for the local stream in the small window
     *
     */
    @Override
    public void onLocalStream(MediaStream localStream) {
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }

    /**
     * Being called when remote stream is added
     *
     * Update render view for the remote stream in the big window
     *
     */
    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType);
    }

    /**
     * Being called when remote stream is removed
     *
     * make local renderer become the big one again
     *
     */
    @Override
    public void onRemoveRemoteStream(int endPoint) {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType);
    }
}