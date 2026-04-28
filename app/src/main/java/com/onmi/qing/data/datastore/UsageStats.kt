package com.onmi.qing.data.datastore

// 使用统计数据类
data class UsageStats(
    val chatCount: Int = 0,
    val breathingCount: Int = 0,
    val checkInCount: Int = 0
)
