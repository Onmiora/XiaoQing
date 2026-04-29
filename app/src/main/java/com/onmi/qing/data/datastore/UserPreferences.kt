package com.onmi.qing.data.datastore

// 用户偏好设置数据类
data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val followSystemTheme: Boolean = true,
    val apiUrl: String = "https://api.xiaoqing.com",
    val userName: String = "小明同学",
    val userDescription: String = "正在使用小晴心理健康助手",
    val modelName: String = "glm-4.5-air"
)
