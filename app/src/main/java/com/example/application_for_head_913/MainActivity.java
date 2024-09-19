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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private boolean isWaitingForNextInput = false;

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
    private SpeechRecognizer mIat;// 语音听写对象
    private RecognizerDialog mIatDialog;// 语音听写UI
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    //    private SharedPreferences mSharedPreferences;//缓存
    private String mEngineType = SpeechConstant.TYPE_CLOUD;// 引擎类型
    private String language = "zh_cn"; //识别语言
    private EditText tvResult;//识别结果
    private Button btnStart;//开始识别
    //拼接字符串
    private StringBuffer buffer = new StringBuffer();
    private String resultType = "json";//结果内容数据格式
    //    private boolean cyclic = false;//音频流识别是否循环调用
    private Toast mToast;
    // 弹框是否显示
    private int dialogType;
    // 函数调用返回值
    private int resultCode = 0;
    //Handler码
    private int handlerCode = 0x123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=b8585b05");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.recordword);
        // 在 onCreate 方法中，初始化 TextView
        serverResponseTextView = findViewById(R.id.serverResponseTextView);
        // 初始化 RecognizerDialog
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mIatDialog.setListener(mRecognizerDialogListener);
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        client = new OkHttpClient();

        userInputEditText = findViewById(R.id.userInputEditText); // 实例化EditText
        sendButton = findViewById(R.id.sendButton); // 实例化发送按钮

        sendButton.setOnClickListener(v -> {
            String userInput = userInputEditText.getText().toString(); // 获取用户输入的文本
            if (!userInput.isEmpty()) {
                PatrobotData data = new PatrobotData();
                data.setPrompt(userInput); // 使用用户输入的文本作为请求数据
                sendRequestToServer(data); // 发送请求
            } else {
                // 提示用户输入文本
                Toast.makeText(MainActivity.this, "请输入文本", Toast.LENGTH_SHORT).show();
            }
        });

        // **先检查权限，然后初始化语音识别组件**
        checkPermissionAndInitialize();
    }

    private void checkPermissionAndInitialize() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予，继续初始化
            initializeSpeechRecognizer();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限授予后，初始化语音识别组件
            initializeSpeechRecognizer();
        } else {
            // 权限被拒绝，关闭应用或提示用户
            Toast.makeText(this, "需要录音权限才能使用语音识别功能", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void initializeSpeechRecognizer() {
        // 初始化 RecognizerDialog
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mIatDialog.setListener(mRecognizerDialogListener);

        // 初始化语音识别器
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        // 开始监听
        startListening();
    }


    private void sendRequestToServer(PatrobotData patrobotData) {
        Gson gson = new Gson();

        // 创建一个新的对象来匹配所需的结构
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sessionId", patrobotData.getSessionId());
        jsonObject.addProperty("newTopic", false); // 布尔值
        jsonObject.addProperty("deviceId", patrobotData.getDeviceId());
        jsonObject.addProperty("deviceModel", patrobotData.getDeviceModel());
        jsonObject.addProperty("city", patrobotData.getCity());
        jsonObject.addProperty("district", patrobotData.getDistrict());
        jsonObject.addProperty("lang", patrobotData.getLang());

        // 创建嵌套的 message 对象
        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("prompt", patrobotData.getPrompt());

        jsonObject.add("message", messageObject); // 添加 message 对象

        String json = gson.toJson(jsonObject);

        // 打印发送的 JSON 格式
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
                // 确保在处理响应后关闭响应体
                try (Response res = response) {
                    if (res.isSuccessful()) {
                        String responseData = res.body().string();
                        Log.d("API Response", responseData);
                        runOnUiThread(() -> {
                            // 将服务器返回的结果显示在屏幕上
                            serverResponseTextView.setText(responseData);
                        });
                        new Thread(() -> {
                            try {
                                Socket socket = new Socket();
                                socket.connect(new InetSocketAddress("192.168.11.236", 5000), 5000);  // 设置超时时间为 5 秒
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
                }}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 检查 mIat 是否已初始化
        if (mIat != null) {
            // 在页面重新回到前台时，重新启动语音识别
            startListening();
        }
    }


    private void startListening(){
        if (mIat == null) {
            showToast("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        buffer.setLength(0);//长度清空
        tvResult.setText("");// 也可以显示内容"开始聆听……"
        mIatResults.clear();//清除存贮结果
        // 设置参数
        setParam();
        // 开始语音监听
        resultCode = mIat.startListening(mRecognizerListener); // 不显示听写对话框(显示的还可注释掉));
        if (dialogType == 0) {
                    /*// 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();*/
            showToast("开始听写");
        } else if (dialogType == 1) {
            // 不显示听写对话框
            resultCode = mIat.startListening((com.iflytek.cloud.RecognizerListener) mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showToast("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showToast("开始听写");
            }
        } else if (dialogType == 2) {
            // 自定义听写对话框
            showAlertDialog();
            resultCode = mIat.startListening((com.iflytek.cloud.RecognizerListener) mRecognizerListener);
            if (resultCode != ErrorCode.SUCCESS) {
                showToast("听写失败,错误码：" + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                showToast("开始听写");
            }
        }

    }
    @Override
    public void onClick(View view) {

    }
    // 初始化监听器
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.e(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showToast("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
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
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
            Log.d(TAG, "onEndOfSpeech: 语音输入结束");
            //player.play();
            // 重新开始听写
            startListeningWithDelay(); // 这里调用 startListening() 方法以继续识别
        }

        ExecutorService executorService = Executors.newCachedThreadPool();

        @Override
        public void onResult(com.iflytek.cloud.RecognizerResult results, boolean isLast) {
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
               // showTip("当前音量较小，音量大小 = " + volume + "请大声点"); // 忒敏感了，老是弹出来，可注释掉
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == handlerCode) {
//                executeStream();
            }
        }
    };

    // 听写UI监听器
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        // 返回结果
        public void onResult(com.iflytek.cloud.RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        // 识别回调错误
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };

    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }
    private void startListeningWithDelay() {
        handler.postDelayed(() -> {
            startListening();
        }, 100); // 延迟1秒重新启动监听
    }


    // 打印听写结果
    private void printResult(RecognizerResult results) {
        String text = "";
        String sn = null;
        // 读取json结果中的sn字段
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
                            // 取出cw数组中的第一个对象的w字段
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
            // 处理英文字母为小写
            String processedText = text.toLowerCase();
            Log.d(TAG, "Processed Text (all lowercase): " + processedText);
            if (processedText.contains("你好")) {
                isWaitingForNextInput = true;
                Log.d(TAG, "printResult: 进入等待下一句输入状态");
            } else if (isWaitingForNextInput) {
                if (!processedText.isEmpty()) {
                    // 如果输入不为空，发送请求到服务器
                    PatrobotData voicedata = new PatrobotData();
                    voicedata.setPrompt(processedText); // 将处理后的文本传递给服务器
                    sendRequestToServer(voicedata);
                    resetInputState();  // 重置输入状态
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
            // 更新 UI，显示识别结果
            tvResult.setText(resultBuffer.toString());
            tvResult.setSelection(tvResult.length()); //将光标设置到文本末尾
            Log.d(TAG, "printResult: 更新 UI，显示结果：" + resultBuffer.toString());
        });
    }

    private void resetInputState(){
        isWaitingForNextInput=false;//重置输入状态
        Log.d(TAG, "resetInputState: 重置输入状态，isWaitingForNextInput = " + isWaitingForNextInput);
    }

    // 听写参数设置
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎类型
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式【目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容】
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        //目前Android SDK支持zh_cn：中文、en_us：英文、ja_jp：日语、ko_kr：韩语、ru-ru：俄语、fr_fr：法语、es_es：西班牙语、
        // 注：小语种若未授权无法使用会报错11200，可到控制台-语音听写（流式版）-方言/语种处添加试用或购买。
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        // 设置语言区域、当前仅在LANGUAGE为简体中文时，支持方言选择，其他语言区域时，可把此参数值设为mandarin。
        // 默认值：mandarin，其他方言参数可在控制台方言一栏查看。
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        //获取当前语言（同理set对应get方法）
        Log.e(TAG, "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");
        //开始录入音频后，音频后面部分最长静音时长，取值范围[0,10000ms]，默认值5000ms
        mIat.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音取值范围[0,10000ms]，默认值1800ms。
        mIat.setParameter(SpeechConstant.VAD_EOS, "1800");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        // 识别出语音时，暂停设备声音
        mIat.setParameter("KEY_REQUEST_FOCUS", "true");
    }

    private void showToast(final String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private AlertDialog dialog;

    private void showAlertDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("自定弹框")//标题
                .setMessage("正在识别，请稍后...")//内容
                .setIcon(R.mipmap.ic_launcher)//图标
                .create();
        dialog.show();
    }

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
