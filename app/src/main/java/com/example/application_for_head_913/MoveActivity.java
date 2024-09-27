package com.example.application_for_head_913;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import cn.inbot.padbotbasesdk.RobotControlManager;

public class MoveActivity extends AppCompatActivity {
    private static final String TAG = "MoveActivity"; // 定义 TAG 常量
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        // 前进按钮
        Button btnMoveForward = findViewById(R.id.btnMoveForward);
        btnMoveForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 前进 50cm
                Log.i(TAG, "前进功能开始"); // 开始功能前打印
                RobotControlManager.getInstance().moveSpecifiedDistance(50, 100); // 50厘米，线速度100mm/s
                Log.i(TAG, "前进功能完成"); // 功能开始后打印

            }
        });

        // 后退按钮
        Button btnMoveBackward = findViewById(R.id.btnMoveBackward);
        btnMoveBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 后退 50cm
//                RobotControlManager.getInstance().moveSpecifiedAngle(-180, 30.0f); // 左转90度，角速度30度/秒

                RobotControlManager.getInstance().moveSpecifiedDistance(-50, 100); // 后退50厘米
            }
        });

        // 左转按钮
        Button btnTurnLeft = findViewById(R.id.btnTurnLeft);
        btnTurnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 左转90度
                RobotControlManager.getInstance().moveSpecifiedAngle(-90, 30.0f); // 左转90度，角速度30度/秒
            }
        });

        // 右转按钮
        Button btnTurnRight = findViewById(R.id.btnTurnRight);
        btnTurnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 右转90度
                RobotControlManager.getInstance().moveSpecifiedAngle(90, 30.0f); // 右转90度，角速度30度/秒
            }
        });

        // 右转360度按钮
        Button btnTurnRight360 = findViewById(R.id.btnTurnRight360);
        btnTurnRight360.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 右转360度
                RobotControlManager.getInstance().moveSpecifiedAngle(360, 30.0f); // 右转360度，角速度30度/秒
            }
        });
    }
}
