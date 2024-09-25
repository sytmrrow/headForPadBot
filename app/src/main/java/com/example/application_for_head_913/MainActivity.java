package com.example.application_for_head_913;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.Player; // 确保导入了 Player

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private boolean isWaitingForNextInput = false;
    // 声明状态变量
    private boolean isWaitingForResponse = false; // 用于标记是否在等待后端响应
    // 声明一个状态变量
    private boolean isSpeaking = false;
    private boolean isListening = false;
    private TextToSpeechUtil ttsUtil;
    private boolean ttsInitialized = false;
    private Button activatedButton;
    // 声明自定义 Toast
    private Toast wakeUpToast;
    private AlertDialog dialog; // 添加这个声明


    private ExoPlayer player;
    private PlayerView playerView;
    private TextView serverResponseTextView; // 用于显示服务器响应的TextView
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static OkHttpClient client;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "MainActivity";
    private SpeechRecognizer mIat; // 语音听写对象
    private RecognizerDialog mIatDialog; // 语音听写UI
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private String mEngineType = SpeechConstant.TYPE_CLOUD; // 引擎类型
    private String language = "zh_cn"; // 识别语言
    private EditText tvResult; // 识别结果
    private Button btnStart; // 开始识别
    private StringBuffer buffer = new StringBuffer();
    private String resultType = "json"; // 结果内容数据格式
    private Toast mToast;
    private int dialogType;
    private int resultCode = 0;
    private int handlerCode = 0x123;
    private Button pauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=b8585b05");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissionAndInitialize();
        initializeViews();
        pauseButton = findViewById(R.id.pause_button);

        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        // 设置 TTS 监听器
        ttsUtil.setListener(() -> {
            if (!isListening) {
                isListening = true;
                startListening(); // 语音播放完毕后继续录音
            }
        });
        client = new OkHttpClient();

        setupExoPlayer();
    }


    private void initializeViews() {
        tvResult = findViewById(R.id.myEditText);
        serverResponseTextView = findViewById(R.id.serverResponseTextView);
        activatedButton = findViewById(R.id.activatedButton); // 初始化未激活按钮
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mIatDialog.setListener(mRecognizerDialogListener);
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
    }

    private void setupExoPlayer() {
        playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri("file:///android_asset/padbot.webm");
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
    }

    private void checkPermissionAndInitialize() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer();
        } else {
            Toast.makeText(this, "需要录音权限才能使用语音识别功能", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeSpeechRecognizer() {
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mIatDialog.setListener(mRecognizerDialogListener);
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        startListening();
    }

    private void sendRequestToServer(PatrobotData patrobotData) {
        isWaitingForResponse = true; // 开始等待响应
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sessionId", patrobotData.getSessionId());
        jsonObject.addProperty("newTopic", false);
        jsonObject.addProperty("deviceId", patrobotData.getDeviceId());
        jsonObject.addProperty("deviceModel", patrobotData.getDeviceModel());
        jsonObject.addProperty("city", patrobotData.getCity());
        jsonObject.addProperty("district", patrobotData.getDistrict());
        jsonObject.addProperty("lang", patrobotData.getLang());

        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("prompt", patrobotData.getPrompt());
        jsonObject.add("message", messageObject);

        String json = gson.toJson(jsonObject);
        Log.d("Request JSON", "发送的数据: " + json);
        // 更新按钮状态为“请等待”
        runOnUiThread(() -> activatedButton.setText("请等待"));

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url("http://222.200.184.74:8082/api/processRequest_origin")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API Failure", "Error: " + e.getMessage());
                isWaitingForResponse = false; // 请求失败或超时后重置状态
                runOnUiThread(() -> {
                    showToast("请求超时或失败");
                    activatedButton.setText("未激活"); // 更新按钮文字为“未激活”
                    startListening(); // 确保在请求失败后继续录音
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response res = response) {
                    if (res.isSuccessful()) {
                        String responseData = res.body().string();
                        Log.d("API Response", responseData);
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            // 处理返回结果
                            String handledResponse = handleResponseType(jsonResponse);
                            runOnUiThread(() -> {serverResponseTextView.setText(handledResponse);
                                activatedButton.setText("未激活"); // 更新按钮文字为“未激活”
                            // 调用 TTS 播放 content
                            playTextToSpeech(handledResponse);});
                        } catch (JSONException e) {
                            Log.e("JSON Error", "Failed to parse JSON: " + e.getMessage());
                            runOnUiThread(() -> serverResponseTextView.setText("JSON Parse Error"));
                        }
                        new Thread(() -> {
                            try {
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress("192.168.11.236", 5000), 5000);
                                OutputStream outputStream = socket.getOutputStream();
                                PrintWriter writer = new PrintWriter(outputStream, true);
                                writer.println(responseData);
                                writer.flush();
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        Log.e("API Error", "Error code: " + res.code());
                    }
                    isWaitingForResponse = false; // 请求成功或失败后都重置等待状态
                } catch (IOException e) {
                    Log.e("API Error", "Error closing response: " + e.getMessage());
                    isWaitingForResponse = false; // 处理异常后重置状态
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIat != null) {
            startListening();
        }
    }

    private void startListening() {
        if (mIat == null || isSpeaking || isWaitingForResponse) {
            //showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        buffer.setLength(0);
        tvResult.setText("");
        mIatResults.clear();
        setParam();
        resultCode = mIat.startListening(mRecognizerListener);
        if (dialogType == 0) {
            showTip("开始听写");
        } else if (dialogType == 1) {
            resultCode = mIat.startListening(mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showTip("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showTip("开始听写");
            }
        } else if (dialogType == 2) {
            resultCode = mIat.startListening(mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showTip("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showTip("开始听写");
            }
        }
    }
    private void playTextToSpeech(String text) {
        if (!ttsInitialized) {
            ttsUtil = TextToSpeechUtil.getInstance(this);
            ttsUtil.init(); // 初始化 TTS
            ttsInitialized = true;
        }
        ttsUtil.setListener(() -> {
            isSpeaking = false; // 播放结束后标记不再播放
            // 播放完成后继续录音
            startListening();
        });
        // 设置暂停按钮的点击事件
        pauseButton.setOnClickListener(v -> {
            ttsUtil.stopSpeaking(); // 暂停 TTS 播放
            isSpeaking = false;
            startListening(); // 启动录音
        });
        // 播放文本前设置 isSpeaking 为 true
        isSpeaking = true;
        ttsUtil.speakText(text); // 播放文本
    }


    @Override
    public void onClick(View view) {
    }

    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.e(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

   // 听写监听器
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

       @Override
       public void onBeginOfSpeech() {
           // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
           showTip("开始说话");
           Log.d(TAG, "onBeginOfSpeech: 开始语音输入");

           // 显示“再次使用‘你好’唤醒可中断发言”提示
           handler.postDelayed(() -> {
               //showTip("再次使用‘你好’唤醒可中断发言");
               Log.d(TAG, "showTip: 显示中间提示 '再次使用‘你好’唤醒可中断发言'");
           }, 500); // 延时500毫秒显示
       }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            Log.d(TAG, "onError " + error.getPlainDescription(true));
            showTip(error.getPlainDescription(true));
            if (error.getErrorCode() == 20006) {
                showTip("启动录音失败，请检查录音权限和设备设置。");
            }
        }

       @Override
       public void onEndOfSpeech() {
           showTip("结束说话");
           Log.d(TAG, "onEndOfSpeech: 语音输入结束");
           isListening = false; // 结束录音后重置
           if (isWaitingForNextInput) {
               // 播放语音并在结束后继续录音
               speakText("小信来啦", () -> {
                   isListening = true; // 播放后设置为正在录音
                   startListening(); // 开始录音
               });
           } else {
               startListening(); // 继续进行语音识别
           }
       }

        ExecutorService executorService = Executors.newCachedThreadPool();

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if (isLast) {
                Log.d(TAG, "onResult 结束" + results.getResultString());
            }
            if (resultType.equals("json")) {
                printResult(results);
                return;
            }
            if (resultType.equals("plain")) {
                buffer.append(results.getResultString());
                tvResult.setText(buffer.toString());
                tvResult.setSelection(tvResult.length());
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            if (volume < 3) {
                // 可以在这里调整是否显示提示音量
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == handlerCode) {
            }
        }
    };
    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }
    };
    // 封装 Toast 显示方法，确保运行在主线程
    private void showToast(final String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "showToast: 显示 Toast - " + message);
        });
    }


    private void startListeningWithDelay() {
        mainHandler.postDelayed(this::startListening, 100); // 1秒后继续识别
    }

    private void printResult(RecognizerResult results) {
        String text = "";
        String sn = null;
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
            Log.d(TAG, "printResult: 解析结果 JSON = " + resultJson.toString());

            JSONArray wsArray = resultJson.optJSONArray("ws");
            StringBuilder sb = new StringBuilder();
            if (wsArray != null) {
                for (int i = 0; i < wsArray.length(); i++) {
                    JSONObject wsObject = wsArray.optJSONObject(i);
                    if (wsObject != null) {
                        JSONArray cwArray = wsObject.optJSONArray("cw");
                        if (cwArray != null && cwArray.length() > 0) {
                            JSONObject cwObject = cwArray.optJSONObject(0);
                            if (cwObject != null) {
                                sb.append(cwObject.optString("w"));
                            }
                        }
                    }
                }
            }
            text = sb.toString();
            Log.d(TAG, "printResult: 识别的文本 = " + text);
            String processedText = text.toLowerCase();
            Log.d(TAG, "Processed Text (all lowercase): " + processedText);
            if (processedText.contains("你好")||processedText.contains("您好")) {
                isWaitingForNextInput = true;
                runOnUiThread(() -> activatedButton.setText("已激活")); // 更新按钮文字为“已激活”
                Log.d(TAG, "printResult: 进入等待下一句输入状态");
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "E");
                } catch (JSONException e) {
                    Log.e(TAG, "JSON 构建错误: " + e.getMessage());
                }
                sendJsonToDeviceB(jsonObject.toString());
                Log.d(TAG, "发送JSON到设备B: " + jsonObject.toString());
            } else if (isWaitingForNextInput) {
                if (!processedText.isEmpty()) {
                    PatrobotData voicedata = new PatrobotData();
                    voicedata.setPrompt(processedText);
                    sendRequestToServer(voicedata);
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("type", "F");
                        jsonObject.put("content", processedText);
                        sendJsonToDeviceB(jsonObject.toString());
                        Log.d(TAG, "发送 JSON 到设备B: " + jsonObject.toString());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON 构建错误: " + e.getMessage());
                    }
                    resetInputState();
                } else {
                    Log.d(TAG, "printResult: 输入为空，继续等待...");
                }
                Log.d(TAG, "printResult: 发送文本到服务器：" + processedText);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "printResult: JSON 解析错误", e);
        }

        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        runOnUiThread(() -> {
            tvResult.setText(resultBuffer.toString());
            tvResult.setSelection(tvResult.length());
            Log.d(TAG, "printResult: 更新 UI，显示结果：" + resultBuffer.toString());
        });
    }

    private void resetInputState() {
        isWaitingForNextInput = false;
        Log.d(TAG, "resetInputState: 重置输入状态，isWaitingForNextInput = " + isWaitingForNextInput);
    }

    private void sendJsonToDeviceB(String jsonData) {
        new Thread(() -> {
            String ip = "192.168.11.236";
            int port = 5000;
            try {
                Log.d(TAG, "准备发送数据到设备B");
                Log.d(TAG, "发送目标IP: " + ip);
                Log.d(TAG, "发送目标端口: " + port);
                Log.d(TAG, "发送的数据: " + jsonData);

                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000);
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                writer.println(jsonData);
                writer.flush();
                socket.close();

                Log.d(TAG, "成功发送数据到设备B: " + jsonData);
            } catch (IOException e) {
                Log.e(TAG, "发送数据到设备B时出错: " + e.getMessage());
            }
        }).start();
    }

    public void setParam() {
        mIat.setParameter(SpeechConstant.PARAMS, null);
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        Log.e(TAG, "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));
        mIat.setParameter(SpeechConstant.VAD_BOS, "5000");
        mIat.setParameter(SpeechConstant.VAD_EOS, "1800");
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        mIat.setParameter("KEY_REQUEST_FOCUS", "true");
    }
    private void speakText(String content, Runnable onComplete) {
        // 这里添加你播放语音的逻辑
        // 播放结束后执行 onComplete.run()
        TextToSpeechUtil ttsUtil = TextToSpeechUtil.getInstance(this);
        ttsUtil.speakText(content);
    }
    private String handleResponseType(JSONObject jsonResponse) {
        StringBuilder contentBuilder = new StringBuilder(); // 用于累积内容
        try {
            if (jsonResponse.has("response")) {
                JSONObject response = jsonResponse.getJSONObject("response");
                if (response.has("events")) {
                    JSONArray events = response.getJSONArray("events");
                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        if (event.has("data") && event.getJSONObject("data").has("content")) {
                            String content = event.getJSONObject("data").getString("content");
                            contentBuilder.append(content).append("\n"); // 添加内容并换行
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace(); // 错误处理
            return ""; // 返回空字符串以指示错误
        }
        return contentBuilder.toString().trim(); // 返回累积的内容
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIatDialog != null) {
            mIatDialog = null;
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        if (ttsUtil != null) {
            ttsUtil.release();
        }
    }
}
