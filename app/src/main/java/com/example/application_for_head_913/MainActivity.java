package com.example.application_for_head_913;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioManager;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private boolean isWaitingForNextInput = false;
    // 声明自定义 Toast
    private Toast wakeUpToast;
    private AlertDialog dialog; // 添加这个声明



    private TextView serverResponseTextView; // 用于显示服务器响应的TextView
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private EditText userInputEditText; // 新增的EditText组件
    private Button sendButton; // 发送按钮
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=b8585b05");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.recordword);
        serverResponseTextView = findViewById(R.id.serverResponseTextView);
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mIatDialog.setListener(mRecognizerDialogListener);
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        client = new OkHttpClient();

        userInputEditText = findViewById(R.id.userInputEditText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {
            String userInput = userInputEditText.getText().toString();
            if (!userInput.isEmpty()) {
                PatrobotData data = new PatrobotData();
                data.setPrompt(userInput);
                sendRequestToServer(data);
            } else {
                Toast.makeText(MainActivity.this, "请输入文本", Toast.LENGTH_SHORT).show();
            }
        });

        checkPermissionAndInitialize();
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

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url("http://222.200.184.74:8082/api/processRequest_origin")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API Failure", "Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response res = response) {
                    if (res.isSuccessful()) {
                        String responseData = res.body().string();
                        Log.d("API Response", responseData);
                        runOnUiThread(() -> serverResponseTextView.setText(responseData));
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
                } catch (IOException e) {
                    Log.e("API Error", "Error closing response: " + e.getMessage());
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
        if (mIat == null) {
            showCustomToast("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        buffer.setLength(0);
        tvResult.setText("");
        mIatResults.clear();
        setParam();
        resultCode = mIat.startListening(mRecognizerListener);
        if (dialogType == 0) {
            showCustomToast("开始听写");
        } else if (dialogType == 1) {
            resultCode = mIat.startListening(mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showCustomToast("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showCustomToast("开始听写");
            }
        } else if (dialogType == 2) {
            showAlertDialog();
            resultCode = mIat.startListening(mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showCustomToast("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showCustomToast("开始听写");
            }
        }
    }

    @Override
    public void onClick(View view) {
    }

    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.e(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showCustomToast("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onBeginOfSpeech() {
            showCustomToast("开始说话");
            Log.d(TAG, "onBeginOfSpeech: 开始语音输入");
            // 确保“再次使用‘你好’唤醒可中断发言”的 Toast 在开始说话时消失
            if (wakeUpToast != null) {
                wakeUpToast.cancel();
            }
            // 延迟显示“再次使用‘你好’唤醒可中断发言”提示
            handler.postDelayed(() -> showCustomWakeUpToast(), 500); // 延迟 500ms 后显示提示
        }

        @Override
        public void onError(SpeechError error) {
            Log.d(TAG, "onError " + error.getPlainDescription(true));
            showCustomToast(error.getPlainDescription(true));
            if (error.getErrorCode() == 20006) {
                showCustomToast("启动录音失败，请检查录音权限和设备设置。");
            }
        }

        @Override
        public void onEndOfSpeech() {
            showCustomToast("结束说话");
            Log.d(TAG, "onEndOfSpeech: 语音输入结束");

            // 结束后关闭 Toast
            if (wakeUpToast != null) {
                wakeUpToast.cancel();
            }
            // 在开始下一次听写前添加一个小间隔
            handler.postDelayed(() -> startListeningWithDelay(), 500); // 延迟500毫秒后重新开始听写
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

    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        public void onError(SpeechError error) {
            showCustomToast(error.getPlainDescription(true));
        }
    };

    // 自定义 Toast 显示方法
    private void showCustomToast(String message) {
        // 加载自定义布局
        View layout = getLayoutInflater().inflate(R.layout.toast_custom_layout, findViewById(android.R.id.content), false);
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        toastMessage.setText(message);

        // 创建并配置 Toast
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150); // 确保 Toast 显示在页面底部
        toast.show();
    }


    private void startListeningWithDelay() {
        handler.postDelayed(() -> startListening(), 100);
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
            if (processedText.contains("你好")) {
                isWaitingForNextInput = true;
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

    private void showAlertDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("自定弹框")
                .setMessage("正在识别，请稍后...")
                .setIcon(R.mipmap.ic_launcher)
                .create();
        dialog.show();
    }
    // 在语音输入开始和结束之间显示自定义 Toast
    // 显示自定义的“再次使用‘你好’唤醒可中断发言” Toast
    private void showCustomWakeUpToast() {
        // 如果已有 Toast 显示，则取消它
        if (wakeUpToast != null) {
            wakeUpToast.cancel();
        }

        // 加载自定义布局
        View layout = getLayoutInflater().inflate(R.layout.toast_custom_layout, null);
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        toastMessage.setText("再次使用‘你好’唤醒可中断发言");

        // 创建并配置 Toast
        wakeUpToast = new Toast(getApplicationContext());
        wakeUpToast.setView(layout);
        wakeUpToast.setDuration(Toast.LENGTH_LONG);
        wakeUpToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150); // 确保 Toast 显示在页面底部
        wakeUpToast.show();
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
    }
}
