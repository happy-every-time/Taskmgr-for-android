package com.shizuku.taskmgr

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MinApp"
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private var userService: IUserService? = null
    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "UserService connected")
            userService = IUserService.Stub.asInterface(binder)
            executeCommand("ps")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "UserService disconnected")
            userService = null
        }
    }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Shizuku permission granted")
                    bindUserService()
                } else {
                    Log.e(TAG, "Shizuku permission denied")
                    updateStatus("权限被拒绝")
                }
            }
        }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.e(TAG, "Shizuku binder dead")
        userService = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        if (checkShizukuAvailable()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
        }
    }

    private fun checkShizukuAvailable(): Boolean {
        if (!Shizuku.pingBinder()) {
            Log.e(TAG, "Shizuku 未运行，请先启动 Shizuku")
            updateStatus("Shizuku 未运行")
            return false
        }
        return true
    }

    private fun bindUserService() {
        val args: Shizuku.UserServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(packageName, UserService::class.java.name)
        )
            .processNameSuffix("myservice")
            .daemon(false)
            .version(3)

        userServiceArgs = args  // 保存起来，onDestroy 里要用

        val conn: ServiceConnection = userServiceConnection
        Shizuku.bindUserService(args, conn)
    }

    private fun executeCommand(command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = userService
                if (service == null) {
                    withContext(Dispatchers.Main) {
                        updateStatus("UserService 未连接")
                    }
                    return@launch
                }
                val result = service.execCommand(command)
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "exitCode=${result.exitCode}")
                    Log.d(TAG, "output=${result.output}")
                    Log.d(TAG, "error=${result.error}")
                    updateStatus(
                        "exitCode: ${result.exitCode}\n" +
                        "${result.output}\n" +
                        "error: ${result.error}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "执行命令失败", e)
                withContext(Dispatchers.Main) {
                    updateStatus("执行失败: ${e.message}")
                }
            }
        }
    }

    private fun updateStatus(text: String) {
        findViewById<TextView>(R.id.textView).text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)

        val args = userServiceArgs
        if (args != null) {
            // 注意：真实签名是 unbindUserService(UserServiceArgs, ServiceConnection, boolean)
            // 第三个参数 remove=true 表示同时让 Shizuku 服务端停止并清理 UserService 进程
            Shizuku.unbindUserService(args, userServiceConnection, true)
        }
        userServiceArgs = null
    }
}